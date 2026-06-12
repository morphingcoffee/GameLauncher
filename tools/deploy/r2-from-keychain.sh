#!/usr/bin/env bash
# Load Cloudflare R2 S3-compatible credentials from macOS Keychain.
# Source this file — never commit keys to any config file.
#
#   source tools/deploy/r2-from-keychain.sh   # optional; Python deploy tools read Keychain directly
#
# Do not enable set -e here — this file is sourced and would affect your interactive shell.

ACCESS_KEY_SERVICE="${GAME_LAUNCHER_R2_ACCESS_KEY_SERVICE:-gamelauncher-r2-access-key-id}"
SECRET_KEY_SERVICE="${GAME_LAUNCHER_R2_SECRET_KEY_SERVICE:-gamelauncher-r2-secret-access-key}"
KEYCHAIN_ACCOUNT="${GAME_LAUNCHER_R2_KEYCHAIN_ACCOUNT:-$USER}"

_keychain_read() {
  # Trim whitespace — copy/paste into Keychain often adds trailing newlines (SignatureDoesNotMatch).
  security find-generic-password -a "$KEYCHAIN_ACCOUNT" -s "$1" -w 2>/dev/null \
    | tr -d '\r\n' \
    | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'
}

_access_key="$(_keychain_read "$ACCESS_KEY_SERVICE" || true)"
_secret_key="$(_keychain_read "$SECRET_KEY_SERVICE" || true)"

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

if [[ ${#_access_key} -ne 32 ]]; then
  echo "r2-from-keychain: access key length is ${#_access_key} (expected 32) — re-store Keychain item '$ACCESS_KEY_SERVICE'" >&2
  return 1 2>/dev/null || exit 1
fi

if [[ ${#_secret_key} -lt 32 ]]; then
  echo "r2-from-keychain: secret key looks too short (${#_secret_key} chars) — re-store Keychain item '$SECRET_KEY_SERVICE'" >&2
  return 1 2>/dev/null || exit 1
fi

export R2_ACCESS_KEY_ID="$_access_key"
export R2_SECRET_ACCESS_KEY="$_secret_key"

unset _access_key _secret_key _keychain_read
