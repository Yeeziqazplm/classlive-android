#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa
# UI tests run the same dex/resources without the ARM-only engine on x86.
# The delivered APK above remains the full ARM64 build.
mkdir -p app/build/ui-test
cp app/build/outputs/apk/debug/app-debug.apk app/build/ui-test/source.apk
zip -d app/build/ui-test/source.apk 'lib/*'
"$ANDROID_HOME/build-tools/34.0.0/zipalign" -f 4 app/build/ui-test/source.apk app/build/ui-test/ui.apk
"$ANDROID_HOME/build-tools/34.0.0/apksigner" sign --ks .ci-signing/debug.keystore --ks-pass pass:android app/build/ui-test/ui.apk
adb install -r app/build/ui-test/ui.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
for size in 390 360 430 landscape; do
  if [ "$size" = landscape ]; then adb shell wm size 844x390; else adb shell wm size "${size}x844"; fi
  adb shell wm density 160
  if [ "$size" = 360 ]; then adb shell settings put system font_scale 1.3; else adb shell settings put system font_scale 1.0; fi
  adb shell am force-stop com.yeezi.classlive.v1
  adb shell am instrument -w -e suffix "-$size" com.yeezi.classlive.v1.test/com.yeezi.classlive.UiRegression | tee "qa/ui-$size.txt"
  adb pull /sdcard/Android/data/com.yeezi.classlive.v1/files/qa qa/screenshots
  grep -q CLASSLIVE_UI_PASS "qa/ui-$size.txt"
done
