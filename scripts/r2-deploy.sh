#!/usr/bin/env bash
# R2 deploy entry point — directory sync (rclone) and Cloudflare ops (wrangler).
#
#   ./scripts/r2-deploy.sh sync <local-dir> [remote-prefix]   # default
#   ./scripts/r2-deploy.sh wrangler <wrangler-args...>
#
# See docs/r2-deploy.md for setup, Keychain storage, and when to use each tool.
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

usage() {
  cat <<'EOF' >&2
Usage:
  r2-deploy.sh sync <local-dir> [remote-prefix]
  r2-deploy.sh wrangler <wrangler-args...>

  sync      Upload/sync a local directory to R2 (rclone). Default subcommand.
  wrangler  Cloudflare-specific CLI ops (bucket info, object get, etc.)

Examples:
  ./scripts/r2-deploy.sh sync ./build/dist
  ./scripts/r2-deploy.sh sync ./build/dist releases/v1.0.0
  ./scripts/r2-deploy.sh wrangler r2 bucket list
EOF
  exit 2
}

CMD="${1:-sync}"
if [[ $# -eq 0 ]]; then
  usage
fi

case "$CMD" in
  sync)
    shift
    exec "$ROOT/scripts/r2-sync.sh" "$@"
    ;;
  wrangler)
    shift
    exec "$ROOT/scripts/r2-wrangler.sh" "$@"
    ;;
  -h | --help | help)
    usage
    ;;
  *)
    # Treat first arg as local dir when subcommand omitted
    exec "$ROOT/scripts/r2-sync.sh" "$@"
    ;;
esac
