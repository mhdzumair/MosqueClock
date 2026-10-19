#!/usr/bin/env bash
set -euo pipefail

workspace_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
output_dir="${1:-$workspace_dir/build/face-screenshots}"
resolution_matrix="${FACE_PREVIEW_RESOLUTIONS:-current}"
face_matrix="${FACE_PREVIEW_FACES:-classic,horizon,ufuq,safaa,noor,sahar,kaash,mihrab}"
scenario_matrix="${FACE_PREVIEW_SCENARIOS:-digital,analog,azan-hour,iqamah-hour,countdown,iqamah-countdown,friday-night-bayan,dua,bayan}"
language_matrix="${FACE_PREVIEW_LANGUAGES:-en,ta,si}"
# Leave unset to use each face's own canonical palette (see face_theme below). Set to a
# comma-separated theme list to sweep every selected face through those themes instead.
theme_override="${FACE_PREVIEW_THEMES:-}"
verify_ui="${FACE_PREVIEW_VERIFY_UI:-true}"
settle_seconds="${FACE_PREVIEW_SETTLE_SECONDS:-3}"
clean_output_dir="${FACE_PREVIEW_CLEAN:-true}"
package_name="com.mosque.prayerclock.debug"
preview_activity="$package_name/com.mosque.prayerclock.FacePreviewActivity"

# Each face's canonical palette — mirrors ScreenFace.recommendedColorThemeId in Kotlin.
face_theme() {
    case "$1" in
        classic) echo "classic_mosque" ;;
        horizon) echo "slate_steel" ;;
        horizon_mobile) echo "slate_steel" ;;
        ufuq) echo "horizon_dusk" ;;
        safaa) echo "notice_paper" ;;
        noor) echo "amber_matrix" ;;
        sahar) echo "daylight_ivory" ;;
        kaash) echo "ink_saffron" ;;
        mihrab) echo "sand_terracotta" ;;
    esac
}

if ! command -v adb >/dev/null 2>&1; then
    echo "adb is required" >&2
    exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
    echo "No Android device or emulator is connected" >&2
    exit 1
fi

if [[ "$clean_output_dir" == "true" ]]; then
    rm -rf "$output_dir"
fi
mkdir -p "$output_dir"
if [[ "${FACE_PREVIEW_INSTALL:-true}" == "true" ]]; then
    "$workspace_dir/gradlew" :app:installPlaystoreDebug >/dev/null
fi

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
        --es mazhab "${FACE_PREVIEW_MAZHAB:-shafi}" \
        --es theme "$theme" \
        --ez show24Hour "${FACE_PREVIEW_SHOW_24_HOUR:-false}" \
        --ez showSeconds "${FACE_PREVIEW_SHOW_SECONDS:-true}" \
        --ez showAzan "${FACE_PREVIEW_SHOW_AZAN:-true}" \
        --ez showWeather "${FACE_PREVIEW_SHOW_WEATHER:-true}" >/dev/null
    case "$scenario" in
        digital|analog|azan-hour|iqamah-hour|after-isha|sunrise-hour|ishraq-hour|loading|error|error-analog)
            if [[ "$face" == "horizon_mobile" ]]; then
                expected_text="Dickwella Jummah Mosque"
            elif [[ "$face" == *mobile ]]; then
                expected_text="1448"
            else
                expected_text="Dickwella Jummah Mosque"
            fi ;;
        countdown|iqamah-countdown|friday-night-bayan|sunrise-countdown|ishraq-countdown) expected_text="03" ;;
        dua) expected_text="اللَّهُمَّ" ;;
        bayan) expected_text="42:44" ;;
    esac
    if [[ "$verify_ui" == "true" ]]; then
        scene_found=false
        # Capture only after the localized target scene exists.
        for _ in 1 2 3 4 5; do
            sleep 1
            adb shell uiautomator dump /sdcard/face-preview.xml >/dev/null 2>&1 || true
            if adb shell cat /sdcard/face-preview.xml 2>/dev/null | grep -Fiq "$expected_text"; then
                scene_found=true
                break
            fi
        done
        if [[ "$scene_found" != "true" ]]; then
            echo "Preview did not render: $face / $scenario / $language / $resolution" >&2
            adb logcat -b crash -d -t 80 >&2
            exit 1
        fi
    fi
    # Let Compose finish all draw passes after a locale-triggered recreation. In particular,
    # capturing an analogue Canvas too early can produce a valid PNG containing only that layer.
    sleep "$settle_seconds"
    if ! adb shell pidof "$package_name" >/dev/null; then
        echo "Preview crashed: $face / $scenario / $language / $resolution" >&2
        adb logcat -b crash -d -t 80 >&2
        exit 1
    fi
    if [[ "$verify_ui" == "true" ]]; then
        adb exec-out screencap -p >/dev/null
        sleep 1
    fi
    adb exec-out screencap -p > "$output_file"
    echo "$output_file"
    if [[ "${FACE_PREVIEW_CAPTURE_SCROLL:-false}" == "true" && "$face" == "horizon_mobile" ]]; then
        local screen_size screen_width screen_height
        screen_size="$(adb shell wm size | awk '/size:/ {size=$NF} END {print size}')"
        screen_width="${screen_size%x*}"
        screen_height="${screen_size#*x}"
        for _ in 1 2 3; do
            adb shell input swipe "$((screen_width / 2))" "$((screen_height * 4 / 5))" \
                "$((screen_width / 2))" "$((screen_height / 5))" 300
        done
        sleep "$settle_seconds"
        adb exec-out screencap -p > "${output_file%.png}-scrolled.png"
        echo "${output_file%.png}-scrolled.png"
    fi
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
    for language in "${languages[@]}"; do
        for face in "${faces[@]}"; do
            if [[ -n "$theme_override" ]]; then
                IFS=',' read -r -a face_themes <<< "$theme_override"
            else
                face_themes=("$(face_theme "$face")")
            fi
            for theme in "${face_themes[@]}"; do
                for scenario in "${scenarios[@]}"; do
                    capture "$face" "$scenario" "$language" "$theme"
                done
            done
        done
    done
done
