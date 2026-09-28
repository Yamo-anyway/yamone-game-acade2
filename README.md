# 야모네 아케이드 2

6가지 짧은 아케이드 게임을 모은 **Android 네이티브 앱**입니다. 오비트 스냅과 컬러 브레이크는 시간 제한 없이 플레이합니다.

현재 버전: **0.12.0 — 자동 회전·무제한 오비트 스냅**

## 현재 구현

- 홈 / 플레이 / 결과 / 로컬 최고기록 / 설정
- 오비트 스냅: 자동 회전, 목표 구간에서 탭, 속도 증가·구간 축소, 무제한·5회 미스 종료, 파스텔 보드와 전용 일시정지·결과 화면
- 컬러 브레이크: 랜덤 4색 레인과 색 버튼, 상승하는 벽 통과, 연속 콤보, 가속·무제한·5회 미스 종료, 파스텔 보드
- 트윈 탭: 두 레인의 하강 노트, 단일·동시 노트, 포인터별 멀티터치, 타이밍 판정, 콤보, 60초 제한, 5회 실수 종료
- 라인 서프: 누르는 동안 선 위 주행, 손을 떼어 점프, 틈·장애물·충돌 회복, 거리와 연속 통과 점수, 60초 제한
- 포켓 펄스: 확장 파동과 목표 링 맞추기, PERFECT·GREAT·GOOD 정확도, 자동 MISS, 콤보, 60초 제한
- 스택 슬라이스: 좌우 스와이프 절단, 겹침 적층, 층별 무게중심·기울기 판정, 균형 보너스, 60초 제한
- 6개 Canvas 보드의 가로·세로 동시 맞춤, 회전 시 안전 일시정지, 프로세스 재생성 시 미완료 기록 폐기 안내
- 회원가입 없음, 로컬 익명 ID, 닉네임, 게임별 최고기록·플레이 횟수 저장
- Android Canvas 애니메이션, 기기 진동 옵션
- 하단 공식 AdMob 테스트 배너(debug만). 게임 영역·하단 시스템 영역과 분리
- 화면 구성 변경 시 적응형 테스트 배너 크기 재계산, pause/resume/destroy 수명주기 전달
- 기존 Yamone Games Cloudflare Worker/D1에 게임별 최고기록 전송
- 게임별 온라인 TOP 100, 내 순위, 내 주변 순위와 국가 표시
- 오프라인 최고기록·삭제 요청의 로컬 보관 및 재연결 처리
- 파스텔 홈·랭킹·설정, 웹 관리자 통계·게임 배치·게임별 랭킹 및 앱 기록 초기화 동기화
- GitHub Actions 엔진 검사 / lint / debug APK 빌드

6개 게임이 모두 플레이 가능하며 통합·수명주기·화면비·오프라인 기록·배너 정책 검사를 CI에 포함합니다.

## 빌드

Java 17, Gradle **8.11.1**, Android SDK **36**, Build Tools **35.0.0**, AGP **8.10.1**.

Android Studio에서 이 저장소를 열고 SDK 경로를 설정합니다. 시스템 Gradle 8.11.1 또는 IDE의 해당 Gradle 설치를 사용합니다. Gradle Wrapper는 아직 포함하지 않았습니다.

```sh
bash scripts/test-core.sh
gradle :app:lintDebug :app:assembleDebug
```

GitHub의 **Actions → Android → 성공한 실행 → yamone-arcade2-debug**에서도 APK를 받습니다. APK 경로는 `app/build/outputs/apk/debug/app-debug.apk`입니다.

## 이어서 개발

`docs/PRODUCT.md`, `docs/PROGRESS.md`, `docs/API_CONTRACT.md`를 먼저 읽습니다. 게임별 최신 승인 규칙과 공유 game ID를 유지합니다. 랭킹 서버는 기존 Cloudflare Worker/D1을 사용합니다.

무제한 게임의 100,000점 초과 기록 업로드에는 `cloudflare/`에서 최신 D1 마이그레이션 적용이 필요합니다. 자세한 명령은 `cloudflare/README.md`에 있습니다.

기술 확인 자료: [AGP 8.10 호환성](https://developer.android.com/build/releases/agp-8-10-0-release-notes), [AdMob Android SDK](https://developers.google.com/admob/android/quick-start), [테스트 광고 ID](https://developers.google.com/admob/android/test-ads), [적응형 배너](https://developers.google.com/admob/android/banner).

실서비스 광고 ID가 없어도 현재 앱을 테스트할 수 있습니다. 온라인 랭킹은 기존 Cloudflare Worker에 연결되어 있고 운영 배너·스토어 배포는 아직 연결하지 않았습니다. 화면 회전은 진행 중 엔진을 유지하고 일시정지하지만, 프로세스 종료 후에는 미완료 판을 저장하지 않고 재시작 안내를 표시합니다. 실기기 화면비·터치 감각·랭킹 UI·광고 실제 노출 확인은 별도 QA가 필요합니다.
