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

# shellcheck source=scripts/r2-from-keychain.sh
source "$ROOT/scripts/r2-from-keychain.sh"

ENV_FILE="$ROOT/.env"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a
  source "$ENV_FILE"
  set +a
fi

: "${R2_ACCOUNT_ID:?Set R2_ACCOUNT_ID in .env (see .env.example)}"
: "${R2_BUCKET_NAME:?Set R2_BUCKET_NAME in .env (see .env.example)}"

R2_ENDPOINT="https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"

# Inline S3 remote — no rclone.conf on disk
REMOTE_SPEC=":s3,provider=Cloudflare,access_key_id=${R2_ACCESS_KEY_ID},secret_access_key=${R2_SECRET_ACCESS_KEY},endpoint=${R2_ENDPOINT}:${R2_BUCKET_NAME}"
if [[ -n "$REMOTE_PREFIX" ]]; then
  REMOTE_SPEC="${REMOTE_SPEC}/${REMOTE_PREFIX}"
fi

echo "r2-deploy: $LOCAL_DIR -> r2://${R2_BUCKET_NAME}/${REMOTE_PREFIX:-}" >&2

rclone sync "$LOCAL_DIR" "$REMOTE_SPEC" \
  --progress \
  --retries 5 \
  --retries-sleep 5s \
  --low-level-retries 10 \
  --stats-one-line \
  --stats 5s
