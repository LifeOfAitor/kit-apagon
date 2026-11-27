#!/usr/bin/env bash
# The monthly publication of the packages of all of Spain (decision 0014). It is run by the workflow
# of the data repository kit-apagon-datos on a GitHub runner, from a fixed commit of this repository.
#
#   1. Deletes what a failed run of the same day left, and stops if the day is already published.
#   2. Exports boundaries and places from the Geofabrik extracts of Spain and of the Canary Islands.
#   3. Compares the municipalities with the official list of the INE.
#   4. Province by province: cuts the map from the Protomaps build of the day, builds the tar and
#      publishes it as a prerelease, then deletes it from the disk.
#   5. Signs the index, checking it with the public key of the app.
#   6. Publishes the index: a commit in the data repository, a draft release with index.json and its
#      signature, and only at the end the switch that makes it the latest release. Then deletes the
#      releases older than the last two versions.
#
# A failure at any step ends the script before the index is published, so phones keep the version
# they had. With PUBLISH_INDEX=false the index stays in a draft release, which is how a trial run of a
# few provinces is checked without phones ever seeing it.
#
# Environment:
#   DATA_REPOSITORY                owner/name of the data repository, such as LifeOfAitor/kit-apagon-datos
#   PUBLISH_INDEX                  true to publish the index as the new version, false for a trial run
#   PROVINCES                      optional, such as 20 or 01,20,48; only allowed in trial runs
#   DATA_CHECKOUT                  working copy of the data repository, needed to publish the index
#   KIT_APAGON_INDEX_SIGNING_KEY   private key of the index, read by the generator
#   GH_TOKEN                       token gh uses; in the workflow, the automatic token of the run
#
# Requires: gh, osmium, pmtiles, curl, python3, git and the package generator built with
#   ./gradlew :tools:package-generator:installDist

set -euo pipefail

readonly SPAIN_EXTRACT="europe/spain"
readonly CANARY_ISLANDS_EXTRACT="africa/canary-islands"
readonly KEPT_VERSIONS=2
readonly UPLOAD_ATTEMPTS=3
readonly COMMIT_AUTHOR_NAME="github-actions[bot]"
readonly COMMIT_AUTHOR_EMAIL="41898282+github-actions[bot]@users.noreply.github.com"
readonly DATA_ATTRIBUTION="Datos © OpenStreetMap (ODbL) y Protomaps."

repository_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=lib/data-sources.sh
source "${repository_directory}/scripts/lib/data-sources.sh"

# Overridable so that the tests can replace the generator and fix the date.
generator="${GENERATOR:-${repository_directory}/tools/package-generator/build/install/package-generator/bin/package-generator}"
public_key_file="${INDEX_PUBLIC_KEY_FILE:-${repository_directory}/tools/package-generator/data/clave-publica-del-indice.txt}"
official_municipalities="${repository_directory}/tools/package-generator/data/municipios-ine-2026.csv"
data_version="${DATA_VERSION:-$(date -u +%Y-%m-%d)}"
retry_wait_seconds="${RETRY_WAIT_SECONDS:-30}"

step() {
    echo "[$(date -u +%H:%M:%S)] $*"
}

fail() {
    echo "$*" >&2
    exit 1
}

check_configuration() {
    [[ -n "${DATA_REPOSITORY:-}" ]] || fail "DATA_REPOSITORY must name the data repository."
    [[ "${PUBLISH_INDEX:-}" == "true" || "${PUBLISH_INDEX:-}" == "false" ]] || fail "PUBLISH_INDEX must be true or false."
    # An index with only some provinces would hide every other municipality from the app.
    if [[ "$PUBLISH_INDEX" == "true" && -n "${PROVINCES:-}" ]]; then
        fail "A run limited to some provinces is a trial: it cannot publish the index."
    fi
    if [[ -n "${PROVINCES:-}" && ! "$PROVINCES" =~ ^[0-9]{2}(,[0-9]{2})*$ ]]; then
        fail "PROVINCES must be two digit codes separated by commas, such as 01,20,48."
    fi
    if [[ "$PUBLISH_INDEX" == "true" && ! -d "${DATA_CHECKOUT:-}/.git" ]]; then
        fail "DATA_CHECKOUT must be a working copy of the data repository to publish the index."
    fi
    [[ -x "$generator" ]] || fail "The package generator is not built: ${generator}"
    [[ -f "$public_key_file" ]] || fail "The public key of the app is missing: ${public_key_file}"
    local tool
    for tool in gh osmium pmtiles curl python3 git; do
        command -v "$tool" > /dev/null || fail "${tool} is not installed."
    done
}

list_releases() {
    gh release list --repo "$DATA_REPOSITORY" --limit 1000 --json tagName,isDraft,isPrerelease > "$1"
}

# Reads one tag per line and deletes each release with its tag.
delete_releases() {
    local tag
    while IFS= read -r tag; do
        [[ -n "$tag" ]] || continue
        step "Deleting release ${tag}"
        gh release delete "$tag" --repo "$DATA_REPOSITORY" --cleanup-tag --yes
    done
}

clean_leftovers() {
    step "Looking for what a failed run of ${data_version} left"
    list_releases "${work_directory}/releases-before.json"
    "$generator" release-leftovers --releases "${work_directory}/releases-before.json" --data-version "$data_version" \
        > "${work_directory}/leftovers.txt"
    delete_releases < "${work_directory}/leftovers.txt"
}

prepare_inputs() {
    local region region_directory extract_file
    for region in "$SPAIN_EXTRACT" "$CANARY_ISLANDS_EXTRACT"; do
        region_directory="${work_directory}/$(basename "$region")"
        # Assigned on its own line so that a failed download stops the script here.
        extract_file="$(geofabrik_extract "$region")"
        step "Exporting boundaries and places of ${region}"
        export_generator_inputs "$extract_file" "$region_directory"
    done
    cat "${work_directory}/spain/boundaries.geojsonseq" "${work_directory}/canary-islands/boundaries.geojsonseq" \
        > "${work_directory}/boundaries.geojsonseq"
    cat "${work_directory}/spain/places.geojsonseq" "${work_directory}/canary-islands/places.geojsonseq" \
        > "${work_directory}/places.geojsonseq"
}

check_municipalities() {
    "$generator" check-municipalities \
        --boundaries "${work_directory}/boundaries.geojsonseq" \
        --official "$official_municipalities" \
        --report "${work_directory}/informe.txt" \
        ${PROVINCES:+--provinces "$PROVINCES"}
}

provinces_to_build() {
    if [[ -n "${PROVINCES:-}" ]]; then
        tr ',' '\n' <<< "$PROVINCES"
    else
        "$generator" provinces --boundaries "${work_directory}/boundaries.geojsonseq"
    fi
}

# Repeats a command that can be repeated safely, waiting longer each time.
with_retries() {
    local attempt
    for ((attempt = 1; attempt <= UPLOAD_ATTEMPTS; attempt++)); do
        if "$@"; then
            return 0
        fi
        echo "Attempt ${attempt} of ${UPLOAD_ATTEMPTS} failed: $*" >&2
        [[ "$attempt" -lt "$UPLOAD_ATTEMPTS" ]] && sleep $((retry_wait_seconds * attempt))
    done
    return 1
}

# The map of a province reaches the margins of its border municipalities (investigation 09).
extract_province_map() {
    local build_url="$1"
    local province_area="$2"
    rm -f "${work_directory}/source.pmtiles"
    pmtiles extract "$build_url" "${work_directory}/source.pmtiles" --bbox="$province_area" --maxzoom="$MAXIMUM_ZOOM"
}

publish_province() {
    local province="$1"
    local build_url="$2"
    local tag="${data_version}-ES-${province}"
    local output_directory="${work_directory}/provinces/${province}"
    local province_area
    province_area="$("$generator" region-area --boundaries "${work_directory}/boundaries.geojsonseq" --provinces "$province")"
    step "Province ${province}: map of ${province_area}"
    with_retries extract_province_map "$build_url" "$province_area"
    step "Province ${province}: packages"
    "$generator" build-packages \
        --boundaries "${work_directory}/boundaries.geojsonseq" \
        --places "${work_directory}/places.geojsonseq" \
        --map "${work_directory}/source.pmtiles" \
        --provinces "$province" \
        --data-version "$data_version" \
        --address-template "https://github.com/${DATA_REPOSITORY}/releases/download/{data_version}-ES-{province}/{file}" \
        --output "$output_directory"
    step "Province ${province}: publishing ${tag}"
    gh release create "$tag" --repo "$DATA_REPOSITORY" --prerelease --title "Provincia ${province}, ${data_version}" \
        --notes "Paquetes de la provincia ${province}, versión ${data_version}. Se marca como preliminar a propósito: la versión vigente es la publicación que contiene index.json. ${DATA_ATTRIBUTION}"
    with_retries gh release upload "$tag" "$output_directory"/*.tar --repo "$DATA_REPOSITORY" --clobber
    mv "${output_directory}/index-part.json" "${work_directory}/parts/${province}.json"
    rm -rf "$output_directory" "${work_directory}/source.pmtiles"
}

sign_index() {
    "$generator" sign-index --parts "${work_directory}/parts" --public-key "$public_key_file" \
        --output "${work_directory}/published"
}

# The commit gives the release its own date, so that GitHub orders the versions, and keeps the
# repository active so that GitHub does not disable the scheduled workflow (investigation 10).
record_version() {
    local package_count="$1"
    local missing_count="$2"
    local versions_file="${DATA_CHECKOUT}/VERSIONES.md"
    [[ -f "$versions_file" ]] || printf '# Versiones publicadas\n\n' > "$versions_file"
    echo "- ${data_version}: ${package_count} paquetes; ${missing_count} municipios sin límite en OpenStreetMap." >> "$versions_file"
    git -C "$DATA_CHECKOUT" add VERSIONES.md
    git -C "$DATA_CHECKOUT" -c user.name="$COMMIT_AUTHOR_NAME" -c user.email="$COMMIT_AUTHOR_EMAIL" \
        commit --quiet -m "datos: publicar la versión ${data_version}"
    git -C "$DATA_CHECKOUT" push --quiet origin HEAD
}

write_index_notes() {
    local package_count="$1"
    {
        echo "Índice de los paquetes municipales de Kit Apagón del ${data_version}."
        echo
        echo "- Paquetes: ${package_count}. Los de cada provincia están en la publicación ${data_version}-ES-NN."
        echo "- index.json está firmado con la clave del proyecto (index.json.sig)."
        echo "- ${DATA_ATTRIBUTION}"
        echo
        cat "${work_directory}/informe.txt"
    } > "${work_directory}/notas.md"
}

create_index_draft() {
    local title="$1"
    shift
    gh release create "$data_version" --repo "$DATA_REPOSITORY" --draft --title "$title" \
        --notes-file "${work_directory}/notas.md" "$@" \
        "${work_directory}/published/index.json" "${work_directory}/published/index.json.sig" "${work_directory}/informe.txt"
}

publish_index() {
    local package_count="$1"
    local missing_count="$2"
    write_index_notes "$package_count"
    if [[ "$PUBLISH_INDEX" == "false" ]]; then
        step "Trial run: the index stays in a draft release that phones never see"
        create_index_draft "Prueba del ${data_version}, sin publicar"
        return
    fi
    step "Recording version ${data_version} in the data repository"
    record_version "$package_count" "$missing_count"
    local version_commit
    version_commit="$(git -C "$DATA_CHECKOUT" rev-parse HEAD)"
    create_index_draft "Datos del ${data_version}" --target "$version_commit"
    step "Publishing ${data_version} as the latest version"
    gh release edit "$data_version" --repo "$DATA_REPOSITORY" --draft=false --latest
    list_releases "${work_directory}/releases-after.json"
    "$generator" obsolete-releases --releases "${work_directory}/releases-after.json" --keep "$KEPT_VERSIONS" \
        > "${work_directory}/obsolete.txt"
    delete_releases < "${work_directory}/obsolete.txt"
}

value_of() {
    sed -n "s/^$1: //p" <<< "$2"
}

write_job_summary() {
    [[ -n "${GITHUB_STEP_SUMMARY:-}" ]] || return 0
    {
        echo "## Versión ${data_version}"
        echo
        cat "${work_directory}/informe.txt"
    } >> "$GITHUB_STEP_SUMMARY"
}

main() {
    check_configuration
    # Global on purpose: the EXIT trap runs after main returns, when local variables no longer exist.
    work_directory="$(mktemp -d)"
    trap 'rm -rf "$work_directory"' EXIT
    mkdir -p "${work_directory}/parts" "${work_directory}/published"
    step "Version ${data_version} of ${DATA_REPOSITORY}"
    clean_leftovers
    prepare_inputs
    step "Comparing the municipalities with the official list of the INE"
    local check_output build_url provinces province sign_output
    check_output="$(check_municipalities)"
    echo "$check_output"
    build_url="$(latest_protomaps_build_url)"
    provinces="$(provinces_to_build)"
    step "Map from ${build_url}"
    for province in $provinces; do
        publish_province "$province" "$build_url"
    done
    step "Signing the index"
    sign_output="$(sign_index)"
    echo "$sign_output"
    publish_index "$(value_of packages "$sign_output")" "$(value_of missing "$check_output")"
    write_job_summary
    step "Done"
}

main
