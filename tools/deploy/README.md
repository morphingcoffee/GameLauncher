# R2 deploy and manifest catalog

Upload artifacts to [Cloudflare R2](https://developers.cloudflare.com/r2/) via [rclone](https://rclone.org/). Credentials in **macOS Keychain** (local) or **GitHub Secrets** (CI). Bucket settings in local `.env` (gitignored).

Manifest schema and CI workflows for issue [#3](https://github.com/morphingcoffee/GameLauncher/issues/3).

## Bucket layout

```
manifest.json                         # live catalog (launcher fetches on startup)
assets/{game_id}/thumbnail.webp       # browse metadata (mutable)
games/{game_id}/versions.json         # full version history (lazy-loaded)
games/{game_id}/v{version}/{platform}/game.zip   # immutable builds
```

Platform keys: `windows-x64`, `macos-arm64`, `macos-x64`.

## R2 API token

Create **one** token in **Cloudflare → R2 → Manage R2 API Tokens**:

| Setting | Value |
|---------|--------|
| Permission | **Object Read & Write** (not Admin) |
| Bucket | Your bucket only (not all buckets) |

Cloudflare static tokens are scoped to the **bucket**, not individual object prefixes. All deploy workflows and local tools share this token.

Object Read & Write does not grant delete — a mistaken `rclone sync` cannot remove remote objects when delete is denied at the token level.

Prefix-level scoping (e.g. `manifest.json` only vs `games/**`) requires [Cloudflare Temporary Credentials](https://developers.cloudflare.com/r2/api/s3/temporary-credentials/) and is not implemented yet.

### GitHub Secrets (repository settings)

| Secret | Value |
|--------|--------|
| `R2_ACCOUNT_ID` | Cloudflare account ID |
| `R2_BUCKET_NAME` | Bucket name |
| `R2_PUBLIC_CDN_BASE_URL` | Public CDN origin, no trailing slash |
| `R2_ACCESS_KEY_ID` | Token access key |
| `R2_SECRET_ACCESS_KEY` | Token secret |

Remove legacy secrets if present: `R2_MANIFEST_*`, `R2_GAME_*`.

## Local setup

```bash
brew install rclone
cp .env.example .env   # R2_ACCOUNT_ID, R2_BUCKET_NAME, R2_PUBLIC_CDN_BASE_URL
```

1. Create an R2 bucket and enable public access (`r2.dev` or custom domain).
2. Create the **Object Read & Write** token scoped to that bucket.
3. Store keys in Keychain (local uploads and env-check):

```bash
security add-generic-password -U -a "$USER" -s "gamelauncher-r2-access-key-id" -w "ACCESS_KEY_ID"
security add-generic-password -U -a "$USER" -s "gamelauncher-r2-secret-access-key" -w "SECRET_ACCESS_KEY"
```

Re-run with `-U` after rotating a token; update **both** items.

## Test and upload

```bash
# Preflight: env vars, credentials, bucket connectivity, no-delete check
python3 tools/deploy/r2_env_check.py

# Quick auth smoke test (read + write only)
python3 tools/deploy/r2_test_auth.py
```

`r2_env_check.py` writes UUID-suffixed probes under `games/.gamelauncher-r2-probe/`. Cleanup is best-effort — when the token correctly lacks delete permission, probe objects remain until removed via the R2 dashboard.

In CI, run **Actions → R2 env check → Run workflow** (see [`.github/workflows/r2-env-check.yml`](../.github/workflows/r2-env-check.yml)) after merge.

Stage artifacts in the gitignored top-level `games/` directory, then upload:

```bash
# Game binary — prefer --copy (append-only, no remote deletes)
python3 tools/deploy/r2_deploy.py --copy ./games/cool_game/v1.2.0/macos-arm64 games/cool_game/v1.2.0/macos-arm64
python3 tools/deploy/r2_deploy.py --copy ./games/cool_game/v1.2.0/windows-x64 games/cool_game/v1.2.0/windows-x64

# Thumbnail / assets
python3 tools/deploy/r2_deploy.py --copy ./games/cool_game/assets assets/cool_game
```

Default mode is **sync** (remote prefix mirrors local, may delete extras). Sync runs a dry-run first; pass `--allow-deletes` only after reviewing the delete list.

## Release workflow (Phase 1 — manual)

1. Upload binaries to R2 with `r2_deploy.py --copy` (see above).
2. **Actions → Register game version → Run workflow** with:
   - `game_id`, `version`, `platforms` (e.g. `macos-arm64,windows-x64`)
   - `builds_json` — per-platform metadata, e.g.  
     `{"macos-arm64":{"executable_path":"Game.app/Contents/MacOS/Game","file_size_bytes":12345,"sha256":"..."}}`
   - For a **new** game, also set `title`, `description`, `thumbnail_url`.
   - Uncheck **update_catalog_latest** when registering an older build for "Other versions" only.
3. The workflow updates `games/{id}/versions.json` in R2, updates `manifests/manifest.json`, uploads live `manifest.json` to R2, and commits the git manifest change.
4. Pushes from `github-actions[bot]` do not trigger other workflows — register uploads the live manifest directly. You can also run **Actions → Deploy manifest** manually to republish from git.

Catalog source of truth: git history of [`manifests/manifest.json`](../manifests/manifest.json). Roll back with `git revert` and push.

## Tests

Deploy logic unit tests use Python stdlib only (no rclone/R2 required):

```bash
cd tools/deploy && python3 -m unittest discover -s tests -v
```

Republish live `manifest.json` from git without re-registering:

```bash
python3 tools/deploy/r2_publish_manifest.py
```

## Security

- Run `tools/dev/scan-secrets.sh` before commit.
- See `.cursor/skills/secret-hygiene/SKILL.md`.
