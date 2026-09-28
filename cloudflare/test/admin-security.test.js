import { after, test } from 'node:test';
import assert from 'node:assert/strict';
import { exportJWK, generateKeyPair, SignJWT } from 'jose';
import worker from '../src/index.js';

// Ephemeral keys, a fake JWKS server and a DB spy: no production calls or secrets.
const issuer = 'https://test-team.cloudflareaccess.com';
const origin = 'https://admin.example.test';
const audience = 'test-audience-not-a-production-id';
const { privateKey, publicKey } = await generateKeyPair('RS256');
const jwk = { ...await exportJWK(publicKey), kid: 'test-key', alg: 'RS256', use: 'sig' };
const realFetch = globalThis.fetch;
let jwksRequests = 0;
globalThis.fetch = async url => {
  assert.equal(String(url), `${issuer}/cdn-cgi/access/certs`, 'No unexpected external requests');
  jwksRequests++;
  return Response.json({ keys: [jwk] });
};
after(() => { globalThis.fetch = realFetch; });
const now = () => Math.floor(Date.now() / 1000);
async function token(overrides = {}, key = privateKey, alg = 'RS256') {
  return new SignJWT({ iss: issuer, aud: [audience], sub: 'admin-one', email: 'admin@example.test',
    type: 'app', iat: now(), exp: now() + 3600, ...overrides })
    .setProtectedHeader({ alg, kid: 'test-key' }).sign(key);
}
function environment(overrides = {}) {
  const calls = { db: 0, keys: [], batches: [] };
  return {
    calls,
    env: {
      ADMIN_ORIGIN: origin, ACCESS_TEAM_DOMAIN: issuer, ACCESS_AUD: audience,
      ADMIN_RATE_LIMITER: { async limit({ key }) { calls.keys.push(key); return { success: true }; } },
      DB: { prepare() { calls.db++; throw new Error('Unexpected database access'); } },
      ...overrides,
    },
  };
}
async function request(path, { jwt, method = 'GET', headers = {}, body, host = origin } = {}, env = environment().env) {
  return worker.fetch(new Request(host + path, { method, body, headers: {
    ...(jwt ? { 'Cf-Access-Jwt-Assertion': jwt } : {}),
    ...(path.startsWith('/v1/admin') ? { 'X-Yamone-Admin': '1' } : {}),
    ...headers,
  } }), env);
}
function privateResponse(res, status) {
  assert.equal(res.status, status);
  assert.equal(res.headers.get('Cache-Control'), 'no-store');
  assert.equal(res.headers.get('X-Frame-Options'), 'DENY');
  assert.equal(res.headers.get('X-Content-Type-Options'), 'nosniff');
  assert.equal(res.headers.get('Referrer-Policy'), 'no-referrer');
  assert.equal([...res.headers.keys()].some(k => k.startsWith('access-control-')), false);
}

for (const path of ['/admin', '/admin/', '/admin/child', '/v1/admin', '/v1/admin/', '/v1/admin/session',
  '/v1/admin/stats', '/v1/admin/catalog', '/v1/admin/games', '/v1/admin/games/visibility',
  '/v1/admin/apps/order', '/v1/admin/apps/settings', '/v1/admin/rankings/reset']) {
  test(`unauthenticated and legacy bearer requests cannot reach ${path}`, async () => {
    const { env, calls } = environment({ RANKING_ADMIN_SECRET: 'test-only-retired-key' });
    for (const method of ['GET', 'HEAD', 'OPTIONS', 'POST', 'PATCH', 'DELETE']) {
      privateResponse(await request(path, { method, headers: { Authorization: 'Bearer test-only-retired-key' } }, env), 401);
    }
    assert.equal(calls.db, 0);
    assert.deepEqual(calls.keys, []);
  });
}

test('missing/invalid Access config and limiter fail closed without touching DB', async () => {
  const jwt = await token();
  for (const overrides of [{ ADMIN_ORIGIN: '' }, { ADMIN_ORIGIN: origin + '/' },
    { ADMIN_ORIGIN: 'http://admin.example.test' }, { ACCESS_TEAM_DOMAIN: '' },
    { ACCESS_TEAM_DOMAIN: 'https://attacker.example/certs' }, { ACCESS_AUD: '' },
    { ADMIN_RATE_LIMITER: undefined }]) {
    const { env, calls } = environment(overrides);
    privateResponse(await request('/v1/admin/session', { jwt }, env), 503);
    assert.equal(calls.db, 0);
  }
});

test('workers.dev, preview, HTTP and alternate hosts reject even a valid JWT', async () => {
  const jwt = await token();
  for (const host of ['https://yamone-games-ranking-api.yamone-game.workers.dev',
    'https://preview.example.test', 'https://other.example.test', 'http://admin.example.test']) {
    const { env, calls } = environment();
    privateResponse(await request('/admin', { jwt, host }, env), 404);
    privateResponse(await request('/v1/admin/session', { jwt, host }, env), 404);
    assert.equal(calls.db, 0);
  }
});

for (const [name, claims] of Object.entries({
  'wrong issuer': { iss: 'https://other.cloudflareaccess.com' },
  'wrong audience': { aud: ['other'] }, expired: { iat: now() - 3700, exp: now() - 100 },
  'future nbf': { nbf: now() + 120 }, 'future iat': { iat: now() + 120 },
  'missing expiry': { exp: undefined }, 'missing issued-at': { iat: undefined },
  'missing subject': { sub: undefined }, 'empty subject': { sub: '' },
  'service identity': { email: undefined }, 'wrong token type': { type: 'org' },
  '24-hour session': { exp: now() + 86400 }, 'reversed lifetime': { iat: now(), exp: now() - 1 },
})) {
  test(`reject ${name}`, async () => {
    const { env, calls } = environment();
    privateResponse(await request('/v1/admin/session', { jwt: await token(claims) }, env), 401);
    assert.equal(calls.db, 0);
    assert.equal(calls.keys.length, 0);
  });
}

test('reject malformed, oversized, unsigned, wrong-algorithm and forged JWTs', async () => {
  const other = await generateKeyPair('RS256');
  const candidates = ['garbage', 'x'.repeat(16385), 'eyJhbGciOiJub25lIn0.eyJzdWIiOiJhZG1pbiJ9.',
    await token({}, other.privateKey), await token({}, new Uint8Array(32), 'HS256')];
  for (const jwt of candidates) privateResponse(await request('/v1/admin/session', { jwt }), 401);
});

test('cookie or identity headers alone are never trusted', async () => {
  const jwt = await token();
  privateResponse(await request('/v1/admin/session', { headers: {
    Cookie: `CF_Authorization=${jwt}`, 'Cf-Access-Authenticated-User-Email': 'admin@example.test',
  } }), 401);
});

test('unknown kid and unavailable JWKS fail closed', async () => {
  const unknown = await new SignJWT({ iss: issuer, aud: audience, sub: 'admin', email: 'admin@example.test',
    type: 'app', iat: now(), exp: now() + 3600 }).setProtectedHeader({ alg: 'RS256', kid: 'unknown' }).sign(privateKey);
  privateResponse(await request('/v1/admin/session', { jwt: unknown }), 401);
  const unavailableIssuer = 'https://unavailable.cloudflareaccess.com';
  const { env, calls } = environment({ ACCESS_TEAM_DOMAIN: unavailableIssuer });
  const savedFetch = globalThis.fetch;
  globalThis.fetch = async url => {
    assert.equal(String(url), `${unavailableIssuer}/cdn-cgi/access/certs`);
    return new Response('Unavailable', { status: 503 });
  };
  try {
    privateResponse(await request('/v1/admin/session', { jwt: await token({ iss: unavailableIssuer }) }, env), 401);
    assert.equal(calls.db, 0);
  } finally { globalThis.fetch = savedFetch; }
});

test('valid JWT grants session; JWKS is cached and rate key uses verified sub', async () => {
  const { env, calls } = environment();
  const jwt = await token();
  const before = jwksRequests;
  for (let i = 0; i < 2; i++) {
    const res = await request('/v1/admin/session', { jwt, headers: {
      'Cf-Access-Authenticated-User-Email': 'spoofed@example.test', 'X-Forwarded-For': '1.2.3.4',
    } }, env);
    privateResponse(res, 200);
    assert.deepEqual(await res.json(), { ok: true });
  }
  assert.ok(jwksRequests - before <= 1);
  assert.deepEqual(calls.keys, Array(2).fill(`yamone-admin:${audience}:admin-one`));
  assert.equal(calls.db, 0);
});

test('admin HTML is authenticated, non-cacheable and has a fresh matching CSP nonce', async () => {
  const jwt = await token();
  const nonces = [];
  for (let i = 0; i < 2; i++) {
    const res = await request('/admin/', { jwt });
    privateResponse(res, 200);
    const html = await res.text();
    const nonce = html.match(/<script nonce="([a-f0-9]+)">/)[1];
    nonces.push(nonce);
    assert.ok(res.headers.get('Content-Security-Policy').includes(`script-src 'nonce-${nonce}'`));
    assert.ok(res.headers.get('Content-Security-Policy').includes("frame-ancestors 'none'"));
    assert.doesNotMatch(html, /\bonclick=|\bonkeydown=|sessionStorage\.setItem|localStorage\.setItem|Bearer |id="token"/);
    new Function(html.match(/<script[^>]*>([\s\S]*?)<\/script>/)[1]);
  }
  assert.notEqual(...nonces);
});

test('CSRF blocks cross-origin and missing custom headers before mutations', async () => {
  const jwt = await token();
  const { env, calls } = environment();
  for (const headers of [{}, { Origin: 'null' }, { Origin: 'https://evil.example' },
    { Origin: origin, 'X-Yamone-Admin': '' }, { Origin: origin, 'Sec-Fetch-Site': 'same-site' },
    { Origin: origin, 'Sec-Fetch-Site': 'cross-site' }]) {
    privateResponse(await request('/v1/admin/rankings/reset', { jwt, method: 'POST', headers, body: '{}' }, env), 403);
  }
  privateResponse(await request('/v1/admin/session', { jwt, headers: { 'X-Yamone-Admin': '' } }, env), 403);
  privateResponse(await request('/v1/admin/session', { jwt, headers: { Origin: 'https://evil.example' } }, env), 403);
  privateResponse(await request('/v1/admin/session', { jwt, method: 'OPTIONS', headers: { Origin: origin } }, env), 404);
  assert.equal(calls.db, 0);
});

test('limiter rejection/errors have private headers and never touch DB', async () => {
  const jwt = await token();
  for (const [status, limit] of [
    [429, async () => ({ success: false })], [503, async () => { throw new Error('binding down'); }],
    [503, async () => undefined],
  ]) {
    const { env, calls } = environment({ ADMIN_RATE_LIMITER: { limit } });
    const res = await request('/v1/admin/stats', { jwt }, env);
    privateResponse(res, status);
    if (status === 429) assert.equal(res.headers.get('Retry-After'), '60');
    assert.equal(calls.db, 0);
  }
});

test('valid mutation reaches handler; async validation and DB errors stay private', async () => {
  const jwt = await token();
  const { env, calls } = environment();
  const options = { jwt, method: 'POST', headers: { Origin: origin, 'Content-Type': 'application/json' } };
  const invalid = await request('/v1/admin/games', { ...options, body: '{' }, env);
  privateResponse(invalid, 400);
  assert.deepEqual(await invalid.json(), { error: 'INVALID_JSON' });
  const databaseError = await request('/v1/admin/stats', { jwt }, env);
  privateResponse(databaseError, 500);
  assert.deepEqual(await databaseError.json(), { error: 'INTERNAL_ERROR' });
  assert.equal(calls.db, 1);
});

test('authorized catalog setting writes a batch and audit; no schema changes', async () => {
  const batches = [];
  const { env } = environment({ DB: {
    prepare(sql) { return { bind(...args) { return { sql, args }; } }; },
    async batch(statements) { batches.push(statements); },
  } });
  const res = await request('/v1/admin/apps/settings', {
    jwt: await token(), method: 'PATCH', headers: { Origin: origin },
    body: JSON.stringify({ appId: 'yamone_arcade2', sortMode: 'manual' }),
  }, env);
  privateResponse(res, 200);
  assert.equal(batches.length, 1);
  assert.match(batches[0][1].sql, /admin_audit_log/);
});

test('public health/catalog/ranking and preflight work with admin disabled', async () => {
  const game = { game_id: 'orbit_snap', mode_id: 'normal', score_unit: 'points', status: 'active', ranking_epoch: 1 };
  const env = { DB: { prepare(sql) {
    return { bind() { return this; }, async first() {
      if (sql.includes('FROM game_catalog')) return game;
      if (sql.includes('COUNT(*)')) return { count: 0 };
      return null;
    }, async all() { return { results: sql.includes('ORDER BY game_id, mode_id') ? [game] : [] }; } };
  } } };
  for (const path of ['/health', '/v1/catalog?appId=yamone_arcade2', '/v1/ranking/orbit_snap/normal']) {
    const res = await request(path, {}, env);
    assert.equal(res.status, 200);
    assert.equal(res.headers.get('Access-Control-Allow-Origin'), '*');
    assert.equal((await res.json()).ok, true);
  }
  const res = await request('/v1/ranking/submit', { method: 'OPTIONS' }, env);
  assert.equal(res.status, 204);
  assert.equal(res.headers.get('Access-Control-Allow-Origin'), '*');
});
