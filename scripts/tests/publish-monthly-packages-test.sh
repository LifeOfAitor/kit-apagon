#!/usr/bin/env bash
# Tests for scripts/publish-monthly-packages.sh. gh, curl, osmium, pmtiles and the package generator
# are replaced by fakes that record every call; git is real, against a local bare repository that
# plays the data repository. Run by ./gradlew verifyAll.

set -uo pipefail

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_UNDER_TEST="${script_directory}/../publish-monthly-packages.sh"
readonly VERSION="2026-12-02"

failures=0
working_directory="$(mktemp -d)"
trap 'rm -rf "$working_directory"' EXIT

write_fakes() {
    local fakes="$1"
    mkdir -p "$fakes"

    # gh: records its arguments; lists the releases in FAKE_RELEASES; fails the first FAKE_FAILED_UPLOADS
    # uploads; like GitHub, has no tag for the drafts in FAKE_DRAFTS and refuses to delete a missing tag.
    cat > "${fakes}/gh" <<'EOF'
#!/usr/bin/env bash
echo "gh $*" >> "${FAKE_LOG}"
tag_exists() {
    [[ " ${FAKE_DRAFTS:-} " != *" ${1##*/} "* ]]
}
if [[ "$1" == "api" && "$2" == "--method" ]]; then
    tag_exists "$4" || { echo "HTTP 422: Reference does not exist" >&2; exit 1; }
elif [[ "$1" == "api" ]]; then
    tag_exists "$2" || { echo "HTTP 404: Not Found" >&2; exit 1; }
elif [[ "$1 $2" == "release delete" && " $* " == *" --cleanup-tag "* ]]; then
    tag_exists "$3" || { echo "HTTP 422: Reference does not exist" >&2; exit 1; }
elif [[ "$1 $2" == "release list" ]]; then
    echo "${FAKE_RELEASES:-[]}"
elif [[ "$1 $2" == "release upload" ]]; then
    uploads=$(( $(cat "${FAKE_LOG}.uploads" 2>/dev/null || echo 0) + 1 ))
    echo "$uploads" > "${FAKE_LOG}.uploads"
    [[ "$uploads" -gt "${FAKE_FAILED_UPLOADS:-0}" ]]
fi
EOF

    # curl: the list of Protomaps builds, a small extract with its MD5 checksum, or Overpass, which fails
    # when FAKE_OVERPASS_FAILS is true. Overpass calls are recorded.
    cat > "${fakes}/curl" <<'EOF'
#!/usr/bin/env bash
url="${*: -1}"
if [[ "$url" == *overpass-api.de* ]]; then
    echo "curl $*" >> "${FAKE_LOG}"
    [[ "${FAKE_OVERPASS_FAILS:-false}" == "true" ]] && { echo "curl: (22) The requested URL returned error: 504" >&2; exit 22; }
fi
output=""
while [[ $# -gt 0 ]]; do
    [[ "$1" == "--output" ]] && output="$2"
    shift
done
if [[ "$url" == *geofabrik* && "${FAKE_GEOFABRIK_REFUSES:-false}" == "true" ]]; then
    echo "curl: (7) Failed to connect to download.geofabrik.de port 443: Couldn't connect to server" >&2
    exit 7
elif [[ "$url" == *overpass-api.de* ]]; then
    echo "<osm/>" > "$output"
elif [[ "$url" == *builds.json ]]; then
    echo '[{"key":"20261201.pmtiles"},{"key":"20261202.pmtiles"}]'
elif [[ "$url" == *.md5 ]]; then
    echo "$(python3 -I -c 'import hashlib; print(hashlib.md5(b"extract\n").hexdigest())')  extract.osm.pbf" > "$output"
else
    echo "extract" > "$output"
fi
EOF

    # osmium and pmtiles: write their output file.
    cat > "${fakes}/osmium" <<'EOF'
#!/usr/bin/env bash
echo "osmium $*" >> "${FAKE_LOG}"
while [[ $# -gt 0 ]]; do
    [[ "$1" == "-o" ]] && echo "{}" > "$2"
    shift
done
EOF
    cat > "${fakes}/pmtiles" <<'EOF'
#!/usr/bin/env bash
echo "pmtiles $*" >> "${FAKE_LOG}"
echo "map" > "$3"
EOF

    # The generator: answers each command as the real one would, without doing the work.
    cat > "${fakes}/generator" <<'EOF'
#!/usr/bin/env bash
echo "generator $*" >> "${FAKE_LOG}"
command="$1"
shift
# Without associative arrays, which the bash of macOS does not have.
option() {
    local name="--$1"
    shift
    while [[ $# -gt 1 ]]; do
        [[ "$1" == "$name" ]] && { echo "$2"; return; }
        shift 2
    done
}
arguments=("$@")
case "$command" in
    release-leftovers)
        [[ "${FAKE_ALREADY_PUBLISHED:-false}" == "true" ]] && { echo "already published" >&2; exit 1; }
        printf '%s\n' ${FAKE_LEFTOVERS:-} ;;
    missing-boundaries)
        printf '%s\n' ${FAKE_MISSING:-} ;;
    complete-boundaries)
        echo "{}" > "$(option output "${arguments[@]}")"
        echo "completed: 1 of 1" ;;
    check-municipalities)
        echo "Municipios de la relación oficial del INE: 8132" > "$(option report "${arguments[@]}")"
        printf 'official: 8132\nmissing: 3\nunexpected: 0\n' ;;
    provinces)
        printf '20\n48\n' ;;
    region-area)
        echo "-2.5,42.9,-1.6,43.5" ;;
    build-packages)
        province="$(option provinces "${arguments[@]}")"
        output="$(option output "${arguments[@]}")"
        [[ "$province" == "${FAKE_FAILING_PROVINCE:-none}" ]] && { echo "broken province" >&2; exit 1; }
        mkdir -p "$output"
        echo "tar" > "${output}/ES-${province}.tar"
        echo "{}" > "${output}/provincia-ES-${province}.json" ;;
    sign-index)
        output="$(option output "${arguments[@]}")"
        mkdir -p "$output"
        # One compressed file per part it receives, so the tests see which provinces reached the index.
        for part in "$(option parts "${arguments[@]}")"/*.json; do
            echo "{}" > "${output}/$(basename "$part").gz"
        done
        echo "list" > "${output}/municipios.json.gz"
        echo "signature" > "${output}/municipios.json.gz.sig"
        printf 'packages: 2\nprovinces: 2\ndata version: %s\nlist: 5 bytes\n' "$DATA_VERSION" ;;
    obsolete-releases)
        printf '%s\n' ${FAKE_OBSOLETE:-} ;;
esac
EOF
    chmod +x "${fakes}"/*
}

# Runs the script in a fresh directory with the given variables; prints its exit code.
run_script() {
    local case_directory="$1"
    shift
    mkdir -p "${case_directory}/home"
    git init --quiet --bare "${case_directory}/remote.git"
    git clone --quiet "${case_directory}/remote.git" "${case_directory}/checkout" 2> /dev/null
    echo "public key" > "${case_directory}/public-key.txt"
    (
        export PATH="${working_directory}/fakes:${PATH}"
        export HOME="${case_directory}/home"
        export FAKE_LOG="${case_directory}/calls.log"
        export GENERATOR="${working_directory}/fakes/generator"
        export INDEX_PUBLIC_KEY_FILE="${case_directory}/public-key.txt"
        export DATA_VERSION="$VERSION"
        export DATA_REPOSITORY="LifeOfAitor/kit-apagon-datos"
        export DATA_CHECKOUT="${case_directory}/checkout"
        export RETRY_WAIT_SECONDS=0
        export GITHUB_STEP_SUMMARY="${case_directory}/summary.md"
        for assignment in "$@"; do
            export "${assignment?}"
        done
        touch "$FAKE_LOG"
        "$SCRIPT_UNDER_TEST" > "${case_directory}/output.log" 2>&1
        echo $?
    )
}

# Shows what the script printed, so that a failing case can be understood from the test output.
show_output_unless_successful() {
    local case_directory="$1"
    local exit_code="$2"
    [[ "$exit_code" -eq 0 ]] || sed 's/^/    | /' "${case_directory}/output.log"
}

line_of() {
    grep -n -F -- "$2" "$1" | head -n 1 | cut -d: -f1
}

check() {
    local description="$1"
    shift
    if "$@"; then
        echo "ok: ${description}"
    else
        echo "FAILED: ${description}"
        failures=$((failures + 1))
    fi
}

write_fakes "${working_directory}/fakes"

# A full monthly run.
full="${working_directory}/full"
exit_code="$(run_script "$full" PUBLISH_INDEX=true "FAKE_LEFTOVERS=${VERSION} ${VERSION}-ES-01" "FAKE_DRAFTS=${VERSION}" \
    FAKE_OBSOLETE=2026-10-02 FAKE_FAILED_UPLOADS=1 "FAKE_MISSING=22074 51001")"
log="${full}/calls.log"
check "a full run ends well" test "$exit_code" -eq 0
show_output_unless_successful "$full" "$exit_code"
check "it deletes what a failed run of the same day left before anything else" \
    test "$(line_of "$log" "gh release delete ${VERSION}-ES-01 --repo LifeOfAitor/kit-apagon-datos --yes")" \
    -lt "$(line_of "$log" "osmium")"
check "it deletes the tag of a leftover that has one" \
    grep -q -- "gh api --method DELETE repos/LifeOfAitor/kit-apagon-datos/git/refs/tags/${VERSION}-ES-01" "$log"
check "it deletes a leftover draft, which has no tag, without failing" \
    bash -c "grep -q -- 'gh release delete ${VERSION} --repo' '$log' && ! grep -q -- 'git/refs/tags/${VERSION}\$' '$log'"
check "it exports both the extract of Spain and the one of the Canary Islands" \
    test "$(grep "osmium export" "$log" | grep -c -v "/overpass/")" -eq 4
check "it publishes each province as a prerelease with its tar" \
    grep -q -- "gh release create ${VERSION}-ES-48 --repo LifeOfAitor/kit-apagon-datos --prerelease" "$log"
check "it repeats a failed upload" \
    test "$(grep -c "gh release upload ${VERSION}-ES-20 " "$log")" -eq 2
check "only the municipalities of the official list of the INE get a package" \
    bash -c "grep 'generator provinces ' '$log' | grep -q -- '--official .*municipios-ine-2026.csv' && grep 'generator build-packages ' '$log' | grep -q -- '--official .*municipios-ine-2026.csv'"
check "it asks Overpass only for the municipalities the extract could not give" \
    test "$(grep -c "overpass-api.de" "$log")" -eq 2
check "it asks Overpass for a municipality by its INE code" \
    grep -q -F 'relation["ine:municipio"="51001"]' "$log"
check "it identifies itself to Overpass with the address of the data repository" \
    grep -q -F -- "--user-agent kit-apagon-datos (+https://github.com/LifeOfAitor/kit-apagon-datos)" "$log"
check "it completes the boundaries before comparing them with the official list" \
    test "$(line_of "$log" "generator complete-boundaries")" -lt "$(line_of "$log" "generator check-municipalities")"
check "the map of each province is cut from the build of the day" \
    grep -q -- "pmtiles extract https://build.protomaps.com/20261202.pmtiles" "$log"
check "it signs the index with the public key of the app" \
    grep -q -- "generator sign-index --parts .* --public-key ${full}/public-key.txt" "$log"
check "the index points to the releases of the data repository" \
    grep -q -F -- "--details-address https://github.com/LifeOfAitor/kit-apagon-datos/releases/download/{data_version}/{file} --bundle-address https://github.com/LifeOfAitor/kit-apagon-datos/releases/download/{data_version}-ES-{province}/{file}" "$log"
index_release="$(grep "gh release create ${VERSION} " "$log" || true)"
check "the index release carries the signed list, its signature and the report" \
    bash -c "grep -q -- '/municipios.json.gz ' <<< '$index_release' && grep -q -- '/municipios.json.gz.sig ' <<< '$index_release' && grep -q -- '/informe.txt' <<< '$index_release'"
check "the index release carries the details of every province" \
    bash -c "grep -q -- '/provincia-ES-20.json.gz ' <<< '$index_release' && grep -q -- '/provincia-ES-48.json.gz ' <<< '$index_release'"
check "it records the version in the data repository" \
    bash -c "git -C '${full}/remote.git' log -1 --format=%s | grep -q 'datos: publicar la versión ${VERSION}'"
commit="$(git -C "${full}/remote.git" rev-parse HEAD 2> /dev/null)"
check "the index release points to that commit and starts as a draft" \
    grep -q -- "gh release create ${VERSION} --repo LifeOfAitor/kit-apagon-datos --draft --title Datos del ${VERSION} .*--target ${commit}" "$log"
check "it becomes the latest version only after every province is published" \
    test "$(line_of "$log" "gh release edit ${VERSION} --repo LifeOfAitor/kit-apagon-datos --draft=false --latest")" \
    -gt "$(line_of "$log" "gh release upload ${VERSION}-ES-48")"
check "it deletes the versions older than the last two after publishing" \
    test "$(line_of "$log" "gh release delete 2026-10-02")" -gt "$(line_of "$log" "gh release edit ${VERSION}")"
check "the summary of the job shows the report" grep -q "Municipios de la relación oficial del INE" "${full}/summary.md"

# A trial run of one province.
trial="${working_directory}/trial"
exit_code="$(run_script "$trial" PUBLISH_INDEX=false PROVINCES=20)"
log="${trial}/calls.log"
check "a trial run ends well" test "$exit_code" -eq 0
show_output_unless_successful "$trial" "$exit_code"
check "a trial run builds only the provinces asked for" \
    bash -c "grep -q 'gh release create ${VERSION}-ES-20' '$log' && ! grep -q 'ES-48' '$log'"
check "a trial run leaves the index in a draft that phones never see" \
    bash -c "grep -q 'gh release create ${VERSION} .*--draft' '$log' && ! grep -q 'gh release edit' '$log'"
check "a trial run does not commit to the data repository" \
    bash -c "! git -C '${trial}/remote.git' rev-parse --quiet --verify HEAD > /dev/null"
check "a trial run does not delete old versions" \
    bash -c "! grep -q 'obsolete-releases' '$log'"

# Overpass is a help, not a requirement.
busy="${working_directory}/busy"
exit_code="$(run_script "$busy" PUBLISH_INDEX=true FAKE_MISSING=51001 FAKE_OVERPASS_FAILS=true)"
check "a failure of Overpass does not stop the publication" \
    bash -c "[[ $exit_code -eq 0 ]] && grep -q 'gh release edit ${VERSION} ' '${busy}/calls.log'"

broken_extract="${working_directory}/broken-extract"
exit_code="$(run_script "$broken_extract" PUBLISH_INDEX=false PROVINCES=20 "FAKE_MISSING=$(seq -w 20001 20021 | tr '\n' ' ')")"
check "with more than 20 missing municipalities Overpass is not asked" \
    bash -c "[[ $exit_code -eq 0 ]] && ! grep -q 'overpass-api.de' '${broken_extract}/calls.log' && ! grep -q 'complete-boundaries' '${broken_extract}/calls.log'"
check "a trial run looks only for the missing municipalities of its provinces" \
    grep -q -- "generator missing-boundaries .* --provinces 20" "${broken_extract}/calls.log"

# Runs that must stop.
partial="${working_directory}/partial"
exit_code="$(run_script "$partial" PUBLISH_INDEX=true PROVINCES=20)"
check "a run limited to some provinces cannot publish the index" \
    bash -c "[[ $exit_code -ne 0 ]] && ! grep -q 'gh ' '${partial}/calls.log'"

malformed="${working_directory}/malformed"
exit_code="$(run_script "$malformed" PUBLISH_INDEX=false "PROVINCES=20, 48")"
check "malformed provinces are refused before any work" \
    bash -c "[[ $exit_code -ne 0 ]] && [[ ! -s '${malformed}/calls.log' ]]"

published="${working_directory}/published"
exit_code="$(run_script "$published" PUBLISH_INDEX=true FAKE_ALREADY_PUBLISHED=true)"
check "a day that is already published stops before any upload" \
    bash -c "[[ $exit_code -ne 0 ]] && ! grep -q 'gh release create' '${published}/calls.log'"

refused="${working_directory}/refused"
exit_code="$(run_script "$refused" PUBLISH_INDEX=true FAKE_GEOFABRIK_REFUSES=true)"
check "a refused download stops the run before anything is exported or published" \
    bash -c "[[ $exit_code -ne 0 ]] && ! grep -q 'osmium' '${refused}/calls.log' && ! grep -q 'gh release create' '${refused}/calls.log'"
check "a refused download leaves no partial extract behind" \
    bash -c "[[ -z \"\$(find '${refused}/home' -name '*.osm.pbf*')\" ]]"

broken="${working_directory}/broken"
exit_code="$(run_script "$broken" PUBLISH_INDEX=true FAKE_FAILING_PROVINCE=48)"
check "a broken province stops the run before the index is published" \
    bash -c "[[ $exit_code -ne 0 ]] && ! grep -q 'gh release create ${VERSION} ' '${broken}/calls.log' && ! grep -q 'gh release edit' '${broken}/calls.log'"

if [[ "$failures" -gt 0 ]]; then
    echo "${failures} test(s) failed"
    exit 1
fi
echo "All tests passed"
