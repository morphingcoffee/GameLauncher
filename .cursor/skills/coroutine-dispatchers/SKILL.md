---
name: coroutine-dispatchers
description: >-
  Pick the right Kotlin coroutine dispatcher for GameLauncher work — keep UI
  responsive on Compose Desktop (Swing main thread). Use when adding suspend
  functions, viewModelScope.launch, blocking file/network calls, hashing,
  compression, or directory walks.
---

# Coroutine dispatchers (GameLauncher)

## Default rule

**Never run blocking or long-running work on the UI thread.**

On desktop, `viewModelScope.launch { }` uses the **main/Swing dispatcher**. Any synchronous file, network, hash, or zip work inside that coroutine without `withContext` will freeze Compose (including animations).

## Dispatcher cheat sheet

| Work | Dispatcher | Examples in this repo |
|------|------------|------------------------|
| Compose state updates, short pure logic | **Main** (default `viewModelScope`) | `updateState { }`, parsing small JSON already in memory |
| Blocking I/O, waiting on disk/network | **`Dispatchers.IO`** | HTTP download to staging, delete install dir, read/write install record, directory walk for size |
| CPU-bound work | **`Dispatchers.Default`** | SHA-256 verify, zip inflate/extract, JSON encode of large blobs |

Zip extract and hashing are **both** I/O and CPU — prefer **`Default`** for verify/extract; use **`IO`** for raw download and file deletes.

## Where to apply `withContext`

1. **Repository / installer** — preferred for operations every caller needs off main (e.g. `getOnDiskSizeBytes`, `downloadAndInstall` post-download phases).
2. **ViewModel** — only when the repository is still synchronous or a one-off probe; do not rely on “suspend = background”.

```kotlin
// Good — installer splits phases
withContext(Dispatchers.IO) { downloadToStaging(...) }
withContext(Dispatchers.Default) { verifySha256(...); extractZip(...) }

// Good — repository wraps blocking installer API
override suspend fun getOnDiskSizeBytes(gameId: String): Long? =
    withContext(Dispatchers.IO) { gameInstaller.getOnDiskSizeBytes(gameId) }
```

## Guards before `launch`

Set in-flight flags **synchronously** before `viewModelScope.launch` when double-tap/double-callback can start duplicate jobs (e.g. `isUninstalling = true` before launching uninstall). Do not set the guard only inside the coroutine.

## Checklist (new async feature)

- [ ] Identified blocking or CPU-heavy steps
- [ ] Wrapped in `withContext(IO)` or `withContext(Default)` — not Main
- [ ] Progress/status callbacks still safe for `StateFlow` (thread-safe updates are OK)
- [ ] Concurrent user actions guarded with flags set before `launch`
- [ ] Desktop manual check: UI stays responsive during install/uninstall/probes
