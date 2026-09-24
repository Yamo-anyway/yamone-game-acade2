# Development progress

Current stage: **M01 / v0.1.0**. Repository is the source of truth for subsequent hourly runs.

| Milestone | Scope | State |
|---|---|---|
| M01 | Android shell + Orbit Snap + local records + test banner + ranking boundary | complete; core checks, lint and debug APK passed |
| M02 | Color Break | next |
| M03 | Twin Tap, true multi-touch | pending |
| M04 | Line Surf | pending |
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

## Known limits / next

- M02: implement Color Break and add engine tests for lane matching, combo reset, wall crossing and completion. Wire through the shared shell without breaking Orbit.
- M07: Activity recreation currently returns home and loses an unfinished round; add proper saved state or a clearly designed abandonment flow before release. Rotation/tablet/landscape rendering not manually verified.
- Canvas visual/game feel and actual test ad rendering still require emulator/device QA.
- Gradle wrapper is not yet committed; CI installs exact Gradle 8.11.1. Add standard wrapper in an environment with Gradle.
- Local result writes use SharedPreferences.apply (asynchronous). Durable score policy can be strengthened during integration.
- Live ad IDs, server connection and store submission pending user-provided information; these do not block game development.

## Automation

Hourly development is configured for this project, first follow-up 2026-09-25 00:12:43 Asia/Seoul. Each run reads latest code/progress, implements next milestone and briefly reports. When all autonomous client work is complete, report once and pause only this project's automation. Do not change other project schedules.
