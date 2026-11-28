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
# when it is missing or older than a week, and checks its MD5 checksum before keeping it.
# It is called inside $(...), where set -e does not stop a function, so each step checks its result.
geofabrik_extract() {
    local region="$1"
    local region_name extract_file extract_url
    region_name="$(basename "$region")"
    extract_file="${EXTRACT_CACHE_DIRECTORY}/${region_name}-latest.osm.pbf"
    extract_url="${GEOFABRIK_BASE_URL}/${region}/${region_name}-latest.osm.pbf"
    mkdir -p "$EXTRACT_CACHE_DIRECTORY" || return 1
    if [[ -z "$(find "$extract_file" -mtime "-${EXTRACT_MAXIMUM_AGE_DAYS}" 2>/dev/null)" ]]; then
        echo "Downloading the OpenStreetMap extract ${region}" >&2
        # A partial download never takes the place of a good extract.
        download "$extract_url" "${extract_file}.part" || return 1
        download "${extract_url}.md5" "${extract_file}.md5" || return 1
        verify_md5 "${extract_file}.part" "$(cut -d' ' -f1 "${extract_file}.md5")" || return 1
        mv "${extract_file}.part" "$extract_file" || return 1
    fi
    echo "$extract_file"
}

# Also repeats refused connections, which curl does not repeat by default: Geofabrik sometimes refuses
# the shared addresses of GitHub runners (docs/investigacion/10-publicacion-mensual-de-los-paquetes.md).
download() {
    local url="$1"
    local output_file="$2"
    curl --fail --silent --show-error --location --retry 3 --retry-connrefused \
        --retry-delay "${DOWNLOAD_RETRY_DELAY_SECONDS:-30}" --output "$output_file" "$url" \
        || { echo "Could not download ${url}" >&2; return 1; }
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

# Writes the two inputs of the generator, in the line by line format of osmium, into a directory:
# boundaries.geojsonseq with the administrative boundaries (decision 0013) and places.geojsonseq with
# the candidate places (decision 0011).
export_generator_inputs() {
    local extract_file="$1"
    local output_directory="$2"
    mkdir -p "$output_directory"
    osmium tags-filter --overwrite "$extract_file" r/boundary=administrative -o "${output_directory}/boundaries.osm.pbf"
    osmium export --overwrite --geometry-types=polygon -f geojsonseq \
        "${output_directory}/boundaries.osm.pbf" -o "${output_directory}/boundaries.geojsonseq"
    filter_place_candidates "$extract_file" "${output_directory}/candidates.osm.pbf"
    osmium export --overwrite --add-unique-id=type_id --geometry-types=point,polygon -f geojsonseq \
        "${output_directory}/candidates.osm.pbf" -o "${output_directory}/places.geojsonseq"
    rm -f "${output_directory}/boundaries.osm.pbf" "${output_directory}/candidates.osm.pbf"
}

# Overpass refuses requests that do not say who sends them (investigation 10, section 14). The
# identification is the public address of the data repository, as decided with the owner (decision 0016).
readonly OVERPASS_URL="https://overpass-api.de/api/interpreter"
readonly OVERPASS_USER_AGENT="kit-apagon-datos (+https://github.com/LifeOfAitor/kit-apagon-datos)"
# More missing municipalities than this means a broken extract, not a few broken boundaries.
readonly MAXIMUM_OVERPASS_QUERIES=20

# Writes the relations with an INE code, with every way and node they need to be drawn.
overpass_relations_by_ine_code() {
    local code="$1"
    local output_file="$2"
    curl --fail --silent --show-error --max-time 180 --retry 2 --retry-connrefused \
        --retry-delay "${DOWNLOAD_RETRY_DELAY_SECONDS:-30}" --user-agent "$OVERPASS_USER_AGENT" --output "$output_file" \
        --data-urlencode "data=[out:xml][timeout:120];relation[\"ine:municipio\"=\"${code}\"];(._;>;);out body;" "$OVERPASS_URL"
}

# Completes from Overpass the boundaries of the official municipalities that the extract could not give,
# and appends them to the boundaries file (decision 0016). A municipality Overpass does not give stays
# without a package that month; only a failure of the generator stops the script. The provinces, such
# as 01,20,48, limit the check to the ones a regional extract covers; empty means all of them.
complete_missing_boundaries() {
    local generator="$1"
    local boundaries_file="$2"
    local official_file="$3"
    local overpass_directory="$4"
    local provinces="${5:-}"
    local missing_codes code
    missing_codes="$("$generator" missing-boundaries --boundaries "$boundaries_file" --official "$official_file" \
        ${provinces:+--provinces "$provinces"})" || return 1
    if [[ -z "$missing_codes" ]]; then
        echo "Every official municipality has its boundary in the extract." >&2
        return 0
    fi
    if [[ "$(wc -w <<< "$missing_codes")" -gt "$MAXIMUM_OVERPASS_QUERIES" ]]; then
        echo "More than ${MAXIMUM_OVERPASS_QUERIES} municipalities without a boundary: Overpass is not asked." >&2
        return 0
    fi
    mkdir -p "$overpass_directory" || return 1
    : > "${overpass_directory}/relations.geojsonseq"
    for code in $missing_codes; do
        echo "Asking Overpass for the boundary of ${code}" >&2
        if overpass_relations_by_ine_code "$code" "${overpass_directory}/${code}.osm" \
            && osmium export --overwrite --geometry-types=polygon -f geojsonseq \
                "${overpass_directory}/${code}.osm" -o "${overpass_directory}/${code}.geojsonseq"; then
            cat "${overpass_directory}/${code}.geojsonseq" >> "${overpass_directory}/relations.geojsonseq"
        else
            echo "Overpass did not give the boundary of ${code}; it stays without a package." >&2
        fi
    done
    "$generator" complete-boundaries --boundaries "$boundaries_file" --official "$official_file" \
        --overpass "${overpass_directory}/relations.geojsonseq" --output "${overpass_directory}/completed.geojsonseq" \
        ${provinces:+--provinces "$provinces"} || return 1
    cat "${overpass_directory}/completed.geojsonseq" >> "$boundaries_file"
}
