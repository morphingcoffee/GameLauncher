#!/usr/bin/env python3
"""Register a game build in R2 versions.json and update manifests/manifest.json."""

from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Any, Dict, List, Optional

from catalog_layout import (
    catalog_manifest_path,
    git_versions_path,
    r2_versions_object_key,
    r2_zip_object_key,
)
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
    return r2_zip_object_key(game_id, version, platform)


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
        result[platform] = dict(meta)
        result[platform]["download_url"] = build_download_url(cdn_base, game_id, version, platform)
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
            die(f"build metadata missing for platform: {platform}")


def new_versions_index(game_id: str) -> Dict[str, Any]:
    return {"game_id": game_id, "versions": []}


def find_version_entry(versions_data: Dict[str, Any], version: str) -> Optional[Dict[str, Any]]:
    for entry in versions_data.get("versions", []):
        if entry.get("version") == version:
            return entry
    return None


def merge_platform_builds(
    existing: Dict[str, Any],
    incoming: Dict[str, Any],
) -> Dict[str, Any]:
    """Deep-merge per-platform build metadata (incoming wins on conflict)."""
    merged = {platform: dict(meta) for platform, meta in existing.items()}
    for platform, meta in incoming.items():
        platform_meta = dict(merged.get(platform, {}))
        platform_meta.update(meta)
        merged[platform] = platform_meta
    return merged


def append_version_entry(
    versions_data: Dict[str, Any],
    version: str,
    released_at: str,
    builds: Dict[str, Any],
) -> Dict[str, Any]:
    if find_version_entry(versions_data, version) is not None:
        die(f"version {version} already registered for {versions_data.get('game_id')}")

    entry = {
        "version": version,
        "released_at": released_at,
        "builds": builds,
    }
    versions_data = dict(versions_data)
    versions_data["versions"] = [entry] + list(versions_data.get("versions", []))
    return versions_data


def upsert_version_entry(
    versions_data: Dict[str, Any],
    version: str,
    released_at: str,
    builds: Dict[str, Any],
) -> Dict[str, Any]:
    """Insert a version or merge builds into an existing version entry."""
    versions_data = dict(versions_data)
    versions = list(versions_data.get("versions", []))
    existing = find_version_entry(versions_data, version)

    if existing is None:
        versions.insert(
            0,
            {
                "version": version,
                "released_at": released_at,
                "builds": builds,
            },
        )
        versions_data["versions"] = versions
        return versions_data

    updated_entry = dict(existing)
    if released_at:
        updated_entry["released_at"] = released_at
    updated_entry["builds"] = merge_platform_builds(existing.get("builds", {}), builds)
    versions_data["versions"] = [
        updated_entry if entry.get("version") == version else entry for entry in versions
    ]
    return versions_data


def merge_catalog_builds_into_versions(
    manifest: Dict[str, Any],
    game_id: str,
    versions_data: Dict[str, Any],
) -> Dict[str, Any]:
    """Merge inline catalog builds into matching version entries (metadata patches)."""
    index = find_game_index(manifest, game_id)
    if index is None:
        return versions_data

    game = manifest["games"][index]
    catalog_builds = game.get("builds") or {}
    if not catalog_builds:
        return versions_data

    target_version = game.get("latest_version")
    if not target_version:
        return versions_data

    entry = find_version_entry(versions_data, target_version)
    if entry is None:
        return versions_data

    versions_data = dict(versions_data)
    versions = []
    for item in versions_data.get("versions", []):
        if item.get("version") != target_version:
            versions.append(item)
            continue
        updated = dict(item)
        updated["builds"] = merge_platform_builds(item.get("builds", {}), catalog_builds)
        versions.append(updated)
    versions_data["versions"] = versions
    return versions_data


def write_versions_index(path: Path, versions_data: Dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(versions_data, indent=2) + "\n")


def load_git_versions_index(repo_root: Path, game_id: str) -> Optional[Dict[str, Any]]:
    path = git_versions_path(repo_root, game_id)
    if not path.is_file():
        return None
    return json.loads(path.read_text())


def find_game_index(manifest: Dict[str, Any], game_id: str) -> Optional[int]:
    for i, game in enumerate(manifest.get("games", [])):
        if game.get("id") == game_id:
            return i
    return None


def catalog_game_entry(manifest: Dict[str, Any], game_id: str) -> Optional[Dict[str, Any]]:
    index = find_game_index(manifest, game_id)
    if index is None:
        return None
    return manifest["games"][index]


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
    if copy_result.returncode == 0 and local_path.is_file():
        return json.loads(local_path.read_text())

    if copy_result.returncode == 0 or is_object_not_found(copy_result):
        print(
            f"register-game-version: creating new versions index for {game_id}",
            file=sys.stderr,
        )
        return new_versions_index(game_id)

    detail = (copy_result.stderr or copy_result.stdout or "unknown error").strip()
    die(f"failed to download {versions_remote_path}: {detail}")
    raise AssertionError("unreachable")


def publish_versions_to_r2(remote: str, versions_remote_path: str, versions_path: Path) -> None:
    print(f"register-game-version: uploading {versions_remote_path}", file=sys.stderr)
    rclone_run(
        ["copyto", str(versions_path), f"{remote}/{versions_remote_path}"],
        check=True,
    )


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


def publish_manifest_to_r2(remote: str, manifest_path: Path) -> None:
    """Upload live catalog manifest.json to R2 (launcher fetches this URL)."""
    remote_manifest = f"{remote}/manifest.json"
    print("register-game-version: uploading manifest.json to R2", file=sys.stderr)
    rclone_run(
        ["copyto", str(manifest_path), remote_manifest],
        check=True,
    )


def commit_catalog_changes(repo_root: Path, paths: List[Path]) -> None:
    rel_paths = [str(path.relative_to(repo_root)) for path in paths if path.is_file()]
    if not rel_paths:
        return

    diff = subprocess.run(
        ["git", "-C", str(repo_root), "diff", "--quiet", "--", *rel_paths],
        check=False,
    )
    untracked = subprocess.run(
        ["git", "-C", str(repo_root), "ls-files", "--error-unmatch", *rel_paths],
        capture_output=True,
        check=False,
    )
    has_changes = diff.returncode != 0 or untracked.returncode != 0
    if not has_changes:
        return

    ensure_git_author(repo_root)
    subprocess.run(
        ["git", "-C", str(repo_root), "add", *rel_paths],
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
            "Update catalog manifest and versions index.",
        ],
        check=True,
    )
    subprocess.run(["git", "-C", str(repo_root), "push"], check=True)
    print("register-game-version: committed and pushed catalog changes", file=sys.stderr)


@dataclass
class RegisterOptions:
    game_id: str
    version: str
    platform_list: List[str]
    builds_input: Dict[str, Any]
    cdn_base: str
    released_at: str
    upsert: bool = False
    update_catalog_latest: bool = True
    publish_r2: bool = True
    title: Optional[str] = None
    description: Optional[str] = None
    thumbnail_url: Optional[str] = None


def register_version(repo_root: Path, options: RegisterOptions) -> None:
    manifest_path = catalog_manifest_path(repo_root)
    if not manifest_path.is_file():
        die(f"manifest not found: {manifest_path}")

    builds_for_version = enrich_builds_with_urls(
        options.builds_input,
        options.game_id,
        options.version,
        options.cdn_base,
    )
    validate_platforms(options.platform_list, builds_for_version)

    versions_url = f"{options.cdn_base}/{r2_versions_object_key(options.game_id)}"
    versions_remote_path = r2_versions_object_key(options.game_id)
    git_versions = git_versions_path(repo_root, options.game_id)

    if not options.publish_r2:
        versions_data = load_git_versions_index(repo_root, options.game_id) or new_versions_index(
            options.game_id
        )
        if options.upsert:
            versions_data = upsert_version_entry(
                versions_data,
                options.version,
                options.released_at,
                builds_for_version,
            )
        else:
            versions_data = append_version_entry(
                versions_data,
                options.version,
                options.released_at,
                builds_for_version,
            )
        write_versions_index(git_versions, versions_data)

        if not options.update_catalog_latest:
            return

        manifest = json.loads(manifest_path.read_text())
        updated_manifest = update_catalog_manifest(
            manifest,
            options.game_id,
            options.version,
            versions_url,
            builds_for_version,
            title=options.title,
            description=options.description,
            thumbnail_url=options.thumbnail_url,
        )
        manifest_path.write_text(json.dumps(updated_manifest, indent=2) + "\n")
        return

    use_keychain = not os.environ.get("R2_ACCESS_KEY_ID")
    with R2Session(repo_root=repo_root, use_keychain=use_keychain) as session:
        remote = remote_url(session.bucket)

        git_data = load_git_versions_index(repo_root, options.game_id)
        if git_data is not None:
            versions_data = git_data
        else:
            with tempfile.TemporaryDirectory() as tmpdir:
                tmp_versions = Path(tmpdir) / "versions.json"
                versions_data = fetch_versions_index(
                    remote,
                    versions_remote_path,
                    options.game_id,
                    tmp_versions,
                )

        existing_game_id = versions_data.get("game_id")
        if existing_game_id != options.game_id:
            die(
                f"versions.json game_id mismatch: expected {options.game_id}, got {existing_game_id}"
            )

        if options.upsert:
            versions_data = upsert_version_entry(
                versions_data,
                options.version,
                options.released_at,
                builds_for_version,
            )
        else:
            versions_data = append_version_entry(
                versions_data,
                options.version,
                options.released_at,
                builds_for_version,
            )

        write_versions_index(git_versions, versions_data)
        publish_versions_to_r2(remote, versions_remote_path, git_versions)

        if not options.update_catalog_latest:
            manifest = json.loads(manifest_path.read_text())
            if find_game_index(manifest, options.game_id) is None:
                die(
                    f"update_catalog_latest=False requires an existing catalog entry for {options.game_id}"
                )
            print(
                "register-game-version: skipping catalog manifest update",
                file=sys.stderr,
            )
            return

        manifest = json.loads(manifest_path.read_text())
        updated_manifest = update_catalog_manifest(
            manifest,
            options.game_id,
            options.version,
            versions_url,
            builds_for_version,
            title=options.title,
            description=options.description,
            thumbnail_url=options.thumbnail_url,
        )
        manifest_path.write_text(json.dumps(updated_manifest, indent=2) + "\n")
        print(f"register-game-version: updated {manifest_path}", file=sys.stderr)
        publish_manifest_to_r2(remote, manifest_path)


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

    try:
        builds_input = json.loads(builds_json_raw)
    except json.JSONDecodeError as exc:
        die(f"BUILDS_JSON is not valid JSON: {exc}")

    skip_git = os.environ.get("SKIP_GIT", "0").lower() in ("1", "true")
    upsert = os.environ.get("UPSERT", "0").lower() in ("1", "true")
    update_catalog_latest = os.environ.get("UPDATE_CATALOG_LATEST", "true").lower() not in (
        "0",
        "false",
    )

    register_version(
        repo_root,
        RegisterOptions(
            game_id=game_id,
            version=version,
            platform_list=parse_platform_list(platforms),
            builds_input=builds_input,
            cdn_base=cdn_base_raw.rstrip("/"),
            released_at=os.environ.get("RELEASED_AT") or date.today().isoformat(),
            upsert=upsert,
            update_catalog_latest=update_catalog_latest,
            title=os.environ.get("TITLE") or None,
            description=os.environ.get("DESCRIPTION") or None,
            thumbnail_url=os.environ.get("THUMBNAIL_URL") or None,
        ),
    )

    if skip_git:
        print("register-game-version: SKIP_GIT=1 — not committing", file=sys.stderr)
        return

    commit_catalog_changes(
        repo_root,
        [
            catalog_manifest_path(repo_root),
            git_versions_path(repo_root, game_id),
        ],
    )


if __name__ == "__main__":
    main()
