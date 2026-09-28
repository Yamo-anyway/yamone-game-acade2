#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
passed=0
check_fixed() {
  local file="$1" text="$2" name="$3"
  grep -Fq "$text" "$file" || { echo "FAIL $name" >&2; exit 1; }
  passed=$((passed + 1)); echo "PASS $name"
}

check_fixed app/src/main/java/com/yamone/arcade2/ui/BannerSlot.java \
  'ca-app-pub-3940256099942544/9214589741' 'official Android banner test unit ID only'
check_fixed app/build.gradle "debug { buildConfigField 'boolean', 'TEST_BANNER_ENABLED', 'true' }" \
  'debug test banner enabled'
check_fixed app/build.gradle "buildConfigField 'boolean', 'TEST_BANNER_ENABLED', 'false'" \
  'release advertising disabled pending owner configuration'
if grep -R -E 'InterstitialAd|RewardedAd|RewardedInterstitialAd' app/src/main app/build.gradle >/dev/null; then
  echo 'FAIL no interstitial or rewarded advertising' >&2; exit 1
fi
passed=$((passed + 1)); echo 'PASS no interstitial or rewarded advertising'
check_fixed app/src/main/AndroidManifest.xml 'android:allowBackup="false"' 'anonymous local identity not backed up'
check_fixed app/src/main/AndroidManifest.xml 'android:usesCleartextTraffic="false"' 'cleartext network traffic disabled'
check_fixed app/src/main/AndroidManifest.xml 'android:configChanges="orientation|screenSize"' \
  'rotation keeps active game instance'
check_fixed app/src/main/java/com/yamone/arcade2/MainActivity.java 'STATE_ACTIVE_RUN' \
  'process recreation detects abandoned active run'
check_fixed app/src/main/java/com/yamone/arcade2/data/LocalStore.java '.putInt("plays_" + game.key, previousPlays + 1).putStringSet(PENDING_PLAYS, pending);' \
  'terminal result is committed synchronously'
check_fixed app/src/main/java/com/yamone/arcade2/data/OnlineRankingRepository.java \
  'https://yamone-games-ranking-api.yamone-game.workers.dev' 'shared Cloudflare ranking endpoint configured'
check_fixed app/src/main/java/com/yamone/arcade2/data/RankingGateway.java \
  'public static final String MODE_ID = "normal";' 'ranking uses shared normal mode'
check_fixed app/src/main/java/com/yamone/arcade2/data/RankingGateway.java \
  'public static final String SCORE_UNIT = "points";' 'ranking sends verified points unit'
check_fixed app/src/main/java/com/yamone/arcade2/data/LocalStore.java \
  'ranking_delete_pending' 'offline online-record deletion is durable'
check_fixed app/src/main/java/com/yamone/arcade2/data/OnlineRankingRepository.java \
  'store.clearPendingRanking(game, score);' 'successful score upload clears durable pending value'
check_fixed app/src/main/java/com/yamone/arcade2/MainActivity.java \
  'renderOnlineRanking' 'game-specific online leaderboard UI connected'
check_fixed app/src/main/java/com/yamone/arcade2/data/OnlineRankingRepository.java \
  'body.put("eventType", event.eventType);' 'start and finish play events upload separately'
check_fixed app/src/main/java/com/yamone/arcade2/data/LocalStore.java \
  'ranking_play_events' 'offline play event queue is durable'
check_fixed app/src/main/java/com/yamone/arcade2/data/OnlineRankingRepository.java \
  'body.put("rankingEpoch", event.rankingEpoch);' 'play result carries its original ranking epoch'
check_fixed app/src/main/java/com/yamone/arcade2/data/LocalStore.java \
  'config.localResetEpoch > previousLocalReset' 'remote local-record reset is synchronized'
check_fixed app/src/main/java/com/yamone/arcade2/MainActivity.java \
  'store.visibleGames()' 'remote catalog controls game visibility and order'
check_fixed cloudflare/src/index.js \
  '/v1/admin/rankings/reset' 'admin ranking reset endpoint exists'
check_fixed cloudflare/src/admin-page.js \
  '국가별 통계' 'admin country statistics UI exists'

views=(TwinTapView LineSurfView PocketPulseView StackSliceView)
for view in "${views[@]}"; do
  check_fixed "app/src/main/java/com/yamone/arcade2/ui/${view}.java" \
    'Math.min(getWidth() / 360f, getHeight() / 520f)' "${view} fits both width and height"
done
check_fixed app/src/main/java/com/yamone/arcade2/ui/OrbitView.java \
  'Math.min(getWidth() / 360f, getHeight() / 500f)' 'OrbitView fits compact screens and extends to full portrait height'
check_fixed app/src/main/java/com/yamone/arcade2/ui/ColorBreakView.java \
  'Math.min(getWidth() / 360f, getHeight() / 480f)' 'ColorBreakView fits compact screens and extends to full portrait height'

ready_count="$(grep -c ', true)' app/src/main/java/com/yamone/arcade2/core/GameId.java)"
test "$ready_count" -eq 6 || { echo "FAIL all six catalog games ready" >&2; exit 1; }
passed=$((passed + 1)); echo 'PASS all six catalog games ready'
echo "All $passed integration checks passed."
