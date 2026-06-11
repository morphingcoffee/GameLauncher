#!/usr/bin/env bash
# Sync a local directory to Cloudflare R2 via rclone (S3-compatible API).
#
#   ./scripts/r2-deploy.sh <local-dir> [remote-prefix]
#
# Requires: rclone, .env with R2_ACCOUNT_ID and R2_BUCKET_NAME, Keychain R2 keys.
# See docs/r2-deploy.md for setup and Keychain storage.
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

usage() {
  cat <<'EOF' >&2
Usage: r2-deploy.sh <local-dir> [remote-prefix]

  local-dir       Directory to upload (synced to bucket root or prefix)
  remote-prefix   Optional path inside the bucket (no leading slash)

Examples:
  ./scripts/r2-deploy.sh ./build/dist
  ./scripts/r2-deploy.sh ./build/dist releases/v1.0.0
EOF
  exit 2
}

case "${1:-}" in
  -h | --help | help | "")
    usage
    ;;
esac

LOCAL_DIR="$1"
REMOTE_PREFIX="${2:-}"

if [[ ! -d "$LOCAL_DIR" ]]; then
  echo "r2-deploy: local directory not found: $LOCAL_DIR" >&2
  exit 1
fi

if ! command -v rclone >/dev/null 2>&1; then
  echo "r2-deploy: rclone not found — install with: brew install rclone" >&2
  exit 1
fi

ENV_FILE="$ROOT/.env"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a
  source "$ENV_FILE"
  set +a
fi

# Keychain credentials override any R2_* secret vars that may exist in .env
# shellcheck source=scripts/r2-from-keychain.sh
source "$ROOT/scripts/r2-from-keychain.sh" || {
  echo "r2-deploy: failed to load Keychain credentials" >&2
  exit 1
}

: "${R2_BUCKET_NAME:?Set R2_BUCKET_NAME in .env (see .env.example)}"

# shellcheck source=scripts/r2-rclone-env.sh
source "$ROOT/scripts/r2-rclone-env.sh"
r2_rclone_configure
trap r2_rclone_cleanup EXIT

REMOTE="${RCLONE_REMOTE}:${R2_BUCKET_NAME}"
if [[ -n "$REMOTE_PREFIX" ]]; then
  REMOTE="${REMOTE}/${REMOTE_PREFIX}"
fi

echo "r2-deploy: $LOCAL_DIR -> r2://${R2_BUCKET_NAME}/${REMOTE_PREFIX:-}" >&2

rclone sync "$LOCAL_DIR" "$REMOTE" \
  --s3-no-check-bucket \
  --progress \
  --retries 5 \
  --retries-sleep 5s \
  --low-level-retries 10 \
  --stats-one-line \
  --stats 5s
