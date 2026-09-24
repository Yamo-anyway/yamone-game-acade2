# Ranking connection draft — no server connected

This is a proposal for later adaptation. No base URL, credential, DNS lookup, HTTP client, provisioning, or score upload is currently active. `RankingGateway.Disabled` returns NOT_CONNECTED and an empty list. UI only displays actual local records.

## Proposed operations

- `POST /v1/players/anonymous`: server-issued anonymous identity/session (no signup UI). Installation UUID alone is not authorization.
- `POST /v1/scores`: idempotent `run_id`, `player_id` from authorized session, `game_id`, `mode_id=60s`, `rules_version`, integer `score`, `duration_ms`, `seed`, `app_version`.
- `GET /v1/leaderboards?game_id=...&mode_id=60s&rules_version=1&cursor=...`: verified ranks, nickname, best score, next cursor, current player's rank if authorized.

One best entry per server player/game/mode/rules version is the proposed layout. Tie rule, seasons, nickname constraints, anonymous session renewal, deletion and anti-cheat validation need agreement with the owner's server. Never use client clock as authoritative achievement time or trust a client player_id as authentication. Avoid promising secure online rankings before server validation exists.

## Local integration types

`RankingGateway.ScoreSubmission` contains runId/playerId/nickname/game/score/durationMs/seed plus current mode and rule version. `Entry` contains rank/score/nickname. The disabled implementation returns an explicit connection state. A future adapter should make requests off the UI thread and model loading/error/empty pages independently.

Completed local games are not a durable upload queue. Offline upload/retry/idempotency and server-validation policy will be added only after a real contract is supplied; old local scores will not silently enter online rankings.

## Information needed later

- Server base URL and endpoint specification or sample requests/responses.
- Anonymous session/token issuance and score validation rules.
- Ranking tie-break, pagination, season/reset, per-game units.
- AdMob Android application ID and banner unit ID for live ads. Do not paste private credentials into this repository.
