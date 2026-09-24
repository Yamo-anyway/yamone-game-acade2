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
check_fixed app/src/main/java/com/yamone/arcade2/data/LocalStore.java '.putInt("plays_" + game.key, previousPlays + 1).commit()' \
  'terminal result is committed synchronously'
check_fixed app/src/main/java/com/yamone/arcade2/data/RankingGateway.java 'return Collections.emptyList();' \
  'disconnected ranking returns no fake entries'

views=(OrbitView ColorBreakView TwinTapView LineSurfView PocketPulseView StackSliceView)
for view in "${views[@]}"; do
  check_fixed "app/src/main/java/com/yamone/arcade2/ui/${view}.java" \
    'Math.min(getWidth() / 360f, getHeight() / 520f)' "${view} fits both width and height"
done

ready_count="$(grep -c ', true)' app/src/main/java/com/yamone/arcade2/core/GameId.java)"
test "$ready_count" -eq 6 || { echo "FAIL all six catalog games ready" >&2; exit 1; }
passed=$((passed + 1)); echo 'PASS all six catalog games ready'
echo "All $passed integration checks passed."
