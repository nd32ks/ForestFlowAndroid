#!/bin/bash
# Waits for the phone (USB debugging on, screen unlocked), then runs the full instrumented suite on it
# (debug and the R8-minified release build) and installs the release build.
# usage: tools/phone_verify.sh [serial]
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
A=~/Library/Android/sdk/platform-tools/adb
S=${1:-001481515000878}
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
until $A devices | grep -q "^$S	device"; do sleep 3; done; echo "phone connected $(date +%T)"
until [ "$($A -s $S shell dumpsys window | grep -c 'isKeyguardShowing=false')" -ge 1 ]; do sleep 2; done; echo "unlocked"
( while true; do $A -s $S shell input keyevent KEYCODE_SHIFT_LEFT >/dev/null 2>&1; sleep 5; done ) & KEEP=$!
for V in Debug Release; do
  EXTRA=""; [ $V = Release ] && EXTRA="-PtestRelease"
  ANDROID_SERIAL=$S ./gradlew -q connected${V}AndroidTest $EXTRA > "$TMPDIR/ff_phone_$V.log" 2>&1; e=$?
  D=app/build.nosync/outputs/androidTest-results/connected/$(echo $V | tr A-Z a-z)
  echo "phone $V exit=$e: $(find $D -name '*.xml' -print0 | xargs -0 grep -h -o '<testcase name="[^"]*"' | sort -u | wc -l) tests, $(find $D -name '*.xml' -print0 | xargs -0 grep -h -c '<failure' | paste -sd+ - | bc) failures"
  find $D -name '*.xml' -print0 | xargs -0 grep -h -B2 -A3 '<failure' | grep -E 'testcase|<failure' | cut -c1-200 | head -4
done
kill $KEEP 2>/dev/null; wait $KEEP 2>/dev/null
./gradlew -q assembleRelease >/dev/null 2>&1; $A -s $S install -r app/build.nosync/outputs/apk/release/app-release.apk | tail -1
echo "PHONE VERIFY DONE"
