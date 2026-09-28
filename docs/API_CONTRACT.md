# Live ranking contract

Base URL: `https://yamone-games-ranking-api.yamone-game.workers.dev`

The app uses the same Cloudflare Worker and D1 `leaderboard` table as Yamone Games. It never receives a D1 credential or the Worker's `RANKING_SIGNING_SECRET`. Each Android package creates and retains its own random installation player ID, so the integrated app and a standalone app remain separate players even on the same phone.

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
  "scoreUnit": "points"
}
```

The Worker hashes the installation ID before storage, validates the game, mode, unit and maximum score, and keeps one highest score per player/game/mode. A lower or equal submission updates nickname/country without replacing the best score. Server time decides the achievement timestamp; ties sort by earlier achievement and then hashed player ID.

On the first connected-ranking launch, existing local bests are queued once so an app update does not discard prior play. After that, only a new local best or nickname change queues a submission. The pending best survives process death and retries after a validated network connection. A repeated submission is safe because the Worker retains the highest value.

## Read one game's ranking

`GET /v1/ranking/{gameId}/normal?playerId={installation UUID}`

The response includes `totalPlayers`, `top` (up to 100), `me`, and `nearby`. Rows contain rank, nickname, country code, score and an `isMe` marker. The app renders offline, server-error and empty states without fabricated entries.

## Delete this app's online records

`DELETE /v1/ranking/player` with `{ "playerId": "installation UUID" }`.

Because player IDs are package-local, this removes only this installation identity's records. The settings action clears local scores immediately and persists a server-deletion request until it succeeds online. If the user records a new score before reconnection, deletion runs first and the new pending best uploads afterward.

## Still needed for release

- AdMob production Android application ID and banner unit ID.
- Store listing/privacy disclosure and installed-device QA for ranking, gameplay and banner rendering.
