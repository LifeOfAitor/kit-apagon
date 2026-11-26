#!/usr/bin/env bash
# Development only: extracts the map of one municipality from the latest Protomaps build and installs
# it in the app on a connected phone or emulator, until the app can download packages by itself.
# Works only with debug builds: the file is pushed to the app's external files directory and a
# debug-only receiver (TestPackageImportReceiver) moves it into private storage. `adb shell run-as` is avoided on purpose,
# because some systems such as crDroid block it.
#
# Usage:
#   scripts/install-test-package.sh                         installs Irun (ES-20045)
#   scripts/install-test-package.sh PACKAGE_ID BOUNDING_BOX  BOUNDING_BOX is west,south,east,north
#
# Requires: pmtiles (brew install pmtiles), python3 and adb from the Android SDK.

set -euo pipefail

readonly APPLICATION_ID="io.github.lifeofaitor.kitapagon"
readonly PUSHED_PACKAGES_DIRECTORY="/sdcard/Android/data/${APPLICATION_ID}/files/municipal-packages"
readonly IMPORT_ACTION="${APPLICATION_ID}.IMPORT_TEST_PACKAGES"
readonly IMPORT_RECEIVER="${APPLICATION_ID}/.app.development.TestPackageImportReceiver"
readonly MAXIMUM_ZOOM=14
readonly BUILDS_INDEX_URL="https://build-metadata.protomaps.dev/builds.json"
readonly BUILDS_BASE_URL="https://build.protomaps.com"
# Irun plus a 3 km margin, as measured in docs/investigacion/05-paquetes-municipales-y-datos-oficiales.md.
readonly DEFAULT_PACKAGE_ID="ES-20045"
readonly DEFAULT_BOUNDING_BOX="-1.88930,43.25947,-1.69223,43.38024"

package_id="${1:-$DEFAULT_PACKAGE_ID}"
bounding_box="${2:-$DEFAULT_BOUNDING_BOX}"
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
    echo "Extracting ${package_id} from ${build_url}"
    pmtiles extract "$build_url" "$output_file" --bbox="$bounding_box" --maxzoom="$MAXIMUM_ZOOM"
}

request_import() {
    "$adb_command" shell am broadcast -a "$IMPORT_ACTION" -n "$IMPORT_RECEIVER" > /dev/null
}

install_on_device() {
    local map_file="$1"
    # The first call makes the app create the directory as its own; the second imports the map.
    request_import
    "$adb_command" push "$map_file" "${PUSHED_PACKAGES_DIRECTORY}/${package_id}.pmtiles"
    request_import
    echo "Installed ${package_id} in ${APPLICATION_ID}. Open the map again to see it."
}

main() {
    # Global on purpose: the EXIT trap runs after main returns, when local variables no longer exist.
    working_directory="$(mktemp -d)"
    trap 'rm -rf "$working_directory"' EXIT
    extract_map "${working_directory}/map.pmtiles"
    install_on_device "${working_directory}/map.pmtiles"
}

main
