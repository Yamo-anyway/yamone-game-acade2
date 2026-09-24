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

## Ads and records

Only banners, no interstitial/rewarded ads, play limits, payments or account screens. Fixed banner strip separated from controls and system insets. Official test IDs in debug. Release ads disabled pending owner configuration and release prerequisites.

Local best scores, online ranking placeholder that explicitly says preparing; no fake entries. API contract is a proposal awaiting the owner's server. App reinstall clears data; Android backup disabled to avoid promising identity transfer.

## Next development boundaries

Implement one game per stage using the same shared shell. UI artwork is code-native. Original image is a concept reference, not a requirement to rasterize its mock phone UI into the app.

Integration milestone must cover aspect ratios, Android lifecycle/Activity recreation, multi-touch, tutorial readability, frame pacing, safe areas, record persistence, offline ads, test APK and manual QA checklist. No server provisioning, production ad deployment or store submission in this task.
