# Live ranking contract

Base URL: `https://yamone-games-ranking-api.yamone-game.workers.dev`

The app uses the same Cloudflare Worker and D1 `leaderboard` table as Yamone Games. It never receives a D1 credential or the Worker's `RANKING_SIGNING_SECRET`. Each Android package creates and retains its own random installation player ID, so the integrated app and a standalone app remain separate players even on the same phone.

Arcade 2 sends `appId: "yamone_arcade2"`. This keeps app-specific statistics
and catalog placement separate while the stable `gameId` still joins the same
game across integrated and standalone packages.

## Stable game and mode IDs

| Game | `gameId` | `modeId` | `scoreUnit` |
|---|---|---|---|
| Orbit Snap | `orbit_snap` | `normal` | `points` |
| Color Break | `color_break` | `normal` | `points` |
| Twin Tap | `twin_tap` | `normal` | `points` |
| Line Surf | `line_surf` | `normal` | `points` |
| Pocket Pulse | `pocket_pulse` | `normal` | `points` |
| Stack Slice | `stack_slice` | `normal` | `points` |

These game IDs remain unchanged if games move between standalone and integrated apps.

## Submit a best score

`POST /v1/ranking/submit`

```json
{
  "playerId": "installation UUID",
  "nickname": "플레이어",
  "countryCode": "KR",
  "gameId": "orbit_snap",
  "modeId": "normal",
  "score": 1234,
  "scoreUnit": "points",
  "rankingEpoch": 1
}
```

The Worker hashes the installation ID before storage, validates the game, mode, unit and maximum score, and keeps one highest score per player/game/mode. A lower or equal submission updates nickname/country without replacing the best score. Server time decides the achievement timestamp; ties sort by earlier achievement and then hashed player ID.

After a ranking reset, submissions from an older epoch are rejected. This
prevents an offline pre-reset best score from recreating the cleared ranking.

## Count actual game sessions

`POST /v1/plays/event`

The app durably sends one `started` and, when the game reaches a result, one
`finished` event with the same UUID `playId`. The payload also contains
`appId`, installation `playerId`, country, game/mode, and the ranking epoch
captured when that run began. A finished event adds score and score unit.

The `(appId, playId)` pair is unique, so retries never increase the play count
twice. A start counts as an actual attempt even if the app is closed before a
result. A finish contributes to completion statistics and updates the best
ranking only when its original epoch is still current. Old-epoch plays remain
valid analytics but cannot repopulate a reset leaderboard.

## Synchronize the game catalog

`GET /v1/catalog?appId=yamone_arcade2`

The response supplies visible games, display order, featured game,
`rankingEpoch` and `localResetEpoch`. The app refreshes it on launch/resume,
ignores unknown game IDs until a binary update implements them, and applies
known-game visibility/order without changing the stable IDs.

When `rankingEpoch` increases, pending old best-score uploads are discarded.
When `localResetEpoch` increases, that game's device best is also cleared; its
local play counter and server analytics are preserved.

On the first connected-ranking launch, existing local bests are queued once so an app update does not discard prior play. After that, only a new local best or nickname change queues a submission. The pending best survives process death and retries after a validated network connection. A repeated submission is safe because the Worker retains the highest value.

## Read one game's ranking

`GET /v1/ranking/{gameId}/normal?playerId={installation UUID}`

The response includes `totalPlayers`, `top` (up to 100), `me`, and `nearby`. Rows contain rank, nickname, country code, score and an `isMe` marker. The app renders offline, server-error and empty states without fabricated entries.

## Delete this app's online records

`DELETE /v1/ranking/player` with `{ "playerId": "installation UUID" }`.

Because player IDs are package-local, this removes only this installation identity's records. The settings action clears local scores immediately and persists a server-deletion request until it succeeds online. If the user records a new score before reconnection, deletion runs first and the new pending best uploads afterward.

The v2 delete also removes that hashed player's detailed play receipts and
per-player counters. Other users and unrelated aggregate history are untouched.

## Administration

After the approved hardening rollout, `/admin` and `/v1/admin/*` require a
Cloudflare Access user session on the configured administrator origin. The
Worker validates the Access JWT and limits requests by verified identity. API
requests require `X-Yamone-Admin: 1`; writes also require a matching `Origin`.
There is no legacy Bearer fallback. See
[`cloudflare/ADMIN_SECURITY.md`](../cloudflare/ADMIN_SECURITY.md) for configuration
and pending rollout steps. The page shows total starts/completions,
unique players, game and country totals; manages game visibility, ordering and
automatic popularity sort modes; registers new catalog IDs; and resets one
game/mode ranking with an optional app-local record-reset epoch. Every mutation
is written to `admin_audit_log`.

## Still needed for release

- AdMob production Android application ID and banner unit ID.
- Store listing/privacy disclosure and installed-device QA for ranking, gameplay and banner rendering.
