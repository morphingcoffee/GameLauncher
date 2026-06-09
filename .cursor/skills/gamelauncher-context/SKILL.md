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

## Stack

| Layer | Choice |
|-------|--------|
| UI | Kotlin Multiplatform + Compose Multiplatform (desktop JVM) |
| Networking | Ktor Client (CIO engine), Content Negotiation |
| Serialization | kotlinx.serialization (JSON) |
| Concurrency | Coroutines + StateFlow for download progress |
| Shaders | Skia RuntimeEffect / SkSL (`iTime`, `iResolution`) |
| Packaging | Gradle — DMG (macOS), MSI (Windows) |

## Manifest contract (`manifest.json`)

Remote static JSON — single source of truth for builds.

```json
{
  "launcher_minimum_version": "1.0.0",
  "games": [{
    "id": "game_id",
    "title": "...",
    "description": "...",
    "thumbnail_url": "https://...",
    "version": "1.0.0",
    "builds": {
      "windows": {
        "download_url": "https://...",
        "executable_path": "GameBinary.exe",
        "file_size_bytes": 0
      },
      "macos": {
        "download_url": "https://...",
        "executable_path": "GameBinary.app/Contents/MacOS/GameBinary",
        "file_size_bytes": 0
      }
    }
  }]
}
```

Use `@SerialName` for snake_case JSON fields in Kotlin models.

## Platform detection

`System.getProperty("os.name")` → `windows` / `macos` manifest keys.

- Build available → active Download / Play
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
- **Modules:** `:composeApp`, `:core:architecture`, `:core:designsystem`, `:core:navigation`, `:feature:home`

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
