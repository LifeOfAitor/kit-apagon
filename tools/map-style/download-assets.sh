#!/usr/bin/env bash
# Downloads the fonts and sprites the offline map styles need (decision 0010), from a pinned commit
# of protomaps/basemaps-assets. Fonts: SIL Open Font License 1.1. Sprites: MIT.
# Only the glyph ranges for Spanish, Catalan, Basque and Galician are included, to keep the app small.

set -euo pipefail

readonly ASSETS_COMMIT="028c18f713baecad011301ff7a69acc39bcc2ae7"
readonly BASE_URL="https://raw.githubusercontent.com/protomaps/basemaps-assets/${ASSETS_COMMIT}"
# The sprites derive from tangrams/icons, whose MIT license must travel with them.
readonly ICONS_LICENSE_URL="https://raw.githubusercontent.com/tangrams/icons/92510779634f4a006c61ea70e50cb8c52c765a81/LICENSE.md"
readonly FONTS=("Noto Sans Regular" "Noto Sans Medium" "Noto Sans Italic")
readonly GLYPH_RANGES=("0-255" "256-511" "8192-8447")
readonly SPRITE_FILES=("light.json" "light.png" "light@2x.json" "light@2x.png" "dark.json" "dark.png" "dark@2x.json" "dark@2x.png")

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly OUTPUT_DIRECTORY="${script_directory}/../../feature/map/src/main/assets/map"

download() {
    local remote_path="$1"
    local local_file="$2"
    mkdir -p "$(dirname "$local_file")"
    curl --fail --silent --show-error --location --output "$local_file" "${BASE_URL}/${remote_path// /%20}"
}

download_fonts() {
    local font range
    for font in "${FONTS[@]}"; do
        for range in "${GLYPH_RANGES[@]}"; do
            download "fonts/${font}/${range}.pbf" "${OUTPUT_DIRECTORY}/fonts/${font}/${range}.pbf"
        done
    done
    download "fonts/OFL.txt" "${OUTPUT_DIRECTORY}/fonts/OFL.txt"
}

download_sprites() {
    local sprite_file
    for sprite_file in "${SPRITE_FILES[@]}"; do
        download "sprites/v4/${sprite_file}" "${OUTPUT_DIRECTORY}/sprites/${sprite_file}"
    done
    curl --fail --silent --show-error --location \
        --output "${OUTPUT_DIRECTORY}/sprites/LICENSE-tangrams-icons.md" "$ICONS_LICENSE_URL"
}

download_fonts
download_sprites
echo "Map fonts and sprites downloaded to ${OUTPUT_DIRECTORY}"
