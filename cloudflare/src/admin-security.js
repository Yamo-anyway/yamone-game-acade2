import { createRemoteJWKSet, jwtVerify } from "jose";

let cachedIssuer;
let cachedKeys;

export class AdminSecurityError extends Error {
  constructor(status, code) {
    super(code);
    this.status = status;
    this.code = code;
  }
}

export function isAdminPath(path) {
  return path === "/admin" || path.startsWith("/admin/") ||
    path === "/v1/admin" || path.startsWith("/v1/admin/");
}

export async function authorizeAdmin(request, env) {
  const origin = env.ADMIN_ORIGIN;
  const issuer = env.ACCESS_TEAM_DOMAIN;
  const audience = env.ACCESS_AUD;
  // No legacy bearer fallback, including when Access is incompletely configured.
  let validOrigin = false;
  try {
    const url = new URL(origin);
    validOrigin = url.protocol === "https:" && url.origin === origin;
  } catch { /* Fail closed below. */ }
  if (!validOrigin || !/^https:\/\/[a-z0-9-]+\.cloudflareaccess\.com$/.test(issuer || "") ||
      typeof audience !== "string" || !audience.trim() ||
      typeof env.ADMIN_RATE_LIMITER?.limit !== "function") {
    throw new AdminSecurityError(503, "ADMIN_NOT_CONFIGURED");
  }
  // workers.dev, preview URLs and other routes must not bypass the Access edge.
  if (new URL(request.url).origin !== origin) throw new AdminSecurityError(404, "NOT_FOUND");

  const token = request.headers.get("Cf-Access-Jwt-Assertion");
  if (!token || token.length > 16384) throw new AdminSecurityError(401, "ADMIN_UNAUTHORIZED");
  let payload;
  try {
    if (cachedIssuer !== issuer) {
      cachedKeys = createRemoteJWKSet(new URL(`${issuer}/cdn-cgi/access/certs`), {
        timeoutDuration: 3000, cooldownDuration: 30000, cacheMaxAge: 600000,
      });
      cachedIssuer = issuer;
    }
    ({ payload } = await jwtVerify(token, cachedKeys, {
      issuer, audience, algorithms: ["RS256"],
      requiredClaims: ["exp", "iat", "sub", "email"],
      maxTokenAge: "1h", clockTolerance: 5,
    }));
    // This dashboard is for human administrators, not Access service tokens.
    if (payload.type !== "app" || typeof payload.sub !== "string" || !payload.sub ||
        typeof payload.email !== "string" || !payload.email ||
        payload.exp <= payload.iat || payload.exp - payload.iat > 3600) throw new Error("claims");
  } catch {
    // Never return/log JWTs, cookies or verification internals.
    throw new AdminSecurityError(401, "ADMIN_UNAUTHORIZED");
  }

  const suppliedOrigin = request.headers.get("Origin");
  const fetchSite = request.headers.get("Sec-Fetch-Site");
  if ((suppliedOrigin !== null && suppliedOrigin !== origin) ||
      (fetchSite && !["same-origin", "none"].includes(fetchSite))) {
    throw new AdminSecurityError(403, "ADMIN_ORIGIN_REJECTED");
  }
  if (new URL(request.url).pathname.startsWith("/v1/admin")) {
    // A custom header prevents form/ambient-cookie CSRF; writes also need Origin.
    if (request.headers.get("X-Yamone-Admin") !== "1" ||
        (!["GET", "HEAD"].includes(request.method) && suppliedOrigin !== origin)) {
      throw new AdminSecurityError(403, "ADMIN_CSRF_REJECTED");
    }
  }

  let result;
  try {
    // Only verified identity is a key; never caller-supplied email/IP/Authorization.
    result = await env.ADMIN_RATE_LIMITER.limit({ key: `yamone-admin:${audience}:${payload.sub}` });
  } catch {
    throw new AdminSecurityError(503, "ADMIN_RATE_LIMIT_UNAVAILABLE");
  }
  if (result?.success === false) throw new AdminSecurityError(429, "ADMIN_RATE_LIMITED");
  if (result?.success !== true) throw new AdminSecurityError(503, "ADMIN_RATE_LIMIT_UNAVAILABLE");
}

export function secureAdminResponse(response) {
  const headers = new Headers(response.headers);
  for (const name of [...headers.keys()]) {
    if (name.startsWith("access-control-")) headers.delete(name);
  }
  headers.set("Cache-Control", "no-store");
  headers.set("Referrer-Policy", "no-referrer");
  headers.set("X-Content-Type-Options", "nosniff");
  headers.set("X-Frame-Options", "DENY");
  headers.set("Vary", "Cookie, Cf-Access-Jwt-Assertion, Origin");
  if (response.status === 429) headers.set("Retry-After", "60");
  return new Response(response.body, { status: response.status, headers });
}
