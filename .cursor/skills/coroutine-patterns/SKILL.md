---
name: coroutine-patterns
description: >-
  Kotlin coroutine error-handling and cancellation patterns for GameLauncher —
  ensureActive in catch blocks, runCatching pitfalls, Result vs thrown errors,
  and viewModelScope.launch structure. Use when adding try/catch in coroutines,
  handling failures in ViewModels, or wrapping suspend/repository calls.
---

# Coroutine patterns (GameLauncher)

Companion to [`coroutine-dispatchers`](../coroutine-dispatchers/SKILL.md) (which thread). This skill covers **cancellation and errors** inside coroutines.

## Cancellation: use `ensureActive()` in catch blocks

**Do not swallow cancellation.** If a coroutine is cancelled, failure handlers must not run and state must not be updated as if a real error occurred.

When catching broad exceptions inside `viewModelScope.launch` or other coroutine scopes, call **`ensureActive()`** as the first line of the `catch` block:

```kotlin
import kotlinx.coroutines.ensureActive

viewModelScope.launch {
    try {
        repository.doWork()
            .onSuccess { updateState { copy(statusLabel = "READY") } }
            .onFailure { error -> surfaceError(error) }
    } catch (e: Throwable) {
        ensureActive() // rethrows CancellationException when the job was cancelled
        AppLog.e("Feature", "Unexpected failure", e)
        surfaceError(e)
    }
}
```

Why this works: on cancellation the job is not active, so `ensureActive()` throws `CancellationException` and the handler below does not run. For a real failure while the job is still active, `ensureActive()` is a no-op and you proceed to log and update UI.

**Prefer this over** a separate `catch (e: CancellationException) { throw e }` branch — same behaviour, one catch block, less boilerplate.

**Do not** catch `CancellationException` and ignore it. **Do not** map cancellation to user-visible error state.

## `runCatching` in coroutines

Stdlib `runCatching { }` **catches** `CancellationException` and wraps it in `Result.failure`. Inside coroutine code that is usually wrong.

| Location | Pattern |
|----------|---------|
| Repository returning `Result` | OK — outer caller is a coroutine; use `runCatching` only for expected failures |
| Inside `viewModelScope.launch` | Prefer `try/catch` + `ensureActive()`, or repository `Result` + `.onFailure` |
| Must use `runCatching` in a coroutine | Rethrow cancellation: `runCatching { ... }.also { ensureActive() }` or catch and `ensureActive()` before handling |

GameLauncher repositories (`GameLauncher.openUrl`, install/uninstall) already return `Result` via `runCatching` at the boundary — ViewModels should use `.onFailure` for those, with a broad `catch (Throwable)` + `ensureActive()` only for unexpected throws.

## Surfacing errors to UI

1. **Expected failure** — repository returns `Result.failure` → `.onFailure { updateState { copy(errorMessage = …) } }`, log with `AppLog.e`.
2. **Unexpected throw** — `catch (Throwable)` after `ensureActive()` → same UI path + log.
3. **Always** provide a fallback message when `error.message` is null (e.g. `"See F12 logs for details."`).

Avoid macOS/native crash dialogs: never let exceptions escape `viewModelScope.launch` uncaught when the failure can be shown in-app.

## AWT / Desktop API from coroutines

Compose Desktop uses Swing. **`java.awt.Desktop`** (e.g. `browse()`) must run on **`Dispatchers.Main`**, not `IO`:

```kotlin
withContext(Dispatchers.Main) {
    Desktop.getDesktop().browse(URI(url))
}
```

See `GameLauncher.desktop.kt` `openUrl`.

## Guards and structure

- Set in-flight flags **before** `launch` (see `coroutine-dispatchers` skill).
- Prefer one `launch` per user action; cancel superseded work with `Job.cancel()` or epoch counters when needed.
- Do not use `GlobalScope`.

## Checklist (new coroutine + error path)

- [ ] Blocking/CPU work off Main (`coroutine-dispatchers`)
- [ ] Broad `catch (Throwable)` calls `ensureActive()` first
- [ ] Cancellation never shown as user error
- [ ] Expected failures use `Result` + `.onFailure` where repository supports it
- [ ] Errors logged with `AppLog.e` and surfaced in UI state
- [ ] AWT/Desktop calls on `Dispatchers.Main`
