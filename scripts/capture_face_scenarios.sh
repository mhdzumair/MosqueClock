#!/usr/bin/env bash
set -euo pipefail

workspace_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
output_dir="${1:-$workspace_dir/build/face-screenshots}"
resolution_matrix="${FACE_PREVIEW_RESOLUTIONS:-current}"
face_matrix="${FACE_PREVIEW_FACES:-horizon,classic}"
scenario_matrix="${FACE_PREVIEW_SCENARIOS:-digital,analog,azan-hour,countdown,dua,bayan}"
language_matrix="${FACE_PREVIEW_LANGUAGES:-en,ta,si}"
theme_matrix="${FACE_PREVIEW_THEMES:-classic_mosque}"
verify_ui="${FACE_PREVIEW_VERIFY_UI:-true}"
settle_seconds="${FACE_PREVIEW_SETTLE_SECONDS:-3}"
package_name="com.mosque.prayerclock.debug"
preview_activity="$package_name/com.mosque.prayerclock.FacePreviewActivity"

if ! command -v adb >/dev/null 2>&1; then
    echo "adb is required" >&2
    exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
    echo "No Android device or emulator is connected" >&2
    exit 1
fi

rm -rf "$output_dir"
mkdir -p "$output_dir"
"$workspace_dir/gradlew" :app:installPlaystoreDebug >/dev/null

original_size="$(adb shell wm size | awk '/Override size:/ {print $3; found=1} END {if (!found) print "reset"}')"
restore_display_size() {
    if [[ "$original_size" == "reset" ]]; then
        adb shell wm size reset >/dev/null
    else
        adb shell wm size "$original_size" >/dev/null
    fi
}
trap restore_display_size EXIT

resolution_suffix=""

capture() {
    local face="$1"
    local scenario="$2"
    local language="$3"
    local theme="$4"
    local output_file="$output_dir/${face}-${theme}-${scenario}-${language}${resolution_suffix}.png"

    adb shell am force-stop "$package_name"
    adb shell am start -W \
        -n "$preview_activity" \
        --es face "$face" \
        --es scenario "$scenario" \
        --es language "$language" \
        --es theme "$theme" >/dev/null
    case "$scenario" in
        digital|analog|azan-hour) expected_text="Dickwella Jummah Mosque" ;;
        countdown) expected_text="03:46" ;;
        dua) expected_text="اللَّهُمَّ" ;;
        bayan) expected_text="42:44" ;;
    esac
    if [[ "$verify_ui" == "true" ]]; then
        # Capture only after the localized target scene exists.
        for _ in 1 2 3 4 5; do
            sleep 1
            adb shell uiautomator dump /sdcard/face-preview.xml >/dev/null 2>&1 || true
            if adb shell cat /sdcard/face-preview.xml 2>/dev/null | grep -Fq "$expected_text"; then
                break
            fi
        done
    fi
    # Let Compose finish all draw passes after a locale-triggered recreation. In particular,
    # capturing an analogue Canvas too early can produce a valid PNG containing only that layer.
    sleep "$settle_seconds"
    if [[ "$verify_ui" == "true" ]]; then
        adb exec-out screencap -p >/dev/null
        sleep 1
    fi
    adb exec-out screencap -p > "$output_file"
    echo "$output_file"
}

IFS=',' read -r -a resolutions <<< "$resolution_matrix"
for resolution in "${resolutions[@]}"; do
    if [[ "$resolution" == "current" ]]; then
        resolution_suffix=""
        restore_display_size
    elif [[ "$resolution" =~ ^[0-9]+x[0-9]+$ ]]; then
        adb shell wm size "$resolution" >/dev/null
        resolution_suffix="-$resolution"
    else
        echo "Invalid resolution '$resolution' (use current or WIDTHxHEIGHT)" >&2
        exit 1
    fi

    IFS=',' read -r -a faces <<< "$face_matrix"
    IFS=',' read -r -a scenarios <<< "$scenario_matrix"
    IFS=',' read -r -a languages <<< "$language_matrix"
    IFS=',' read -r -a themes <<< "$theme_matrix"
    for language in "${languages[@]}"; do
        for theme in "${themes[@]}"; do
            for face in "${faces[@]}"; do
                for scenario in "${scenarios[@]}"; do
                    capture "$face" "$scenario" "$language" "$theme"
                done
            done
        done
    done
done
