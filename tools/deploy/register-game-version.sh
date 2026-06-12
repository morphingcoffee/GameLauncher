#!/usr/bin/env bash
# Register a game build in R2 versions.json and update manifests/manifest.json in the repo.
#
# Intended for GitHub Actions (register-game-version workflow) and local dry-runs.
#
# Required environment variables:
#   GAME_ID, VERSION, PLATFORMS (comma-separated platform keys)
#   BUILDS_JSON — JSON object keyed by platform, e.g.:
#     {"macos-arm64":{"executable_path":"Game.app/...","file_size_bytes":123,"sha256":"abc..."}}
#   R2_PUBLIC_CDN_BASE_URL — public CDN origin (no trailing slash)
#   R2_ACCOUNT_ID, R2_BUCKET_NAME, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY
#
# Optional:
#   TITLE, DESCRIPTION, THUMBNAIL_URL — required when adding a new game to the catalog
#   RELEASED_AT — ISO date (YYYY-MM-DD); defaults to UTC today
#   MANIFEST_PATH — defaults to manifests/manifest.json
#   SKIP_GIT — set to 1 to skip git commit (useful for dry-run)
#   UPDATE_CATALOG_LATEST — set to 0 to only append versions.json (historical release)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [[ -z "$ROOT" ]]; then
  ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
fi

die() {
  echo "register-game-version: $*" >&2
  exit 1
}

require_var() {
  local name="$1"
  [[ -n "${!name:-}" ]] || die "$name is required"
}

require_var GAME_ID
require_var VERSION
require_var PLATFORMS
require_var BUILDS_JSON
require_var R2_PUBLIC_CDN_BASE_URL
require_var R2_ACCOUNT_ID
require_var R2_BUCKET_NAME
require_var R2_ACCESS_KEY_ID
require_var R2_SECRET_ACCESS_KEY

if ! command -v jq >/dev/null 2>&1; then
  die "jq is required"
fi
if ! command -v rclone >/dev/null 2>&1; then
  die "rclone is required"
fi

MANIFEST_PATH="${MANIFEST_PATH:-$ROOT/manifests/manifest.json}"
RELEASED_AT="${RELEASED_AT:-$(date -u +%Y-%m-%d)}"
CDN_BASE="${R2_PUBLIC_CDN_BASE_URL%/}"

[[ -f "$MANIFEST_PATH" ]] || die "manifest not found: $MANIFEST_PATH"

VALID_PLATFORMS=(windows-x64 macos-arm64 macos-x64)

IFS=',' read -r -a PLATFORM_LIST <<<"$PLATFORMS"
[[ ${#PLATFORM_LIST[@]} -ge 1 ]] || die "PLATFORMS must list at least one platform key"

is_valid_platform() {
  local key="$1"
  local p
  for p in "${VALID_PLATFORMS[@]}"; do
    [[ "$p" == "$key" ]] && return 0
  done
  return 1
}

build_download_url() {
  local platform="$1"
  printf '%s/games/%s/v%s/%s/game.zip' "$CDN_BASE" "$GAME_ID" "$VERSION" "$platform"
}

versions_url="${CDN_BASE}/games/${GAME_ID}/versions.json"

# Build the version entry builds object from BUILDS_JSON + generated download URLs.
builds_for_version="$(
  jq -c --arg version "$VERSION" --arg game_id "$GAME_ID" '
    to_entries
    | map({
        key: .key,
        value: (.value + {download_url: ("games/" + $game_id + "/v" + $version + "/" + .key + "/game.zip")})
      })
    | from_entries
  ' <<<"$BUILDS_JSON"
)"

# Inject full CDN URLs.
builds_for_version="$(
  jq -c --arg cdn "$CDN_BASE" '
    with_entries(.value.download_url = ($cdn + "/" + .value.download_url))
  ' <<<"$builds_for_version"
)"

for platform in "${PLATFORM_LIST[@]}"; do
  platform="${platform// /}"
  [[ -n "$platform" ]] || continue
  is_valid_platform "$platform" || die "unsupported platform key: $platform (expected one of: ${VALID_PLATFORMS[*]})"
  jq -e --arg p "$platform" '.[$p] != null' <<<"$builds_for_version" >/dev/null \
    || die "BUILDS_JSON missing metadata for platform: $platform"
done

# shellcheck source=r2-rclone-env.sh
source "$SCRIPT_DIR/r2-rclone-env.sh"
r2_rclone_configure
cleanup_register() {
  r2_rclone_cleanup
}
trap cleanup_register EXIT

REMOTE="${RCLONE_REMOTE}:${R2_BUCKET_NAME}"
versions_remote_path="games/${GAME_ID}/versions.json"
tmpdir="$(mktemp -d)"
versions_local="$tmpdir/versions.json"

if rclone copyto "$REMOTE/$versions_remote_path" "$versions_local" "${RCLONE_FLAGS[@]}" 2>/dev/null; then
  :
else
  echo "register-game-version: creating new versions index for $GAME_ID" >&2
  jq -n --arg game_id "$GAME_ID" '{game_id: $game_id, versions: []}' >"$versions_local"
fi

existing_game_id="$(jq -r '.game_id // empty' "$versions_local")"
[[ "$existing_game_id" == "$GAME_ID" ]] || die "versions.json game_id mismatch: expected $GAME_ID, got $existing_game_id"

if jq -e --arg v "$VERSION" '.versions[] | select(.version == $v)' "$versions_local" >/dev/null; then
  die "version $VERSION already registered for $GAME_ID"
fi

new_version_entry="$(
  jq -n \
    --arg version "$VERSION" \
    --arg released_at "$RELEASED_AT" \
    --argjson builds "$builds_for_version" \
    '{version: $version, released_at: $released_at, builds: $builds}'
)"

jq --argjson entry "$new_version_entry" \
  '.versions = ([$entry] + .versions)' \
  "$versions_local" >"$tmpdir/versions.updated.json"

echo "register-game-version: uploading $versions_remote_path" >&2
rclone copyto "$tmpdir/versions.updated.json" "$REMOTE/$versions_remote_path" "${RCLONE_FLAGS[@]}"

if [[ "${UPDATE_CATALOG_LATEST:-1}" == "0" ]]; then
  game_exists="$(jq --arg id "$GAME_ID" '[.games[] | select(.id == $id)] | length' "$MANIFEST_PATH")"
  [[ "$game_exists" != "0" ]] || die "UPDATE_CATALOG_LATEST=0 requires an existing catalog entry for $GAME_ID"
  echo "register-game-version: UPDATE_CATALOG_LATEST=0 — skipping catalog manifest update" >&2
  exit 0
fi

# Update catalog manifest (latest_version + inline builds for registered platforms).
manifest_tmp="$tmpdir/manifest.json"
cp "$MANIFEST_PATH" "$manifest_tmp"

game_index="$(jq --arg id "$GAME_ID" '[.games[] | select(.id == $id)] | length' "$manifest_tmp")"

if [[ "$game_index" == "0" ]]; then
  require_var TITLE
  require_var DESCRIPTION
  require_var THUMBNAIL_URL
  new_game="$(
    jq -n \
      --arg id "$GAME_ID" \
      --arg title "$TITLE" \
      --arg description "$DESCRIPTION" \
      --arg thumbnail_url "$THUMBNAIL_URL" \
      --arg latest_version "$VERSION" \
      --arg versions_url "$versions_url" \
      --argjson builds "$builds_for_version" \
      '{
        id: $id,
        title: $title,
        description: $description,
        thumbnail_url: $thumbnail_url,
        latest_version: $latest_version,
        versions_url: $versions_url,
        builds: $builds
      }'
  )"
  jq --argjson game "$new_game" '.games += [$game]' "$manifest_tmp" >"$tmpdir/manifest.updated.json"
else
  # Merge builds: keep existing platform entries, overwrite keys present in this release.
  jq \
    --arg id "$GAME_ID" \
    --arg latest_version "$VERSION" \
    --arg versions_url "$versions_url" \
    --argjson new_builds "$builds_for_version" \
    '
    .games = [
      .games[]
      | if .id == $id then
          .latest_version = $latest_version
          | .versions_url = $versions_url
          | .builds = (.builds + $new_builds)
          | (if env.TITLE != "" then .title = env.TITLE else . end)
          | (if env.DESCRIPTION != "" then .description = env.DESCRIPTION else . end)
          | (if env.THUMBNAIL_URL != "" then .thumbnail_url = env.THUMBNAIL_URL else . end)
        else .
        end
    ]
    ' \
    TITLE="${TITLE:-}" \
    DESCRIPTION="${DESCRIPTION:-}" \
    THIRBNAIL_URL="${THUMBNAIL_URL:-}" \
    "$manifest_tmp" >"$tmpdir/manifest.updated.json"
fi

cp "$tmpdir/manifest.updated.json" "$MANIFEST_PATH"
echo "register-game-version: updated $MANIFEST_PATH" >&2

if [[ "${SKIP_GIT:-0}" == "1" ]]; then
  echo "register-game-version: SKIP_GIT=1 — not committing" >&2
  exit 0
fi

if ! git -C "$ROOT" diff --quiet -- "$MANIFEST_PATH"; then
  git -C "$ROOT" add "$MANIFEST_PATH"
  git -C "$ROOT" commit -m "$(cat <<EOF
Register $GAME_ID v$VERSION

Update catalog manifest and R2 versions index.
EOF
)"
  git -C "$ROOT" push
  echo "register-game-version: committed and pushed manifest change" >&2
fi
