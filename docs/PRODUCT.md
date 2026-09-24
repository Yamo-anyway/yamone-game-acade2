# Product / approved scope

Source: user's 2026-09-24 six-concept image “초간단 아케이드 게임 아이디어 보드.png”, inspected directly. User asked for hourly app development in this exact repository.

One Android app, six quick games. One or two fingers; approximately 60-second rounds; immediate retry. Native Java/Canvas/View implementation. No signup/login. Start without entering a nickname; optional nickname editing later. Local UUID identifies installation only, not authenticated server identity. No account recovery promise.

| ID | 이름 | 원본 조작·핵심 | 점수 방향 |
|---|---|---|---|
| orbit_snap | 오비트 스냅 | 누르면 원을 돌고 떼면 다음 궤도로 점프 | 통과·정확도 |
| color_break | 컬러 브레이크 | 좌우 탭으로 아래에서 올라오는 벽의 같은 색 구역 통과 | 통과·연속 콤보 |
| twin_tap | 트윈 탭 | 두 레인의 내려오는 점을 한/두 손가락으로 처리 | 타이밍·동시 성공 |
| line_surf | 라인 서프 | 누르면 선 위를 달리고 떼면 점프, 틈과 장애물 통과 | 거리 |
| pocket_pulse | 포켓 펄스 | 중심에서 퍼지는 파동과 목표 링 크기가 같을 때 탭 | 정확도·콤보 |
| stack_slice | 스택 슬라이스 | 좌우 스와이프로 블록을 잘라 균형 유지, 과도한 기울기 종료 | 적층·균형 |

Exact numbers below are initial playable tuning, not a claim that the user fixed every threshold. Keep initial rules version 1; if changing score semantics later use a new version.

## Orbit Snap v1

- READY waits for first touch. Game clock then runs even when finger is up; 60 seconds max.
- Three lives. Release outside the mint arc costs one. A ring deadline of 4.5→3.3 seconds prevents idle camping.
- Hold rotates dot; lift in target arc awards 100. Within 7° awards 150 total. No passive points.
- Target tolerance shrinks 25°→14.2°; angular speed rises 130→232°/second.
- Short .32s orbit transition. Extra input during transition cannot earn another jump.
- Background/pause freezes time, cancels held input, requires explicit resume. Unfinished rounds abandoned to home do not save scores.
- Completed rounds save best score locally. Scores do not upload retroactively by default.

## Color Break v1

- Tap the left/right half to select one of two lanes; holding/moving/additional fingers do not repeat input. First valid tap starts the clock.
- One wall rises from below. Match the player's current color/number at the crossing line; the two lanes always have distinct colors and exactly one matches.
- Three colors carry redundant numbers (1/2/3). Player and wall colors are refreshed together for each new wall.
- Hit: 100 points plus 10 per previous consecutive success, capped at +100. Miss: lose one of three lives and reset combo. Best combo is retained for the result screen.
- Wall travel time: 2.4 seconds initially, decreasing by .022 per elapsed second, minimum 1.1 seconds. A .28-second feedback/recovery separates walls.
- 60 active seconds maximum. Time-limit termination takes precedence over a crossing exactly at 60 seconds. No score is awarded on tap alone; one judgement per wall.
- Pause freezes timer and wall/recovery. Returning from background needs explicit resume, without lane movement on the resume tap. Finished games reject input.
- Home, instructions, pause/retry, result and local records share the native shell. Online ranking remains disconnected; ads unchanged.

## Twin Tap v1

- A valid start tap begins the clock without judging a note. Notes descend in two lanes to one hit line; a note can require left, right or both lanes.
- Android input tracks every pointer ID. `DOWN` and `POINTER_DOWN` each produce at most one lane hit; move/hold and duplicate taps from the same lane cannot create repeated scores. One finger may alternate lanes, while double notes accept two physical pointers within the same timing window.
- Hit window is ±180ms. Within ±55ms is PERFECT. A wrong lane inside the window or an incomplete/untouched note at its deadline costs one of five lives and resets combo; very early/late free taps outside the current window are ignored.
- A single note is worth 150 PERFECT / 100 HIT; a double note is worth 300 / 200. Each completed note adds a 10-point combo bonus per prior consecutive success, capped at +100.
- The first target arrives at 1.6 seconds. Inter-note interval accelerates from 1.15 seconds to a .72-second minimum; visual travel time decreases from 1.6 seconds to a .95-second minimum. Double notes begin after the first two notes and occur at a deterministic 30% rate from the seeded sequence.
- 60 active seconds maximum; the time boundary wins over a deadline at the same instant. Pause freezes a partially completed double note and all timing. Resume taps do not judge a note. Finished games reject input.
- Results include hits, successful double notes and best combo. Local score/play count uses the existing per-game store; ranking server and production ads remain disconnected.

## Line Surf v1

- The first press starts the active clock and holds the rider to the line. Releasing an armed press while grounded launches one jump; duplicate release, canceled touch and release without a fresh press cannot jump.
- The course is deterministic from the run seed and presents one upcoming feature at a time: 58–90px gaps or 34–56px obstacles with 35–50px height. The next feature begins at least 250px beyond the previous end, with extra seeded spacing for reaction time.
- The rider travels at 155px/s plus 1.35px/s for every elapsed second. Jump velocity is 285px/s with 520px/s² gravity. Landing in a gap or touching an obstacle below its top causes a crash.
- A crash removes one of three lives, resets the clear combo, moves the failed feature behind the rider and gives a .65-second recovery window/arc. Three crashes end the round; no clear bonus is awarded for the failed feature.
- Passing a feature awards one clear and increases best combo. Score is integer distance in meters (`floor(world pixels / 10)`) plus 100 per cleared feature. Distance cannot increase before the first press.
- 60 active seconds maximum; round completion wins over a collision on the same simulation instant. Pause freezes distance, physics, hazards and time, cancels held input and requires a fresh press after resume. Finished games reject input.
- Android Canvas draws the deterministic parallax course, gap/obstacle preview, rider height, lives, distance and touch state. Results include distance, clears and jumps; local records use the shared shell.

## Ads and records

Only banners, no interstitial/rewarded ads, play limits, payments or account screens. Fixed banner strip separated from controls and system insets. Official test IDs in debug. Release ads disabled pending owner configuration and release prerequisites.

Local best scores, online ranking placeholder that explicitly says preparing; no fake entries. API contract is a proposal awaiting the owner's server. App reinstall clears data; Android backup disabled to avoid promising identity transfer.

## Next development boundaries

Implement one game per stage using the same shared shell. UI artwork is code-native. Original image is a concept reference, not a requirement to rasterize its mock phone UI into the app.

Integration milestone must cover aspect ratios, Android lifecycle/Activity recreation, multi-touch, tutorial readability, frame pacing, safe areas, record persistence, offline ads, test APK and manual QA checklist. No server provisioning, production ad deployment or store submission in this task.
