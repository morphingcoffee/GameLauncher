#!/usr/bin/env bash
# Launch Cursor with GITHUB_PAT injected from Keychain (GUI apps do not read .zshrc).
#
#   ./scripts/open-cursor-with-github.sh [path]
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=scripts/github-pat-from-keychain.sh
source "$ROOT/scripts/github-pat-from-keychain.sh"

TARGET="${1:-$ROOT}"
open -a Cursor "$TARGET"
