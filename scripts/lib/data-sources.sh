#!/usr/bin/env bash
# Shared by the development scripts: where the map and the OpenStreetMap data come from.
#   - Map: the latest daily build of Protomaps, read in place by `pmtiles extract`.
#   - OpenStreetMap: regional extracts of Geofabrik, kept for a week instead of being downloaded every time.
# Sourced, not run: it only defines constants and functions.

readonly PROTOMAPS_BUILDS_INDEX_URL="https://build-metadata.protomaps.dev/builds.json"
readonly PROTOMAPS_BUILDS_BASE_URL="https://build.protomaps.com"
readonly GEOFABRIK_BASE_URL="https://download.geofabrik.de"
readonly EXTRACT_CACHE_DIRECTORY="${HOME}/.cache/kit-apagon/osm"
readonly EXTRACT_MAXIMUM_AGE_DAYS=7
# Decision 0006: tiles up to zoom 14, enlarged by MapLibre beyond that.
readonly MAXIMUM_ZOOM=14

latest_protomaps_build_url() {
    curl --fail --silent --show-error "$PROTOMAPS_BUILDS_INDEX_URL" \
        | python3 -I -c 'import json, sys; print(json.load(sys.stdin)[-1]["key"])' \
        | sed "s|^|${PROTOMAPS_BUILDS_BASE_URL}/|"
}

# Prints the path of the extract of a Geofabrik region such as europe/spain/pais-vasco. Downloads it
# when it is missing or older than a week, and checks its MD5 checksum.
geofabrik_extract() {
    local region="$1"
    local region_name extract_file extract_url
    region_name="$(basename "$region")"
    extract_file="${EXTRACT_CACHE_DIRECTORY}/${region_name}-latest.osm.pbf"
    extract_url="${GEOFABRIK_BASE_URL}/${region}/${region_name}-latest.osm.pbf"
    mkdir -p "$EXTRACT_CACHE_DIRECTORY"
    if [[ -z "$(find "$extract_file" -mtime "-${EXTRACT_MAXIMUM_AGE_DAYS}" 2>/dev/null)" ]]; then
        echo "Downloading the OpenStreetMap extract ${region}" >&2
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

# The filter is deliberately broad: the generator decides which places belong in a package (decision 0011).
filter_place_candidates() {
    local input_file="$1"
    local output_file="$2"
    osmium tags-filter --overwrite "$input_file" nwr/amenity nwr/healthcare nwr/emergency -o "$output_file"
}
