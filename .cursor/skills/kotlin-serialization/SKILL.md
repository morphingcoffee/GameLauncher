---
name: kotlin-serialization
description: >-
  kotlinx.serialization rules for GameLauncher wire models — every serialized
  property must use @SerialName so Kotlin renames never change JSON keys. Use
  when adding or editing @Serializable types, manifest models, API DTOs, or
  persisted JSON contracts.
---

# Kotlin serialization (wire models)

## Rule

**Every property** on a `@Serializable` class that participates in an external or persisted JSON contract must have an explicit `@SerialName("wire_key")` annotation.

Refactoring a Kotlin property name must have **zero impact** on serialized field names.

## Applies to

- Manifest / catalog models in `:core:model` (`Manifest`, `GameCatalogEntry`, …)
- Any DTO decoded from R2, CDN, or backend JSON
- Any JSON written for cross-version or cross-process consumption

## Does not require @SerialName on

- Properties of types that are **not** `@Serializable` wire models (plain Kotlin helpers)
- `@Serializable` types used only for **internal** ephemeral state with **no properties** (e.g. empty `data object` nav keys)
- Map **keys** in `Map<String, GameBuild>` — platform slugs (`windows-x64`) are data, not Kotlin property names

## Pattern

```kotlin
@Serializable
data class GameBuild(
    @SerialName("download_url")
    val downloadUrl: String,
    @SerialName("executable_path")
    val executablePath: String,
    @SerialName("file_size_bytes")
    val fileSizeBytes: Long,
    @SerialName("uncompressed_size_bytes")
    val uncompressedSizeBytes: Long? = null,
    @SerialName("sha256")
    val sha256: String,
)
```

Use snake_case `@SerialName` values matching the manifest schema in `manifests/` and `gamelauncher-context`.

## Checklist (new or edited model)

1. Add `@Serializable` on the class
2. Annotate **each** property with `@SerialName` — including fields whose wire name would match the default (e.g. `@SerialName("id") val id: String`)
3. Run `:core:model` tests (or module tests) after changes
4. Update fixture JSON / `manifests/` only when the **wire** schema changes — not when renaming Kotlin properties

## Anti-patterns

```kotlin
// BAD — rename `title` → `displayTitle` breaks JSON without @SerialName
@Serializable
data class GameCatalogEntry(
    val id: String,
    val title: String,
)

// GOOD
@Serializable
data class GameCatalogEntry(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val displayTitle: String,
)
```

Do not rely on `Json { namingStrategy = … }` for wire models — explicit `@SerialName` per field is the source of truth.

## Related

- Manifest field names: `gamelauncher-context` skill, `manifests/manifest.json`
- Models live in `launcher/core/model/`
- **Breaking wire changes:** read `launcher-minimum-version` skill before publishing
