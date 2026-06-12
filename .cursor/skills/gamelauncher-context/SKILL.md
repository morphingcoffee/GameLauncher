---
name: gamelauncher-context
description: >-
  Concise briefing for the GameLauncher KMP desktop project — stack, manifest
  schema, platform paths, and phase map. Use when working on GameLauncher code,
  architecture, manifest handling, or any implementation phase.
---

# GameLauncher Context

## Repository

- **GitHub:** `morphingcoffee/GameLauncher` (public)
- **Product:** Cross-platform desktop game launcher (Windows + macOS)

### Layout

| Path | Purpose |
|------|---------|
| `launcher/` | KMP desktop app (Gradle root — `composeApp`, `core/*`, `feature/*`) |
| `manifests/` | Catalog manifest (`manifest.json`) — git source of truth, CI deploys to R2 |
| `tools/deploy/` | Cloudflare R2 upload scripts — see `tools/deploy/README.md` |
| `tools/dev/` | Repo tooling (secret scan, GitHub PAT helpers) |
| `.github/` | CI workflows |
| `.cursor/` | Agent skills, rules, hooks |

## Stack

| Layer | Choice |
|-------|--------|
| UI | Kotlin Multiplatform + Compose Multiplatform (desktop JVM) |
| Networking | Ktor Client (CIO engine), Content Negotiation |
| Serialization | kotlinx.serialization (JSON) |
| Concurrency | Coroutines + StateFlow for download progress |
| Shaders | Skia RuntimeEffect / SkSL (`iTime`, `iResolution`) |
| Packaging | Gradle — DMG (macOS), MSI (Windows) |

## Manifest contract

Remote static JSON — catalog on startup, per-game version history on demand.

### Catalog (`manifest.json`)

Git source of truth: [`manifests/manifest.json`](../../manifests/manifest.json). Deployed to R2 on push to `main`.

```json
{
  "schema_version": 1,
  "launcher_minimum_version": "0.0.1",
  "games": [{
    "id": "game_id",
    "title": "...",
    "description": "...",
    "thumbnail_url": "https://cdn.../assets/game_id/thumbnail.webp",
    "latest_version": "1.2.0",
    "versions_url": "https://cdn.../games/game_id/versions.json",
    "builds": {
      "windows-x64": {
        "download_url": "https://...",
        "executable_path": "GameBinary.exe",
        "file_size_bytes": 0,
        "sha256": "..."
      },
      "macos-arm64": { "...": "..." },
      "macos-x64": { "...": "..." }
    }
  }]
}
```

### Version index (`games/{game_id}/versions.json`)

Lazy-loaded when user opens "Other versions". Lives in R2 only; maintained by the register-game-version workflow.

Kotlin models in `:core:model` — `Manifest`, `GameCatalogEntry`, `GameVersionIndex`, `PlatformKey`.

Follow `kotlin-serialization` skill: **every** wire-model property uses `@SerialName` (explicit JSON keys, refactor-safe).

## Platform detection

`PlatformKey.current()` combines `os.name` + `os.arch` → `windows-x64`, `macos-arm64`, or `macos-x64`.

- Build available for current platform → active Download / Play
- Build absent → card visible but grayed (`.alpha(0.4f)`), no interaction

## Local library paths (runtime only)

Resolve at runtime — never hardcode usernames or machine-specific home paths in committed code.

| OS | Root |
|----|------|
| Windows | `%APPDATA%/GameLauncher` |
| macOS | `~/Library/Application Support/GameLauncher` |

Subdirs: `downloads/`, `games/{gameId}/`

## Configuration

- Public defaults in `Config.kt` (placeholder CDN host)
- Overrides via env: `GAME_LAUNCHER_MANIFEST_URL` (see `.env.example`)
- Secrets: macOS Keychain only (`security`); see `secret-hygiene` — never in project config files

## Architecture (Stage 1)

- **MVI:** lightweight `MviViewModel` in `:core:architecture`; features expose `State` / `Event` / `Effect`
- **DI:** Koin 4.2 + Compiler Plugin (`compileSafety = true`); `@KoinApplication` aggregator in `:composeApp`
- **Navigation:** Navigation 3; typed `AppDestination : NavKey` in `:core:navigation`
- **Modules:** `:composeApp`, `:core:architecture`, `:core:designsystem`, `:core:model`, `:core:navigation`, `:feature:home`

## Implementation phases

| Phase | Scope |
|-------|-------|
| 0 | GitHub MCP, skills, secret hooks |
| 1 | KMP project init |
| 2 | Dashboard + platform availability UI |
| 3 | Ktor download + ZipInputStream extract |
| 4 | ProcessBuilder launch (correct working dir) |
| 5 | SkSL shader background |
| 6 | GitHub Actions DMG + MSI matrix |

Track progress on the repo Project board and linked GitHub issues.
