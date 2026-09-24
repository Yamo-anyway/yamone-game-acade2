# 야모네 아케이드 2

6가지 1분 아케이드 게임을 모은 **Android 네이티브 앱**입니다. 웹사이트나 WebView 게임이 아닙니다.

현재 버전: **0.2.0 — 컬러 브레이크 추가**

## 현재 구현

- 홈 / 플레이 / 결과 / 로컬 최고기록 / 설정
- 오비트 스냅: 누르기·떼기 입력, 궤도 타이밍, 60초 제한, 3회 실수 종료, 점수와 PERFECT, 일시정지
- 컬러 브레이크: 좌우 탭, 상승하는 색 벽 통과, 색+숫자 구분, 연속 콤보, 60초 제한, 3회 실수 종료, 개별 최고기록
- 회원가입 없음, 로컬 익명 ID, 닉네임, 게임별 최고기록·플레이 횟수 저장
- Android Canvas 애니메이션, 기기 진동 옵션
- 하단 공식 AdMob 테스트 배너(debug만). 게임 영역·하단 시스템 영역과 분리
- 랭킹 DTO/인터페이스와 미연결 어댑터. 온라인 요청·가짜 순위 없음
- GitHub Actions 엔진 검사 / lint / debug APK 빌드

트윈 탭 / 라인 서프 / 포켓 펄스 / 스택 슬라이스는 아직 준비 중이며 후속 단계에서 순서대로 추가합니다.

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

실서비스 광고 ID와 랭킹 서버 정보가 없어도 개발을 계속할 수 있습니다. 운영 배너·서버·스토어 배포는 아직 연결하지 않았습니다. 진행 중 라운드는 메모리 상태이며 프로세스 종료/Activity 재생성 시 복원되지 않습니다. 화면비·실기기·광고 실제 노출 확인은 후속 검증 항목입니다.
