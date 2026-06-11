#!/usr/bin/env bash
# Load Cloudflare API token for Wrangler from macOS Keychain.
# Source this file — never commit tokens to any config file.
#
#   source scripts/wrangler-from-keychain.sh
#
set -euo pipefail

KEYCHAIN_SERVICE="${GAME_LAUNCHER_WRANGLER_KEYCHAIN_SERVICE:-gamelauncher-wrangler-api-token}"
KEYCHAIN_ACCOUNT="${GAME_LAUNCHER_WRANGLER_KEYCHAIN_ACCOUNT:-$USER}"

_token="$(security find-generic-password -a "$KEYCHAIN_ACCOUNT" -s "$KEYCHAIN_SERVICE" -w 2>/dev/null || true)"

if [[ -z "$_token" ]]; then
  echo "wrangler-from-keychain: no item '$KEYCHAIN_SERVICE' for account '$KEYCHAIN_ACCOUNT'" >&2
  echo "Store with: security add-generic-password -U -a \"\$USER\" -s \"$KEYCHAIN_SERVICE\" -w \"API_TOKEN\"" >&2
  return 1 2>/dev/null || exit 1
fi

export CLOUDFLARE_API_TOKEN="$_token"
unset _token
