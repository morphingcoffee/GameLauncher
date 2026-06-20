---
name: launcher-minimum-version
description: Checklist for breaking manifest/wire changes that require bumping launcher_minimum_version on publish. Use when editing @Serializable wire models, manifest.json launcher block, removing/renaming JSON keys, or publishing launcher releases after contract changes.
---

# Launcher minimum version (breaking changes)

`launcher_minimum_version` in [`manifests/manifest.json`](../../manifests/manifest.json) is the **deliberate forced-update floor**. Routine CI publishes update per-channel `version` only; bump the minimum when older launchers must not run against the new contract.

## When to read this skill

- Editing `@Serializable` types in `:core:model` (manifest, launcher release, install records)
- Changing the `launcher { … }` block or other manifest wire fields
- Renaming/removing JSON keys or making formerly optional fields required
- Changing catalog URL contract or install-channel detection old builds cannot satisfy
- Running [`register_launcher_release.py`](../../tools/deploy/register_launcher_release.py) after any of the above

## Checklist

1. **Backward compatible?** Can already-shipped binaries parse and safely use the remote JSON?
2. **If no** → bump `launcher_minimum_version`; publish with `register_launcher_release.py --bump-minimum` (or `--minimum-version`); document why in PR/release notes.
3. **If yes** → update staged channel `version` + artifact fields only; leave `launcher_minimum_version` unchanged.
4. **Security fix** → bump minimum even when wire shape is unchanged.

## Publish commands

```bash
# Routine release (optional update nudge only)
python3 tools/deploy/register_launcher_release.py 0.0.1-build51 --channel windows-x64-msi

# Breaking change (forced update gate)
python3 tools/deploy/register_launcher_release.py 0.0.2-build1 --channel windows-x64-msi --bump-minimum
```

Then upload staged blobs and publish manifest (`r2_deploy.py`, `r2_publish_manifest.py`). See [`tools/deploy/README.md`](../../tools/deploy/README.md).

## Runtime behavior (reminder)

| Condition | Client behavior |
|-----------|-------------------|
| Decode failure (HTTP 200) | Block catalog → manual GitHub Releases |
| Runtime &lt; `launcher_minimum_version` | Forced update gate |
| Runtime ≥ minimum, channel newer | Optional UPDATE in About/header |

`schema_version` was removed; minimum + decode failure are the breaking-change signals.

## Related skills

- [`kotlin-serialization`](../kotlin-serialization/SKILL.md) — `@SerialName` on every wire property
- [`gamelauncher-context`](../gamelauncher-context/SKILL.md) — manifest layout and phase map
- [`r2-catalog-integrity`](../r2-catalog-integrity/SKILL.md) — verify live R2 after publish
