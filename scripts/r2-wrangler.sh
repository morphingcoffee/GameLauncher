#!/usr/bin/env bash
# Run Wrangler with CLOUDFLARE_API_TOKEN from macOS Keychain.
#
#   ./scripts/r2-wrangler.sh r2 bucket list
#   ./scripts/r2-wrangler.sh r2 bucket info <bucket-name>
#   ./scripts/r2-wrangler.sh r2 object get <bucket>/<key> --file=./out
#
# For bulk upload/sync, use scripts/r2-sync.sh (rclone) instead.
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if ! command -v wrangler >/dev/null 2>&1; then
  echo "r2-wrangler: wrangler not found — install with: brew install wrangler" >&2
  exit 1
fi

# shellcheck source=scripts/wrangler-from-keychain.sh
source "$ROOT/scripts/wrangler-from-keychain.sh"

if [[ $# -eq 0 ]]; then
  echo "Usage: $0 <wrangler-args...>" >&2
  echo "Examples:" >&2
  echo "  $0 r2 bucket list" >&2
  echo "  $0 r2 bucket info <bucket-name>" >&2
  exit 2
fi

exec wrangler "$@"
