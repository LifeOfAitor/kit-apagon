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

    # gh: records its arguments; lists the releases in FAKE_RELEASES; fails the first FAKE_FAILED_UPLOADS uploads.
    cat > "${fakes}/gh" <<'EOF'
#!/usr/bin/env bash
echo "gh $*" >> "${FAKE_LOG}"
if [[ "$1 $2" == "release list" ]]; then
    echo "${FAKE_RELEASES:-[]}"
elif [[ "$1 $2" == "release upload" ]]; then
    uploads=$(( $(cat "${FAKE_LOG}.uploads" 2>/dev/null || echo 0) + 1 ))
    echo "$uploads" > "${FAKE_LOG}.uploads"
    [[ "$uploads" -gt "${FAKE_FAILED_UPLOADS:-0}" ]]
fi
EOF

    # curl: the list of Protomaps builds, or a small extract with its MD5 checksum.
    cat > "${fakes}/curl" <<'EOF'
#!/usr/bin/env bash
url="${*: -1}"
output=""
while [[ $# -gt 0 ]]; do
    [[ "$1" == "--output" ]] && output="$2"
    shift
done
if [[ "$url" == *builds.json ]]; then
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
        echo "{}" > "${output}/index-part.json" ;;
    sign-index)
        output="$(option output "${arguments[@]}")"
        mkdir -p "$output"
        echo "{}" > "${output}/index.json"
        echo "signature" > "${output}/index.json.sig"
        printf 'packages: 2\ndata version: %s\n' "$DATA_VERSION" ;;
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
exit_code="$(run_script "$full" PUBLISH_INDEX=true "FAKE_LEFTOVERS=${VERSION}-ES-01" FAKE_OBSOLETE=2026-10-02 FAKE_FAILED_UPLOADS=1)"
log="${full}/calls.log"
check "a full run ends well" test "$exit_code" -eq 0
show_output_unless_successful "$full" "$exit_code"
check "it deletes what a failed run of the same day left before anything else" \
    test "$(line_of "$log" "gh release delete ${VERSION}-ES-01 --repo LifeOfAitor/kit-apagon-datos --cleanup-tag --yes")" \
    -lt "$(line_of "$log" "osmium")"
check "it exports both the extract of Spain and the one of the Canary Islands" \
    test "$(grep -c "osmium export" "$log")" -eq 4
check "it publishes each province as a prerelease with its tar" \
    grep -q -- "gh release create ${VERSION}-ES-48 --repo LifeOfAitor/kit-apagon-datos --prerelease" "$log"
check "it repeats a failed upload" \
    test "$(grep -c "gh release upload ${VERSION}-ES-20 " "$log")" -eq 2
check "the map of each province is cut from the build of the day" \
    grep -q -- "pmtiles extract https://build.protomaps.com/20261202.pmtiles" "$log"
check "it signs the index with the public key of the app" \
    grep -q -- "generator sign-index --parts .* --public-key ${full}/public-key.txt" "$log"
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

broken="${working_directory}/broken"
exit_code="$(run_script "$broken" PUBLISH_INDEX=true FAKE_FAILING_PROVINCE=48)"
check "a broken province stops the run before the index is published" \
    bash -c "[[ $exit_code -ne 0 ]] && ! grep -q 'gh release create ${VERSION} ' '${broken}/calls.log' && ! grep -q 'gh release edit' '${broken}/calls.log'"

if [[ "$failures" -gt 0 ]]; then
    echo "${failures} test(s) failed"
    exit 1
fi
echo "All tests passed"
