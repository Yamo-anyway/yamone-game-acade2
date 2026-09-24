# Development progress

Current stage: **M03 / v0.3.0**. Repository is the source of truth for subsequent hourly runs.

| Milestone | Scope | State |
|---|---|---|
| M01 | Android shell + Orbit Snap + local records + test banner + ranking boundary | complete; core checks, lint and debug APK passed |
| M02 | Color Break | complete; 33 core checks, Android lint and debug APK passed |
| M03 | Twin Tap, true multi-touch | complete; 52 core checks, Android lint and debug APK passed |
| M04 | Line Surf | next |
| M05 | Pocket Pulse | pending |
| M06 | Stack Slice | pending |
| M07 | Integration, lifecycle/aspect ratios, debug APK QA | pending |

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

## Known limits / next

- M04: implement Line Surf hold-to-ride/release-to-jump mechanics, gaps/obstacles, distance score and deterministic collision regression tests; preserve all three playable games.
- M07: Activity recreation currently returns home and loses an unfinished round; add proper saved state or a clearly designed abandonment flow before release. Rotation/tablet/landscape rendering not manually verified.
- Canvas visual/game feel and actual test ad rendering still require emulator/device QA.
- Gradle wrapper is not yet committed; CI installs exact Gradle 8.11.1. Add standard wrapper in an environment with Gradle.
- Local result writes use SharedPreferences.apply (asynchronous). Durable score policy can be strengthened during integration.
- Live ad IDs, server connection and store submission pending user-provided information; these do not block game development.

## Automation

Hourly development is configured for this project, first follow-up 2026-09-25 00:12:43 Asia/Seoul. Each run reads latest code/progress, implements next milestone and briefly reports. When all autonomous client work is complete, report once and pause only this project's automation. Do not change other project schedules.
