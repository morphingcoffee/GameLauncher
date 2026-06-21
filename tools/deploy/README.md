# R2 deploy and manifest catalog

Upload artifacts to [Cloudflare R2](https://developers.cloudflare.com/r2/) via [rclone](https://rclone.org/). Credentials in **macOS Keychain** (local) or **GitHub Secrets** (CI). Bucket settings in local `.env` (gitignored).

Manifest schema and CI workflows for issue [#3](https://github.com/morphingcoffee/GameLauncher/issues/3).

## Repository layout

| Path | Git | Purpose |
|------|-----|---------|
| `manifests/manifest.json` | Yes | Live catalog (latest builds per game) |
| `r2_staging/games/{game_id}/v{version}/{platform}/game.zip` | **No** | Local mirror of R2 build path before upload |
| `r2_staging/assets/{game_id}/` | **No** | Local mirror of R2 assets before upload |
| `manifests/games/{game_id}/versions.json` | Yes | Version history (source of truth) |
| `manifests/games/{game_id}/releases/{version}.json` | Yes | Optional overrides (`executable_path`, `released_at`) |

Paths under `r2_staging/` match R2 object keys — the copy destination is the same path without the `r2_staging/` prefix.

## Bucket layout (R2)

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

Object Read & Write includes S3 delete for objects in the scoped bucket. Avoid accidental remote deletes by using `rclone copy` (`r2_deploy.py --copy`) instead of sync unless you explicitly pass `--allow-deletes`.

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

Re-run with `-U` after rotating a token; update **both** items.

## Test and upload

```bash
# Preflight: env vars, credentials, bucket read/write/delete connectivity
python3 tools/deploy/r2_env_check.py

# Quick auth smoke test (read + write only)
python3 tools/deploy/r2_test_auth.py
```

`r2_env_check.py` writes UUID-suffixed probes under `games/.gamelauncher-r2-probe/` and deletes them when the token allows delete.

In CI, run **Actions → R2 env check → Run workflow** (see [`.github/workflows/r2-env-check.yml`](../.github/workflows/r2-env-check.yml)) after merge.

Stage binaries in the gitignored [`r2_staging/`](../r2_staging/) directory (paths mirror R2 keys), then upload:

```bash
# Game binary — prefer --copy (append-only, no remote deletes)
python3 tools/deploy/r2_deploy.py --copy \
  ./r2_staging/games/cool_game/v1.2.0/macos-arm64 \
  games/cool_game/v1.2.0/macos-arm64
python3 tools/deploy/r2_deploy.py --copy \
  ./r2_staging/games/cool_game/v1.2.0/windows-x64 \
  games/cool_game/v1.2.0/windows-x64

# Thumbnail / assets
python3 tools/deploy/r2_deploy.py --copy ./r2_staging/assets/cool_game assets/cool_game
```

Default mode is **sync** (remote prefix mirrors local, may delete extras). Sync runs a dry-run first; pass `--allow-deletes` only after reviewing the delete list.

## Local release workflow (recommended)

```bash
# 1. Upload zip(s) to R2
python3 tools/deploy/r2_deploy.py --copy \
  ./r2_staging/games/krabs_v1/v0.0.1/windows-x64 \
  games/krabs_v1/v0.0.1/windows-x64

# 2. Register — scans r2_staging zip for sha256/sizes, updates git JSON, publishes to R2
python3 tools/deploy/register_version.py krabs_v1 0.0.1 --platform windows-x64
# executable_path comes from manifests/games/krabs_v1/releases/0.0.1.json if present
# New games: pass --title, --description, --thumbnail-url (user-supplied — never placeholder copy)

# 3. Commit catalog changes
git add manifests/manifest.json manifests/games/
git commit -m "Register krabs_v1 v0.0.1"
```

### Launcher self-update publish

Prod installers live under `r2_staging/launcher/releases/{artifact_version}/{channel}/`. Register patches the `launcher` block in `manifests/manifest.json` (per-channel `version`, sha256, CDN URLs). **`launcher_minimum_version`** is the forced-update floor — bump only with `--bump-minimum` on breaking changes; see [`launcher-minimum-version`](../../.cursor/skills/launcher-minimum-version/SKILL.md).

**CI (two workflows):**

1. **Desktop installers** — build only. Check **Build Windows MSI (prod)** and **Build Windows portable ZIP (prod)** (defaults on). Note the run ID from the URL.
2. **Publish launcher release** — pass that run ID. Uploads to R2, commits and pushes `manifests/manifest.json`, publishes manifest.

**Local:**

```bash
# Stage prod MSI (example)
mkdir -p r2_staging/launcher/releases/0.0.1-build51/windows-x64-msi
cp GameLauncher-0.0.1-build51.msi r2_staging/launcher/releases/0.0.1-build51/windows-x64-msi/

python3 tools/deploy/publish_launcher_release.py 0.0.1-build51
# or register + r2_deploy + r2_publish_manifest separately:
python3 tools/deploy/register_launcher_release.py 0.0.1-build51 --channel windows-x64-msi

python3 tools/deploy/r2_deploy.py --copy \
  ./r2_staging/launcher/releases/0.0.1-build51/windows-x64-msi \
  launcher/releases/0.0.1-build51/windows-x64-msi

python3 tools/deploy/r2_publish_manifest.py
```

`publish_launcher_release.py` runs register, blob upload, git commit/push, and manifest publish in one step.

### Patch metadata on an existing version

When catalog fields change (e.g. add `uncompressed_size_bytes`) but the zip is unchanged:

```bash
# Merge catalog inline builds into git versions.json for latest_version
python3 tools/deploy/sync_versions_index.py krabs_v1 --merge-catalog

# Publish to R2
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
python3 tools/deploy/r2_catalog_check.py
python3 tools/deploy/r2_catalog_check.py --game krabs_v1 --compare-git
```

See [`.cursor/skills/r2-catalog-integrity/SKILL.md`](../.cursor/skills/r2-catalog-integrity/SKILL.md).

## GitHub Actions register (CI / cross-repo)

For automation or when you prefer the workflow UI:

1. Upload binaries to R2 with `r2_deploy.py --copy` (see above).
2. **Actions → Register game version → Run workflow** with:
   - `game_id`, `version`, `platforms` (e.g. `macos-arm64,windows-x64`)
   - `builds_json` — per-platform metadata, e.g.  
     `{"macos-arm64":{"executable_path":"Game.app/Contents/MacOS/Game","file_size_bytes":12345,"uncompressed_size_bytes":45678,"sha256":"..."}}`
   - For a **new** game, also set `title`, `description`, `thumbnail_url`.
   - Uncheck **update_catalog_latest** when registering an older build for "Other versions" only.
   - Set env `UPSERT=1` in workflow (or use `--patch` locally) to update an existing version.
3. The workflow updates R2 `versions.json`, updates git `manifests/manifest.json` and `manifests/games/{id}/versions.json`, uploads live `manifest.json` to R2, and commits.

Catalog source of truth: git history of [`manifests/manifest.json`](../manifests/manifest.json) and [`manifests/games/`](../manifests/games/). Roll back with `git revert` and republish.

## Tests

Deploy logic unit tests use Python stdlib only (no rclone/R2 required):

```bash
cd tools/deploy && python3 -m unittest discover -s tests -v
```

## Security

- Run `tools/dev/scan-secrets.sh` before commit.
- See `.cursor/skills/secret-hygiene/SKILL.md`.
