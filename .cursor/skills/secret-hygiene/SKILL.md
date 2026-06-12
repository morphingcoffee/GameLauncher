---
name: secret-hygiene
description: >-
  Pre-commit and pre-push secret scanning workflow for the public GameLauncher
  repo. Use before git commit, git push, or when adding config files, env vars,
  URLs, API keys, or CDN/bucket credentials.
---

# Secret Hygiene

This repo is **public**. Never commit credentials, signing keys, or machine-specific paths.

## GitHub PAT policy (mandatory)

The PAT must **only** live in **macOS Keychain**, injected via `security` — **never** in any project file (including gitignored `.cursor/mcp.json`, `.env`, or Cursor project settings committed to the repo).

### 1. Store in Keychain (once)

```bash
security add-generic-password -U \
  -a "$USER" -s "gamelauncher-github-pat" -w "TOKEN"
```

### 2. Global Cursor MCP config (not in this repo)

Copy [`tools/dev/mcp-github.global.json.example`](../../../tools/dev/mcp-github.global.json.example) to **`~/.cursor/mcp.json`**.

That template uses `"Bearer ${env:GITHUB_PAT}"` — no literal token in any file.

### 3. Inject PAT before Cursor starts

macOS GUI apps do not read `.zshrc`. Use one of:

**Option A — launch script (recommended):**

```bash
./tools/dev/open-cursor-with-github.sh
```

**Option B — source in terminal, then open:**

```bash
source tools/dev/github-pat-from-keychain.sh
open -a Cursor .
```

**Option C — login shell export** (`~/.zprofile`):

```bash
# Loads PAT from Keychain when a login shell starts
export GITHUB_PAT=$(security find-generic-password -a "$USER" -s "gamelauncher-github-pat" -w 2>/dev/null)
export GH_TOKEN="$GITHUB_PAT"
```

Still launch Cursor via Option A if the Dock app does not see `GITHUB_PAT`.

### 4. gh CLI

Either `gh auth login` (Keychain-backed) or:

```bash
source tools/dev/github-pat-from-keychain.sh
gh api user
```

## Before every commit

1. Run `tools/dev/scan-secrets.sh` on staged files (pre-commit hook does this automatically once configured)
2. Confirm `.env`, `.cursor/mcp.json`, `local.properties`, `*.pem` are **not** staged
3. Use placeholders in committed code; document env var names in `.env.example`

## Before every push

1. Re-run `tools/dev/scan-secrets.sh` (Cursor hook blocks `git push` on failure)
2. Review `git log` on the branch for accidental secret commits

## Never commit

- GitHub PATs (`ghp_`, `gho_`, `github_pat_`)
- AWS keys (`AKIA…`), GCS service account JSON, R2/S3 secrets
- CDN signing keys, webhook secrets, Bearer JWTs in source
- `.env`, project `.cursor/mcp.json`, `local.properties`
- URLs with `token=`, `secret=`, `X-Amz-Signature` auth query params

## Future CDN / bucket keys

Same Keychain pattern:

```bash
security add-generic-password -U -a "$USER" -s "gamelauncher-<name>" -w "VALUE"
```

Load at runtime via `security find-generic-password` in app code or sourced shell scripts — not in config files.

## If a secret was committed

1. **Do not push**
2. `git reset` to unstage / undo the commit
3. If already pushed: rotate the secret immediately, then seek history rewrite guidance

## Enable git hooks in this repo

```bash
git config core.hooksPath .githooks
```
