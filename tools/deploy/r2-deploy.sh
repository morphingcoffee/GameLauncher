#!/usr/bin/env bash
# Sync or copy a local directory to Cloudflare R2 via rclone (S3-compatible API).
#
#   ./tools/deploy/r2-deploy.sh [--copy] [--allow-deletes] <local-dir> [remote-prefix]
#
# Requires: rclone, .env with R2_ACCOUNT_ID and R2_BUCKET_NAME, Keychain R2 keys.
# See tools/deploy/README.md for setup and Keychain storage.
#
# Prefer --copy for game binaries (append-only versioned prefixes). Use default sync
# only when you intentionally want the remote prefix to mirror local exactly.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [[ -z "$ROOT" ]]; then
  ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
fi

usage() {
  cat <<'EOF' >&2
Usage: r2-deploy.sh [--copy] [--allow-deletes] <local-dir> [remote-prefix]

  local-dir       Directory to upload
  remote-prefix   Optional path inside the bucket (no leading slash)

  --copy          Upload only (rclone copy). Does not delete remote objects.
                  Recommended for immutable game builds under versioned prefixes.

  sync (default)  Makes the remote prefix match local — files/dirs on the remote that
                  are not in local-dir are DELETED. A dry-run runs first; if deletes
                  are needed, re-run with --allow-deletes after reviewing the list.

Examples:
  # Game build (append-only, safe default)
  ./tools/deploy/r2-deploy.sh --copy ./build/v1.2.0/macos-arm64 games/cool_game/v1.2.0/macos-arm64

  # Sync a prefix that should exactly mirror local (use with care)
  ./tools/deploy/r2-deploy.sh ./build/dist
  ./tools/deploy/r2-deploy.sh ./build/dist releases/v1.0.0
  ./tools/deploy/r2-deploy.sh --allow-deletes ./build/dist releases/v1.0.0
EOF
  exit 2
}

COPY_MODE=false
ALLOW_DELETES=false
REMOTE_PREFIX=""

while [[ $# -gt 0 && "$1" == -* ]]; do
  case "$1" in
    --copy)
      COPY_MODE=true
      shift
      ;;
    --allow-deletes)
      ALLOW_DELETES=true
      shift
      ;;
    -h | --help | help)
      usage
      ;;
    --)
      shift
      break
      ;;
    -*)
      echo "r2-deploy: unknown option: $1" >&2
      usage
      ;;
  esac
done

[[ $# -ge 1 ]] || usage

LOCAL_DIR="$1"
shift

while [[ $# -gt 0 ]]; do
  case "$1" in
    --copy)
      COPY_MODE=true
      shift
      ;;
    --allow-deletes)
      ALLOW_DELETES=true
      shift
      ;;
    -*)
      echo "r2-deploy: unknown option: $1" >&2
      usage
      ;;
    *)
      if [[ -n "$REMOTE_PREFIX" ]]; then
        echo "r2-deploy: too many arguments" >&2
        usage
      fi
      REMOTE_PREFIX="$1"
      shift
      ;;
  esac
done

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
# shellcheck source=r2-from-keychain.sh
source "$SCRIPT_DIR/r2-from-keychain.sh" || {
  echo "r2-deploy: failed to load Keychain credentials" >&2
  exit 1
}

: "${R2_BUCKET_NAME:?Set R2_BUCKET_NAME in .env (see .env.example)}"

# shellcheck source=r2-rclone-env.sh
source "$SCRIPT_DIR/r2-rclone-env.sh"
r2_rclone_configure

REMOTE="${RCLONE_REMOTE}:${R2_BUCKET_NAME}"
if [[ -n "$REMOTE_PREFIX" ]]; then
  REMOTE="${REMOTE}/${REMOTE_PREFIX}"
fi

mode="sync"
[[ "$COPY_MODE" == true ]] && mode="copy"
echo "r2-deploy: $LOCAL_DIR -> r2://${R2_BUCKET_NAME}/${REMOTE_PREFIX:-} ($mode)" >&2

dry_log="$(mktemp)"
cleanup_deploy() {
  local rc=$?
  if (( rc != 0 )) && [[ -f "${dry_log:-}" ]]; then
    echo "r2-deploy: dry-run output:" >&2
    cat "$dry_log" >&2
  fi
  rm -f "${dry_log:-}"
  r2_rclone_cleanup
}
trap cleanup_deploy EXIT

if [[ "$COPY_MODE" == true ]]; then
  echo "r2-deploy: copy (no remote deletes)..." >&2
  rclone copy "$LOCAL_DIR" "$REMOTE" \
    "${RCLONE_FLAGS[@]}" \
    --progress \
    --retries 5 \
    --retries-sleep 5s \
    --low-level-retries 10 \
    --stats-one-line \
    --stats 5s
  exit 0
fi

echo "r2-deploy: dry-run (checking for remote deletes)..." >&2
if ! rclone sync "$LOCAL_DIR" "$REMOTE" "${RCLONE_FLAGS[@]}" --dry-run -v --stats-one-line >"$dry_log" 2>&1; then
  echo "r2-deploy: dry-run failed" >&2
  exit 1
fi

dry_run_destructive='Skipped (delete|remove directory) as --dry-run is set'
if grep -qE "$dry_run_destructive" "$dry_log"; then
  echo "r2-deploy: WARNING — sync would DELETE remote files/dirs not present in $LOCAL_DIR:" >&2
  grep -E "$dry_run_destructive" "$dry_log" \
    | sed -E 's/^.*NOTICE: ([^:]+):.*/  \1/' >&2
  if [[ "$ALLOW_DELETES" != true ]]; then
    echo "r2-deploy: aborted — re-run with --allow-deletes to proceed" >&2
    exit 1
  fi
  echo "r2-deploy: --allow-deletes set, continuing with sync" >&2
fi

rclone sync "$LOCAL_DIR" "$REMOTE" \
  "${RCLONE_FLAGS[@]}" \
  --progress \
  --retries 5 \
  --retries-sleep 5s \
  --low-level-retries 10 \
  --stats-one-line \
  --stats 5s
