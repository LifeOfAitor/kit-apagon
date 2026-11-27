#!/usr/bin/env bash
# Prints true when the data repository already has a published version of a month, and false when it
# has none. The scheduled workflow runs on days 2 to 8 and stops early on the days after a successful
# publication (decision 0015). Only a published index release counts: drafts, the prereleases of the
# provinces and releases with other names do not.
#
# Usage: DATA_REPOSITORY=owner/name scripts/month-is-published.sh [YYYY-MM]
#   The month is the current one in UTC when it is not given.
# Requires: gh and python3.

set -euo pipefail

month="${1:-$(date -u +%Y-%m)}"
[[ -n "${DATA_REPOSITORY:-}" ]] || { echo "DATA_REPOSITORY must name the data repository." >&2; exit 1; }
[[ "$month" =~ ^[0-9]{4}-[0-9]{2}$ ]] || { echo "The month must be written as YYYY-MM: ${month}" >&2; exit 1; }

gh release list --repo "$DATA_REPOSITORY" --limit 1000 --json tagName,isDraft,isPrerelease \
    | python3 -I -c '
import json, re, sys
month = sys.argv[1]
releases = json.load(sys.stdin)
published = any(
    not release["isDraft"] and not release["isPrerelease"]
    and re.fullmatch(re.escape(month) + r"-[0-9]{2}", release["tagName"])
    for release in releases
)
print("true" if published else "false")
' "$month"
