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

# Content checks use added lines only — deletions must not block secret removal.
ADDED="$(echo "$DIFF" | grep '^+' | grep -v '^+++' || true)"

# --- Secret patterns (added lines only) ---
echo "$ADDED" | grep -qE 'ghp_[A-Za-z0-9]{20,}' && report "GitHub PAT (ghp_) detected"
echo "$ADDED" | grep -qE 'gho_[A-Za-z0-9]{20,}' && report "GitHub OAuth token (gho_) detected"
echo "$ADDED" | grep -qE 'github_pat_[A-Za-z0-9_]{20,}' && report "GitHub fine-grained PAT detected"
echo "$ADDED" | grep -qE 'AKIA[0-9A-Z]{16}' && report "AWS access key ID detected"
echo "$ADDED" | grep -qE '(aws_secret_access_key|AWS_SECRET_ACCESS_KEY)[[:space:]]*[=:]' && report "AWS secret key reference detected"
echo "$ADDED" | grep -qE '"private_key"[[:space:]]*:[[:space:]]*"' && report "JSON private_key field detected"
echo "$ADDED" | grep -qE 'Bearer eyJ[A-Za-z0-9_-]{10,}\.' && report "Bearer JWT detected"
echo "$ADDED" | grep -qE '(password|passwd|secret|api[_-]?key)[[:space:]]*[=:][[:space:]]*["'"'"'][^"'"'"']{8,}' && report "Hardcoded credential assignment detected"
# R2 / Cloudflare literals (skip Keychain loaders: values start with $ not a literal)
echo "$ADDED" | grep -qE '(R2_SECRET_ACCESS_KEY|R2_ACCESS_KEY_ID|CLOUDFLARE_API_TOKEN)[[:space:]]*=[[:space:]]*["'"'"'][^$"'"'"']{8,}' && report "R2/Cloudflare credential literal detected"

# --- Forbidden files in diff ---
# Match the post-change path (b/...). New files appear as "a/dev/null b/<path>".
echo "$DIFF" | grep -qE '^diff --git .* b/\.env$' && report ".env file must not be committed"
echo "$DIFF" | grep -qE '^diff --git .* b/\.cursor/mcp\.json$' && report ".cursor/mcp.json must not exist in project (use ~/.cursor/mcp.json + Keychain)"
echo "$DIFF" | grep -qE '^diff --git .* b/local\.properties$' && report "local.properties must not be committed"

# --- Local path leaks (added lines only) ---
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
