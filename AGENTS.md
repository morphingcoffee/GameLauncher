# GameLauncher — agent briefing

Cross-platform desktop game launcher (Windows + macOS). **GitHub:** `morphingcoffee/GameLauncher` (public).

For full schema, phase map, JSON examples, and command patterns, read [`.cursor/skills/gamelauncher-context/SKILL.md`](.cursor/skills/gamelauncher-context/SKILL.md) and sibling skills under `.cursor/skills/` before large changes.

## Layout

| Path | Purpose |
|------|---------|
| `launcher/` | KMP desktop app — `composeApp`, `core/*`, `feature/*` |
| `manifests/` | Catalog (`manifest.json`) — git source of truth, CI deploys to R2 |
| `manifests/games/{id}/` | Per-game version history (`versions.json`) |
| `tools/deploy/` | Cloudflare R2 upload — see `tools/deploy/README.md` |
| `tools/dev/` | Secret scan, GitHub PAT helpers |
| `.github/` | CI workflows |
| `.cursor/` | Agent skills, rules, hooks |

## Stack

Kotlin Multiplatform + Compose Multiplatform (desktop JVM), Ktor Client, kotlinx.serialization (JSON), coroutines + StateFlow, Skia/SkSL shaders, Gradle packaging (DMG / MSI).

## Manifest contract

- **Catalog** (`manifests/manifest.json`): games list, launcher self-update channels, CDN URLs, `executable_path` per platform build.
- **Version index** (`manifests/games/{id}/versions.json`): lazy-loaded when user opens "Other versions".
- **Kotlin models** in `:core:model` — `Manifest`, `GameCatalogEntry`, `GameVersionIndex`, `PlatformKey`.
- **Serialization:** every wire-model property uses `@SerialName` (see `kotlin-serialization` skill).

## Platform and paths

- `PlatformKey.current()` → `windows-x64`, `macos-arm64`, or `macos-x64`.
- Build missing for current platform → card grayed, no interaction.
- Local library roots (resolve at runtime — never hardcode usernames in committed code):
  - Windows: `%APPDATA%/GameLauncher`
  - macOS: `~/Library/Application Support/GameLauncher`
- Subdirs: `downloads/`, `games/{gameId}/`

## Architecture

- **MVI:** `MviViewModel` in `:core:architecture`; features expose `State` / `Event` / `Effect`.
- **DI:** Koin 4.2 + compiler plugin; `@KoinApplication` in `:composeApp`.
- **Navigation:** Navigation 3; `AppDestination : NavKey` in `:core:navigation`.
- **Modules:** `:composeApp`, `:core:architecture`, `:core:designsystem`, `:core:model`, `:core:navigation`, `:feature:home`, `:feature:settings`.

## Guardrails

- **Public repo** — follow `secret-hygiene` skill; run `tools/dev/scan-secrets.sh` before commit/push.
- **No push without approval** — follow `prompt-before-api-ops` / `cursor-api-usage` rule.
- **Feature UI** — use `:core:designsystem` composables, not raw Material3 (see `designsystem-enforcement` rule).
- **Gradle in agent shells** — reuse host `~/.gradle` cache (see `agent-gradle` rule).
- **GitHub issues** — attach to [Game Launcher Roadmap](https://github.com/users/morphingcoffee/projects/1) project #1 (`github-task-creation` skill).

## Deep reference

Full manifest JSON samples, configuration env vars, implementation phase table, and platform UI rules → [`.cursor/skills/gamelauncher-context/SKILL.md`](.cursor/skills/gamelauncher-context/SKILL.md).
