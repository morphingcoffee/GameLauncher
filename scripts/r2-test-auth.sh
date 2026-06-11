#!/usr/bin/env bash
# Verify R2 Keychain credentials and bucket access without touching your shell.
#
#   ./scripts/r2-test-auth.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if ! command -v rclone >/dev/null 2>&1; then
  echo "r2-test-auth: rclone not found — install with: brew install rclone" >&2
  exit 1
fi

ENV_FILE="$ROOT/.env"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a
  source "$ENV_FILE"
  set +a
fi

# shellcheck source=scripts/r2-from-keychain.sh
source "$ROOT/scripts/r2-from-keychain.sh" || {
  echo "r2-test-auth: failed to load Keychain credentials" >&2
  exit 1
}

: "${R2_BUCKET_NAME:?Set R2_BUCKET_NAME in .env (see .env.example)}"

# shellcheck source=scripts/r2-rclone-env.sh
source "$ROOT/scripts/r2-rclone-env.sh"
r2_rclone_configure
trap r2_rclone_cleanup EXIT

echo "r2-test-auth: listing r2://${R2_BUCKET_NAME}/ (first 20 keys)..." >&2

if rclone lsf "${RCLONE_REMOTE}:${R2_BUCKET_NAME}" --s3-no-check-bucket --max-depth 1 | head -20; then
  echo "r2-test-auth: OK — credentials and bucket access work." >&2
else
  echo "r2-test-auth: failed — if you see SignatureDoesNotMatch, re-create the R2 token and update both Keychain items (see docs/r2-deploy.md)." >&2
  exit 1
fi
