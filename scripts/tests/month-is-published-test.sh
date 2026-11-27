#!/usr/bin/env bash
# Tests for scripts/month-is-published.sh with a fake gh that lists the releases in FAKE_RELEASES.
# Run by ./gradlew verifyAll.

set -uo pipefail

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_UNDER_TEST="${script_directory}/../month-is-published.sh"

failures=0
working_directory="$(mktemp -d)"
trap 'rm -rf "$working_directory"' EXIT

cat > "${working_directory}/gh" <<'FAKE'
#!/usr/bin/env bash
echo "gh $*" >> "${FAKE_LOG}"
[[ "${FAKE_GH_FAILS:-false}" == "true" ]] && { echo "HTTP 502" >&2; exit 1; }
echo "${FAKE_RELEASES}"
FAKE
chmod +x "${working_directory}/gh"

# Runs the script with the given releases and month; prints its output and exit code.
answer_for() {
    local releases="$1"
    shift
    PATH="${working_directory}:${PATH}" FAKE_LOG="${working_directory}/calls.log" FAKE_RELEASES="$releases" \
        DATA_REPOSITORY="LifeOfAitor/kit-apagon-datos" "$SCRIPT_UNDER_TEST" "$@" 2> /dev/null
    echo "exit $?"
}

expect() {
    local description="$1"
    local expected="$2"
    local actual="$3"
    if [[ "$actual" == "$expected" ]]; then
        echo "ok: ${description}"
    else
        echo "FAILED: ${description} (got: ${actual//$'\n'/ | }; expected: ${expected//$'\n'/ | })"
        failures=$((failures + 1))
    fi
}

release() {
    printf '{"tagName":"%s","isDraft":%s,"isPrerelease":%s}' "$1" "$2" "$3"
}

december="[$(release 2026-12-02 false false),$(release 2026-12-02-ES-20 false true)]"
expect "a published index of the month counts" $'true\nexit 0' "$(answer_for "$december" 2026-12)"
expect "a version of another month does not count" $'false\nexit 0' "$(answer_for "$december" 2027-01)"

trial="[$(release 2026-12-05 true false),$(release 2026-12-05-ES-20 false true)]"
expect "a trial left as a draft does not count" $'false\nexit 0' "$(answer_for "$trial" 2026-12)"
expect "the prerelease of a province does not count" $'false\nexit 0' \
    "$(answer_for "[$(release 2026-12-05-ES-20 false false)]" 2026-12)"
expect "releases with other names do not count" $'false\nexit 0' \
    "$(answer_for "[$(release 2026-12-notas false false),$(release v2026-12-02 false false)]" 2026-12)"
expect "nothing published yet" $'false\nexit 0' "$(answer_for "[]" 2026-12)"
expect "the current month is used when none is given" $'true\nexit 0' \
    "$(answer_for "[$(release "$(date -u +%Y-%m)-02" false false)]")"

expect "a malformed month is refused" "exit 1" "$(answer_for "[]" 2026-1)"
expect "a failure of gh is not taken for an unpublished month" "exit 1" \
    "$(FAKE_GH_FAILS=true answer_for "[]" 2026-12)"

if [[ "$failures" -gt 0 ]]; then
    echo "${failures} test(s) failed"
    exit 1
fi
echo "All tests passed"
