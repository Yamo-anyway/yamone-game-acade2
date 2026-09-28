#!/usr/bin/env bash
set -euo pipefail
# This is a new CI emulator, not a user's device. Keep fixtures and app requests offline.
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
mkdir -p ui-preview
capture() {
  adb shell am force-stop com.yamone.arcade2
  adb shell am instrument -w -e scenario "$1" com.yamone.arcade2.test/com.yamone.arcade2.UiSmokeInstrumentation | tee "ui-preview/$1.log"
  grep -q 'UI_SMOKE_OK' "ui-preview/$1.log"
}
adb shell wm size 1080x2400
adb shell wm density 420
capture standard
adb shell wm size 720x1280
adb shell wm density 320
adb shell settings put system font_scale 1.3
capture large-text
adb pull /sdcard/Android/data/com.yamone.arcade2/files/ui-preview/. ui-preview/
