#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
APK_PATH="${REPO_ROOT}/app/build/outputs/apk/release/app-release.apk"

cd "${REPO_ROOT}"

echo "Building release APK..."
./gradlew :app:assembleRelease

if [[ ! -f "${APK_PATH}" ]]; then
    echo "Release build finished, but APK was not found: ${APK_PATH}" >&2
    exit 1
fi

echo
echo "Release APK:"
ls -lh "${APK_PATH}"
