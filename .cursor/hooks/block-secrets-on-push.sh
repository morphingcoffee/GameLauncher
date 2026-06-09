#!/usr/bin/env bash
# Cursor beforeShellExecution hook — deny git push if secret scan fails.
set -euo pipefail

INPUT="$(cat)"
COMMAND="$(echo "$INPUT" | python3 -c "import sys,json; print(json.load(sys.stdin).get('command',''))" 2>/dev/null || true)"

if [[ ! "$COMMAND" =~ git[[:space:]]+push ]]; then
  echo '{ "permission": "allow" }'
  exit 0
fi

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
if ! "$ROOT/scripts/scan-secrets.sh" all; then
  echo '{
    "permission": "deny",
    "user_message": "Push blocked: secret scan found issues. Fix before pushing to the public repo.",
    "agent_message": "Run scripts/scan-secrets.sh all, fix findings, then retry git push."
  }'
  exit 0
fi

echo '{ "permission": "allow" }'
exit 0
