#!/usr/bin/env bash
# Launch Cursor with GITHUB_PAT injected from Keychain (GUI apps do not read .zshrc).
#
#   ./tools/dev/open-cursor-with-github.sh [path]
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [[ -z "$ROOT" ]]; then
  ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
fi
# shellcheck source=github-pat-from-keychain.sh
source "$SCRIPT_DIR/github-pat-from-keychain.sh"

TARGET="${1:-$ROOT}"
open -a Cursor "$TARGET"
