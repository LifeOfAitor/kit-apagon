#!/usr/bin/env bash
# Development only: builds on this computer the packages of every municipality of a region and their
# signed index, the same steps the monthly job of the data repository will run for all of Spain
# (decision 0013, part 3a). Nothing is published, and the index is signed with a temporary key.
#
# Usage:
#   scripts/build-region-packages.sh                                 the Basque Country (01, 20 and 48)
#   scripts/build-region-packages.sh GEOFABRIK_REGION PROVINCES       for example europe/spain/navarra 31
#
# Writes to build/region-packages/:
#   provinces/CODE/   the two files of each package and the index part of the province
#   published/        index.json, index.json.sig and the temporary public key that checks it
#
# Requires: pmtiles and osmium (brew install pmtiles osmium-tool), openssl, python3 and curl.

set -euo pipefail

readonly DEFAULT_GEOFABRIK_REGION="europe/spain/pais-vasco"
readonly DEFAULT_PROVINCES="01,20,48"
readonly ADDRESS_TEMPLATE="https://github.com/LifeOfAitor/kit-apagon-datos/releases/download/{data_version}-ES-{province}/{file}"

geofabrik_region="${1:-$DEFAULT_GEOFABRIK_REGION}"
provinces="${2:-$DEFAULT_PROVINCES}"
data_version="$(date +%Y-%m-%d)"
repository_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
output_directory="${repository_directory}/build/region-packages"
generator="${repository_directory}/tools/package-generator/build/install/package-generator/bin/package-generator"
# shellcheck source=lib/data-sources.sh
source "${repository_directory}/scripts/lib/data-sources.sh"

step() {
    echo "[$(date +%H:%M:%S)] $*"
}

install_generator() {
    step "Building the package generator"
    "${repository_directory}/gradlew" -q -p "$repository_directory" :tools:package-generator:installDist
}

export_osm_data() {
    local extract_file
    extract_file="$(geofabrik_extract "$geofabrik_region")"
    step "Exporting municipal boundaries and places from $(basename "$extract_file")"
    osmium tags-filter --overwrite "$extract_file" r/boundary=administrative -o "${working_directory}/boundaries.osm.pbf"
    osmium export --overwrite --geometry-types=polygon -f geojsonseq \
        "${working_directory}/boundaries.osm.pbf" -o "${working_directory}/boundaries.geojsonseq"
    filter_place_candidates "$extract_file" "${working_directory}/candidates.osm.pbf"
    osmium export --overwrite --add-unique-id=type_id --geometry-types=point,polygon -f geojsonseq \
        "${working_directory}/candidates.osm.pbf" -o "${working_directory}/places.geojsonseq"
}

# The map is cut from an archive that reaches the margins of the border municipalities
# (docs/investigacion/09-paquetes-de-toda-espana.md).
extract_source_map() {
    local region_area build_url
    region_area="$("$generator" region-area --boundaries "${working_directory}/boundaries.geojsonseq" --provinces "$provinces")"
    build_url="$(latest_protomaps_build_url)"
    step "Extracting ${region_area} from ${build_url}"
    pmtiles extract "$build_url" "${working_directory}/source.pmtiles" --bbox="$region_area" --maxzoom="$MAXIMUM_ZOOM"
}

build_provinces() {
    local province
    mkdir -p "${working_directory}/parts"
    for province in ${provinces//,/ }; do
        step "Building the packages of province ${province}"
        "$generator" build-packages \
            --boundaries "${working_directory}/boundaries.geojsonseq" \
            --places "${working_directory}/places.geojsonseq" \
            --map "${working_directory}/source.pmtiles" \
            --provinces "$province" \
            --data-version "$data_version" \
            --address-template "$ADDRESS_TEMPLATE" \
            --output "${output_directory}/provinces/${province}"
        cp "${output_directory}/provinces/${province}/index-part.json" "${working_directory}/parts/${province}.json"
    done
}

# A temporary P-256 key: the real one will only exist in the secrets of the data repository.
sign_index() {
    step "Signing the index with a temporary key"
    openssl ecparam -name prime256v1 -genkey -noout -out "${working_directory}/signing-key.pem"
    openssl pkey -in "${working_directory}/signing-key.pem" -pubout -out "${output_directory}/published/temporary-public-key.pem"
    KIT_APAGON_INDEX_SIGNING_KEY="$(openssl pkcs8 -topk8 -nocrypt -outform DER -in "${working_directory}/signing-key.pem" | base64 | tr -d '\n')" \
        "$generator" sign-index --parts "${working_directory}/parts" --output "${output_directory}/published"
}

# Checked with other tools than the ones that wrote them, as the app will check them.
verify_output() {
    step "Checking the signature with openssl"
    openssl dgst -sha256 -verify "${output_directory}/published/temporary-public-key.pem" \
        -signature "${output_directory}/published/index.json.sig" "${output_directory}/published/index.json"
    step "Checking the size and SHA-256 of every file in the index"
    python3 -I - "${output_directory}/published/index.json" "${output_directory}/provinces" <<'PYTHON'
import hashlib, json, os, sys
index_path, provinces_directory = sys.argv[1], sys.argv[2]
index = json.load(open(index_path))
checked = 0
for package in index["packages"]:
    for listed in package["files"]:
        path = os.path.join(provinces_directory, package["province_code"], listed["name"])
        content = open(path, "rb").read()
        if len(content) != listed["size"] or hashlib.sha256(content).hexdigest() != listed["sha256"]:
            sys.exit("Mismatch in " + path)
        checked += 1
print("%d files of %d packages match the index" % (checked, len(index["packages"])))
PYTHON
}

main() {
    # Global on purpose: the EXIT trap runs after main returns, when local variables no longer exist.
    working_directory="$(mktemp -d)"
    trap 'rm -rf "$working_directory"' EXIT
    rm -rf "$output_directory"
    mkdir -p "${output_directory}/published"
    install_generator
    export_osm_data
    extract_source_map
    build_provinces
    sign_index
    verify_output
    step "Done: ${output_directory}"
}

main
