#!/usr/bin/env bash
# Tests for scripts/run-with-download-retries.sh, using fake commands instead of a real build.
# Run by ./gradlew verifyAll.

set -uo pipefail

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_UNDER_TEST="${script_directory}/../run-with-download-retries.sh"
export DOWNLOAD_RETRY_FIRST_WAIT_SECONDS=0

failures=0
working_directory="$(mktemp -d)"
trap 'rm -rf "$working_directory"' EXIT

# Writes a fake command that prints the given lines in order, one per run, and fails while the
# printed line does not start with "ok". The number of runs is counted in a file.
fake_command() {
    local name="$1"
    shift
    local command_file="${working_directory}/${name}"
    printf '%s\n' "$@" > "${command_file}.outputs"
    cat > "$command_file" <<EOF
#!/usr/bin/env bash
runs_file="${command_file}.runs"
runs=\$(( \$(cat "\$runs_file" 2>/dev/null || echo 0) + 1 ))
echo "\$runs" > "\$runs_file"
line="\$(sed -n "\${runs}p" "${command_file}.outputs")"
[[ -z "\$line" ]] && line="\$(tail -n 1 "${command_file}.outputs")"
echo "\$line"
[[ "\$line" == ok* ]]
EOF
    chmod +x "$command_file"
    echo "$command_file"
}

runs_of() {
    cat "${1}.runs"
}

expect() {
    local description="$1"
    local expected_exit_code="$2"
    local expected_runs="$3"
    local command_file="$4"
    "$SCRIPT_UNDER_TEST" "$command_file" > /dev/null 2>&1
    local exit_code=$?
    local runs
    runs="$(runs_of "$command_file")"
    if [[ "$exit_code" -eq "$expected_exit_code" && "$runs" -eq "$expected_runs" ]]; then
        echo "ok: ${description}"
    else
        echo "FAILED: ${description} (exit code ${exit_code}, expected ${expected_exit_code}; runs ${runs}, expected ${expected_runs})"
        failures=$((failures + 1))
    fi
}

expect "a successful command runs once" 0 1 \
    "$(fake_command success "ok: BUILD SUCCESSFUL")"

expect "a code failure is not repeated" 1 1 \
    "$(fake_command test_failure "MapViewModelTest > loadsThePlacesOfThePackage FAILED")"

expect "a 429 answer is repeated until it works" 0 2 \
    "$(fake_command rate_limited_once "Received status code 429 from server: Too Many Requests" "ok: BUILD SUCCESSFUL")"

expect "a timeout is repeated until it works" 0 3 \
    "$(fake_command timeouts "Read timed out" "Connect timed out" "ok: BUILD SUCCESSFUL")"

expect "it stops after three attempts if the network keeps failing" 1 3 \
    "$(fake_command always_rate_limited "Received status code 429 from server: Too Many Requests")"

expect "a code failure after a network failure is not repeated again" 1 2 \
    "$(fake_command network_then_code "Received status code 503" "Compilation error in PlacesFile.kt")"

if "$SCRIPT_UNDER_TEST" > /dev/null 2>&1; then
    echo "FAILED: running without a command must fail"
    failures=$((failures + 1))
else
    echo "ok: running without a command fails"
fi

if [[ "$failures" -gt 0 ]]; then
    echo "${failures} test(s) failed." >&2
    exit 1
fi
echo "All tests passed."
