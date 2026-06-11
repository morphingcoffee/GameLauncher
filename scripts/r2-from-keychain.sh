#!/usr/bin/env bash
# Load Cloudflare R2 S3-compatible credentials from macOS Keychain.
# Source this file — never commit keys to any config file.
#
#   source scripts/r2-from-keychain.sh
#
set -euo pipefail

ACCESS_KEY_SERVICE="${GAME_LAUNCHER_R2_ACCESS_KEY_SERVICE:-gamelauncher-r2-access-key-id}"
SECRET_KEY_SERVICE="${GAME_LAUNCHER_R2_SECRET_KEY_SERVICE:-gamelauncher-r2-secret-access-key}"
KEYCHAIN_ACCOUNT="${GAME_LAUNCHER_R2_KEYCHAIN_ACCOUNT:-$USER}"

_access_key="$(security find-generic-password -a "$KEYCHAIN_ACCOUNT" -s "$ACCESS_KEY_SERVICE" -w 2>/dev/null || true)"
_secret_key="$(security find-generic-password -a "$KEYCHAIN_ACCOUNT" -s "$SECRET_KEY_SERVICE" -w 2>/dev/null || true)"

if [[ -z "$_access_key" ]]; then
  echo "r2-from-keychain: no item '$ACCESS_KEY_SERVICE' for account '$KEYCHAIN_ACCOUNT'" >&2
  echo "Store with: security add-generic-password -U -a \"\$USER\" -s \"$ACCESS_KEY_SERVICE\" -w \"ACCESS_KEY_ID\"" >&2
  return 1 2>/dev/null || exit 1
fi

if [[ -z "$_secret_key" ]]; then
  echo "r2-from-keychain: no item '$SECRET_KEY_SERVICE' for account '$KEYCHAIN_ACCOUNT'" >&2
  echo "Store with: security add-generic-password -U -a \"\$USER\" -s \"$SECRET_KEY_SERVICE\" -w \"SECRET_ACCESS_KEY\"" >&2
  return 1 2>/dev/null || exit 1
fi

export R2_ACCESS_KEY_ID="$_access_key"
export R2_SECRET_ACCESS_KEY="$_secret_key"

# rclone S3 backend — r2-deploy.sh reads these instead of inlining keys in argv
export RCLONE_S3_ACCESS_KEY_ID="$_access_key"
export RCLONE_S3_SECRET_ACCESS_KEY="$_secret_key"

unset _access_key _secret_key
