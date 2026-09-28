# Changelog

## 0.14.0 — 2026-09-28

- Fix disposable-CI debug signing with an explicitly public development key for stable future updates. The previously delivered v0.13.0 has a different certificate and cannot be updated in place; document the local-data/identity consequence of uninstalling.

- Hide Line Surf from the home/ranking selectors, including stale/offline catalogs, while preserving its records and shared ID.
- Rebuild Pocket Pulse as endless concentric colored waves: progressively 1–5 simultaneous circles, increasing speed, thin dashed inner/outer timing guides and five misses.
- Add pastel full-height gameplay, inline start, multi-pointer fresh-down input, five hearts, combo and custom pause/result sheets.
- Preserve ranking/analytics identity and prepare an app-scoped visibility/score-ceiling migration.
- Cover timing edges, color separation, multiring ordering, ten-minute play, pause, score bounds, terminal input and legacy-record visibility.

## 0.13.0 — 2026-09-28

- Rename Twin Tap to 탭탭 (Tap Tap) while preserving the shared `twin_tap` ID and existing records.
- Expand to four colored/shaped lanes with single notes and all six two-finger pairings. Remove the 60-second cutoff; the fifth missed row ends play.
- Continuously shorten note travel time, retain timing/combo scoring and safely support long-run scores up to 1,000,000,000.
- Add a full-height pastel board, four pads, five hearts, immediate entry and custom pause/result cards; update the home illustration to four lanes.
- Add a repeatable D1 migration for the display name and score ceiling, preserving IDs, records, placement and reset epochs.
- Pass 148 core and 29 integration checks; extend native UI coverage to four-lane input, actual two-pointer chords, ignored hold/release, lifecycle and exactly-once records.

## 0.12.0 — 2026-09-28

- Rebuild Orbit Snap as continuous automatic rotation with target-tap timing, no time limit and five misses. Missing a target also costs a life.
- Gradually increase angular speed and narrow the target, retain smooth motion between targets and award 100/150 points for HIT/PERFECT.
- Add a pastel full-height ring board with top score/hearts, trails, combo and a timing pad; enter immediately and use custom pause/result sheets.
- Preserve installation identity, stable game/mode IDs and existing ranking/play-event integration. Add a non-destructive D1 migration for long-run scores.
- Pass 137 core checks and 29 integration checks; extend native emulator coverage to automatic motion, target taps, pause/foreground recovery, result storage and retry.

## 0.11.0 — 2026-09-28

- Rebuild Color Break with four randomly shuffled color/shape lanes, colored tap pads, five misses and endless survival instead of a 60-second timer.
- Accelerate continuously, require a fresh selection per wall and preserve exact crossing/combo scoring through pauses and delayed frames.
- Add a full-height pastel board, top score/lives, immediate entry without a start popup and custom pause/result sheets.
- Preserve shared game IDs, app-local identity, ranking and play receipt behavior. Prepare a non-destructive D1 migration for scores above the former 100,000 ceiling.
- Expand engine coverage to 127 checks and native emulator coverage to actual four-pad input, pause, result storage and retry.

## 0.10.0 — 2026-09-28

- Redesign home, rankings, settings and result styling in a cream/lilac/peach palette with rounded cards, clear typography and selected icon navigation.
- Add six original scalable Canvas game illustrations and a small bunny profile avatar; no image downloads or additional runtime dependencies.
- Add featured-game art, adaptive game tiles, horizontally scrolling ranking choices, real-data medal podiums, personal ranking cards and designed loading/empty/offline/error states.
- Group settings into profile, haptics and records; preserve nickname sync, confirmation before deletion and all existing ranking/catalog behavior.
- Keep dark gameplay boards, scoring, installation identity and the isolated test banner unchanged in behavior. Add offline emulator UI fixtures/screenshots for standard and larger-text layouts.

## 0.9.0 — 2026-09-28

- Add idempotent game-start and game-finish receipts so actual attempts, completions, per-game users, app source and country totals can be measured without double-counting offline retries.
- Add a shared D1 game catalog with app-specific visibility, featured game, manual order and recent/lifetime popularity sort modes; Android applies known-game changes on launch/resume.
- Add per-game ranking epochs so a reset cannot be undone by delayed pre-reset uploads, plus an optional local-reset epoch for clearing that game's device best on next sync.
- Add the authenticated Cloudflare `/admin` dashboard for summary/game/country statistics, game add/edit/hide/order, ranking reset and an audit log.
- Keep existing ranking routes and all legacy Yamone game/mode IDs compatible; detailed play identity remains an HMAC hash and app-local player IDs are never merged.
- Expand executable validation to 29 Android integration checks plus repeatable D1 migration and Worker/Admin contract checks; retain all 115 game-engine checks.

## 0.8.0 — 2026-09-28

- Connect all six stable game IDs to the existing Yamone Games Cloudflare Worker and shared D1 leaderboard using `normal` mode and `points` units.
- Queue existing local bests once during migration, then submit new local best scores with the app-local installation ID, nickname and device country; keep failed uploads durably queued for reconnection.
- Add separate game selectors, online TOP 100, total participants, current-player rank, nearby rows, country flags and explicit offline/error/empty states.
- Keep integrated and standalone app identities separate while preserving the same game IDs across future packaging changes.
- Synchronize nickname changes and make local/online record deletion durable and retryable without exposing Worker or D1 secrets to the app.
- Retain 115 game-engine checks and expand executable integration-policy coverage to 22 checks for the live endpoint, shared mode/unit, upload retry, deletion retry and online UI.

## 0.7.0 — 2026-09-25

- Integrate all six games with one orientation/process-recreation policy: rotations retain and pause the live engine; process recreation explicitly abandons unsaved partial runs and offers a restart.
- Fit Orbit Snap to both width and height like the other five 360×520 Canvas boards, preventing short/landscape layouts from cropping controls.
- Recreate the anchored adaptive test banner after configuration changes while preserving pause/resume/destroy forwarding and separation from game touch targets.
- Commit installation identity, terminal results and record deletion synchronously so a completed result is durable before navigation.
- Add 17 executable integration-policy checks for ad scope, six-game board fitting/unlock, lifecycle markers, local durability, network safety and disconnected ranking behavior; retain all 115 engine checks.
- Local Java and integration checks passed. Exact-commit CI passed all checks, Android lint and debug APK assembly/upload.
- Physical device layout/touch/rotation, process-kill UI and actual test-ad display remain unverified; no production ads or ranking server connected.

## 0.6.0 — 2026-09-25

- Add playable Stack Slice with moving blocks, left/right edge cuts, overlap-only stacking, minimum-width collapse and 60-second rounds.
- Add a layer-by-layer center-of-mass stability model, tilt-based score bonus, balanced streak, anti-camping block deadline and deterministic seeded motion.
- Add fitted native Canvas tower, cut guides, tilt meter, horizontal swipe filtering, pause/retry/result routing and per-game local records.
- Add 19 Stack Slice/catalog regression checks (115 total), covering aligned/wrong cuts, duplicate input, collapse, deadline, pause, exact round end, layer capacity, refresh-rate and delayed-frame consistency.
- Local Java checks and whitespace validation passed. Exact fixed-commit CI passed Android lint and debug APK assembly after correcting Canvas cut-guide float coordinates.
- Device swipe feel/layout and actual test-ad display remain unverified; no production ads or ranking server connected.

## 0.5.0 — 2026-09-25

- Add playable Pocket Pulse with expanding seeded waves, target rings, PERFECT/GREAT/GOOD timing windows, automatic misses, four lives and 60-second rounds.
- Add accuracy scoring, capped combo bonus, deterministic target generation, recovery lockout and gradually increasing pulse speed.
- Add fitted native Canvas rings/tolerance glow, accuracy/error feedback, pause/retry/result routing and per-game local records.
- Add 21 Pocket Pulse/catalog regression checks (96 total), covering score boundaries, duplicate/early/late input, automatic miss, recovery generation, pause, exact round end, refresh-rate and delayed-frame consistency.
- Local Java checks and whitespace validation passed. Local Android build unavailable (Gradle/SDK absent); exact-commit CI verification is recorded in PROGRESS.
- Device timing feel/layout and actual test-ad display remain unverified; no production ads or ranking server connected.

## 0.4.0 — 2026-09-25

- Add playable Line Surf with hold-to-ride/release-to-jump input, deterministic gaps and obstacles, airborne physics, three lives and 60-second rounds.
- Add distance plus clear-bonus scoring, clear combo, collision recovery arc, seeded spacing and gradually increasing speed.
- Add fitted native Canvas course, parallax skyline, hazard preview, jump/landing feedback, pause/retry/result routing and per-game local records.
- Add 23 Line Surf/catalog regression checks (75 total), covering input edges, pause, safe gaps/obstacles, collision/recovery, terminal state, exact round end, refresh-rate and delayed-frame consistency.
- Local Java checks and whitespace validation passed. Local Android build unavailable (Gradle/SDK absent); exact-commit CI verification is recorded in PROGRESS.
- Device jump feel/layout and actual test-ad display remain unverified; no production ads or ranking server connected.

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
