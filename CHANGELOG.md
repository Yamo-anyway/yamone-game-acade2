# Changelog

## 0.2.0 — 2026-09-24

- Add playable Color Break: two-lane tap control, rising color/number walls, combo bonus, three lives, 60-second limit and increasing speed.
- Add fit-to-bounds native Canvas board, matching-number cues and optional judgement haptics.
- Share game lifecycle/result/retry routing while retaining separate engines and per-game local scores; stale result callbacks are rejected by run ID.
- Add 18 Color Break/catalog checks (33 total), including crossings, combo reset/cap, pause, long frames, 60/120Hz and exact round end.
- Local Java checks and whitespace validation passed. Local Android build unavailable (Gradle/SDK absent); exact-commit CI verification recorded in PROGRESS.
- Device gameplay, touch/layout and actual ad display remain unverified; no production ads or server connected.

## 0.1.0 — 2026-09-24

- Initial Android native app, six approved concept catalog.
- Playable Orbit Snap engine/Canvas, timer, lives, precision score, pause and retry.
- No signup, optional nickname and local records.
- Test-only banner integration, disconnected ranking contract.
- Core regression checks and Android lint/APK CI.
