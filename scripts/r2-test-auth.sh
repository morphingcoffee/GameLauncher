#!/usr/bin/env bash
# Verify R2 Keychain credentials (read + write) without affecting your shell.
#
#   ./scripts/r2-test-auth.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if ! command -v rclone >/dev/null 2>&1; then
  echo "r2-test-auth: rclone not found — brew install rclone" >&2
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
  echo "r2-test-auth: Keychain load failed — see docs/r2-deploy.md" >&2
  exit 1
}

: "${R2_BUCKET_NAME:?Set R2_BUCKET_NAME in .env (see .env.example)}"

# shellcheck source=scripts/r2-rclone-env.sh
source "$ROOT/scripts/r2-rclone-env.sh"
r2_rclone_configure

REMOTE="${RCLONE_REMOTE}:${R2_BUCKET_NAME}"
PROBE_FILE=""
PROBE_KEY=""

cleanup_all() {
  if [[ -n "$PROBE_FILE" ]]; then
    rm -f "$PROBE_FILE"
  fi
  if [[ -n "$PROBE_KEY" ]]; then
    rclone deletefile "${REMOTE}/${PROBE_KEY}" >/dev/null 2>&1 || true
  fi
  r2_rclone_cleanup
}
trap cleanup_all EXIT

fail_read() {
  echo "r2-test-auth: read failed." >&2
  echo "  Check .env (R2_ACCOUNT_ID, R2_BUCKET_NAME), token bucket scope, and Keychain S3 keys." >&2
  echo "  SignatureDoesNotMatch → recreate token, update both Keychain items (no stray whitespace)." >&2
  exit 1
}

fail_write() {
  echo "r2-test-auth: write failed (PutObject denied)." >&2
  echo "  Token is likely read-only — recreate with Object Read & Write, then update both Keychain items." >&2
  exit 1
}

echo "r2-test-auth: read — r2://${R2_BUCKET_NAME}/" >&2
rclone lsf "$REMOTE" --max-depth 1 | head -20
lsf_status=${PIPESTATUS[0]}
# head closes the pipe after 20 lines; rclone exits 141 (SIGPIPE) — not an auth failure
if (( lsf_status != 0 && lsf_status != 141 )); then
  fail_read
fi

PROBE_FILE="$(mktemp)"
PROBE_KEY=".gamelauncher-r2-probe/auth-$(date +%s).txt"
echo "probe" >"$PROBE_FILE"

echo "r2-test-auth: write — ${PROBE_KEY}" >&2
if ! rclone copyto "$PROBE_FILE" "${REMOTE}/${PROBE_KEY}"; then
  fail_write
fi

echo "r2-test-auth: OK" >&2
