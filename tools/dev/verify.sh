#!/usr/bin/env bash
# Canonical local verification — same checks as .github/workflows/ci.yml
# Usage: ./tools/dev/verify.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

# Prefer host Gradle cache (avoids Cursor sandbox cold wrapper downloads).
# shellcheck source=/dev/null
source "$ROOT/launcher/scripts/ensure-host-gradle-home.sh"

echo "==> Gradle build + ktlintCheck (launcher/)"
(
  cd "$ROOT/launcher"
  ./gradlew build ktlintCheck --warning-mode all
)

echo "==> Python unit tests (tools/deploy)"
(
  cd "$ROOT/tools/deploy"
  python3 -m unittest discover -s tests -v
)

echo "==> Offline catalog validation (git manifests)"
(
  cd "$ROOT"
  python3 tools/deploy/r2_catalog_check.py --offline
)

echo "verify: OK"
