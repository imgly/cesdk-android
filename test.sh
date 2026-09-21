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

# Run tests and generate the coverage report separately so a failing test
# doesn't block report generation (AGP flushes the .ec regardless). Capture the
# test exit code and re-raise it at the end.
TEST_EXIT=0
dry_runnable apps/cesdk_android/gradlew -p apps/cesdk_android \
  smoke-tests-app:connectedDebugAndroidTest || TEST_EXIT=$?

# `-x ...connectedDebugAndroidTest` reuses the existing .ec instead of re-running.
dry_runnable apps/cesdk_android/gradlew -p apps/cesdk_android \
  smoke-tests-app:createDebugAndroidTestCoverageReport \
  -x smoke-tests-app:connectedDebugAndroidTest

# One --source-root per editor module src dir so jacoco_xml_to_lcov.py resolves
# <package>/<file> to apps/cesdk_android/sources/ paths the --filter matches.
JACOCO_XML="apps/cesdk_android/smoke-tests-app/build/reports/coverage/androidTest/debug/connected/report.xml"
if [[ -f "${JACOCO_XML}" ]]; then
  SOURCE_ROOT_ARGS=()
  for module in apps/cesdk_android/sources/*/; do
    # Skip *-dummy modules (substituted for the real one in the app), whose
    # sources would duplicate the real module's under the same package.
    [[ "${module}" == *-dummy/ ]] && continue
    for src in "${module}src/main/java" "${module}src/main/kotlin"; do
      [[ -d "$src" ]] && SOURCE_ROOT_ARGS+=(--source-root "$src")
    done
  done
  dry_runnable mkdir -p apps/cesdk_android/coverage
  dry_runnable python3 scripts/coverage/jacoco_xml_to_lcov.py \
    "${JACOCO_XML}" apps/cesdk_android/coverage/lcov.info \
    "${SOURCE_ROOT_ARGS[@]}" \
    --filter "apps/cesdk_android/sources/"
fi

exit "${TEST_EXIT}"
