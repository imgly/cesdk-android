#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}" && git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"
source scripts/common-test.sh
finish_arg_parsing

[[ $CHEAP_ONLY -eq 1 ]] && exit 0

# Smoke-tests in apps/cesdk_android/smoke-tests-app/src/androidTest/ drive the
# editor UI (ShowcasesTest, GuidesTest). CI runs them on Firebase Test Lab.
if [[ -z "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ]] || ! command -v adb &>/dev/null; then
  >&2 echo "apps/cesdk_android: Android SDK not on PATH"
  >&2 echo "  Install via: ./scripts/dev/install-android-sdk.sh"
  exit 1
fi

if ! adb devices | grep -q "device$"; then
  >&2 echo "apps/cesdk_android: no connected Android device/emulator"
  >&2 echo "  Boot via: ./scripts/dev/setup-android-emulator.sh"
  exit 1
fi

dry_runnable apps/cesdk_android/gradlew -p apps/cesdk_android smoke-tests-app:connectedDebugAndroidTest

# TODO(coverage): wire Jacoco → lcov conversion. See bindings/android/test.sh
# for the same TODO — the two share infrastructure and should ship together.
>&2 echo "apps/cesdk_android: coverage collection not yet wired up (see TODO in test.sh)"
