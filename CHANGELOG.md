# Changelog

## 0.3.0 — 2026-09-25

- Add playable Twin Tap with two descending lanes, single/double notes, timing windows, PERFECT/HIT scoring, combo bonus, five lives and 60-second rounds.
- Track Android pointer IDs and consume each `DOWN`/`POINTER_DOWN` once, supporting genuine two-finger chords without move/hold repeats.
- Add fitted native Canvas board, pressed-lane feedback, partial chord display, pause/resume, retry/result routing and per-game local records.
- Add 19 Twin Tap/catalog regression checks (52 total), covering split multi-touch chords, duplicate/wrong/early/late input, partial-chord pause, exact round end, frame-rate and delayed-frame consistency.
- Local Java checks and whitespace validation passed. Local Android build unavailable (Gradle/SDK absent); exact-commit CI verification is recorded in PROGRESS.
- Device multi-touch feel, layout and actual test-ad display remain unverified; no production ads or ranking server connected.

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
