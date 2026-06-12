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

## R2 API tokens (scoped, no delete)

Create **two** tokens in **Cloudflare → R2 → Manage R2 API Tokens**. Use **Object Read & Write** only — do **not** grant delete permission.

| Token | Prefix scope | Used by |
|-------|--------------|---------|
| `manifest-deploy` | `manifest.json` | GitHub Actions [`deploy-manifest.yml`](../.github/workflows/deploy-manifest.yml) |
| `game-upload` | `games/**`, `assets/**` | Local `r2_deploy.py`, [Register game version](../../.github/workflows/register-game-version.yml) |

Even a mistaken `rclone sync` against a version prefix cannot delete older builds when the token lacks delete permission.

### GitHub Secrets (repository settings)

| Secret | Token |
|--------|-------|
| `R2_ACCOUNT_ID` | Cloudflare account ID |
| `R2_BUCKET_NAME` | Bucket name |
| `R2_PUBLIC_CDN_BASE_URL` | Public CDN origin, no trailing slash |
| `R2_MANIFEST_ACCESS_KEY_ID` / `R2_MANIFEST_SECRET_ACCESS_KEY` | `manifest-deploy` |
| `R2_GAME_ACCESS_KEY_ID` / `R2_GAME_SECRET_ACCESS_KEY` | `game-upload` |

## Local setup

```bash
brew install rclone
cp .env.example .env   # R2_ACCOUNT_ID, R2_BUCKET_NAME, R2_PUBLIC_CDN_BASE_URL
```

1. Create an R2 bucket and enable public access (`r2.dev` or custom domain).
2. Create the **game-upload** token (scoped to `games/**` and `assets/**`).
3. Store keys in Keychain (local uploads):

```bash
security add-generic-password -U -a "$USER" -s "gamelauncher-r2-access-key-id" -w "ACCESS_KEY_ID"
security add-generic-password -U -a "$USER" -s "gamelauncher-r2-secret-access-key" -w "SECRET_ACCESS_KEY"
```

Re-run with `-U` after rotating a token; update **both** items.

## Test and upload

```bash
# Preflight: env vars, Keychain keys, and permission scope (read/write boundaries, no delete)
python3 tools/deploy/r2_env_check.py

# Quick auth smoke test (read + write only)
python3 tools/deploy/r2_test_auth.py
```

`r2_env_check.py` writes UUID-suffixed probes under `.gamelauncher-r2-probe/`. Cleanup is best-effort — when tokens correctly lack delete permission, probe objects remain until removed with a delete-capable token or the R2 dashboard.

For GitHub Actions secrets (both tokens), run **Actions → R2 env check → Run workflow** (see [`.github/workflows/r2-env-check.yml`](../.github/workflows/r2-env-check.yml)), or export secrets locally and run:

```bash
python3 tools/deploy/r2_env_check.py --ci
```

Checks include:

| Mode | Credentials validated |
|------|------------------------|
| `--local` (default) | `.env` + Keychain game-upload token |
| `--ci` | All seven `R2_*` GitHub secrets; both manifest and game tokens |

Permission probes confirm each token can read/write only within its scoped prefix and cannot delete objects.

```bash
# Game binary — prefer --copy (append-only, no remote deletes)
python3 tools/deploy/r2_deploy.py --copy ./build/v1.2.0/macos-arm64 games/cool_game/v1.2.0/macos-arm64
python3 tools/deploy/r2_deploy.py --copy ./build/v1.2.0/windows-x64 games/cool_game/v1.2.0/windows-x64

# Thumbnail / assets
python3 tools/deploy/r2_deploy.py --copy ./art assets/cool_game
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
3. The workflow updates `games/{id}/versions.json` in R2, commits `manifests/manifest.json`, and pushes.
4. Push to `main` triggers **Deploy manifest**, uploading the live `manifest.json`.

Catalog source of truth: git history of [`manifests/manifest.json`](../manifests/manifest.json). Roll back with `git revert` and push.

## Tests

Deploy logic unit tests use Python stdlib only (no rclone/R2 required):

```bash
cd tools/deploy && python3 -m unittest discover -s tests -v
```

## Security

- Run `tools/dev/scan-secrets.sh` before commit.
- See `.cursor/skills/secret-hygiene/SKILL.md`.
