#!/usr/bin/env bash
set -euo pipefail
collect() {
  mkdir -p screenshots
  adb pull /sdcard/Download/pmddcam-tests/ screenshots/ || true
  adb logcat -d -s AndroidRuntime > screenshots/runtime-log.txt || true
}
trap collect EXIT
adb shell cmd connectivity airplane-mode enable
adb shell svc wifi disable
adb shell svc data disable
./gradlew connectedDebugAndroidTest --no-daemon
