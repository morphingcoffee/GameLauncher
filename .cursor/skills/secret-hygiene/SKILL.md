---
name: secret-hygiene
description: >-
  Pre-commit and pre-push secret scanning workflow for the public GameLauncher
  repo. Use before git commit, git push, or when adding config files, env vars,
  URLs, API keys, or CDN/bucket credentials.
---

# Secret Hygiene

This repo is **public**. Never commit credentials, signing keys, or machine-specific paths.

## Before every commit

1. Run `scripts/scan-secrets.sh` on staged files (pre-commit hook does this automatically once configured)
2. Confirm `.env`, `.cursor/mcp.json`, `local.properties`, `*.pem` are **not** staged
3. Use placeholders in committed code; document env var names in `.env.example`

## Before every push

1. Re-run `scripts/scan-secrets.sh` (Cursor hook blocks `git push` on failure)
2. Review `git log` on the branch for accidental secret commits

## Never commit

- GitHub PATs (`ghp_`, `gho_`, `github_pat_`)
- AWS keys (`AKIA…`), GCS service account JSON, R2/S3 secrets
- CDN signing keys, webhook secrets, Bearer JWTs in source
- `.env`, `.cursor/mcp.json`, `local.properties`
- URLs with `token=`, `secret=`, `X-Amz-Signature` auth query params

## macOS Keychain (recommended)

Store secrets locally in Keychain — not in the repo.

### GitHub PAT (for Cursor MCP or scripts)

```bash
# Store (run once; replace TOKEN)
security add-generic-password -U \
  -a "$USER" -s "gamelauncher-github-pat" -w "TOKEN"

# Retrieve into env for a session (never echo to terminal in shared logs)
export GITHUB_PAT=$(security find-generic-password -a "$USER" -s "gamelauncher-github-pat" -w)
```

Paste the PAT into **Cursor → Settings → Tools & MCP → github** (pencil icon), or copy `.cursor/mcp.json.example` → `.cursor/mcp.json` locally and substitute — `.cursor/mcp.json` is gitignored.

### gh CLI

`gh auth login` stores credentials in Keychain by default. Prefer this over committing tokens.

### Future CDN / bucket keys

When added, use the same pattern:

```bash
security add-generic-password -U -a "$USER" -s "gamelauncher-<name>" -w "VALUE"
```

Load at runtime via env var populated in your shell profile — not in source files.

## If a secret was committed

1. **Do not push**
2. `git reset` to unstage / undo the commit
3. If already pushed: rotate the secret immediately, then seek history rewrite guidance

## Enable git hooks in this repo

```bash
git config core.hooksPath .githooks
```
