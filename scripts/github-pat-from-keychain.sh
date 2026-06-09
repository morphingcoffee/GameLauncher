#!/usr/bin/env bash
# Load GitHub PAT from macOS Keychain into the environment.
# Source this file — never commit tokens to any config file.
#
#   source scripts/github-pat-from-keychain.sh
#
set -euo pipefail

KEYCHAIN_SERVICE="${GAME_LAUNCHER_GITHUB_KEYCHAIN_SERVICE:-gamelauncher-github-pat}"
KEYCHAIN_ACCOUNT="${GAME_LAUNCHER_GITHUB_KEYCHAIN_ACCOUNT:-$USER}"

_pat="$(security find-generic-password -a "$KEYCHAIN_ACCOUNT" -s "$KEYCHAIN_SERVICE" -w 2>/dev/null || true)"

if [[ -z "$_pat" ]]; then
  echo "github-pat-from-keychain: no item '$KEYCHAIN_SERVICE' for account '$KEYCHAIN_ACCOUNT'" >&2
  echo "Store with: security add-generic-password -U -a \"\$USER\" -s \"$KEYCHAIN_SERVICE\" -w \"TOKEN\"" >&2
  return 1 2>/dev/null || exit 1
fi

export GITHUB_PAT="$_pat"
export GH_TOKEN="$_pat"
unset _pat
