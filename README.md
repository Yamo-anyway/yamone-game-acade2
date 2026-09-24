# 야모네 아케이드 2

6가지 1분 아케이드 게임을 모은 **Android 네이티브 앱**입니다. 웹사이트나 WebView 게임이 아닙니다.

현재 버전: **0.7.0 — 6개 게임 통합 안정화**

## 현재 구현

- 홈 / 플레이 / 결과 / 로컬 최고기록 / 설정
- 오비트 스냅: 누르기·떼기 입력, 궤도 타이밍, 60초 제한, 3회 실수 종료, 점수와 PERFECT, 일시정지
- 컬러 브레이크: 좌우 탭, 상승하는 색 벽 통과, 색+숫자 구분, 연속 콤보, 60초 제한, 3회 실수 종료, 개별 최고기록
- 트윈 탭: 두 레인의 하강 노트, 단일·동시 노트, 포인터별 멀티터치, 타이밍 판정, 콤보, 60초 제한, 5회 실수 종료
- 라인 서프: 누르는 동안 선 위 주행, 손을 떼어 점프, 틈·장애물·충돌 회복, 거리와 연속 통과 점수, 60초 제한
- 포켓 펄스: 확장 파동과 목표 링 맞추기, PERFECT·GREAT·GOOD 정확도, 자동 MISS, 콤보, 60초 제한
- 스택 슬라이스: 좌우 스와이프 절단, 겹침 적층, 층별 무게중심·기울기 판정, 균형 보너스, 60초 제한
- 6개 Canvas 보드의 가로·세로 동시 맞춤, 회전 시 안전 일시정지, 프로세스 재생성 시 미완료 기록 폐기 안내
- 회원가입 없음, 로컬 익명 ID, 닉네임, 게임별 최고기록·플레이 횟수 저장
- Android Canvas 애니메이션, 기기 진동 옵션
- 하단 공식 AdMob 테스트 배너(debug만). 게임 영역·하단 시스템 영역과 분리
- 화면 구성 변경 시 적응형 테스트 배너 크기 재계산, pause/resume/destroy 수명주기 전달
- 랭킹 DTO/인터페이스와 미연결 어댑터. 온라인 요청·가짜 순위 없음
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

`docs/PRODUCT.md`, `docs/PROGRESS.md`, `docs/API_CONTRACT.md`를 먼저 읽습니다. 원본 시안의 이름·조작을 유지합니다. 외부 서버는 사용자가 추후 제공합니다.

기술 확인 자료: [AGP 8.10 호환성](https://developer.android.com/build/releases/agp-8-10-0-release-notes), [AdMob Android SDK](https://developers.google.com/admob/android/quick-start), [테스트 광고 ID](https://developers.google.com/admob/android/test-ads), [적응형 배너](https://developers.google.com/admob/android/banner).

실서비스 광고 ID와 랭킹 서버 정보가 없어도 현재 앱을 테스트할 수 있습니다. 운영 배너·서버·스토어 배포는 아직 연결하지 않았습니다. 화면 회전은 진행 중 엔진을 유지하고 일시정지하지만, 프로세스 종료 후에는 미완료 판을 저장하지 않고 재시작 안내를 표시합니다. 실기기 화면비·터치 감각·광고 실제 노출 확인은 별도 QA가 필요합니다.
