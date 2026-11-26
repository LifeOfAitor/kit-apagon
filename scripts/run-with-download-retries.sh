#!/usr/bin/env bash
# Runs a command and repeats it only when it failed because a download failed (decision 0012).
# Maven Central sometimes answers 429 (too many requests) to the shared GitHub runners. Any other
# failure, such as a failing test or a compilation error, ends the script at once with that error,
# so a real problem in the code is never repeated or hidden.
#
# Usage: scripts/run-with-download-retries.sh COMMAND [ARGUMENTS...]
# The first wait is DOWNLOAD_RETRY_FIRST_WAIT_SECONDS (120 by default) and doubles before each retry.

set -uo pipefail

readonly MAXIMUM_ATTEMPTS=3
readonly NETWORK_FAILURE_PATTERN='status code (429|502|503|504)|Too Many Requests|Read timed out|Connection reset|Connect timed out'

first_wait_seconds="${DOWNLOAD_RETRY_FIRST_WAIT_SECONDS:-120}"

failed_because_of_the_network() {
    local output_file="$1"
    grep -E -q "$NETWORK_FAILURE_PATTERN" "$output_file"
}

main() {
    if [[ $# -eq 0 ]]; then
        echo "Usage: $0 COMMAND [ARGUMENTS...]" >&2
        exit 2
    fi
    local output_file attempt exit_code wait_seconds
    output_file="$(mktemp)"
    trap 'rm -f "$output_file"' EXIT
    wait_seconds="$first_wait_seconds"
    for ((attempt = 1; attempt <= MAXIMUM_ATTEMPTS; attempt++)); do
        "$@" 2>&1 | tee "$output_file"
        exit_code="${PIPESTATUS[0]}"
        if [[ "$exit_code" -eq 0 ]]; then
            exit 0
        fi
        if ! failed_because_of_the_network "$output_file"; then
            exit "$exit_code"
        fi
        if [[ "$attempt" -lt "$MAXIMUM_ATTEMPTS" ]]; then
            echo "Attempt ${attempt} failed while downloading; retrying in ${wait_seconds} seconds." >&2
            sleep "$wait_seconds"
            wait_seconds=$((wait_seconds * 2))
        fi
    done
    echo "The download kept failing after ${MAXIMUM_ATTEMPTS} attempts." >&2
    exit "$exit_code"
}

main "$@"
