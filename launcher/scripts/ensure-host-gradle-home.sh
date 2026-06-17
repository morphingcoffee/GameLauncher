#!/usr/bin/env bash
# Use the host Gradle cache (~/.gradle) instead of Cursor's ephemeral sandbox cache.
# Speeds up wrapper, dependency, and configuration-cache reuse for hooks and agent shells.
if [[ "${GRADLE_USER_HOME:-}" == *cursor-sandbox-cache* ]] || [[ -n "${CURSOR_SANDBOX:-}" ]]; then
  export GRADLE_USER_HOME="${HOME}/.gradle"
fi
