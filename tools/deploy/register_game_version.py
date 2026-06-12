#!/usr/bin/env python3
"""Register a game build in R2 versions.json and update manifests/manifest.json."""

from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
from datetime import date
from pathlib import Path
from typing import Any, Dict, List, Optional

from r2_config import (
    R2Session,
    find_repo_root,
    remote_url,
    rclone_run,
)

VALID_PLATFORMS = frozenset({"windows-x64", "macos-arm64", "macos-x64"})


def die(message: str) -> None:
    print(f"register-game-version: {message}", file=sys.stderr)
    sys.exit(1)


def build_download_path(game_id: str, version: str, platform: str) -> str:
    return f"games/{game_id}/v{version}/{platform}/game.zip"


def build_download_url(cdn_base: str, game_id: str, version: str, platform: str) -> str:
    return f"{cdn_base}/{build_download_path(game_id, version, platform)}"


def enrich_builds_with_urls(
    builds_json: Dict[str, Any],
    game_id: str,
    version: str,
    cdn_base: str,
) -> Dict[str, Any]:
    """Add full CDN download_url to each platform build entry."""
    result: Dict[str, Any] = {}
    for platform, meta in builds_json.items():
        path = build_download_path(game_id, version, platform)
        result[platform] = dict(meta)
        result[platform]["download_url"] = f"{cdn_base}/{path}"
    return result


def parse_platform_list(platforms: str) -> List[str]:
    items = [p.strip() for p in platforms.split(",") if p.strip()]
    if not items:
        die("PLATFORMS must list at least one platform key")
    return items


def validate_platforms(platform_list: List[str], builds: Dict[str, Any]) -> None:
    for platform in platform_list:
        if platform not in VALID_PLATFORMS:
            die(
                f"unsupported platform key: {platform} "
                f"(expected one of: {' '.join(sorted(VALID_PLATFORMS))})"
            )
        if platform not in builds:
            die(f"BUILDS_JSON missing metadata for platform: {platform}")


def new_versions_index(game_id: str) -> Dict[str, Any]:
    return {"game_id": game_id, "versions": []}


def append_version_entry(
    versions_data: Dict[str, Any],
    version: str,
    released_at: str,
    builds: Dict[str, Any],
) -> Dict[str, Any]:
    if any(v.get("version") == version for v in versions_data.get("versions", [])):
        die(f"version {version} already registered for {versions_data.get('game_id')}")

    entry = {
        "version": version,
        "released_at": released_at,
        "builds": builds,
    }
    versions_data = dict(versions_data)
    versions_data["versions"] = [entry] + list(versions_data.get("versions", []))
    return versions_data


def find_game_index(manifest: Dict[str, Any], game_id: str) -> Optional[int]:
    for i, game in enumerate(manifest.get("games", [])):
        if game.get("id") == game_id:
            return i
    return None


def add_game_to_manifest(
    manifest: Dict[str, Any],
    game_id: str,
    title: str,
    description: str,
    thumbnail_url: str,
    version: str,
    versions_url: str,
    builds: Dict[str, Any],
) -> Dict[str, Any]:
    manifest = dict(manifest)
    games = list(manifest.get("games", []))
    games.append(
        {
            "id": game_id,
            "title": title,
            "description": description,
            "thumbnail_url": thumbnail_url,
            "latest_version": version,
            "versions_url": versions_url,
            "builds": builds,
        }
    )
    manifest["games"] = games
    return manifest


def update_game_in_manifest(
    manifest: Dict[str, Any],
    game_id: str,
    version: str,
    versions_url: str,
    builds: Dict[str, Any],
    title: Optional[str] = None,
    description: Optional[str] = None,
    thumbnail_url: Optional[str] = None,
) -> Dict[str, Any]:
    manifest = dict(manifest)
    games: List[Dict[str, Any]] = []
    for game in manifest.get("games", []):
        if game.get("id") != game_id:
            games.append(game)
            continue
        updated = dict(game)
        updated["latest_version"] = version
        updated["versions_url"] = versions_url
        merged_builds = dict(updated.get("builds", {}))
        merged_builds.update(builds)
        updated["builds"] = merged_builds
        if title:
            updated["title"] = title
        if description:
            updated["description"] = description
        if thumbnail_url:
            updated["thumbnail_url"] = thumbnail_url
        games.append(updated)
    manifest["games"] = games
    return manifest


def update_catalog_manifest(
    manifest: Dict[str, Any],
    game_id: str,
    version: str,
    versions_url: str,
    builds: Dict[str, Any],
    title: Optional[str] = None,
    description: Optional[str] = None,
    thumbnail_url: Optional[str] = None,
) -> Dict[str, Any]:
    index = find_game_index(manifest, game_id)
    if index is None:
        if not title or not description or not thumbnail_url:
            die("TITLE, DESCRIPTION, and THUMBNAIL_URL are required when adding a new game")
        return add_game_to_manifest(
            manifest,
            game_id,
            title,
            description,
            thumbnail_url,
            version,
            versions_url,
            builds,
        )
    return update_game_in_manifest(
        manifest,
        game_id,
        version,
        versions_url,
        builds,
        title=title,
        description=description,
        thumbnail_url=thumbnail_url,
    )


NOT_FOUND_MARKERS = (
    "nosuchkey",
    "no such key",
    "object not found",
    "specified key does not exist",
    "source object does not exist",
)


def is_object_not_found(copy_result: subprocess.CompletedProcess) -> bool:
    """Return True when rclone failed because the remote object is missing."""
    stderr = copy_result.stderr
    stdout = copy_result.stdout
    if isinstance(stderr, bytes):
        stderr = stderr.decode(errors="replace")
    if isinstance(stdout, bytes):
        stdout = stdout.decode(errors="replace")
    output = f"{stderr or ''}{stdout or ''}".lower()
    return any(marker in output for marker in NOT_FOUND_MARKERS)


def fetch_versions_index(
    remote: str,
    versions_remote_path: str,
    game_id: str,
    local_path: Path,
) -> Dict[str, Any]:
    """Download versions.json or return a fresh index when the object does not exist yet."""
    copy_result = rclone_run(
        ["copyto", f"{remote}/{versions_remote_path}", str(local_path)],
        capture_output=True,
        text=True,
    )
    if copy_result.returncode == 0:
        return json.loads(local_path.read_text())

    if is_object_not_found(copy_result):
        print(
            f"register-game-version: creating new versions index for {game_id}",
            file=sys.stderr,
        )
        return new_versions_index(game_id)

    detail = (copy_result.stderr or copy_result.stdout or "unknown error").strip()
    die(f"failed to download {versions_remote_path}: {detail}")
    raise AssertionError("unreachable")


def ensure_git_author(repo_root: Path) -> None:
    """Set commit author in GitHub Actions where checkout has no global git identity."""
    if os.environ.get("GITHUB_ACTIONS") != "true":
        return

    author_name = os.environ.get("GIT_AUTHOR_NAME", "github-actions[bot]")
    author_email = os.environ.get(
        "GIT_AUTHOR_EMAIL",
        "41898282+github-actions[bot]@users.noreply.github.com",
    )
    subprocess.run(
        ["git", "-C", str(repo_root), "config", "user.name", author_name],
        check=True,
    )
    subprocess.run(
        ["git", "-C", str(repo_root), "config", "user.email", author_email],
        check=True,
    )


def commit_manifest_if_changed(repo_root: Path, manifest_path: Path) -> None:
    rel = manifest_path.relative_to(repo_root)
    result = subprocess.run(
        ["git", "-C", str(repo_root), "diff", "--quiet", "--", str(rel)],
        check=False,
    )
    if result.returncode == 0:
        return

    ensure_git_author(repo_root)
    subprocess.run(
        ["git", "-C", str(repo_root), "add", str(rel)],
        check=True,
    )
    subprocess.run(
        [
            "git",
            "-C",
            str(repo_root),
            "commit",
            "-m",
            f"Register {os.environ['GAME_ID']} v{os.environ['VERSION']}\n\n"
            "Update catalog manifest and R2 versions index.",
        ],
        check=True,
    )
    subprocess.run(["git", "-C", str(repo_root), "push"], check=True)
    print("register-game-version: committed and pushed manifest change", file=sys.stderr)


def main() -> None:
    repo_root = find_repo_root()
    game_id = os.environ.get("GAME_ID")
    version = os.environ.get("VERSION")
    platforms = os.environ.get("PLATFORMS")
    builds_json_raw = os.environ.get("BUILDS_JSON")
    cdn_base_raw = os.environ.get("R2_PUBLIC_CDN_BASE_URL")

    if not game_id:
        die("GAME_ID is required")
    if not version:
        die("VERSION is required")
    if not platforms:
        die("PLATFORMS is required")
    if not builds_json_raw:
        die("BUILDS_JSON is required")
    if not cdn_base_raw:
        die("R2_PUBLIC_CDN_BASE_URL is required")

    cdn_base = cdn_base_raw.rstrip("/")
    manifest_path = Path(
        os.environ.get("MANIFEST_PATH", repo_root / "manifests" / "manifest.json")
    )
    released_at = os.environ.get("RELEASED_AT", date.today().isoformat())
    skip_git = os.environ.get("SKIP_GIT", "0").lower() in ("1", "true")
    update_catalog_latest = os.environ.get("UPDATE_CATALOG_LATEST", "true").lower() not in (
        "0",
        "false",
    )

    title = os.environ.get("TITLE") or None
    description = os.environ.get("DESCRIPTION") or None
    thumbnail_url = os.environ.get("THUMBNAIL_URL") or None

    if not manifest_path.is_file():
        die(f"manifest not found: {manifest_path}")

    try:
        builds_input = json.loads(builds_json_raw)
    except json.JSONDecodeError as exc:
        die(f"BUILDS_JSON is not valid JSON: {exc}")

    platform_list = parse_platform_list(platforms)
    builds_for_version = enrich_builds_with_urls(builds_input, game_id, version, cdn_base)
    validate_platforms(platform_list, builds_for_version)

    versions_url = f"{cdn_base}/games/{game_id}/versions.json"
    versions_remote_path = f"games/{game_id}/versions.json"

    # CI provides credentials via env; local uses Keychain when vars are unset.
    use_keychain = not os.environ.get("R2_ACCESS_KEY_ID")
    with R2Session(repo_root=repo_root, use_keychain=use_keychain) as session:
        bucket = session.bucket
        remote = remote_url(bucket)
        versions_local: Path

        with tempfile.TemporaryDirectory() as tmpdir:
            tmp = Path(tmpdir)
            versions_local = tmp / "versions.json"

            versions_data = fetch_versions_index(
                remote,
                versions_remote_path,
                game_id,
                versions_local,
            )

            existing_game_id = versions_data.get("game_id")
            if existing_game_id != game_id:
                die(
                    f"versions.json game_id mismatch: expected {game_id}, got {existing_game_id}"
                )

            versions_data = append_version_entry(
                versions_data, version, released_at, builds_for_version
            )
            versions_updated = tmp / "versions.updated.json"
            versions_updated.write_text(json.dumps(versions_data, indent=2) + "\n")

            print(
                f"register-game-version: uploading {versions_remote_path}",
                file=sys.stderr,
            )
            rclone_run(
                ["copyto", str(versions_updated), f"{remote}/{versions_remote_path}"],
                check=True,
            )

            if not update_catalog_latest:
                manifest = json.loads(manifest_path.read_text())
                if find_game_index(manifest, game_id) is None:
                    die(
                        f"UPDATE_CATALOG_LATEST=0 requires an existing catalog entry for {game_id}"
                    )
                print(
                    "register-game-version: UPDATE_CATALOG_LATEST=0 — skipping catalog manifest update",
                    file=sys.stderr,
                )
                return

            manifest = json.loads(manifest_path.read_text())
            updated_manifest = update_catalog_manifest(
                manifest,
                game_id,
                version,
                versions_url,
                builds_for_version,
                title=title,
                description=description,
                thumbnail_url=thumbnail_url,
            )
            manifest_path.write_text(json.dumps(updated_manifest, indent=2) + "\n")
            print(f"register-game-version: updated {manifest_path}", file=sys.stderr)

    if skip_git:
        print("register-game-version: SKIP_GIT=1 — not committing", file=sys.stderr)
        return

    commit_manifest_if_changed(repo_root, manifest_path)


if __name__ == "__main__":
    main()
