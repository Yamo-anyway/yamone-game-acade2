# 관리자 보안 점검 및 적용 절차 — 2026-09-28

## 범위와 결론

점검 기준은 `main`의 `3cd4a4b5b05d96218806ccd057c121ef0309e1be`다.
[`b8dd769`](https://github.com/Yamo-anyway/yamone-game-acade2/commit/b8dd769a452475f7f41202adbe97abb5794e9638)는
D1 migration, Worker version `8bb537a1-aebe-412e-a4df-9023f204005b`, 실제 관리자 로그인 smoke test의
**수행 기록**이다. 이 문서 변경만으로 현재 운영 배포물·계정 설정이 소스와 일치한다고 단정하지 않는다.
이번 작업에서는 운영 Cloudflare 계정 설정, secret, D1, Worker 배포를 변경하지 않았다.

소수의 운영자가 브라우저로 관리하는 현 규모에는 **Access 앱 1개 + Worker JWT 검증 +
관리자별 rate limit binding 1개**를 선택한다. 인증/세션은 Access에 맡기고 별도 세션 테이블,
비밀번호 로그인 API, Durable Object, KV, 추가 migration은 만들지 않는다.

| 확인한 사항 | 영향 / 판단 | 패치 |
|---|---|---|
| `/v1/admin/*`가 단일 장기 Bearer 값만 검증, 시도 제한 없음 | 높은 우선순위. 키가 탈취되면 통계 열람·게임 변경·랭킹 초기화 가능. constant-time 비교만으로 온라인 추측/탈취를 막지 못함 | Access 사용자 인증으로 교체. 기존 키 fallback 없음 |
| `/admin` 공개 HTML | HTML 공개 자체가 인증 우회는 아니나 관리 진입점 노출 | HTML도 같은 JWT 검증 적용 |
| `sessionStorage`에 장기 키 저장, 서버 만료/폐기 없는 로그아웃 | XSS/브라우저 접근 시 키 재사용 가능. 탭 저장 수명은 서버 세션 수명이 아님 | 비밀 입력 UI 제거. Access 쿠키·만료·로그아웃 사용 |
| 관리자 JSON과 OPTIONS가 공개 API의 `Access-Control-Allow-Origin: *` 공유 | 단독으로 인증 우회는 아님. 쿠키 인증 전환 시 CSRF 경계 필요 | 관리자 CORS 제거, custom header + 쓰기 Origin 검사 |
| CSP가 inline script/handler 허용, framing 제한 없음 | 스크립트 삽입·클릭재킹 방어 부족 | nonce script, event listener, frame/base/object 제한 |
| `.env`, `.env.*`는 제외되지만 `.dev.vars*` 제외 누락 | Wrangler 로컬 비밀 파일의 실수 커밋 위험 | `.dev.vars*`, `.wrangler/`, `node_modules/` 제외 |
| 검토한 소스·설정에 실제 secret 값 없음 | 유출 확인 사례가 아니다. D1 UUID는 인증 비밀이 아님 | secret 회전/삭제 안 함. 전체 과거 이력·외부 로그까지 무유출을 보증하지 않음 |

현재 `admin_audit_log`는 작업 이력이고 수행자 필드는 없다. Access 인증 로그와 동일한 것이 아니며,
다인 운영 확대 시 JWT `sub`를 작업 이력에 연결하는 후속 개선이 필요하다.
공개 점수 API의 익명 설치 ID/부정 점수 방지는 이번 관리자 패치 범위 밖이다.

## 선택한 구성

- 기존 `yamone-games-ranking-api.yamone-game.workers.dev`는 게임 API용으로 유지한다.
- 소유한 Cloudflare zone에서 **관리자 전용 Custom Domain** 하나를 같은 Worker에 연결한다.
  예시 `games-admin.example.com`은 실제 주소가 아니다. 해당 호스트 **전체**를 Access로 보호한다.
  `/admin`과 `/v1/admin/*`의 정책 누락을 피하고 Android에 Access 로그인을 요구하지 않기 위한 선택이다.
  사용할 zone/도메인은 아직 확인하지 않았다. 준비되지 않으면 관리자 기능을 닫은 상태로 두며
  Bearer fallback을 열지 않는다. 신규 domain 구매/설정은 이번에 수행하지 않는다.
- Worker는 `ADMIN_ORIGIN`과 정확히 일치하는 HTTPS origin에서만 관리자 요청을 처리한다.
  `workers.dev`, 다른 custom host, preview host로는 유효 JWT를 보내도 관리자 경로가 404다.
- 고정된 팀 도메인의 JWKS로 RS256 서명, issuer, audience, 만료, 발급 시각,
  사용자 `sub`/email, `type=app`을 검증한다. 임의 헤더 email이나 JWT 내 URL은 신뢰하지 않는다.
  JWKS를 캐시하고 갱신은 JOSE가 담당한다. 키 조회/검증 실패는 접근 거부로 처리한다.
- 애플리케이션 토큰 최대 수명은 **1시간**이다. Access 앱/정책도 1시간으로 설정해야 한다.
  24시간 토큰은 코드에서 거부한다. 이는 절대적인 1시간 강제 재로그인과 다르다.
  Access global/IdP 세션이 살아 있으면 앱 토큰이 자동 재발급될 수 있다.[3]
- 인증된 `sub` 기준 **60회/60초**, HTML과 모든 관리자 API에 합산 적용한다.
  IP/NAT 공유 문제를 피하며, 바인딩 오류는 503, 초과는 429 + `Retry-After: 60`이다.
  이 값은 초기 운영 기준이며 Cloudflare가 보장하는 전역 정확한 60회 상한이 아니다.
  카운터는 colo 단위·eventually consistent다.[2]
- 비인증 로그인은 Access/IdP가 담당한다. Worker에는 추측할 공유 비밀번호와 로그인 API가
  남지 않으므로 별도 로그인 실패 D1 카운터를 두지 않는다. 이 rate limiter는 Access 로그인
  자체를 제한하는 장치가 아니며, 도난 계정/JWT나 분산 공격을 완전히 해결하지도 않는다.
- 관리자 쿠키는 Access가 관리한다. HttpOnly를 명시적으로 켜고 SameSite=Lax를 선택한다.
  Secure 속성도 실제 HTTPS 응답에서 확인한다. 자체 쿠키/JWT 발급·브라우저 저장은 없다.
  API는 `X-Yamone-Admin: 1`, 쓰기는 정확한 Origin을 요구한다.
  `X-Requested-With: XMLHttpRequest`로 Access 세션 만료의 401 처리를 요청한다.[3][4]

## 적용 전 준비 — 운영 변경은 별도 승인 후

이 PR은 **아직 배포하지 않는다**. 검토/merge 전에 Cloudflare Workers Builds나 별도 자동배포가
main push/PR과 연결되어 있는지 확인한다. 저장소의 GitHub workflow에는 Android 검증만 있지만,
Cloudflare 대시보드의 외부 배포 설정은 이번에 확인하지 않았다.

1. 같은 Worker를 쓰는 Yamone Games/Arcade 2 운영자와 배포 소스를 맞춘다. 다른 저장소의 오래된
   Worker로 나중에 덮어쓰면 보호가 사라질 수 있다. 다른 저장소는 이번에 수정하지 않는다.
2. Cloudflare 계정·현재 배포 version·route·Workers Builds·D1 binding·Access 앱/정책을 확인하고
   되돌릴 구성을 기록한다. 문서에 적힌 과거 version을 현재 version으로 간주하지 않는다.
3. 최소 권한의 관리자 개별 email만 Allow로 지정한다. Everyone, 이메일 도메인 전체,
   Bypass, Service Auth 정책을 추가하지 않는다. 기존 MFA 가능한 IdP와 MFA 요구 정책을
   사용한다. 이메일 OTP만 사용하면 MFA와 동등하지 않음을 인지하고 허용 메일 계정 보호를 확인한다.
4. Access Self-hosted 앱을 관리자 전용 호스트 **전체**에 먼저 만든다. 앱/Allow 정책 세션 1시간,
   HttpOnly 활성화, SameSite=Lax 설정. 글로벌 세션은 다른 앱에도 영향을 주므로 임의 변경하지 않는다.
   해당 호스트에 더 구체적인 Bypass 앱/정책이 없는지 확인한다.[5]
5. 실제 `ADMIN_ORIGIN`, `ACCESS_TEAM_DOMAIN`, 앱 `ACCESS_AUD`를 확인한다. 모두 비밀값이 아닌
   설정 식별자다. `wrangler.toml`의 빈 값 3개를 실제 값으로 교체한다. 빈 값 그대로 배포하면
   **관리자만 503**이 된다. 대시보드 설정만 바꾸고 TOML은 비워두면 다음 배포에 덮어쓸 수 있다.
6. 아래 custom route를 TOML에 추가한다. 기존 route가 있다면 함께 보존한다.
   `workers_dev = true`는 유지하고 Worker 전체에 Access를 켜지 않는다. 공개 앱이 중단된다.
   `preview_urls = false`의 영향을 확인하고, 기존 preview/version URL도 직접 거부되는지 검증한다.
7. rate limit namespace `2026092801`이 이 계정에서 사용 중인지 확인한다. 다른 binding과
   공유하면 같은 key 카운터가 합쳐질 수 있으므로 충돌 시 미사용 양의 정수 문자열로 바꾼다.[2]

```toml
# 예시일 뿐이다. 기존 [vars]를 편집하고 실제 값으로 교체한다.
[vars]
ADMIN_ORIGIN = "https://games-admin.example.com"
ACCESS_TEAM_DOMAIN = "https://YOUR-TEAM.cloudflareaccess.com"
ACCESS_AUD = "COPY-THE-APPLICATION-AUD"

[[routes]]
pattern = "games-admin.example.com"
custom_domain = true
```

Access 앱을 먼저 만들고 **보호 확인 후 route를 활성화**한다. 아직 구 버전 Worker가 동작하는
동안 새 도메인의 관리 API는 구 Bearer도 필요하므로, route를 먼저 붙였다면 Access 차단만 검증하고
구 키를 새 브라우저에 저장하지 않는다. JWT만 요구하는 동작은 새 코드 배포 이후 검증한다.
기존 public `/admin`는 새 코드가 적용되기 전까지 구 동작을 유지한다.

## 로컬 검증 — 지금 실행해도 운영 변경 없음

저장소 루트에서 Node 22 이상, npm, Python 3을 사용한다.

```bash
npm ci --prefix cloudflare
bash scripts/test-operations.sh
bash scripts/check-integration.sh
bash scripts/test-core.sh
npm --prefix cloudflare run check:bundle
git diff --check
```

`check:bundle`은 고정된 Wrangler 4.142.0의 `deploy --dry-run`이다. 실제 업로드/배포하지 않는다.
JWT 테스트는 실행 시 만든 일회용 키·가짜 JWKS·DB spy만 사용한다. migration 검사는 메모리 SQLite다.
Wrangler `.dev.vars*`에는 로컬 테스트 값만 사용하고 git에 넣지 않는다. 기존 signing secret을
개발용으로 내려받지 않는다. 버전 고정 `jose`와 lockfile을 함께 설치한다.

이번 패치는 데이터 schema/Android 버전을 바꾸지 않는다. Android 전체 빌드는 PR CI 결과로 별도 확인한다.

## 운영 실행 — 승인 후에만

1. 위 준비와 검증, PR 검토를 끝낸다. 배포할 commit SHA를 기록한다.
2. **새 D1 migration, `secret put`, `secret delete`, signing secret 회전은 필요 없다.**
   `RANKING_SIGNING_SECRET`은 플레이어 HMAC 식별자에 쓰이므로 그대로 보존한다.
   기존 `RANKING_ADMIN_SECRET`도 이번 배포에서 변경/삭제하지 않는다. 새 코드는 무시한다.
3. 승인된 구성으로 `cloudflare`에서 로컬 고정 CLI를 실행한다.

   ```bash
   npm exec -- wrangler deploy
   ```

4. 출력 version ID와 아래 검증 결과만 남긴다. Cookie/JWT/Authorization, 키 입력 화면,
   HAR/헤더 전체를 이슈·채팅·git·CI 로그에 붙이지 않는다.
5. 과거 관리자 탭을 모두 닫고 **기존 workers.dev origin**의 `yamoneAdminSecret` sessionStorage를
   브라우저 개발자 도구에서 삭제한다. 새 호스트는 이전 호스트의 저장소를 삭제할 수 없다.
   같은 호스트에서 실행될 때에는 패치가 이전 항목을 제거한다.
6. 안정화 후 구 admin secret 폐기는 별도 승인·작업으로 처리한다. 실제 노출 증거가 생기면
   Access 세션 폐기와 해당 비밀 회전을 별도로 진행한다. 이번 점검만으로 유출을 단정하지 않는다.

## 배포 후 검증표

운영 변경/로그인은 아직 실행하지 않았다. 실제 Access 정책·쿠키·colo 카운터는 로컬 테스트로
검증할 수 없으므로 아래 항목은 배포 후 확인한다. 조회 smoke test를 우선하고 랭킹 초기화는
별도 테스트 환경/대상으로만 수행한다.

| 점검 | 기대 결과 |
|---|---|
| 로그아웃/시크릿 창에서 관리자 호스트 `/admin`, `/v1/admin/session` | Access 로그인/차단. 원본 관리자 HTML/JSON에 접근 불가 |
| Allow에 없는 email, 임의 Access 헤더 | 접근 거부. email 헤더만으로 통과 불가 |
| Allow 관리자 로그인 → `/admin` | 통계/카탈로그 조회 정상. 비밀값 입력란 없음 |
| 개발자 도구 쿠키/저장소 | CF_Authorization Secure·HttpOnly·SameSite=Lax, 앱 토큰 1시간. local/sessionStorage에 인증값 없음 |
| 관리자 쓰기 요청 | 정확한 Origin + custom header 있는 요청만 성공. 운영 데이터 변경 테스트는 별도 승인 대상 |
| cross-origin preflight/POST | 성공 ACAO/ACAC 없음. CSRF 요청 거부. 실제 Access 앞단 상태코드는 302/401/403일 수 있음 |
| 관리자 응답/실패 응답 | Worker 도달 응답에 no-store, nosniff, DENY. HTML CSP에 nonce 및 frame-ancestors |
| 기존 workers.dev `/admin`, `/v1/admin/*`, 다른 route/preview | 설정 완료 시 Worker 404. preview 자체 비활성화면 경로 도달 전 차단 가능 |
| 기존 public `/health`, `/v1/catalog`, 두 앱의 랭킹 조회 | Access 로그인 요구 없이 기존 JSON. public CORS 유지 |
| 한 사용자로 연속 읽기 요청 | 같은 colo에서 60/60초 근처에 429·Retry-After, 새 창/새 IP로 같은 sub를 써도 key 동일. 정확한 61번째 보장은 아님 |
| 세션 만료/관리자 revoke/로그아웃 | API 접근 거부 및 UI 잠금·다시 로그인 안내. 로그아웃은 Access 전체 앱 세션에 영향, 토큰 폐기 반영 20–30초 가능[3] |
| Back 버튼 / BFCache | 기존 관리 화면 재노출 대신 잠금/재검증 |

쓰기 smoke는 staging에서만 catalog 순서 변경/원복·랭킹 reset을 테스트한다. 운영에서 잘못된
Origin을 시험할 때도 유효한 파괴 payload를 사용하지 않는다. 제한 테스트는 인증 후 단순
`GET /v1/admin/session`을 적은 범위에서 수행하고 운영 로그인 공격·부하 시험은 하지 않는다.

## 실패 시 조치 / 롤백

- 관리자 장애만 발생하면 공개 게임 API 상태부터 확인하고 관리자 닫힘을 유지한다.
  issuer/AUD/정책 세션/host/namespace를 점검한다. 임시 Bearer 우회, public CORS,
  `ADMIN_AUTH_DISABLED` 같은 해제 스위치를 만들지 않는다.
- 긴급하게 관리 접근을 멈추려면 Access Allow 제거/사용자 세션 revoke를 사용한다.
  코드 검증은 오프라인 JWT 서명 검증이므로 즉시 폐기의 보장은 **Access 앞단**에 의존한다.
  Access 앱 자체를 삭제해서 공개 상태로 만들지 않는다.
- D1 변경이 없어 DB rollback은 필요 없다. 구 Worker로 단순 rollback하면 public workers.dev의
  장기 Bearer 관리자 API가 다시 열린다. 관리자 장애만으로 그 rollback을 실행하지 않는다.
- 공개 API 장애로 이전 버전이 꼭 필요하면, 운영 승인 후 관리 경로를 모든 route에서 먼저
  확실히 차단한 안전한 버전을 준비해 배포한다. 구 버전 선택만으로 보안이 유지된다고 가정하지 않는다.
- JWT/쿠키 탈취 시 사용자 세션 revoke, Access 정책 점검 및 최대 1시간 토큰 만료를 고려한다.
  보호 호스트 경유를 강제하는 origin 검사를 제거하지 않는다.

## 근거 — 2026-09-28 확인한 Cloudflare 공식 문서

1. [Validate JWTs](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/authorization-cookie/validating-json/)
   — Worker도 Access JWT를 검증해야 하며 `jose`와 issuer/AUD/JWKS 검증 예제를 제공한다.
2. [Workers Rate Limiting](https://developers.cloudflare.com/workers/runtime-apis/bindings/rate-limit/)
   — 안정적인 사용자 식별 key 권장, namespace 공유, 10/60초 window, colo 범위·eventual consistency.
3. [Session management](https://developers.cloudflare.com/cloudflare-one/access-controls/access-settings/session-management/)
   — 앱/정책/global 세션 우선순위, 재발급, revoke/logout, AJAX 401 헤더.
4. [Authorization cookie](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/authorization-cookie/)
   — HttpOnly/SameSite 설정. Strict는 로그인 redirect 반복을 유발할 수 있어 Lax를 선택했다.
5. [Application paths](https://developers.cloudflare.com/cloudflare-one/access-controls/policies/app-paths/)
   — 더 구체적인 정책이 우선하므로 호스트 전체 보호와 Bypass 누락 검사가 필요하다.
6. [Workers Secrets](https://developers.cloudflare.com/workers/configuration/secrets/)
   — 민감값은 vars에 넣지 않고 `.dev.vars*`/`.env*`를 git에서 제외한다.
7. [Custom Domains](https://developers.cloudflare.com/workers/configuration/routing/custom-domains/)
   및 [workers.dev](https://developers.cloudflare.com/workers/configuration/routing/workers-dev/)
   — hostname Access 및 Custom Domain route 설정. 공개 앱 때문에 Worker 전체 보호는 선택하지 않았다.

도메인 분리, 1시간, 60회/분은 이 프로젝트 규모에 맞춘 설계 판단이며 Cloudflare가 이 서비스에
강제하는 수치는 아니다. 플랜/좌석/zone 보유 여부와 실제 계정 설정은 배포 준비 시 확인한다.
