#!/bin/bash
# Builds the app and runs it, the normal way: Gradle build + install, then launch on the emulator or phone.
# Usage: tools/run.sh   (run from the project root)
set -o pipefail
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
SDK="$HOME/Library/Android/sdk"; ADB="$SDK/platform-tools/adb"

if ! "$ADB" devices | grep -qw device; then
  echo "No device found, starting the emulator (ForestFlow_API36)..."
  nohup "$SDK/emulator/emulator" -avd ForestFlow_API36 -no-snapshot -no-boot-anim >/dev/null 2>&1 &
  "$ADB" wait-for-device
  until [ "$("$ADB" shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 3; done
fi

echo "Building and installing..."
./gradlew :app:installDebug || { echo "BUILD FAILED, fix the errors above and run again."; exit 1; }

"$ADB" shell monkey -p forest.flow -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 && echo "Launched Forest Flow."
