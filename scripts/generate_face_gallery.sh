#!/usr/bin/env bash
set -euo pipefail

workspace_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
raw_dir="$workspace_dir/build/face-gallery-raw"
gallery_dir="$workspace_dir/docs/face-gallery"
themes="classic_mosque,midnight_blue,desert_sand,royal_purple,ocean_teal,elegant_charcoal,emerald_garden,burgundy_elegance,sapphire_night,amber_sunset"

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

if [[ "${FACE_GALLERY_CAPTURE:-true}" == "true" ]]; then
    FACE_PREVIEW_RESOLUTIONS=1280x720 \
    FACE_PREVIEW_FACES=classic,horizon \
    FACE_PREVIEW_LANGUAGES=en,ta,si \
    FACE_PREVIEW_THEMES="$themes" \
    FACE_PREVIEW_SCENARIOS=digital,analog,azan-hour,bayan,countdown,dua \
    FACE_PREVIEW_VERIFY_UI=false \
    FACE_PREVIEW_SETTLE_SECONDS="${FACE_GALLERY_SETTLE_SECONDS:-0.75}" \
        "$workspace_dir/scripts/capture_face_scenarios.sh" "$raw_dir"
fi

mkdir -p "$gallery_dir"

face_name() {
    case "$1" in
        classic) echo "Classic" ;;
        horizon) echo "Horizon" ;;
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
    esac
}

IFS=',' read -r -a theme_ids <<< "$themes"
for language in en ta si; do
    language_name="$(printf '%s' "$language" | tr '[:lower:]' '[:upper:]')"
    for theme in "${theme_ids[@]}"; do
        for face in classic horizon; do
            prefix="$raw_dir/${face}-${theme}"
            output="$gallery_dir/$(face_name "$face")-$(theme_name "$theme")-$language_name.png"
            montage \
                -label "Digital" "$prefix-digital-$language-1280x720.png" \
                -label "Analog" "$prefix-analog-$language-1280x720.png" \
                -label "Azan under one hour" "$prefix-azan-hour-$language-1280x720.png" \
                -label "Friday bayan" "$prefix-bayan-$language-1280x720.png" \
                -label "Countdown" "$prefix-countdown-$language-1280x720.png" \
                -label "Dua" "$prefix-dua-$language-1280x720.png" \
                -font "$gallery_font" \
                -pointsize 22 -fill white -background '#101010' \
                -thumbnail 640x360 -tile 3x2 -geometry +6+6 "$output"
            optimized_output="${output%.png}.optimized.png"
            magick "$output" -strip -colors 256 "PNG8:$optimized_output"
            mv "$optimized_output" "$output"
            echo "$output"
        done
    done
done
