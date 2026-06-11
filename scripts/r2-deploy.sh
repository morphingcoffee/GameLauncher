#!/usr/bin/env bash
# Sync a local directory to a Cloudflare R2 bucket via AWS CLI (S3-compatible API).
#
# Usage:
#   ./scripts/r2-deploy.sh <local-path> [remote-prefix]
#
# Requires:
#   - aws CLI (https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
#   - R2 credentials in Keychain (loaded via scripts/r2-from-keychain.sh)
#   - R2_ACCOUNT_ID and R2_BUCKET_NAME in .env or environment
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if ! command -v aws >/dev/null 2>&1; then
  echo "r2-deploy: aws CLI not found." >&2
  echo "Install: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html" >&2
  exit 1
fi

if [[ -f "$ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env"
  set +a
fi

# shellcheck source=scripts/r2-from-keychain.sh
source "$ROOT/scripts/r2-from-keychain.sh"

LOCAL_PATH="${1:-}"
REMOTE_PREFIX="${2:-}"

if [[ -z "$LOCAL_PATH" ]]; then
  echo "Usage: $0 <local-path> [remote-prefix]" >&2
  exit 1
fi

if [[ ! -d "$LOCAL_PATH" ]]; then
  echo "r2-deploy: local path is not a directory: $LOCAL_PATH" >&2
  exit 1
fi

ACCOUNT_ID="${R2_ACCOUNT_ID:-}"
BUCKET="${R2_BUCKET_NAME:-}"

if [[ -z "$ACCOUNT_ID" ]]; then
  echo "r2-deploy: R2_ACCOUNT_ID is not set. Add it to .env (see .env.example)." >&2
  exit 1
fi

if [[ -z "$BUCKET" ]]; then
  echo "r2-deploy: R2_BUCKET_NAME is not set. Add it to .env (see .env.example)." >&2
  exit 1
fi

ENDPOINT="https://${ACCOUNT_ID}.r2.cloudflarestorage.com"
DEST="s3://${BUCKET}"
if [[ -n "$REMOTE_PREFIX" ]]; then
  REMOTE_PREFIX="${REMOTE_PREFIX#/}"
  REMOTE_PREFIX="${REMOTE_PREFIX%/}"
  DEST="${DEST}/${REMOTE_PREFIX}"
fi

export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-auto}"

echo "r2-deploy: syncing $(cd "$LOCAL_PATH" && pwd) -> ${DEST}"
echo "r2-deploy: endpoint ${ENDPOINT}"

aws s3 sync "$LOCAL_PATH" "$DEST" --endpoint-url "$ENDPOINT"
