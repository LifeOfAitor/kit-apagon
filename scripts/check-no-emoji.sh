#!/usr/bin/env bash
# Fails if any file tracked by Git, or any commit message in the given range, contains an emoji.
# The project forbids emojis in code, commits and documentation (CLAUDE.md).
#
# Usage:
#   scripts/check-no-emoji.sh                 checks the tracked files
#   scripts/check-no-emoji.sh main..HEAD      also checks the commit messages in that range

set -euo pipefail

readonly EMOJI_PATTERN='[\x{1F000}-\x{1FAFF}\x{2600}-\x{27BF}\x{2B00}-\x{2BFF}\x{FE0F}]'

find_emojis_in_tracked_files() {
    git ls-files -z \
        | grep -z -v -E '\.(jar|png|jpg|webp|pmtiles|pbf)$' \
        | xargs -0 perl -CSD -ne "print \"\$ARGV:\$.: \$_\" if /$EMOJI_PATTERN/; close ARGV if eof"
}

find_emojis_in_commit_messages() {
    local commit_range="$1"
    git log --format='%H %B' "$commit_range" | perl -CSD -ne "print if /$EMOJI_PATTERN/"
}

main() {
    local findings
    findings="$(find_emojis_in_tracked_files)"
    if [[ $# -gt 0 ]]; then
        findings+="$(find_emojis_in_commit_messages "$1")"
    fi

    if [[ -n "$findings" ]]; then
        echo "Emojis are not allowed in this project. Found:" >&2
        echo "$findings" >&2
        exit 1
    fi
    echo "No emojis found."
}

main "$@"
