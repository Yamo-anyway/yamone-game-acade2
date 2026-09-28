import { ADMIN_HTML } from "./admin-page.js";
import { AdminSecurityError, authorizeAdmin, isAdminPath, secureAdminResponse } from "./admin-security.js";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, PATCH, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
};
const JSON_HEADERS = { ...CORS_HEADERS, "Content-Type": "application/json; charset=utf-8" };
const APP_ID_PATTERN = /^[a-z0-9][a-z0-9_-]{1,63}$/;
const KEY_PATTERN = /^[a-z0-9][a-z0-9_]{1,63}$/;
const PLAYER_PATTERN = /^[A-Za-z0-9_-]{8,128}$/;
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, "") || "/";
    // Admin dispatch must precede public CORS preflight and database handling.
    if (isAdminPath(path)) return handleAdmin(request, env, url, path);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: CORS_HEADERS });
    if (!env.DB) return json({ error: "DATABASE_NOT_CONFIGURED" }, 500);

    try {
      if (request.method === "GET" && path === "/health") return health(env);
      if (request.method === "GET" && path === "/v1/catalog") return publicCatalog(env, url);
      if (request.method === "POST" && path === "/v1/plays/event") return recordPlayEvent(request, env);
      if (request.method === "POST" && path === "/v1/ranking/submit") return submitRanking(request, env);
      if (request.method === "DELETE" && path === "/v1/ranking/player") return deletePlayer(request, env);

      const rankingMatch = path.match(/^\/v1\/ranking\/([a-z0-9_]+)\/([a-z0-9_]+)$/);
      if (request.method === "GET" && rankingMatch) {
        return readRanking(env, rankingMatch[1], rankingMatch[2], url.searchParams.get("playerId"));
      }

      return json({ error: "NOT_FOUND" }, 404);
    } catch (error) {
      if (error instanceof HttpError) return json({ error: error.code, ...error.details }, error.status);
      console.error(error);
      return json({ error: "INTERNAL_ERROR" }, 500);
    }
  },
};

async function handleAdmin(request, env, url, path) {
  try {
    await authorizeAdmin(request, env);
    if (!env.DB) throw new HttpError(503, "DATABASE_NOT_CONFIGURED");
    let response;
    if (request.method === "GET" && path === "/admin") {
      const nonce = crypto.randomUUID().replaceAll("-", "");
      response = new Response(ADMIN_HTML.replace("__ADMIN_SCRIPT_NONCE__", nonce), {
        headers: {
          "Content-Type": "text/html; charset=utf-8",
          "Content-Security-Policy": `default-src 'none'; style-src 'unsafe-inline'; script-src 'nonce-${nonce}'; connect-src 'self'; base-uri 'none'; object-src 'none'; frame-ancestors 'none'; form-action 'none'`,
        },
      });
    } else if (request.method === "GET" && path === "/v1/admin/session") response = json({ ok: true });
    else if (request.method === "GET" && path === "/v1/admin/stats") response = await adminStats(env, url);
    else if (request.method === "GET" && path === "/v1/admin/catalog") response = await adminCatalog(env, url);
    else if (request.method === "POST" && path === "/v1/admin/games") response = await adminSaveGame(request, env);
    else if (request.method === "PATCH" && path === "/v1/admin/games/visibility") response = await adminVisibility(request, env);
    else if (request.method === "PATCH" && path === "/v1/admin/apps/order") response = await adminOrder(request, env);
    else if (request.method === "PATCH" && path === "/v1/admin/apps/settings") response = await adminSettings(request, env);
    else if (request.method === "POST" && path === "/v1/admin/rankings/reset") response = await adminResetRanking(request, env);
    else response = json({ error: "NOT_FOUND" }, 404);
    return secureAdminResponse(response);
  } catch (error) {
    // Awaited handlers keep validation/DB errors inside the admin header boundary.
    if (error instanceof HttpError || error instanceof AdminSecurityError) {
      return secureAdminResponse(json({ error: error.code, ...error.details }, error.status));
    }
    return secureAdminResponse(json({ error: "INTERNAL_ERROR" }, 500));
  }
}

async function health(env) {
  const rows = await env.DB.prepare(
    "SELECT game_id, mode_id, score_unit, ranking_epoch FROM game_catalog WHERE status != 'retired' ORDER BY game_id, mode_id"
  ).all();
  const modes = rows.results || [];
  const games = {};
  for (const row of modes) games[row.game_id] = row.score_unit;
  return json({ ok: true, service: "yamone-games-ranking-api", version: "2.0", games, modes });
}

async function publicCatalog(env, url) {
  const appId = validAppId(url.searchParams.get("appId") || "");
  const catalog = await loadCatalog(env, appId, false);
  return json({ ok: true, appId, sortMode: catalog.sortMode, games: catalog.games });
}

async function submitRanking(request, env) {
  const body = await requestJson(request);
  const input = await validateScoreInput(env, body);
  validateEpoch(input.game, body.rankingEpoch);
  const playerHash = await hashPlayerId(env, body.playerId);
  const update = await upsertLeaderboard(env, playerHash, input);
  return json({
    ok: true,
    updated: update.updated,
    bestScore: Math.max(input.score, update.previousBest || 0),
    rankingEpoch: input.game.ranking_epoch,
    localResetEpoch: input.game.local_reset_epoch,
  });
}

async function recordPlayEvent(request, env) {
  const body = await requestJson(request);
  const eventType = body.eventType === "started" || body.eventType === "finished" ? body.eventType : null;
  if (!eventType) throw new HttpError(400, "INVALID_EVENT_TYPE");
  const receiptId = String(body.playId || "").trim();
  if (!UUID_PATTERN.test(receiptId)) throw new HttpError(400, "INVALID_PLAY_ID");
  const appId = validAppId(body.appId || "");
  const playerId = validPlayerId(body.playerId);
  const playerHash = await hashPlayerId(env, playerId);
  const gameId = validKey(body.gameId, "INVALID_GAME");
  const modeId = validKey(body.modeId || "normal", "INVALID_MODE");
  const game = await getGame(env, gameId, modeId);
  if (!game || game.status === "retired") throw new HttpError(400, "INVALID_GAME_MODE");
  const countryCode = normalizeCountry(body.countryCode);
  const submittedEpoch = Number(body.rankingEpoch);
  if (!Number.isInteger(submittedEpoch) || submittedEpoch < 1) throw new HttpError(400, "INVALID_RANKING_EPOCH");
  const rankingAccepted = submittedEpoch === Number(game.ranking_epoch);
  const now = Math.floor(Date.now() / 1000);

  const inserted = await env.DB.prepare(
    `INSERT OR IGNORE INTO play_receipts
      (app_id, receipt_id, player_id, game_id, mode_id, country_code, ranking_epoch, started_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
     RETURNING receipt_id`
  ).bind(appId, receiptId, playerHash, gameId, modeId, countryCode, submittedEpoch, now, now).first();

  if (inserted) {
    await env.DB.prepare(
      `INSERT INTO player_game_stats
        (player_id, app_id, game_id, mode_id, country_code, play_count, completed_count, first_played_at, last_played_at)
       VALUES (?, ?, ?, ?, ?, 1, 0, ?, ?)
       ON CONFLICT(player_id, app_id, game_id, mode_id) DO UPDATE SET
         country_code = excluded.country_code,
         play_count = player_game_stats.play_count + 1,
         last_played_at = excluded.last_played_at`
    ).bind(playerHash, appId, gameId, modeId, countryCode, now, now).run();
  } else if (countryCode) {
    await env.DB.prepare(
      "UPDATE play_receipts SET country_code = ?, updated_at = ? WHERE app_id = ? AND receipt_id = ?"
    ).bind(countryCode, now, appId, receiptId).run();
  }

  let completedAdded = false;
  let rankingUpdated = false;
  if (eventType === "finished") {
    const score = validScore(body.score, game.max_score);
    const finish = await env.DB.prepare(
      `UPDATE play_receipts SET finished_at = ?, score = ?, country_code = ?, updated_at = ?
       WHERE app_id = ? AND receipt_id = ? AND finished_at IS NULL`
    ).bind(now, score, countryCode, now, appId, receiptId).run();
    completedAdded = Number(finish.meta?.changes || 0) > 0;
    if (completedAdded) {
      await env.DB.prepare(
        `UPDATE player_game_stats SET completed_count = completed_count + 1,
          country_code = ?, last_played_at = ?
         WHERE player_id = ? AND app_id = ? AND game_id = ? AND mode_id = ?`
      ).bind(countryCode, now, playerHash, appId, gameId, modeId).run();
    }
    validateScoreUnit(body.scoreUnit, game.score_unit);
    if (score > 0 && rankingAccepted) {
      const input = {
        playerId,
        nickname: normalizeNickname(body.nickname),
        countryCode,
        gameId,
        modeId,
        score,
        game,
      };
      rankingUpdated = (await upsertLeaderboard(env, playerHash, input)).updated;
    }
  }

  return json({
    ok: true,
    accepted: Boolean(inserted),
    duplicate: !inserted,
    completedAdded,
    rankingUpdated,
    rankingAccepted,
    rankingEpoch: game.ranking_epoch,
    localResetEpoch: game.local_reset_epoch,
  });
}

async function readRanking(env, gameId, modeId, rawPlayerId) {
  const game = await getGame(env, validKey(gameId, "INVALID_GAME"), validKey(modeId, "INVALID_MODE"));
  if (!game || game.status === "retired") throw new HttpError(400, "INVALID_GAME_MODE");
  const playerHash = rawPlayerId ? await hashPlayerId(env, validPlayerId(rawPlayerId)) : null;
  const totalRow = await env.DB.prepare(
    "SELECT COUNT(*) AS count FROM leaderboard WHERE game_id = ? AND mode_id = ?"
  ).bind(gameId, modeId).first();
  const topRows = await env.DB.prepare(
    `SELECT player_id, nickname, country_code, best_score,
      ROW_NUMBER() OVER (ORDER BY best_score DESC, achieved_at ASC, player_id ASC) AS rank
     FROM leaderboard WHERE game_id = ? AND mode_id = ?
     ORDER BY best_score DESC, achieved_at ASC, player_id ASC LIMIT 100`
  ).bind(gameId, modeId).all();

  let me = null;
  let nearby = [];
  if (playerHash) {
    me = await env.DB.prepare(
      `WITH ranked AS (
        SELECT player_id, nickname, country_code, best_score,
          ROW_NUMBER() OVER (ORDER BY best_score DESC, achieved_at ASC, player_id ASC) AS rank
        FROM leaderboard WHERE game_id = ? AND mode_id = ?
      ) SELECT * FROM ranked WHERE player_id = ?`
    ).bind(gameId, modeId, playerHash).first();
    if (me) {
      const around = await env.DB.prepare(
        `WITH ranked AS (
          SELECT player_id, nickname, country_code, best_score,
            ROW_NUMBER() OVER (ORDER BY best_score DESC, achieved_at ASC, player_id ASC) AS rank
          FROM leaderboard WHERE game_id = ? AND mode_id = ?
        ) SELECT * FROM ranked WHERE rank BETWEEN ? AND ? ORDER BY rank`
      ).bind(gameId, modeId, Math.max(1, Number(me.rank) - 2), Number(me.rank) + 2).all();
      nearby = around.results || [];
    }
  }

  return json({
    ok: true,
    gameId,
    modeId,
    scoreUnit: game.score_unit,
    rankingEpoch: game.ranking_epoch,
    localResetEpoch: game.local_reset_epoch,
    totalPlayers: Number(totalRow?.count || 0),
    top: (topRows.results || []).map(row => rankingRow(row, playerHash)),
    me: me ? rankingRow(me, playerHash) : null,
    nearby: nearby.map(row => rankingRow(row, playerHash)),
  });
}

async function deletePlayer(request, env) {
  const body = await requestJson(request);
  const playerHash = await hashPlayerId(env, validPlayerId(body.playerId));
  await env.DB.batch([
    env.DB.prepare("DELETE FROM leaderboard WHERE player_id = ?").bind(playerHash),
    env.DB.prepare("DELETE FROM player_game_stats WHERE player_id = ?").bind(playerHash),
    env.DB.prepare("DELETE FROM play_receipts WHERE player_id = ?").bind(playerHash),
  ]);
  return json({ ok: true });
}

async function adminStats(env, url) {
  const appId = validAppId(url.searchParams.get("appId") || "yamone_arcade2");
  const days = clampInt(url.searchParams.get("days"), 1, 3650, 30);
  const since = Math.floor(Date.now() / 1000) - days * 86400;
  const summary = await env.DB.prepare(
    `SELECT COUNT(*) AS plays, SUM(CASE WHEN finished_at IS NOT NULL THEN 1 ELSE 0 END) AS completed,
      COUNT(DISTINCT player_id) AS players, COUNT(DISTINCT game_id || ':' || mode_id) AS games
     FROM play_receipts WHERE app_id = ? AND started_at >= ?`
  ).bind(appId, since).first();
  const byGame = await env.DB.prepare(
    `SELECT p.game_id, p.mode_id, COALESCE(g.title, p.game_id) AS title,
      COUNT(*) AS plays, SUM(CASE WHEN p.finished_at IS NOT NULL THEN 1 ELSE 0 END) AS completed,
      COUNT(DISTINCT p.player_id) AS players
     FROM play_receipts p LEFT JOIN game_catalog g ON g.game_id = p.game_id AND g.mode_id = p.mode_id
     WHERE p.app_id = ? AND p.started_at >= ?
     GROUP BY p.game_id, p.mode_id ORDER BY plays DESC, p.game_id`
  ).bind(appId, since).all();
  const byCountry = await env.DB.prepare(
    `SELECT country_code, COUNT(*) AS plays, COUNT(DISTINCT player_id) AS players
     FROM play_receipts WHERE app_id = ? AND started_at >= ?
     GROUP BY country_code ORDER BY plays DESC, country_code LIMIT 100`
  ).bind(appId, since).all();
  const byPlayerGame = await env.DB.prepare(
    `SELECT s.player_id, s.country_code, s.game_id, s.mode_id, s.play_count, s.completed_count,
      s.last_played_at, g.title, l.nickname
     FROM player_game_stats s
     LEFT JOIN game_catalog g ON g.game_id = s.game_id AND g.mode_id = s.mode_id
     LEFT JOIN leaderboard l ON l.player_id = s.player_id AND l.game_id = s.game_id AND l.mode_id = s.mode_id
     WHERE s.app_id = ? AND s.last_played_at >= ?
     ORDER BY s.play_count DESC, s.last_played_at DESC LIMIT 200`
  ).bind(appId, since).all();
  const audit = await env.DB.prepare(
    "SELECT action, game_id, mode_id, app_id, detail, created_at FROM admin_audit_log ORDER BY id DESC LIMIT 50"
  ).all();
  return json({
    ok: true,
    appId,
    days,
    summary: {
      plays: Number(summary?.plays || 0),
      completed: Number(summary?.completed || 0),
      players: Number(summary?.players || 0),
      games: Number(summary?.games || 0),
    },
    byGame: (byGame.results || []).map(row => ({
      gameId: row.game_id, modeId: row.mode_id, title: row.title,
      plays: Number(row.plays || 0), completed: Number(row.completed || 0), players: Number(row.players || 0),
    })),
    byCountry: (byCountry.results || []).map(row => ({
      countryCode: row.country_code, plays: Number(row.plays || 0), players: Number(row.players || 0),
    })),
    byPlayerGame: (byPlayerGame.results || []).map(row => ({
      playerRef: String(row.player_id || "").slice(0, 12), nickname: row.nickname || "",
      countryCode: row.country_code, gameId: row.game_id, modeId: row.mode_id, title: row.title,
      plays: Number(row.play_count || 0), completed: Number(row.completed_count || 0),
      lastPlayedAt: Number(row.last_played_at || 0),
    })),
    audit: (audit.results || []).map(row => ({
      action: row.action, gameId: row.game_id, modeId: row.mode_id, appId: row.app_id,
      detail: row.detail, createdAt: Number(row.created_at),
    })),
  });
}

async function adminCatalog(env, url) {
  const appId = validAppId(url.searchParams.get("appId") || "yamone_arcade2");
  const catalog = await loadCatalog(env, appId, true);
  return json({ ok: true, appId, sortMode: catalog.sortMode, games: catalog.games });
}

async function adminSaveGame(request, env) {
  const body = await requestJson(request);
  const appId = validAppId(body.appId || "");
  const gameId = validKey(body.gameId, "INVALID_GAME");
  const modeId = validKey(body.modeId || "normal", "INVALID_MODE");
  const title = cleanText(body.title, 1, 40, "INVALID_TITLE");
  const scoreUnit = cleanText(body.scoreUnit, 1, 24, "INVALID_SCORE_UNIT").toLowerCase();
  const maxScore = clampInt(body.maxScore, 1, 1000000000, 0);
  if (!maxScore) throw new HttpError(400, "INVALID_MAX_SCORE");
  const displayOrder = clampInt(body.displayOrder, 0, 100000, 100);
  const enabled = body.enabled === false ? 0 : 1;
  const featured = body.featured === true ? 1 : 0;
  const statements = [
    env.DB.prepare(
      `INSERT INTO game_catalog(game_id, mode_id, title, score_unit, max_score, status, updated_at)
       VALUES (?, ?, ?, ?, ?, 'active', unixepoch())
       ON CONFLICT(game_id, mode_id) DO UPDATE SET title = excluded.title,
         score_unit = excluded.score_unit, max_score = excluded.max_score,
         status = 'active', updated_at = excluded.updated_at`
    ).bind(gameId, modeId, title, scoreUnit, maxScore),
    env.DB.prepare(
      `INSERT INTO app_games(app_id, game_id, mode_id, enabled, display_order, featured, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, unixepoch())
       ON CONFLICT(app_id, game_id, mode_id) DO UPDATE SET enabled = excluded.enabled,
         display_order = excluded.display_order, featured = excluded.featured, updated_at = excluded.updated_at`
    ).bind(appId, gameId, modeId, enabled, displayOrder, featured),
  ];
  if (featured) {
    statements.push(env.DB.prepare(
      "UPDATE app_games SET featured = 0, updated_at = unixepoch() WHERE app_id = ? AND NOT (game_id = ? AND mode_id = ?)"
    ).bind(appId, gameId, modeId));
  }
  statements.push(auditStatement(env, "game_saved", gameId, modeId, appId, title));
  await env.DB.batch(statements);
  return json({ ok: true });
}

async function adminVisibility(request, env) {
  const body = await requestJson(request);
  const appId = validAppId(body.appId || "");
  const gameId = validKey(body.gameId, "INVALID_GAME");
  const modeId = validKey(body.modeId || "normal", "INVALID_MODE");
  const enabled = body.enabled === true ? 1 : 0;
  const result = await env.DB.prepare(
    "UPDATE app_games SET enabled = ?, updated_at = unixepoch() WHERE app_id = ? AND game_id = ? AND mode_id = ?"
  ).bind(enabled, appId, gameId, modeId).run();
  if (!Number(result.meta?.changes || 0)) throw new HttpError(404, "GAME_NOT_FOUND");
  await auditStatement(env, enabled ? "game_enabled" : "game_hidden", gameId, modeId, appId, "").run();
  return json({ ok: true });
}

async function adminOrder(request, env) {
  const body = await requestJson(request);
  const appId = validAppId(body.appId || "");
  if (!Array.isArray(body.games) || body.games.length < 1 || body.games.length > 200) {
    throw new HttpError(400, "INVALID_GAME_ORDER");
  }
  const statements = body.games.map((item, index) => env.DB.prepare(
    "UPDATE app_games SET display_order = ?, updated_at = unixepoch() WHERE app_id = ? AND game_id = ? AND mode_id = ?"
  ).bind((index + 1) * 10, appId, validKey(item.gameId, "INVALID_GAME"), validKey(item.modeId || "normal", "INVALID_MODE")));
  statements.push(auditStatement(env, "game_order_changed", null, null, appId, `${body.games.length} games`));
  await env.DB.batch(statements);
  return json({ ok: true });
}

async function adminSettings(request, env) {
  const body = await requestJson(request);
  const appId = validAppId(body.appId || "");
  const sortMode = ["manual", "popular_7d", "popular_all"].includes(body.sortMode) ? body.sortMode : null;
  if (!sortMode) throw new HttpError(400, "INVALID_SORT_MODE");
  await env.DB.batch([
    env.DB.prepare(
      `INSERT INTO app_catalog_settings(app_id, sort_mode, updated_at) VALUES (?, ?, unixepoch())
       ON CONFLICT(app_id) DO UPDATE SET sort_mode = excluded.sort_mode, updated_at = excluded.updated_at`
    ).bind(appId, sortMode),
    auditStatement(env, "sort_mode_changed", null, null, appId, sortMode),
  ]);
  return json({ ok: true });
}

async function adminResetRanking(request, env) {
  const body = await requestJson(request);
  const gameId = validKey(body.gameId, "INVALID_GAME");
  const modeId = validKey(body.modeId || "normal", "INVALID_MODE");
  const game = await getGame(env, gameId, modeId);
  if (!game) throw new HttpError(404, "GAME_NOT_FOUND");
  const resetLocal = body.resetLocal === true;
  const reason = cleanText(body.reason || "manual reset", 1, 200, "INVALID_REASON");
  const detail = JSON.stringify({ resetLocal, reason, previousRankingEpoch: game.ranking_epoch });
  await env.DB.batch([
    env.DB.prepare(
      `UPDATE game_catalog SET ranking_epoch = ranking_epoch + 1,
        local_reset_epoch = local_reset_epoch + ?, updated_at = unixepoch()
       WHERE game_id = ? AND mode_id = ?`
    ).bind(resetLocal ? 1 : 0, gameId, modeId),
    env.DB.prepare("DELETE FROM leaderboard WHERE game_id = ? AND mode_id = ?").bind(gameId, modeId),
    auditStatement(env, resetLocal ? "ranking_and_local_reset" : "ranking_reset", gameId, modeId, null, detail),
  ]);
  const updated = await getGame(env, gameId, modeId);
  return json({
    ok: true,
    gameId,
    modeId,
    rankingEpoch: updated.ranking_epoch,
    localResetEpoch: updated.local_reset_epoch,
  });
}

async function loadCatalog(env, appId, includeDisabled) {
  const setting = await env.DB.prepare(
    "SELECT sort_mode FROM app_catalog_settings WHERE app_id = ?"
  ).bind(appId).first();
  const sortMode = setting?.sort_mode || "manual";
  const rows = await env.DB.prepare(
    `SELECT g.game_id, g.mode_id, g.title, g.score_unit, g.max_score, g.status,
      g.ranking_epoch, g.local_reset_epoch, a.enabled, a.display_order, a.featured,
      COUNT(p.receipt_id) AS play_count,
      SUM(CASE WHEN p.started_at >= unixepoch() - 604800 THEN 1 ELSE 0 END) AS play_count_7d
     FROM app_games a JOIN game_catalog g ON g.game_id = a.game_id AND g.mode_id = a.mode_id
     LEFT JOIN play_receipts p ON p.app_id = a.app_id AND p.game_id = a.game_id AND p.mode_id = a.mode_id
     WHERE a.app_id = ? ${includeDisabled ? "" : "AND a.enabled = 1 AND g.status = 'active'"}
     GROUP BY g.game_id, g.mode_id, g.title, g.score_unit, g.max_score, g.status,
       g.ranking_epoch, g.local_reset_epoch, a.enabled, a.display_order, a.featured`
  ).bind(appId).all();
  const games = (rows.results || []).map(row => ({
    gameId: row.game_id,
    modeId: row.mode_id,
    title: row.title,
    scoreUnit: row.score_unit,
    maxScore: Number(row.max_score),
    status: row.status,
    enabled: Boolean(row.enabled),
    displayOrder: Number(row.display_order),
    featured: Boolean(row.featured),
    rankingEpoch: Number(row.ranking_epoch),
    localResetEpoch: Number(row.local_reset_epoch),
    playCount: Number(row.play_count || 0),
    playCount7d: Number(row.play_count_7d || 0),
  }));
  games.sort((left, right) => {
    if (sortMode === "popular_7d" && left.playCount7d !== right.playCount7d) return right.playCount7d - left.playCount7d;
    if (sortMode === "popular_all" && left.playCount !== right.playCount) return right.playCount - left.playCount;
    return left.displayOrder - right.displayOrder || left.gameId.localeCompare(right.gameId);
  });
  return { sortMode, games };
}

async function validateScoreInput(env, body) {
  const playerId = validPlayerId(body.playerId);
  const gameId = validKey(body.gameId, "INVALID_GAME");
  const modeId = validKey(body.modeId || "normal", "INVALID_MODE");
  const game = await getGame(env, gameId, modeId);
  if (!game || game.status === "retired") throw new HttpError(400, "INVALID_GAME_MODE");
  validateScoreUnit(body.scoreUnit, game.score_unit);
  return {
    playerId,
    nickname: normalizeNickname(body.nickname),
    countryCode: normalizeCountry(body.countryCode),
    gameId,
    modeId,
    score: validScore(body.score, game.max_score),
    game,
  };
}

async function getGame(env, gameId, modeId) {
  return env.DB.prepare(
    "SELECT game_id, mode_id, score_unit, max_score, status, ranking_epoch, local_reset_epoch FROM game_catalog WHERE game_id = ? AND mode_id = ?"
  ).bind(gameId, modeId).first();
}

async function upsertLeaderboard(env, playerHash, input) {
  const existing = await env.DB.prepare(
    "SELECT best_score FROM leaderboard WHERE player_id = ? AND game_id = ? AND mode_id = ?"
  ).bind(playerHash, input.gameId, input.modeId).first();
  const previousBest = Number(existing?.best_score || 0);
  const now = Math.floor(Date.now() / 1000);
  await env.DB.prepare(
    `INSERT INTO leaderboard(player_id, game_id, mode_id, nickname, country_code, best_score, achieved_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)
     ON CONFLICT(player_id, game_id, mode_id) DO UPDATE SET
       nickname = excluded.nickname,
       country_code = excluded.country_code,
       best_score = CASE WHEN excluded.best_score > leaderboard.best_score THEN excluded.best_score ELSE leaderboard.best_score END,
       achieved_at = CASE WHEN excluded.best_score > leaderboard.best_score THEN excluded.achieved_at ELSE leaderboard.achieved_at END,
       updated_at = excluded.updated_at`
  ).bind(playerHash, input.gameId, input.modeId, input.nickname, input.countryCode, input.score, now, now).run();
  return { updated: !existing || input.score > previousBest, previousBest };
}

function rankingRow(row, playerHash) {
  return {
    rank: Number(row.rank),
    nickname: row.nickname || "플레이어",
    countryCode: row.country_code || "",
    score: Number(row.best_score),
    isMe: Boolean(playerHash && row.player_id === playerHash),
  };
}

function validateEpoch(game, rawEpoch) {
  const current = Number(game.ranking_epoch);
  if (rawEpoch === undefined || rawEpoch === null || rawEpoch === "") {
    if (current > 1) throw new HttpError(409, "RANKING_EPOCH_REQUIRED", { rankingEpoch: current });
    return;
  }
  if (!Number.isInteger(Number(rawEpoch)) || Number(rawEpoch) !== current) {
    throw new HttpError(409, "STALE_RANKING_EPOCH", {
      rankingEpoch: current,
      localResetEpoch: Number(game.local_reset_epoch),
    });
  }
}

function validateScoreUnit(value, expected) {
  if (String(value || "") !== String(expected)) {
    throw new HttpError(400, "INVALID_SCORE_UNIT", { expected });
  }
}

function validScore(value, maximum) {
  const score = Number(value);
  if (!Number.isInteger(score) || score < 0 || score > Number(maximum)) {
    throw new HttpError(400, "INVALID_SCORE", { maximum: Number(maximum) });
  }
  return score;
}

function validPlayerId(value) {
  const playerId = String(value || "").trim();
  if (!PLAYER_PATTERN.test(playerId)) throw new HttpError(400, "INVALID_PLAYER_ID");
  return playerId;
}

function validAppId(value) {
  const appId = String(value || "").trim().toLowerCase();
  if (!APP_ID_PATTERN.test(appId)) throw new HttpError(400, "INVALID_APP_ID");
  return appId;
}

function validKey(value, code) {
  const key = String(value || "").trim().toLowerCase();
  if (!KEY_PATTERN.test(key)) throw new HttpError(400, code);
  return key;
}

function normalizeNickname(value) {
  const nickname = String(value || "플레이어").replace(/[\u0000-\u001f\u007f]/g, "").trim();
  if (!nickname || [...nickname].length > 12) throw new HttpError(400, "INVALID_NICKNAME");
  return nickname;
}

export function normalizeCountry(value) {
  const country = String(value || "").trim().toUpperCase();
  return /^[A-Z]{2}$/.test(country) ? country : "";
}

function cleanText(value, minimum, maximum, code) {
  const text = String(value || "").replace(/[\u0000-\u001f\u007f]/g, "").trim();
  if ([...text].length < minimum || [...text].length > maximum) throw new HttpError(400, code);
  return text;
}

function clampInt(value, minimum, maximum, fallback) {
  const number = Number(value);
  if (!Number.isInteger(number)) return fallback;
  return Math.max(minimum, Math.min(maximum, number));
}

async function hashPlayerId(env, playerId) {
  if (!env.RANKING_SIGNING_SECRET) throw new HttpError(500, "SIGNING_SECRET_NOT_CONFIGURED");
  const encoder = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw", encoder.encode(env.RANKING_SIGNING_SECRET), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
  );
  const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(playerId));
  return [...new Uint8Array(signature)].map(byte => byte.toString(16).padStart(2, "0")).join("");
}

function auditStatement(env, action, gameId, modeId, appId, detail) {
  return env.DB.prepare(
    "INSERT INTO admin_audit_log(action, game_id, mode_id, app_id, detail) VALUES (?, ?, ?, ?, ?)"
  ).bind(action, gameId, modeId, appId, detail || "");
}

async function requestJson(request) {
  try {
    const body = await request.json();
    if (!body || typeof body !== "object" || Array.isArray(body)) throw new Error("object required");
    return body;
  } catch {
    throw new HttpError(400, "INVALID_JSON");
  }
}

function json(value, status = 200) {
  return new Response(JSON.stringify(value), { status, headers: JSON_HEADERS });
}

class HttpError extends Error {
  constructor(status, code, details = {}) {
    super(code);
    this.status = status;
    this.code = code;
    this.details = details;
  }
}
