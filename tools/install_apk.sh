#!/usr/bin/env bash
###
 # @author: BC
 # @date: 26/06/01
 # @lastEditTime: 26/06/08
 # @description: 
 # @note: 
 # @version: 0.1.0
### 

set -euo pipefail

case "${1:-}" in
    debug)
        apk_path="./app/build/outputs/apk/debug/app-debug.apk"
        ;;
    release)
        apk_path="./app/build/outputs/apk/release/app-release.apk"
        ;;
    *)
        echo "Usage: $0 {debug|release}" >&2
        exit 1
        ;;
esac

if [ ! -f "$apk_path" ]; then
    echo "APK not found: $apk_path" >&2
    exit 1
fi

adb install -r "$apk_path"
