#!/usr/bin/env bash
set -euo pipefail

workspace_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
raw_dir="${FACE_GALLERY_RAW_DIR:-$workspace_dir/build/face-gallery-raw}"
gallery_dir="$workspace_dir/docs/face-gallery"
faces="${FACE_GALLERY_FACES:-classic,horizon,ufuq,safaa,noor,sahar,kaash,mihrab}"
languages="${FACE_GALLERY_LANGUAGES:-en,ta,si}"
# Leave unset to use each face's own canonical palette (see face_theme below). Set to a
# comma-separated theme list to sweep every selected face through those themes instead.
theme_override="${FACE_GALLERY_THEMES:-}"
face_preview_resolutions="${FACE_GALLERY_RESOLUTIONS:-current}"

if ! command -v montage >/dev/null 2>&1 || ! command -v magick >/dev/null 2>&1; then
    echo "ImageMagick's montage and magick commands are required" >&2
    exit 1
fi

gallery_font=""
for candidate in \
    /System/Library/Fonts/Supplemental/Arial.ttf \
    /usr/share/fonts/truetype/dejavu/DejaVuSans.ttf; do
    if [[ -f "$candidate" ]]; then
        gallery_font="$candidate"
        break
    fi
done
if [[ -z "$gallery_font" ]]; then
    echo "A TrueType font (Arial or DejaVu Sans) is required" >&2
    exit 1
fi

face_name() {
    case "$1" in
        classic) echo "Classic" ;;
        horizon) echo "Horizon" ;;
        ufuq) echo "Ufuq" ;;
        safaa) echo "Safaa" ;;
        noor) echo "Noor" ;;
        sahar) echo "Sahar" ;;
        kaash) echo "Kaash" ;;
        mihrab) echo "Mihrab" ;;
    esac
}

# Each face's canonical palette — mirrors ScreenFace.recommendedColorThemeId in Kotlin.
face_theme() {
    case "$1" in
        classic) echo "classic_mosque" ;;
        horizon) echo "slate_steel" ;;
        ufuq) echo "horizon_dusk" ;;
        safaa) echo "notice_paper" ;;
        noor) echo "amber_matrix" ;;
        sahar) echo "daylight_ivory" ;;
        kaash) echo "ink_saffron" ;;
        mihrab) echo "sand_terracotta" ;;
    esac
}

theme_name() {
    case "$1" in
        classic_mosque) echo "Classic" ;;
        midnight_blue) echo "Midnight" ;;
        desert_sand) echo "Desert" ;;
        royal_purple) echo "Royal" ;;
        ocean_teal) echo "Ocean" ;;
        elegant_charcoal) echo "Elegant" ;;
        emerald_garden) echo "Emerald" ;;
        burgundy_elegance) echo "Burgundy" ;;
        sapphire_night) echo "Sapphire" ;;
        amber_sunset) echo "Amber" ;;
        daylight_ivory) echo "Daylight-Ivory" ;;
        morning_mist) echo "Morning-Mist" ;;
        parchment_sepia) echo "Parchment-Sepia" ;;
        sand_terracotta) echo "Sand-Terracotta" ;;
        slate_steel) echo "Slate-Steel" ;;
        forest_moss) echo "Forest-Moss" ;;
        rose_copper) echo "Rose-Copper" ;;
        ink_saffron) echo "Ink-Saffron" ;;
        jewel_amethyst) echo "Jewel-Amethyst" ;;
        mono_contrast) echo "Mono-Contrast" ;;
        horizon_dusk) echo "Horizon-Dusk" ;;
        notice_paper) echo "Notice-Paper" ;;
        amber_matrix) echo "Amber-Matrix" ;;
    esac
}

mkdir -p "$gallery_dir"

# First resolution in the matrix — matches capture_face_scenarios.sh's filename suffix rule
# (empty for "current", "-WIDTHxHEIGHT" otherwise) so the montage step can find the raw captures.
resolution="${face_preview_resolutions%%,*}"
if [[ "$resolution" == "current" ]]; then
    resolution_suffix=""
else
    resolution_suffix="-$resolution"
fi

IFS=',' read -r -a face_ids <<< "$faces"
IFS=',' read -r -a language_ids <<< "$languages"

if [[ "${FACE_GALLERY_CAPTURE:-true}" == "true" ]]; then
    rm -rf "$raw_dir"
    mkdir -p "$raw_dir"
    FACE_PREVIEW_RESOLUTIONS="$face_preview_resolutions" \
    FACE_PREVIEW_FACES="$faces" \
    FACE_PREVIEW_LANGUAGES="$languages" \
    FACE_PREVIEW_THEMES="$theme_override" \
    FACE_PREVIEW_SCENARIOS=digital,analog,azan-hour,iqamah-hour,bayan,countdown,dua,after-isha,iqamah-countdown,friday-night-bayan \
    FACE_PREVIEW_VERIFY_UI=false \
    FACE_PREVIEW_SETTLE_SECONDS="${FACE_GALLERY_SETTLE_SECONDS:-1.5}" \
    FACE_PREVIEW_CLEAN=false \
        "$workspace_dir/scripts/capture_face_scenarios.sh" "$raw_dir"
fi

for language in "${language_ids[@]}"; do
    language_name="$(printf '%s' "$language" | tr '[:lower:]' '[:upper:]')"
    for face in "${face_ids[@]}"; do
        IFS=',' read -r -a face_themes <<< "${theme_override:-$(face_theme "$face")}"
        for theme in "${face_themes[@]}"; do
            prefix="$raw_dir/${face}-${theme}"
            output="$gallery_dir/$(face_name "$face")-$(theme_name "$theme")-$language_name.png"
            montage \
                -label "Digital" "$prefix-digital-$language$resolution_suffix.png" \
                -label "Analog" "$prefix-analog-$language$resolution_suffix.png" \
                -label "Azan under one hour" "$prefix-azan-hour-$language$resolution_suffix.png" \
                -label "Iqamah waiting" "$prefix-iqamah-hour-$language$resolution_suffix.png" \
                -label "Friday bayan" "$prefix-bayan-$language$resolution_suffix.png" \
                -label "Countdown" "$prefix-countdown-$language$resolution_suffix.png" \
                -label "Dua" "$prefix-dua-$language$resolution_suffix.png" \
                -label "Next-day Fajr" "$prefix-after-isha-$language$resolution_suffix.png" \
                -label "Iqamah countdown" "$prefix-iqamah-countdown-$language$resolution_suffix.png" \
                -label "Friday night bayan" "$prefix-friday-night-bayan-$language$resolution_suffix.png" \
                -font "$gallery_font" \
                -pointsize 22 -fill white -background '#101010' \
                -thumbnail 640x360 -tile 5x2 -geometry +6+6 "$output"
            optimized_output="${output%.png}.optimized.png"
            magick "$output" -strip -alpha off -define png:compression-level=9 "$optimized_output"
            mv "$optimized_output" "$output"
            echo "$output"
        done
    done
done
