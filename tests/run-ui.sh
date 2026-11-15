#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
for size in 390 360 430; do
  adb shell wm size "${size}x844"
  adb shell wm density 160
  if [ "$size" = 360 ]; then adb shell settings put system font_scale 1.3; else adb shell settings put system font_scale 1.0; fi
  adb shell am force-stop com.yeezi.classlive.offline
  adb shell am instrument -w -e suffix "-$size" com.yeezi.classlive.offline.test/com.yeezi.classlive.UiRegression | tee "qa/ui-$size.txt"
  adb pull /sdcard/Android/data/com.yeezi.classlive.offline/files/qa qa/screenshots
  grep -q CLASSLIVE_UI_PASS "qa/ui-$size.txt"
done
