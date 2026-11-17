#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}" && git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"
source scripts/common-test.sh
finish_arg_parsing

[[ $CHEAP_ONLY -eq 1 ]] && exit 0

# The smoke-tests app's androidTest APK holds ShowcasesTest, the guide tests (src/guidesTest) and the editor
# component tests (sources/editor/src/componentTest). The nightly runs it on the CI emulator.
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

# Run tests and generate the coverage report separately so a failing test
# doesn't block report generation (AGP flushes the .ec regardless). Capture the
# test exit code and re-raise it at the end.
TEST_EXIT=0
dry_runnable apps/cesdk_android/gradlew -p apps/cesdk_android \
  smoke-tests-app:connectedDebugAndroidTest -PtestOrchestrator || TEST_EXIT=$?

COVERAGE_EXIT=0
dry_runnable scripts/coverage/cesdk_android_lcov.sh \
  "${REPO_ROOT}/apps/cesdk_android/smoke-tests-app/build/outputs/code_coverage/debugAndroidTest/connected" \
  apps/cesdk_android/coverage/lcov.info || COVERAGE_EXIT=$?

# A test run that fails before any test leaves no coverage data, so its exit code goes first.
if [[ "${TEST_EXIT}" -ne 0 ]]; then
  exit "${TEST_EXIT}"
fi
exit "${COVERAGE_EXIT}"
