#!/usr/bin/env bash
# Cursor beforeShellExecution hook — deny git push if secret scan fails.
# hooks.json matcher is "git push"; always scan when this hook is invoked.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

deny_scan_failed() {
  echo '{
    "permission": "deny",
    "user_message": "Push blocked: secret scan found issues. Fix before pushing to the public repo.",
    "agent_message": "Run scripts/scan-secrets.sh all, fix findings, then retry git push."
  }'
}

deny_scan_error() {
  echo '{
    "permission": "deny",
    "user_message": "Push blocked: secret scan could not run.",
    "agent_message": "Ensure scripts/scan-secrets.sh exists and is executable, then retry git push."
  }'
}

if [[ ! -x "$ROOT/scripts/scan-secrets.sh" ]]; then
  deny_scan_error
  exit 0
fi

if ! "$ROOT/scripts/scan-secrets.sh" all; then
  deny_scan_failed
  exit 0
fi

echo '{ "permission": "allow" }'
exit 0
