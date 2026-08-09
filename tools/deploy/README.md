# R2 deploy and manifest catalog

Upload artifacts to [Cloudflare R2](https://developers.cloudflare.com/r2/) via [rclone](https://rclone.org/). Credentials in **macOS Keychain** (local) or **GitHub Secrets** (CI). Bucket settings in local `.env` (gitignored).

Canonical home for bucket layout, credentials, deploy/register/publish flows, and related Actions. App architecture lives in the [root README](../../README.md). Staging path layout: [`r2_staging/README.md`](../../r2_staging/README.md).

## Repository layout

| Path | Git | Purpose |
|------|-----|---------|
| `manifests/manifest.json` | Yes | Live catalog (latest builds per game + launcher channels) |
| `manifests/games/{game_id}/versions.json` | Yes | Version history (source of truth) |
| `manifests/games/{game_id}/releases/{version}.json` | Yes | Optional overrides (`executable_path`, `released_at`) |
| `r2_staging/games/...` | **No** | Local mirror of R2 game build prefixes |
| `r2_staging/assets/...` | **No** | Local mirror of R2 asset prefixes |
| `r2_staging/launcher/releases/...` | **No** | Local mirror of launcher installer prefixes |

Paths under `r2_staging/` match R2 object keys — the copy destination is the same path without the `r2_staging/` prefix.

## Bucket layout (R2)

```
manifest.json                                              # live catalog (launcher fetches on startup)
assets/{game_id}/thumbnail.webp                            # browse metadata (mutable)
games/{game_id}/versions.json                              # full version history (lazy-loaded)
games/{game_id}/v{version}/{platform}/game.zip             # immutable game builds
launcher/releases/{artifact_version}/{channel}/…           # immutable launcher installers
```

Game platform keys: `windows-x64`, `macos-arm64`, `macos-x64`.

Launcher channel keys: `windows-x64-msi`, `windows-x64-portable`, `macos-arm64-dmg`, `macos-x64-dmg`.

## R2 API token

Create **one** token in **Cloudflare → R2 → Manage R2 API Tokens**:

| Setting | Value |
|---------|--------|
| Permission | **Object Read & Write** (not Admin) |
| Bucket | Your bucket only (not all buckets) |

Cloudflare static tokens are scoped to the **bucket**, not individual object prefixes. All deploy workflows and local tools share this token.

Object Read & Write includes S3 delete for objects in the scoped bucket. Prefer `rclone copy` via `r2_deploy.py --copy` for versioned game/launcher blobs (append-only). Default **sync** may delete remote objects — review the dry-run and pass `--allow-deletes` only when intentional.

Prefix-level scoping (e.g. `manifest.json` only vs `games/**`) requires [Cloudflare Temporary Credentials](https://developers.cloudflare.com/r2/api/temporary-credentials/) and is not implemented yet.

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

Re-run with `-U` after rotating a token; update **both** items. Do not commit secrets, Keychain values, or machine-specific home paths.

## Test connectivity

```bash
# Preflight: env vars, credentials, bucket read/write/delete connectivity
python3 tools/deploy/r2_env_check.py

# Quick auth smoke test (read + write only)
python3 tools/deploy/r2_test_auth.py
```

`r2_env_check.py` writes UUID-suffixed probes under `games/.gamelauncher-r2-probe/` and deletes them when the token allows delete.

In CI, run **Actions → R2 env check → Run workflow** (see [`.github/workflows/r2-env-check.yml`](../../.github/workflows/r2-env-check.yml)) after merge.

## Upload blobs (`r2_deploy.py`)

Prefer **`--copy`** for versioned game and launcher prefixes (append-only; does not delete remote objects):

```bash
python3 tools/deploy/r2_deploy.py --copy \
  ./r2_staging/games/cool_game/v1.2.0/macos-arm64 \
  games/cool_game/v1.2.0/macos-arm64

python3 tools/deploy/r2_deploy.py --copy \
  ./r2_staging/assets/cool_game \
  assets/cool_game
```

Default mode is **sync** (remote prefix mirrors local and **may delete** extras). Sync always dry-runs first; pass `--allow-deletes` only after reviewing the delete list.

## Register a game version (canonical local recipe)

```bash
# 1. Upload zip(s) to R2
python3 tools/deploy/r2_deploy.py --copy \
  ./r2_staging/games/krabs_v1/v0.0.1/windows-x64 \
  games/krabs_v1/v0.0.1/windows-x64

# 2. Register — scans staging zip for sha256/sizes, updates git JSON, publishes to R2
python3 tools/deploy/register_version.py krabs_v1 0.0.1 --platform windows-x64
# executable_path from manifests/games/krabs_v1/releases/0.0.1.json if present
# New games: pass --title, --description, --thumbnail-url (never placeholder copy)

# 3. Commit catalog changes
git add manifests/manifest.json manifests/games/
git commit -m "Register krabs_v1 v0.0.1"
```

### Patch metadata on an existing version

When catalog fields change (e.g. add `uncompressed_size_bytes`) but the zip is unchanged:

```bash
python3 tools/deploy/sync_versions_index.py krabs_v1 --merge-catalog
python3 tools/deploy/r2_publish_versions.py krabs_v1
```

Or pull from R2 first, then merge:

```bash
python3 tools/deploy/sync_versions_index.py krabs_v1 --from-r2 --force-r2 --merge-catalog --publish
```

### Publish-only commands

```bash
python3 tools/deploy/r2_publish_manifest.py
python3 tools/deploy/r2_publish_versions.py krabs_v1
```

### Catalog integrity audit

```bash
# Live R2 (credentials required)
python3 tools/deploy/r2_catalog_check.py
python3 tools/deploy/r2_catalog_check.py --game krabs_v1 --compare-git

# Offline / CI — git manifests only (no R2, Keychain, or network)
python3 tools/deploy/r2_catalog_check.py --offline
```

See [`.cursor/skills/r2-catalog-integrity/SKILL.md`](../../.cursor/skills/r2-catalog-integrity/SKILL.md). Repo-root `./tools/dev/verify.sh` includes the offline check.

## Launcher self-update publish (canonical recipe)

Prod installers stage under `r2_staging/launcher/releases/{artifact_version}/{channel}/`. Register patches the `launcher` block in `manifests/manifest.json` (per-channel `version`, sha256, CDN URLs). **`launcher_minimum_version`** is the forced-update floor — bump only with `--bump-minimum` on breaking changes; see [`launcher-minimum-version`](../../.cursor/skills/launcher-minimum-version/SKILL.md).

### CI (two workflows)

1. **Desktop installers** — build only (default run builds all four prod installers). Note the **run ID** from the Actions URL.
2. **Publish launcher release** — pass that run ID. **Require all four prod channels** (`require_all_channels`) defaults **on** for a full release; turn it off to publish only artifacts present in that run. Uploads blobs to R2, **commits and pushes** `manifests/manifest.json`, then publishes the manifest.

Channels: `windows-x64-msi`, `windows-x64-portable`, `macos-arm64-dmg`, `macos-x64-dmg`.

### Local

```bash
mkdir -p r2_staging/launcher/releases/0.0.1-build51/windows-x64-msi
cp GameLauncher-0.0.1-build51.msi r2_staging/launcher/releases/0.0.1-build51/windows-x64-msi/

# One-shot: register + upload + git commit/push + manifest publish
python3 tools/deploy/publish_launcher_release.py 0.0.1-build51

# Upload without committing/pushing manifests/manifest.json:
python3 tools/deploy/publish_launcher_release.py 0.0.1-build51 --skip-git-push
```

`publish_launcher_release.py` **commits and pushes** `manifests/manifest.json` by default. Use `--skip-git-push` when that must not happen. Narrow channels with `--channel` (repeatable).

## GitHub Actions register (CI / cross-repo)

1. Upload binaries to R2 with `r2_deploy.py --copy` (see above).
2. **Actions → Register game version → Run workflow** with:
   - `game_id`, `version`, `platforms` (e.g. `macos-arm64,windows-x64`)
   - `builds_json` — per-platform metadata, e.g.
     `{"macos-arm64":{"executable_path":"Game.app/Contents/MacOS/Game","file_size_bytes":12345,"uncompressed_size_bytes":45678,"sha256":"..."}}`
   - For a **new** game, also set `title`, `description`, `thumbnail_url`.
   - Uncheck **update_catalog_latest** when registering an older build for "Other versions" only.
   - Set env `UPSERT=1` in workflow (or use `--patch` locally) to update an existing version.
3. The workflow updates R2 `versions.json`, updates git `manifests/manifest.json` and `manifests/games/{id}/versions.json`, uploads live `manifest.json` to R2, and commits.

Catalog source of truth: git history of [`manifests/manifest.json`](../../manifests/manifest.json) and [`manifests/games/`](../../manifests/games/). Roll back with `git revert` and republish. Manifest deploy on push to `main`: **Deploy manifest** ([`.github/workflows/deploy-manifest.yml`](../../.github/workflows/deploy-manifest.yml)).

## Tests

Deploy logic unit tests use Python stdlib only (no rclone/R2 required):

```bash
cd tools/deploy && python3 -m unittest discover -s tests -v
```

## Security

- Run `tools/dev/scan-secrets.sh` before commit.
- See [`.cursor/skills/secret-hygiene/SKILL.md`](../../.cursor/skills/secret-hygiene/SKILL.md).
