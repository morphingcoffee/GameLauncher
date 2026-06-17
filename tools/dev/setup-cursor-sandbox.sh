#!/usr/bin/env bash
# Install per-user Cursor sandbox overrides (not committed — contains $HOME/.gradle).
set -euo pipefail

CURSOR_DIR="${HOME}/.cursor"
GRADLE_HOME="${HOME}/.gradle"
mkdir -p "${CURSOR_DIR}" "${GRADLE_HOME}"

cat > "${CURSOR_DIR}/sandbox.json" <<EOF
{
  "enableSharedBuildCache": true,
  "additionalReadwritePaths": [
    "${GRADLE_HOME}"
  ]
}
EOF

echo "Wrote ${CURSOR_DIR}/sandbox.json (enableSharedBuildCache + ${GRADLE_HOME})"
echo "Restart Cursor or start a new agent chat for sandbox changes to apply."
