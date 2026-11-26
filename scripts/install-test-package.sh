#!/usr/bin/env bash
# Development only: builds the package of one municipality (map and places) and installs it in the
# app on a connected phone or emulator, until the app can download packages by itself.
#   - Map: extracted from the latest Protomaps build.
#   - Places: OpenStreetMap data from the Geofabrik extract of the region, converted by
#     tools:package-generator (decision 0011).
# Works only with debug builds: the files are pushed to the app's external files directory and a
# debug-only receiver (TestPackageImportReceiver) moves them into private storage. `adb shell run-as`
# is avoided on purpose, because some systems such as crDroid block it.
#
# Usage:
#   scripts/install-test-package.sh                                        installs Irun (ES-20045)
#   scripts/install-test-package.sh PACKAGE_ID BOUNDING_BOX GEOFABRIK_REGION
#     BOUNDING_BOX is west,south,east,north; GEOFABRIK_REGION is a path such as europe/spain/pais-vasco
#
# Requires: pmtiles and osmium (brew install pmtiles osmium-tool), python3, and adb from the Android SDK.

set -euo pipefail

readonly APPLICATION_ID="io.github.lifeofaitor.kitapagon"
readonly PUSHED_PACKAGES_DIRECTORY="/sdcard/Android/data/${APPLICATION_ID}/files/municipal-packages"
readonly IMPORT_ACTION="${APPLICATION_ID}.IMPORT_TEST_PACKAGES"
readonly IMPORT_RECEIVER="${APPLICATION_ID}/.app.development.TestPackageImportReceiver"
readonly MAXIMUM_ZOOM=14
readonly BUILDS_INDEX_URL="https://build-metadata.protomaps.dev/builds.json"
readonly BUILDS_BASE_URL="https://build.protomaps.com"
readonly GEOFABRIK_BASE_URL="https://download.geofabrik.de"
# Regional extracts are reused for a week instead of being downloaded every time.
readonly EXTRACT_CACHE_DIRECTORY="${HOME}/.cache/kit-apagon/osm"
readonly EXTRACT_MAXIMUM_AGE_DAYS=7
# Irun plus a 3 km margin, as measured in docs/investigacion/05-paquetes-municipales-y-datos-oficiales.md.
readonly DEFAULT_PACKAGE_ID="ES-20045"
readonly DEFAULT_BOUNDING_BOX="-1.88930,43.25947,-1.69223,43.38024"
readonly DEFAULT_GEOFABRIK_REGION="europe/spain/pais-vasco"

package_id="${1:-$DEFAULT_PACKAGE_ID}"
bounding_box="${2:-$DEFAULT_BOUNDING_BOX}"
geofabrik_region="${3:-$DEFAULT_GEOFABRIK_REGION}"
repository_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
adb_command="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"

latest_build_url() {
    curl --fail --silent --show-error "$BUILDS_INDEX_URL" \
        | python3 -I -c 'import json, sys; print(json.load(sys.stdin)[-1]["key"])' \
        | sed "s|^|${BUILDS_BASE_URL}/|"
}

extract_map() {
    local output_file="$1"
    local build_url
    build_url="$(latest_build_url)"
    echo "Extracting the map of ${package_id} from ${build_url}"
    pmtiles extract "$build_url" "$output_file" --bbox="$bounding_box" --maxzoom="$MAXIMUM_ZOOM"
}

# Downloads the regional extract when it is missing or older than a week, and checks its MD5 checksum.
regional_extract() {
    local region_name extract_file extract_url
    region_name="$(basename "$geofabrik_region")"
    extract_file="${EXTRACT_CACHE_DIRECTORY}/${region_name}-latest.osm.pbf"
    extract_url="${GEOFABRIK_BASE_URL}/${geofabrik_region}/${region_name}-latest.osm.pbf"
    mkdir -p "$EXTRACT_CACHE_DIRECTORY"
    if [[ -z "$(find "$extract_file" -mtime "-${EXTRACT_MAXIMUM_AGE_DAYS}" 2>/dev/null)" ]]; then
        echo "Downloading the OpenStreetMap extract ${geofabrik_region}" >&2
        curl --fail --silent --show-error --location --output "$extract_file" "$extract_url"
        curl --fail --silent --show-error --location --output "${extract_file}.md5" "${extract_url}.md5"
        verify_md5 "$extract_file" "$(cut -d' ' -f1 "${extract_file}.md5")"
    fi
    echo "$extract_file"
}

verify_md5() {
    local file="$1"
    local expected_checksum="$2"
    local actual_checksum
    actual_checksum="$(python3 -I -c 'import hashlib, sys; print(hashlib.md5(open(sys.argv[1], "rb").read()).hexdigest())' "$file")"
    if [[ "$actual_checksum" != "$expected_checksum" ]]; then
        rm -f "$file"
        echo "The checksum of ${file} does not match; the download was removed." >&2
        exit 1
    fi
}

# The filter is deliberately broad: the generator decides which places belong in the package.
extract_places() {
    local output_file="$1"
    local extract_file
    extract_file="$(regional_extract)"
    echo "Extracting the places of ${package_id}"
    osmium extract --overwrite --bbox "$bounding_box" --strategy smart "$extract_file" \
        -o "${working_directory}/area.osm.pbf"
    osmium tags-filter --overwrite "${working_directory}/area.osm.pbf" nwr/amenity nwr/healthcare nwr/emergency \
        -o "${working_directory}/candidates.osm.pbf"
    osmium export --overwrite --add-unique-id=type_id --geometry-types=point,polygon \
        "${working_directory}/candidates.osm.pbf" -o "${working_directory}/candidates.geojson" -f geojson
    "${repository_directory}/gradlew" -q -p "$repository_directory" :tools:package-generator:run \
        --args="places ${working_directory}/candidates.geojson ${output_file}"
}

request_import() {
    "$adb_command" shell am broadcast -a "$IMPORT_ACTION" -n "$IMPORT_RECEIVER" > /dev/null
}

install_on_device() {
    local map_file="$1"
    local places_file="$2"
    # The first call makes the app create the directory as its own; the second imports the files.
    request_import
    "$adb_command" push "$map_file" "${PUSHED_PACKAGES_DIRECTORY}/${package_id}.pmtiles"
    "$adb_command" push "$places_file" "${PUSHED_PACKAGES_DIRECTORY}/${package_id}.places.json"
    request_import
    echo "Installed ${package_id} in ${APPLICATION_ID}. Open the map again to see it."
}

main() {
    # Global on purpose: the EXIT trap runs after main returns, when local variables no longer exist.
    working_directory="$(mktemp -d)"
    trap 'rm -rf "$working_directory"' EXIT
    extract_map "${working_directory}/map.pmtiles"
    extract_places "${working_directory}/places.json"
    install_on_device "${working_directory}/map.pmtiles" "${working_directory}/places.json"
}

main
