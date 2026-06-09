#!/usr/bin/env bash
# Scans staged diff (pre-commit) or uncommitted + unpushed changes (pre-push).
# Exit 0 = clean, 1 = findings (blocks commit/push).
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT"

MODE="${1:-staged}"
if [[ "$MODE" == "staged" ]]; then
  LABEL="staged"
elif [[ "$MODE" == "all" ]]; then
  LABEL="push"
else
  echo "Usage: $0 [staged|all]" >&2
  exit 2
fi

# Skip if not a git repo
if ! git rev-parse --git-dir >/dev/null 2>&1; then
  exit 0
fi

push_scan_base() {
  if git rev-parse --verify '@{u}' >/dev/null 2>&1; then
    echo '@{u}'
    return 0
  fi
  local default_branch="main"
  local origin_head
  if origin_head="$(git symbolic-ref --quiet refs/remotes/origin/HEAD 2>/dev/null)"; then
    default_branch="${origin_head#refs/remotes/origin/}"
  fi
  if git rev-parse --verify "origin/${default_branch}" >/dev/null 2>&1; then
    echo "origin/${default_branch}"
    return 0
  fi
  return 1
}

collect_diff() {
  local diff=""
  local part

  if [[ "$MODE" == "staged" ]]; then
    git diff --cached --diff-filter=ACMRTUXB --no-color 2>/dev/null || true
    return 0
  fi

  part="$(git diff HEAD --diff-filter=ACMRTUXB --no-color 2>/dev/null || true)"
  if [[ -n "$part" ]]; then
    diff="$part"
  fi

  local base
  if base="$(push_scan_base)"; then
    part="$(git diff "${base}...HEAD" --diff-filter=ACMRTUXB --no-color 2>/dev/null || true)"
    if [[ -n "$part" ]]; then
      if [[ -n "$diff" ]]; then
        diff="${diff}"$'\n'"${part}"
      else
        diff="$part"
      fi
    fi
  fi

  printf '%s' "$diff"
}

DIFF="$(collect_diff)"
if [[ -z "$DIFF" ]]; then
  exit 0
fi

FOUND=0
report() {
  echo "SECRET-SCAN [$LABEL]: $1" >&2
  FOUND=1
}

# --- Secret patterns ---
echo "$DIFF" | grep -qE 'ghp_[A-Za-z0-9]{20,}' && report "GitHub PAT (ghp_) detected"
echo "$DIFF" | grep -qE 'gho_[A-Za-z0-9]{20,}' && report "GitHub OAuth token (gho_) detected"
echo "$DIFF" | grep -qE 'github_pat_[A-Za-z0-9_]{20,}' && report "GitHub fine-grained PAT detected"
echo "$DIFF" | grep -qE 'AKIA[0-9A-Z]{16}' && report "AWS access key ID detected"
echo "$DIFF" | grep -qE '(aws_secret_access_key|AWS_SECRET_ACCESS_KEY)\s*[=:]' && report "AWS secret key reference detected"
echo "$DIFF" | grep -qE '"private_key"\s*:\s*"' && report "JSON private_key field detected"
echo "$DIFF" | grep -qE 'Bearer eyJ[A-Za-z0-9_-]{10,}\.' && report "Bearer JWT detected"
echo "$DIFF" | grep -qE '(password|passwd|secret|api[_-]?key)[[:space:]]*[=:][[:space:]]*["'"'"'][^"'"'"']{8,}' && report "Hardcoded credential assignment detected"

# --- Forbidden files in diff ---
echo "$DIFF" | grep -qE '^diff --git a/\.env ' && report ".env file must not be committed"
echo "$DIFF" | grep -qE '^diff --git a/\.cursor/mcp\.json ' && report ".cursor/mcp.json must not exist in project (use ~/.cursor/mcp.json + Keychain)"
echo "$DIFF" | grep -qE '^diff --git a/local\.properties ' && report "local.properties must not be committed"

# --- Local path leaks (in added lines only) ---
ADDED="$(echo "$DIFF" | grep '^+' | grep -v '^+++' || true)"
if echo "$ADDED" | grep -qE '/Users/[A-Za-z0-9._-]+/'; then
  report "macOS user home path in added lines"
fi
if echo "$ADDED" | grep -qE 'C:\\\\Users\\\\'; then
  report "Windows user path in added lines"
fi

# --- Auth in URLs ---
if echo "$ADDED" | grep -qE 'https?://[^[:space:]]+[?&](token|secret|key|signature)='; then
  report "URL with auth query parameter detected"
fi

if [[ "$FOUND" -eq 1 ]]; then
  echo "" >&2
  echo "Scan failed. Remove secrets/local paths or move values to macOS Keychain + .env (gitignored)." >&2
  echo "See .cursor/skills/secret-hygiene/SKILL.md" >&2
  exit 1
fi

exit 0
