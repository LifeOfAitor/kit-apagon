#!/usr/bin/env bash
# Development only: builds on this computer the packages of every municipality of a region and their
# signed index, the same steps the monthly job of the data repository will run for all of Spain
# (decisions 0013 and 0014). Nothing is published, and the index is signed with a temporary key.
#
# Usage:
#   scripts/build-region-packages.sh                                 the Basque Country (01, 20 and 48)
#   scripts/build-region-packages.sh GEOFABRIK_REGION PROVINCES       for example europe/spain/navarra 31
#
# Writes to build/region-packages/:
#   provinces/CODE/   the tar of the province, with the two files of each package, and its index part
#   published/        index.json, index.json.sig, the temporary public key that checks it and the
#                     report of the comparison with the official list of the INE
#
# Requires: pmtiles and osmium (brew install pmtiles osmium-tool), openssl, tar, python3 and curl.

set -euo pipefail

readonly DEFAULT_GEOFABRIK_REGION="europe/spain/pais-vasco"
readonly DEFAULT_PROVINCES="01,20,48"
readonly ADDRESS_TEMPLATE="https://github.com/LifeOfAitor/kit-apagon-datos/releases/download/{data_version}-ES-{province}/{file}"
readonly OFFICIAL_MUNICIPALITIES="tools/package-generator/data/municipios-ine-2026.csv"

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
    export_generator_inputs "$extract_file" "$working_directory"
}

check_municipalities() {
    step "Comparing the municipalities with the official list of the INE"
    "$generator" check-municipalities \
        --boundaries "${working_directory}/boundaries.geojsonseq" \
        --official "${repository_directory}/${OFFICIAL_MUNICIPALITIES}" \
        --provinces "$provinces" \
        --report "${output_directory}/published/informe.txt"
}

# The map is cut from an archive that reaches the margins of the border municipalities
# (docs/investigacion/09-paquetes-de-toda-espana.md).
extract_source_map() {
    local region_area build_url
    region_area="$("$generator" region-area --boundaries "${working_directory}/boundaries.geojsonseq" \
        --official "${repository_directory}/${OFFICIAL_MUNICIPALITIES}" --provinces "$provinces")"
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
            --official "${repository_directory}/${OFFICIAL_MUNICIPALITIES}" \
            --places "${working_directory}/places.geojsonseq" \
            --map "${working_directory}/source.pmtiles" \
            --provinces "$province" \
            --data-version "$data_version" \
            --address-template "$ADDRESS_TEMPLATE" \
            --output "${output_directory}/provinces/${province}"
        cp "${output_directory}/provinces/${province}/index-part.json" "${working_directory}/parts/${province}.json"
    done
}

# A temporary key made with the same command the owner uses once for the real one, which will only
# exist in the secrets of the data repository.
sign_index() {
    local keys_directory="${working_directory}/keys"
    step "Signing the index with a temporary key"
    "$generator" new-signing-key --output "$keys_directory" > /dev/null
    cp "${keys_directory}/clave-publica-del-indice.txt" "${output_directory}/published/temporary-public-key.txt"
    KIT_APAGON_INDEX_SIGNING_KEY="$(cat "${keys_directory}/clave-privada-del-indice.txt")" \
        "$generator" sign-index --parts "${working_directory}/parts" \
        --public-key "${keys_directory}/clave-publica-del-indice.txt" --output "${output_directory}/published"
}

# Checked with other tools than the ones that wrote them, as the app will check them.
verify_output() {
    step "Checking the signature with openssl"
    openssl base64 -d -A -in "${output_directory}/published/temporary-public-key.txt" -out "${working_directory}/public-key.der"
    openssl pkey -pubin -inform DER -in "${working_directory}/public-key.der" -out "${working_directory}/public-key.pem"
    openssl dgst -sha256 -verify "${working_directory}/public-key.pem" \
        -signature "${output_directory}/published/index.json.sig" "${output_directory}/published/index.json"
    step "Checking every tar with the tar tool of the system"
    find "${output_directory}/provinces" -name '*.tar' -print0 | xargs -0 -n 1 tar -tf | wc -l | sed 's/^ */files in the tars: /'
    step "Checking the position, size and SHA-256 of every file in the index"
    python3 -I - "${output_directory}/published/index.json" "${output_directory}/provinces" <<'PYTHON'
import hashlib, json, os, sys
index_path, provinces_directory = sys.argv[1], sys.argv[2]
index = json.load(open(index_path))
checked = 0
for package in index["packages"]:
    for listed in package["files"]:
        bundle_name = listed["urls"][0].rsplit("/", 1)[1]
        path = os.path.join(provinces_directory, package["province_code"], bundle_name)
        with open(path, "rb") as bundle:
            bundle.seek(listed["offset"])
            content = bundle.read(listed["size"])
        if len(content) != listed["size"] or hashlib.sha256(content).hexdigest() != listed["sha256"]:
            sys.exit("Mismatch for " + listed["name"] + " in " + path)
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
    check_municipalities
    extract_source_map
    build_provinces
    sign_index
    verify_output
    step "Done: ${output_directory}"
}

main
