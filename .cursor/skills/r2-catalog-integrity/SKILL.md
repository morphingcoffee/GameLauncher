---
name: r2-catalog-integrity
description: >-
  Validates live R2 catalog integrity — manifest.json, each game's versions.json,
  build zip presence and sizes, thumbnails, and optional git drift. Use when
  verifying backend deployment, after publishing manifests/versions, debugging
  missing downloads, or when the user asks to audit R2 game catalog health.
---

# R2 catalog integrity

Run **`tools/deploy/r2_catalog_check.py`** to verify the **live R2 bucket** matches what the launcher expects. Use after deploy/register/publish, or when downloads/metadata look wrong.

**Not** a credentials smoke test — use `r2_env_check.py` for that first if auth is uncertain.

## Quick run

```bash
python3 tools/deploy/r2_catalog_check.py
```

Requires `.env` + Keychain R2 credentials (same as other deploy tools). Read-only on catalog objects; uses `rclone lsjson` / `copyto` only.

Exit `0` = no errors (warnings may still appear). Exit `1` = at least one error.

## What it checks

| Layer | Checks |
|-------|--------|
| **manifest.json** | Loads from R2; `schema_version`, games array |
| **Per game (catalog)** | `id`, `title`, `latest_version`, `versions_url`; inline `builds` metadata |
| **versions.json** | Object exists on R2; `game_id` match; each version has `builds` |
| **Build metadata** | `download_url`, `executable_path`, `file_size_bytes`, `sha256`; warns if `uncompressed_size_bytes` missing |
| **R2 objects** | Each build `download_url` resolves to an object; **size matches** `file_size_bytes` |
| **Thumbnail** | `thumbnail_url` object exists under `assets/` |
| **Consistency** | Catalog `latest_version` exists in `versions.json`; warns if inline catalog builds drift from versions entry |

## Useful flags

```bash
# One game only
python3 tools/deploy/r2_catalog_check.py --game krabs_v1

# Metadata/schema only — no object existence or size probes (offline CDN base not required)
python3 tools/deploy/r2_catalog_check.py --skip-objects

# Compare live R2 JSON with git source of truth
python3 tools/deploy/r2_catalog_check.py --compare-git --compare-git-manifest

# Validate git manifest structure without fetching live manifest (still hits R2 for versions/zips unless --skip-objects)
python3 tools/deploy/r2_catalog_check.py --manifest manifests/manifest.json
```

## Agent workflow

1. Confirm R2 credentials work (`python3 tools/deploy/r2_env_check.py`) if the user reports auth errors.
2. Run `r2_catalog_check.py` with scope flags matching the user's concern (`--game` if single title).
3. Summarize **FAIL** lines first, then **WARN** (e.g. missing `uncompressed_size_bytes`, git drift).
4. Map failures to fixes:

| Failure | Typical fix |
|---------|-------------|
| missing `versions.json` | `python3 tools/deploy/r2_publish_versions.py {game_id}` or register flow |
| catalog/versions build drift | `sync_versions_index.py --merge-catalog` then publish |
| missing zip on R2 | `r2_deploy.py --copy ./r2_staging/games/... games/...` |
| size mismatch | Re-scan zip, update metadata, republish versions + manifest |
| git differs from R2 | Publish from git or re-sync git from R2, then commit |

5. Re-run checker until errors are cleared.

## Output format

Report lines: `PASS|WARN|FAIL  [scope] name — detail`

- `scope` is `manifest`, `game:{id}`, or version-scoped `game:{id} v{version}`
- Treat **WARN** as non-blocking unless the user wants zero warnings
- Missing `uncompressed_size_bytes` is a **FAIL** (required for true install size display)

## Related

- [`tools/deploy/README.md`](../../tools/deploy/README.md) — deploy layout (`r2_staging/` vs `manifests/games/`)
- `r2_env_check.py` — bucket connectivity and token permissions
- `r2_publish_manifest.py` / `r2_publish_versions.py` — push git JSON to R2

## Do not

- Commit `.env` or paste R2 secrets into issues/PRs
- Run `--verify-sha256` (not implemented — full zip download is too heavy for routine checks)
- Assume WARN means launch is broken — use judgment and user intent
