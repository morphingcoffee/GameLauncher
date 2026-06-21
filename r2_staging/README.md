# R2 upload staging

Local mirror of **R2 object key prefixes** for binaries only. This tree is gitignored — do not commit zips or large assets.

JSON catalog metadata lives in git under [`manifests/`](../manifests/) (`manifest.json`, `manifests/games/`).

## Layout (matches R2 keys)

```
r2_staging/
  games/{game_id}/v{version}/{platform}/game.zip
  assets/{game_id}/thumbnail.webp
  launcher/releases/{artifact_version}/{channel}/GameLauncher-…
```

The path under `r2_staging/` is the same as the R2 object key — copy local → remote with matching suffixes:

```bash
# Zip build
python3 tools/deploy/r2_deploy.py --copy \
  ./r2_staging/games/krabs_v1/v0.0.1/windows-x64 \
  games/krabs_v1/v0.0.1/windows-x64

# Thumbnail
python3 tools/deploy/r2_deploy.py --copy \
  ./r2_staging/assets/krabs_v1 \
  assets/krabs_v1
```

`manifest.json` and `games/{id}/versions.json` are **not** staged here — they are edited in `manifests/` and published with `r2_publish_manifest.py` / `r2_publish_versions.py`.

## Register a version

```bash
python3 tools/deploy/register_version.py krabs_v1 0.0.1 --platform windows-x64
```

Scans `r2_staging/games/.../game.zip` for sha256 and sizes. See [`tools/deploy/README.md`](../tools/deploy/README.md).

### Launcher self-update artifacts

Stage prod installers under `r2_staging/launcher/releases/{version}/{channel}/`, then publish:

```bash
python3 tools/deploy/publish_launcher_release.py 0.0.1-build51
```

Or register only (no R2/git):

```bash
python3 tools/deploy/register_launcher_release.py 0.0.1-build51 --channel windows-x64-msi
```

Channel keys: `windows-x64-msi`, `windows-x64-portable`, `macos-arm64-dmg`, `macos-x64-dmg`. Use `--bump-minimum` only for breaking changes — see `launcher-minimum-version` skill.

CI: **Desktop installers** (build) → **Publish launcher release** (pass build run ID).
