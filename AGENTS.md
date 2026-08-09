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
- **DI:** Koin 4.2 + compiler plugin; `@KoinApplication` in `:composeApp` (`compileSafety = false` there for cross-module ViewModels; features may enable it).
- **Navigation:** Navigation 3; `AppDestination : NavKey` — `Home`, `Settings`, `Storage`, `Logs`.
- **Modules:** `:composeApp`, `:core:architecture`, `:core:designsystem`, `:core:model`, `:core:navigation`, `:core:network` (catalog/install/launch/self-update), `:core:logging`, `:core:telemetry`, `:feature:home`, `:feature:settings`, `:feature:logs`.

## Guardrails

- **Public repo** — follow `secret-hygiene` skill; run `tools/dev/scan-secrets.sh` before commit/push.
- **No push without approval** — follow `prompt-before-api-ops` / `cursor-api-usage` rule.
- **Feature UI** — use `:core:designsystem` composables, not raw Material3 (see `designsystem-enforcement` rule).
- **Gradle in agent shells** — reuse host `~/.gradle` cache (see `agent-gradle` rule).
- **GitHub issues** — attach to [Game Launcher Roadmap](https://github.com/users/morphingcoffee/projects/1) project #1 (`github-task-creation` skill).

## Deep reference

Full manifest JSON samples, configuration env vars, implementation phase table, and platform UI rules → [`.cursor/skills/gamelauncher-context/SKILL.md`](.cursor/skills/gamelauncher-context/SKILL.md).

## Cursor Cloud specific instructions

The app is the single `:composeApp` desktop module. All Gradle commands run from `launcher/` (wrapper is Gradle 9.5.0). Standard commands live in [`README.md`](README.md) and [`launcher/composeApp/build.gradle.kts`](launcher/composeApp/build.gradle.kts); the essentials are:

- Lint: `./gradlew ktlintCheck`
- Test: `./gradlew desktopTest`
- Run (dev): `./gradlew :composeApp:runDevDesktop` — fake in-process catalog (16 sample games), simulated network delays, window title `MC.GAME.LAUNCHER [DEV]`. Plain `:composeApp:run` hits the real CDN.

Non-obvious environment notes:

- **CI/packaging use Temurin 25**; this cloud image may still ship JDK 21. Bytecode target stays **17** (`jvmTarget` / Android compiler options). Lint, tests, and run work on JDK 21+ here — no JDK 17 install is needed locally in this environment.
- **Android SDK is required for Gradle sync**, even though the shipped app is desktop-only: every module declares an AGP `com.android.kotlin.multiplatform.library` target (for Compose `@Preview`). The SDK lives at `~/android-sdk` (platform 35 + build-tools 35). Gradle finds it via `launcher/local.properties` (`sdk.dir=…`, gitignored) — the startup update script recreates this file if missing. If Gradle errors with "SDK location not found", ensure that line exists or export `ANDROID_HOME`.
- **GUI runs on `DISPLAY=:1`.** Skiko cannot create a GL context on this VM and logs `RenderException: Cannot create Linux GL context` then falls back to software rendering — this warning is benign; the window renders fine. Launch the app inside a `tmux` session (it is a long-running foreground process).
- **Linux platform limitations for GUI testing:** `PlatformKey.current()` is `null` on Linux, so native game builds show `[ PLATFORM UNAVAILABLE ]` and are dimmed, and clicking a web game's `OPEN` fails with "Opening URLs in a browser is not supported on this platform". These are environment limits, not bugs. For GUI smoke tests use catalog keyboard navigation (↑/↓) and the `STORAGE` / `ABOUT` / `LOGS` screens rather than DOWNLOAD / LAUNCH / OPEN.
