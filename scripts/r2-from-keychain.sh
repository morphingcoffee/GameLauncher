#!/usr/bin/env bash
# Load Cloudflare R2 API credentials from macOS Keychain into the environment.
# Source this file — never commit tokens to any config file.
#
#   source scripts/r2-from-keychain.sh
#
set -euo pipefail

ACCESS_KEY_SERVICE="${GAME_LAUNCHER_R2_ACCESS_KEY_SERVICE:-gamelauncher-r2-access-key-id}"
SECRET_KEY_SERVICE="${GAME_LAUNCHER_R2_SECRET_KEY_SERVICE:-gamelauncher-r2-secret-access-key}"
KEYCHAIN_ACCOUNT="${GAME_LAUNCHER_R2_KEYCHAIN_ACCOUNT:-$USER}"

_access="$(security find-generic-password -a "$KEYCHAIN_ACCOUNT" -s "$ACCESS_KEY_SERVICE" -w 2>/dev/null || true)"
_secret="$(security find-generic-password -a "$KEYCHAIN_ACCOUNT" -s "$SECRET_KEY_SERVICE" -w 2>/dev/null || true)"

if [[ -z "$_access" ]]; then
  echo "r2-from-keychain: no item '$ACCESS_KEY_SERVICE' for account '$KEYCHAIN_ACCOUNT'" >&2
  echo "Store with: security add-generic-password -U -a \"\$USER\" -s \"$ACCESS_KEY_SERVICE\" -w \"ACCESS_KEY_ID\"" >&2
  return 1 2>/dev/null || exit 1
fi

if [[ -z "$_secret" ]]; then
  echo "r2-from-keychain: no item '$SECRET_KEY_SERVICE' for account '$KEYCHAIN_ACCOUNT'" >&2
  echo "Store with: security add-generic-password -U -a \"\$USER\" -s \"$SECRET_KEY_SERVICE\" -w \"SECRET_ACCESS_KEY\"" >&2
  return 1 2>/dev/null || exit 1
fi

export R2_ACCESS_KEY_ID="$_access"
export R2_SECRET_ACCESS_KEY="$_secret"
export AWS_ACCESS_KEY_ID="$_access"
export AWS_SECRET_ACCESS_KEY="$_secret"
unset _access _secret
