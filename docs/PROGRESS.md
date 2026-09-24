# Development progress

Current stage: **M07 / v0.7.0**. Repository is the source of truth for subsequent work.

| Milestone | Scope | State |
|---|---|---|
| M01 | Android shell + Orbit Snap + local records + test banner + ranking boundary | complete; core checks, lint and debug APK passed |
| M02 | Color Break | complete; 33 core checks, Android lint and debug APK passed |
| M03 | Twin Tap, true multi-touch | complete; 52 core checks, Android lint and debug APK passed |
| M04 | Line Surf | complete; 75 core checks, Android lint and debug APK passed |
| M05 | Pocket Pulse | complete; 96 core checks, Android lint and debug APK passed |
| M06 | Stack Slice | complete; 115 core checks, Android lint and debug APK passed |
| M07 | Integration, lifecycle/aspect ratios, debug APK QA | complete; 115 core + 17 integration checks, Android lint and debug APK passed |

## M01 implemented

Native Android application (`com.yamone.arcade2`), six-game catalog (only Orbit unlocked), tutorial/start/game/pause/result/retry/home navigation, nickname and vibration setting, local best scores/play counts, record-delete confirmation, isolated official test banner (debug), disabled release ads, offline ranking boundary. Pure Java Orbit timing/scoring/lives/60s engine separated from Canvas UI. GitHub Actions lint/debug APK pipeline.

## Validation

2026-09-24: `bash scripts/test-core.sh` — **15 checks passed**. Covers first-touch start, precise hit, duplicate release, canceled touch, background pause/resume, idle camping, finished-state input, successful full 60s round, angle wrap, 60/120Hz consistency, invalid deltas and disconnected ranking.

The local `javac` executable is absent, but the installed Java 17 runtime includes the `jdk.compiler` module. The script uses `java com.sun.tools.javac.Main` as a fallback and completed compilation/testing. Android SDK and Gradle are absent locally.

GitHub Actions run **36012804556**, code commit **f4d2b71242fd65f1bf18a3caa768c14b8ba92ba9**: **SUCCESS**. The 15 engine checks, `:app:lintDebug`, `:app:assembleDebug`, debug APK artifact upload and lint report upload all succeeded. APK artifact: `yamone-arcade2-debug`. [Build result](https://github.com/Yamo-anyway/yamone-game-acade2/actions/runs/36012804556).

The initial CI failed because setup-android tried the retired SDK package `tools`. Specifying `platform-tools` fixed setup; the subsequent complete build passed. XML parsing and git whitespace checks also passed. No emulator/device UI or actual ad-display pass is claimed. This checkpoint changes documentation only after the verified code commit.

## M02 implemented and validation

2026-09-24: Native Color Break engine/View, two rising color lanes, left/right taps, redundant number cues, combo scoring, three lives, 60-second cutoff, gradual acceleration, pause/resume, result/retry and per-game records. Shared GameView lifecycle and run-ID guarded result routing preserve Orbit functionality. Color board fits its entire logical area into available width and height; actual device layout has not been checked.

`bash scripts/test-core.sh`: **33 checks passed** (15 prior checks + 18 Color Break/catalog checks). Covers valid start, unique matching lane, crossing-only judgement, last-moment lane change, no duplicate scoring, combo bonus/reset/cap, pause during recovery, READY pause, three misses, terminal input, exact 60s, speed bounds, 60/120Hz and delayed-frame consistency, invalid deltas and unlock scope. `git diff --check` passed.

Local `gradle :app:lintDebug :app:assembleDebug` attempted but unavailable: `gradle: command not found`; no local Android SDK.

GitHub Actions run **36019168249**, exact code commit **301d143fbaee1fd88ab415b2e498e59c0cc2bacd**: **SUCCESS**. All 33 core checks, `:app:lintDebug`, `:app:assembleDebug`, debug APK upload (`yamone-arcade2-debug`) and lint report upload passed. [M02 build result](https://github.com/Yamo-anyway/yamone-game-acade2/actions/runs/36019168249). This follow-up only records the verified result; application code is unchanged.

No device/emulator play, installed APK, real touch, persistence across process restarts or actual test-ad impression is claimed. Git HTTPS push had no terminal credentials, so the connected GitHub API published the identical verified source tree with a non-forced fast-forward update; local main was restored from that remote commit while keeping the original local commit on a checkpoint branch.

## M03 implemented and validation

2026-09-25: Native Twin Tap engine/View, two descending lanes, deterministic single/double notes, genuine pointer-ID multi-touch, ±180ms timing and ±55ms PERFECT windows, combo scoring, five lives, 60-second cutoff, gradual acceleration, pause of partially completed chords, result/retry and per-game records. Each Android `DOWN`/`POINTER_DOWN` is consumed once; move/hold cannot repeat a hit.

`bash scripts/test-core.sh`: **52 checks passed** (33 prior checks + 19 Twin Tap/catalog checks). Coverage includes explicit start, ignored early input, exact single PERFECT, duplicate suppression, wrong-lane failure, deterministic double notes, distinct-lane chord completion across two inputs, pause/resume during a partial chord, exact late miss, five-miss termination, terminal input, perfect 60-second completion, acceleration bounds, 60/120Hz and delayed-frame consistency, invalid deltas and unlock scope. `git diff --check` passed.

Local Android lint/APK verification is unavailable because this environment has no Gradle or Android SDK.

GitHub Actions run **36026649428**, exact code commit **33593a4ebf26fbcf6c5229a198bcd195c505d944**: **SUCCESS**. All 52 core checks, `:app:lintDebug`, `:app:assembleDebug`, debug APK upload (`yamone-arcade2-debug`) and lint report upload passed. [M03 build result](https://github.com/Yamo-anyway/yamone-game-acade2/actions/runs/36026649428). This follow-up only records the verified result; application code is unchanged.

No device/emulator play, installed APK, physical multi-touch, screen-ratio QA or actual test-ad impression is claimed. The connected GitHub API published the verified source tree with a non-forced fast-forward update; local main was restored from the identical remote commit while retaining the original local commit on a checkpoint branch.

## M04 implemented and validation

2026-09-25: Native Line Surf engine/View, hold-to-ride/release-to-jump edges, deterministic gaps and raised obstacles, airborne physics, three lives, collision recovery, distance plus clear scoring, best clear combo, 60-second cutoff, gradual acceleration, pause/resume, result/retry and per-game records. Course art and parallax are derived from engine distance and do not mutate game rules.

`bash scripts/test-core.sh`: **75 checks passed** (52 prior checks + 23 Line Surf/catalog checks). Coverage includes first-hold start, release jump, duplicate/canceled input, paused physics, fresh-hold resume, safe traversal of seeded gap and obstacle types, score composition, hazard bounds/reaction distance, crash/no-bonus behavior, three-crash finish, terminal input, READY pause, perfect 60-second completion, speed bound, 60/120Hz and delayed-frame collision consistency, invalid deltas and unlock scope. `git diff --check` passed.

Local Android lint/APK verification is unavailable because this environment has no Gradle or Android SDK.

GitHub Actions run **36033808759**, exact code commit **3990e8a212824e7d8dd3cb04ff378284290b36ef**: **SUCCESS**. All 75 core checks, `:app:lintDebug`, `:app:assembleDebug`, debug APK upload (`yamone-arcade2-debug`) and lint report upload passed. [M04 build result](https://github.com/Yamo-anyway/yamone-game-acade2/actions/runs/36033808759). This follow-up only records the verified result; application code is unchanged.

No device/emulator play, installed APK, physical jump timing, screen-ratio QA or actual test-ad impression is claimed. The connected GitHub API published the verified source tree with a non-forced fast-forward update; local main was restored from the identical remote commit while retaining the original local commit on a checkpoint branch.

## M05 implemented and validation

2026-09-25: Native Pocket Pulse engine/View, seeded target rings, expanding wave, PERFECT/GREAT/GOOD windows, four lives, automatic overrun miss, combo scoring, result recovery lockout, 60-second cutoff, gradual acceleration, pause/resume, result/retry and per-game records. Canvas visuals read engine state only and do not affect timing.

`bash scripts/test-core.sh`: **96 checks passed** (75 prior checks + 21 Pocket Pulse/catalog checks). Coverage includes start-only first tap, deterministic target bounds, exact PERFECT, GREAT/GOOD score and combo boundaries, recovery duplicate suppression, early and automatic late misses, one-target-per-recovery, paused wave/recovery, READY pause, four-miss finish, terminal input, perfect 60-second completion, capped combo growth, speed bound, 60/120Hz and delayed-frame deadline consistency, invalid deltas and unlock scope. `git diff --check` passed.

Local Android lint/APK verification is unavailable because this environment has no Gradle or Android SDK.

GitHub Actions run **36040393143**, exact code commit **5608dc39e00ddd110e2fe2be223136e3881ac3fb**: **SUCCESS**. All 96 core checks, `:app:lintDebug`, `:app:assembleDebug`, debug APK upload (`yamone-arcade2-debug`) and lint report upload passed. [M05 build result](https://github.com/Yamo-anyway/yamone-game-acade2/actions/runs/36040393143). This follow-up only records the verified result; application code is unchanged.

No device/emulator play, installed APK, physical tap timing, screen-ratio QA or actual test-ad impression is claimed. The connected GitHub API published the verified source tree with a non-forced fast-forward update; local main was restored from the identical remote commit while retaining the original local commit on a checkpoint branch.

## M06 implemented and validation

2026-09-25: Native Stack Slice engine/View, moving incoming blocks, directional left/right edge cuts, overlap-only placement, every-support center-of-mass stability calculation, minimum-width and excessive-tilt collapse, balance score/streak, anti-camping block deadline, 60-second cutoff, gradual acceleration, pause/resume, result/retry and per-game records. Android touch accepts one horizontal swipe per primary pointer and ignores short/vertical or secondary-pointer gestures.

`bash scripts/test-core.sh`: **115 checks passed** (96 prior checks + 19 Stack Slice/catalog checks). Coverage includes explicit start, seeded first block, aligned cut/full score, recovery duplicate suppression, next-block generation, wrong-side narrowing, eventual center-of-mass collapse, terminal input, idle timeout, paused motion/deadline, READY pause, balanced 60-second completion without layer overflow, full balance streak/score, acceleration bounds, 60/120Hz and delayed-frame timeout consistency, invalid deltas and six-game unlock scope. `git diff --check` passed.

Local Android lint/APK verification is unavailable because this environment has no Gradle or Android SDK.

The first CI run **36047251537** caught two Canvas cut-guide coordinates passed as doubles where Android requires floats; core tests passed but compilation failed. Commit **665b71418732a52fa8c639f96cf5445100bbced3** corrected the coordinate type without changing game rules.

GitHub Actions run **36047467358**, exact fixed commit **665b71418732a52fa8c639f96cf5445100bbced3**: **SUCCESS**. All 115 core checks, `:app:lintDebug`, `:app:assembleDebug`, debug APK upload (`yamone-arcade2-debug`) and lint report upload passed. [M06 build result](https://github.com/Yamo-anyway/yamone-game-acade2/actions/runs/36047467358). This follow-up only records the verified result; application code is unchanged.

No device/emulator play, installed APK, physical swipe feel, screen-ratio QA or actual test-ad impression is claimed. The connected GitHub API published both source commits with non-forced fast-forward updates. Production ads and ranking server remain disconnected.

## M07 implemented and validation

2026-09-25: Integrated all six games around an explicit lifecycle policy. Orientation/screen-size changes retain the active Activity/engine, cancel held input, pause for an explicit resume and recreate the adaptive test banner at the new size. Process recreation deliberately abandons an in-memory run without saving a partial score, explains the interruption and offers a same-game restart; non-game destinations restore safely. Orbit now uses the same width-and-height 360×520 fitting policy as the other five boards.

Local installation ID creation, terminal results and record deletion now use synchronous SharedPreferences commits. A completed score is durable before the result screen appears, while run-ID duplicate suppression and per-game best/play counts remain intact. No online submission was enabled.

`bash scripts/test-core.sh`: **115 checks passed**. `bash scripts/check-integration.sh`: **17 checks passed**, covering official debug test banner ID, release ad disablement, absence of interstitial/rewarded ads, no backup/cleartext traffic, rotation retention, process-recreation abandonment detection, terminal-result commit, empty disconnected ranking, all six width/height-fitted boards and full catalog unlock. `git diff --check` passed.

Local Android lint/APK verification is unavailable because this environment has no Gradle or Android SDK.

GitHub Actions run **36053687516**, exact code commit **18c011345223288ffee8c64aa163d9d977a719db**: **SUCCESS**. All 115 core checks, 17 integration-policy checks, `:app:lintDebug`, `:app:assembleDebug`, debug APK upload (`yamone-arcade2-debug`) and lint report upload passed. [M07 build result](https://github.com/Yamo-anyway/yamone-game-acade2/actions/runs/36053687516). This follow-up only records the verified result; application code is unchanged.

No device/emulator play, installed APK, physical touch/rotation, process-kill recovery UI, screen-ratio QA or actual test-ad impression is claimed.

All autonomously implementable client milestones M01–M07 are complete. Further release work is gated by physical-device QA and owner-provided production ad/ranking/store configuration, so scheduled development can pause without implying store readiness.

## Known limits / next

- Canvas visual/game feel, installed-APK safe areas, physical multi-touch/rotation and actual test-ad rendering still require emulator/device QA.
- Gradle wrapper is not yet committed; CI installs exact Gradle 8.11.1. Add standard wrapper in an environment with Gradle.
- Live ad IDs, server connection and store submission pending user-provided information; these do not block game development.

## Automation

Hourly development is configured for this project, first follow-up 2026-09-25 00:12:43 Asia/Seoul. Each run reads latest code/progress, implements next milestone and briefly reports. When all autonomous client work is complete, report once and pause only this project's automation. Do not change other project schedules.
