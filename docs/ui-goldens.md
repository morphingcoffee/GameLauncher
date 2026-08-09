# UI golden screenshots

Deterministic Compose Desktop visual regression tests for GameLauncher.

## Current toolchain (as of this doc)

| Piece | Version / value |
|-------|-----------------|
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.12.0-beta03 |
| Compose Material3 | 1.12.0-alpha03 |
| Gradle wrapper | 9.5.0 |
| JVM bytecode target | 17 |
| CI JDK | Temurin **25** |
| Canonical golden host | **macOS arm64** — GitHub Actions `macos-15` |
| Surface size | 1280×800, density 1.0, fontScale 1.0 |
| Suite location | `:composeApp` `desktopTest` |

## Why one canonical host (macOS arm64 today)

1. **Supported product platforms are Windows + macOS.** `PlatformKey.current()` returns `null` on Linux because Linux is not a launcher target — catalog availability UI therefore differs on Linux CI agents even with identical fixtures.
2. **Pixel-exact Skia rasterization** can still differ by OS even with bundled OFL fonts.

Ubuntu `ci.yml` `allTests` still runs `:composeApp:desktopTest`; golden cases **skip** unless the host matches `CanonicalGoldenHost` (macOS arm64 today). Authoritative check: [`.github/workflows/ui-goldens.yml`](../.github/workflows/ui-goldens.yml).

### Switching to Linux later

Possible, but not a flip of `runs-on` alone:

1. Prefer **injecting platform into composition** (stop reading `PlatformKey.current()` inside catalog UI / `buildForCurrentPlatform()` for render paths) so the tree is fixture-controlled.
2. Change `CanonicalGoldenHost` in `GoldenHost.kt` and `runs-on` in `ui-goldens.yml`.
3. Regenerate **all** baselines on the new host (`-PupdateGolden` / workflow bootstrap) and commit them.

Until (1) lands, Linux goldens would still encode “platform unavailable” for native builds unless fixtures only use `web` builds.

## What is covered

Stateless screen content only (no Koin ViewModels):

| Golden name | Composable |
|-------------|------------|
| `catalog_loading` / `_error` / `_empty` / `_loaded` | `CatalogScreenContent` |
| `catalog_update_gate` | update-required overlay |
| `catalog_launcher_update_sheet` | optional update sheet |
| `about_default` / `about_update_sheet` | `SettingsScreenContent` |
| `storage_loading` / `_loaded` / `_uninstall_all_dialog` | `StorageScreenContent` |

Fixtures pin clock, app version, platform label, sizes, and set `thumbnailUrl = null` (no Coil / network).

## Paths

| Role | Path |
|------|------|
| Baselines (committed) | `launcher/composeApp/screenshots/golden/<name>.png` |
| Actual (gitignored) | `launcher/composeApp/screenshots/actual/` |
| Diff (gitignored) | `launcher/composeApp/screenshots/diff/` |
| Test fonts (OFL) | `launcher/composeApp/src/desktopTest/resources/fonts/` |

## Run / regenerate

Canonical host only (macOS arm64 / Actions `macos-15`):

```bash
source launcher/scripts/ensure-host-gradle-home.sh
./gradlew -p launcher :composeApp:desktopTest
```

Rewrite baselines:

```bash
./gradlew -p launcher :composeApp:desktopTest -PupdateGolden
```

**CI bootstrap:** if `golden/*.png` is empty, `ui-goldens.yml` runs with `-PupdateGolden`, uploads the `screenshots/` artifact, and fails on purpose until those PNGs are committed.

Manual regenerate: Actions → **UI goldens** → Run workflow → `update_golden=true` → download artifact → commit `golden/*.png`.

Baselines regenerated on a non-canonical host are **invalid**.

## Comparator

Hand-rolled PNG compare (ImageIO): fail on size mismatch; ignore per-channel deltas ≤ 2; fail when more than **0.1%** of pixels differ. Missing baselines fail with regenerate instructions (never auto-create unless `-PupdateGolden` / CI bootstrap).
