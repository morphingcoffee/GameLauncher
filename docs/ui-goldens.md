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

## Why macOS arm64 only (not Linux / Windows)

Cross-host goldens are intentionally unsupported for this suite:

1. **Platform-dependent UI tree.** Catalog roster / detail and model helpers call `PlatformKey.current()` (and `isAvailableOnCurrentPlatform()` / `buildForCurrentPlatform()`). On Linux that returns `null`, so availability chips, dimmed rows, and action affordances differ from macOS even with identical fixture JSON.
2. **Pixel-exact rasterization.** Even with bundled OFL fonts, Skia text AA / subpixel shaping can differ by OS. A single canonical host keeps diffs meaningful.
3. **Follow-up (optional):** thread an explicit platform key through composition and accept a small per-channel tolerance — still prefer one CI host for baselines.

Ubuntu `ci.yml` `allTests` still runs `:composeApp:desktopTest`, but golden cases **skip** unless the host is macOS arm64 (`Assume.assumeTrue`). The dedicated workflow [`.github/workflows/ui-goldens.yml`](../.github/workflows/ui-goldens.yml) is the authoritative check.

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
| Actual (build) | `launcher/composeApp/build/screenshots/actual/` |
| Diff (build) | `launcher/composeApp/build/screenshots/diff/` |
| Test fonts (OFL) | `launcher/composeApp/src/desktopTest/resources/fonts/` |

## Run / regenerate

From a **macOS arm64** machine (or Actions `macos-15`):

```bash
source launcher/scripts/ensure-host-gradle-home.sh
./gradlew -p launcher :composeApp:desktopTest
```

Rewrite baselines:

```bash
./gradlew -p launcher :composeApp:desktopTest -PupdateGolden
```

Or: Actions → **UI goldens** → Run workflow → `update_golden=true` → download the artifact → copy PNGs into `launcher/composeApp/screenshots/golden/` → commit.

Baselines regenerated on a non-macOS-arm64 host are **invalid** and will fail CI.

## Comparator

Hand-rolled PNG compare (ImageIO): fail on size mismatch; ignore per-channel deltas ≤ 2; fail when more than **0.1%** of pixels differ. Missing baselines fail with regenerate instructions (never auto-create unless `-PupdateGolden`).
