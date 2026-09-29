#!/usr/bin/env bash
set -euo pipefail
collect() {
  mkdir -p screenshots
  adb pull /sdcard/Download/pmddcam-tests/ screenshots/ || true
  adb logcat -d -s AndroidRuntime > screenshots/runtime-log.txt || true
}
trap collect EXIT
./gradlew connectedDebugAndroidTest --no-daemon
