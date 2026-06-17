#!/usr/bin/env bash
set -euo pipefail

LAUNCHER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$LAUNCHER_DIR"

# shellcheck source=ensure-host-gradle-home.sh
source "$LAUNCHER_DIR/scripts/ensure-host-gradle-home.sh"

if [ -z "${JAVA_HOME:-}" ] && [ -d "${HOME}/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  export JAVA_HOME="${HOME}/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

./gradlew ktlintCheck --quiet
