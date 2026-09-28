# Development progress

Current stage: **M12 / v0.12.0**. Repository is the source of truth for subsequent work.

| Milestone | Scope | State |
|---|---|---|
| M01 | Android shell + Orbit Snap + local records + test banner + ranking boundary | complete; core checks, lint and debug APK passed |
| M02 | Color Break | complete; 33 core checks, Android lint and debug APK passed |
| M03 | Twin Tap, true multi-touch | complete; 52 core checks, Android lint and debug APK passed |
| M04 | Line Surf | complete; 75 core checks, Android lint and debug APK passed |
| M05 | Pocket Pulse | complete; 96 core checks, Android lint and debug APK passed |
| M06 | Stack Slice | complete; 115 core checks, Android lint and debug APK passed |
| M07 | Integration, lifecycle/aspect ratios, debug APK QA | complete; 115 core + 17 integration checks, Android lint and debug APK passed |
| M08 | Shared Cloudflare game-by-game online ranking | complete; 115 core + 22 integration checks and Android CI passed |
| M09 | Play analytics, remote catalog, ranking reset and admin dashboard | complete; production migration/deployment, live smoke test and Android CI passed |
| M10 | Pastel home, rankings and settings redesign | complete; Android lint/APK, native emulator UI and visual review passed |
| M11 | Four-lane endless pastel Color Break | complete; Android lint/APK and native play-screen QA passed; D1 ceiling migration pending |
| M12 | Automatic rotating endless pastel Orbit Snap | implementation and local checks complete; exact-commit Android CI pending |

## M12 implementation

2026-09-28: Replaced Orbit hold/release and 60-second play with automatic
continuous rotation, target taps, five misses, progressive speed and narrower
targets. Passing a target costs one life; HIT/PERFECT award 100/150. The dot
continues through a short input lockout and a new target appears ahead without
teleporting the dot. Pastel ring/trail, top score/hearts, combo and timing pad
use the full board. Entry is immediate; pause/background recovery and results
share the custom pastel sheets with Color Break. Other four rule sets stay intact.

Local validation: 137 engine checks, 29 integration checks and operations tests
pass. Orbit coverage includes successful play beyond ten minutes/100,000 points,
window edges, automatic misses, five-life termination, paused feedback, frame
rate consistency, huge deltas, target wraps and bounded integer scores.
Native smoke coverage adds actual timing-pad input, automatic motion, explicit
foreground resume, exactly-once score/play storage and clean retry at both sizes.
Initial CI passed lint/build and the standard emulator scenario. The compact
scenario exposed a test assumption that all five lives remain after taking a
live-play screenshot; automatic rotation can legitimately miss in that interval.
Assertions now compare lives immediately before/after ignored input, and captures
are collected even if a later assertion fails. Final exact-commit CI is pending.

Added `0004_orbit_snap_endless.sql` to increase only Orbit's max score to
1,000,000,000 without deleting rows or changing reset epochs. Production D1
application is pending owner execution because this workspace has no Cloudflare
deployment connection. Pending Color Break migration can be applied by the same
command. Existing scores and installation identities are kept; no test data is
sent to production by the offline UI runner.

## M11 implementation

2026-09-28: Rebuilt Color Break as four shuffled color lanes with redundant shapes,
five misses, continuous acceleration and no time cutoff. The full-height pastel
board has top score/lives and four colored pads. Game cards open the board directly;
pause, background recovery and results use custom pastel sheets with explicit actions.
Other games retain their rules. Game ID, mode, points, installation identity,
per-game records and idempotent play receipts are preserved.

127 engine checks, 29 integration checks and operations migration checks pass.
New rule coverage includes 1,200 correct walls beyond ten minutes and 100,000 points,
four-way shuffle/target uniqueness, late selection, all five misses, input/recovery,
pause, two refresh rates, huge frame deltas and safe score saturation.
Offline emulator coverage now exercises all four actual pad locations, pause/resume,
custom result/retry and exactly-once local score/play-count storage.

Added additive migration `0003_color_break_endless.sql` to lift only this game's
server score ceiling to 1,000,000,000. It retains existing leaderboard rows and epochs.
This workspace has no Cloudflare deployment connection; production migration is
pending owner execution. Scores above 100,000 cannot upload until it is applied;
the client retains them locally and in its retry queue. No test data was sent online.
GitHub Actions run **36384312712**, exact code commit
**50ee2489f46496bb2dcc5d12ec7126d64d70c939**: **SUCCESS**. Engine/integration/
operations/contrast checks, Android lint and both APK assemblies passed.
Android 10 Pixel 2 emulator passed at 411dp/font 1.0 and 320dp/font 1.3.
Actual pad dispatch selects all four lanes; score-area taps do not start play.
Native pause freezes time, accelerated deterministic wall advancement exercises
the result flow, one terminal result increments play count exactly once, identity
survives, and retry restores zero score/five lives without an entry popup.
Reviewed native ready, playing, custom pause and result screenshots, including
the compact/large-text board and scrollable result sheet. No physical-device
rhythm feel, live score upload or ad rendering is claimed.
APK ZIP SHA-256: `6f265e05a8d731ae60fc761e6aa912544aafa28aa1cf35ce35e9f71f3e04cbdc`.

## M10 implementation

2026-09-28: Replaced the dark menu shell with cream, lilac, mint and peach
surfaces, original code-native game illustrations and a bunny profile avatar.
Home uses an illustrated featured card and responsive game tiles. Rankings
retain real server data and all states, with medal podiums, an explicit personal
rank card and a horizontally scrolling selected game picker. Settings groups
profile, haptics and data controls; destructive deletion still needs confirmation.
Gameplay engines, dark game boards, IDs, catalog sync and scoring are unchanged.
Menus select a one-column game layout on narrow screens or larger font settings.

Local checks: 115 engine checks, 29 integration checks, 10 menu text-contrast
checks, Worker operations tests and whitespace checks passed. Added an offline-only emulator instrumentation
runner for native navigation and screenshots at standard and large-text sizes.
Ranking names/scores in screenshot fixtures are test-only, never shipped in the
app or uploaded to production. Initial Android lint caught an API-27-only theme
attribute; removed it and retained the compatible runtime system-bar flags.
Final exact-commit GitHub Actions run **36381939004**, application commit
**382e8c849f1b9eafc4542c7a00c85a7541f7d55e**: **SUCCESS**. The same
checks, Android lint, debug APK and instrumentation APK assembly passed.
Android 10 Pixel 2 emulator verified home/settings/ranking navigation and
offline, empty and populated ranking states at **411dp / font 1.0** and
**320dp / font 1.3**, producing 16 native screenshots. Representative home,
game-grid, settings, podium and large-text list captures were visually reviewed.
The preview runner asserts the intended device width/font settings. APK ZIP
SHA-256: `9ff51ebcd1792f58250e768f1e60ea960549842315741488f983dc8c3300d2fa`.
No Cloudflare changes or production test data were needed. Physical-device
gameplay, live networking and real ad rendering were not retested in this stage.

## M09 implemented

2026-09-28: Added an additive D1 migration and version-controlled Worker source
for actual play starts/completions, per-player/game/app/country counters,
idempotent offline receipts, remotely ordered/hidden catalog entries, ranking
epochs and optional app-local record-reset epochs. Added the authenticated
same-origin `/admin` dashboard for totals, game/country statistics, manual or
popularity placement, catalog add/edit/hide and audited per-game ranking reset.

Android v0.9.0 records a durable start before opening each board and a durable
finish with its original epoch at the terminal result. It refreshes known-game
visibility/order/featured state, retains unknown IDs for a future binary, and
prevents pre-reset queued scores or lifetime local bests from restoring a new
ranking season. A full online-data delete also clears pending play receipts.

Local validation: **115 core checks**, **29 integration checks**, repeatable
SQLite migration/seed check, rendered Worker/Admin JavaScript syntax and
contract checks, and `git diff --check` passed. GitHub Actions run
**36366050759**, commit **034495742a8de58a227b17ed9f0df7bb700f0c65**,
completed the same service checks, Android lint and debug APK assembly.

Production D1 migration `0002_game_operations.sql` was applied without
removing the existing three leaderboard rows. Worker version
**8bb537a1-aebe-412e-a4df-9023f204005b** was deployed after fixing and testing
the rendered admin dashboard script. Live smoke testing verified idempotent
start receipt handling, finish completion, score/ranking update, country/game
statistics, authenticated admin login and complete deletion of the temporary
test player's ranking and play data. Device installation and physical gameplay
QA are not claimed.

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

M01–M07 completed the original offline client checkpoint. M08 resumed after the owner supplied and extended the shared Cloudflare ranking service.

## M08 implemented and validation

2026-09-28: Reused the existing `yamone-games-ranking-api` Worker and D1 `leaderboard` table. The Worker now accepts `orbit_snap`, `color_break`, `twin_tap`, `line_surf`, `pocket_pulse` and `stack_slice`, all in `normal` mode with `points` units. A production smoke test submitted, ranked, read and deleted a temporary Orbit Snap score successfully, leaving the board empty afterward.

The Android client now submits new game-specific best scores, derives a two-letter country code from the configured locale, retries durable pending scores after reconnection, synchronizes nickname changes, and persists online deletion requests. The ranking screen provides six independent selectors plus TOP 100, participant count, current-player and nearby rows. The app never contains Cloudflare credentials; the Worker hashes the package-local installation ID before D1 storage.

`bash scripts/test-core.sh`: **115 checks passed**. `bash scripts/check-integration.sh`: **22 checks passed**, including the live Worker URL, shared `normal`/`points` contract, durable upload/deletion markers and online ranking UI. `git diff --check` passed. Local Android lint/APK assembly remains unavailable because this environment has no Gradle or Android SDK; exact-commit CI is pending.

## Known limits / next

- Canvas visual/game feel, installed-APK safe areas, physical multi-touch/rotation and actual test-ad rendering still require emulator/device QA.
- Gradle wrapper is not yet committed; CI installs exact Gradle 8.11.1. Add standard wrapper in an environment with Gradle.
- Live ad IDs and store submission remain pending. The ranking server is connected; installed-device ranking UI/network QA is still required.

## Automation

Hourly development is configured for this project, first follow-up 2026-09-25 00:12:43 Asia/Seoul. Each run reads latest code/progress, implements next milestone and briefly reports. When all autonomous client work is complete, report once and pause only this project's automation. Do not change other project schedules.
