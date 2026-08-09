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

The path under `r2_staging/` is the same as the R2 object key (without the `r2_staging/` prefix).

Game platforms: `windows-x64`, `macos-arm64`, `macos-x64`.

Launcher channels: `windows-x64-msi`, `windows-x64-portable`, `macos-arm64-dmg`, `macos-x64-dmg`.

`manifest.json` and `games/{id}/versions.json` are **not** staged here — edit them under `manifests/` and publish with the deploy tools.

## Commands

Upload, register, and publish recipes (including `r2_deploy.py --copy` and launcher release flows) live in [`tools/deploy/README.md`](../tools/deploy/README.md).
