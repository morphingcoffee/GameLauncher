#!/usr/bin/env python3
"""Local-friendly register: scan staging zips, update git JSON, publish to R2."""

from __future__ import annotations

import argparse
import json
import sys
from datetime import date
from pathlib import Path
from typing import Any, Dict, List, Optional

from catalog_layout import (
    catalog_manifest_path,
    git_release_path,
    r2_staging_zip_path,
)
from register_game_version import RegisterOptions, catalog_game_entry, register_version
from r2_config import find_repo_root, load_env_file
from zip_metadata import zip_build_metadata


def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Register a game version from local staging zips",
    )
    parser.add_argument("game_id", help="Game identifier (e.g. krabs_v1)")
    parser.add_argument("version", help="Semantic version (e.g. 0.0.1)")
    parser.add_argument(
        "--platform",
        action="append",
        dest="platforms",
        required=True,
        help="Platform key (repeat for multiple: windows-x64, macos-arm64, macos-x64)",
    )
    parser.add_argument(
        "--executable",
        action="append",
        dest="executables",
        help="Executable path inside zip (one per --platform, or set in releases/{version}.json)",
    )
    parser.add_argument(
        "--released-at",
        help="Release date YYYY-MM-DD (default: today or releases/{version}.json)",
    )
    parser.add_argument(
        "--patch",
        action="store_true",
        help="Update an existing version entry instead of failing on duplicate",
    )
    parser.add_argument(
        "--no-catalog",
        action="store_true",
        help="Do not update manifests/manifest.json latest builds",
    )
    parser.add_argument(
        "--no-publish",
        action="store_true",
        help="Only update git JSON files (no R2 upload)",
    )
    parser.add_argument(
        "--title",
        help="Game title when adding a new catalog entry",
    )
    parser.add_argument(
        "--description",
        help="Game description when adding a new catalog entry",
    )
    parser.add_argument(
        "--thumbnail-url",
        help="Thumbnail CDN URL when adding a new catalog entry",
    )
    return parser.parse_args(argv)


def die(message: str) -> None:
    print(f"register-version: {message}", file=sys.stderr)
    sys.exit(1)


def load_release_overrides(repo_root: Path, game_id: str, version: str) -> Dict[str, Any]:
    path = git_release_path(repo_root, game_id, version)
    if not path.is_file():
        return {}
    return json.loads(path.read_text())


def resolve_executable_path(
    platform: str,
    version: str,
    executables: Optional[List[str]],
    platform_index: int,
    release_overrides: Dict[str, Any],
) -> str:
    release_platforms = release_overrides.get("platforms") or {}
    if platform in release_platforms:
        executable = release_platforms[platform].get("executable_path")
        if executable:
            return executable

    if executables and platform_index < len(executables):
        return executables[platform_index]

    die(
        f"executable_path required for {platform} — pass --executable or set "
        f"manifests/games/{{game_id}}/releases/{version}.json"
    )
    raise AssertionError("unreachable")


def build_platform_metadata(
    repo_root: Path,
    game_id: str,
    version: str,
    platform: str,
    executable_path: str,
) -> Dict[str, Any]:
    zip_path = r2_staging_zip_path(repo_root, game_id, version, platform)
    metadata = zip_build_metadata(zip_path)
    metadata["executable_path"] = executable_path
    return metadata


def catalog_defaults(repo_root: Path, game_id: str) -> Dict[str, Optional[str]]:
    manifest_path = catalog_manifest_path(repo_root)
    if not manifest_path.is_file():
        return {"title": None, "description": None, "thumbnail_url": None}
    manifest = json.loads(manifest_path.read_text())
    game = catalog_game_entry(manifest, game_id)
    if game is None:
        return {"title": None, "description": None, "thumbnail_url": None}
    return {
        "title": game.get("title"),
        "description": game.get("description"),
        "thumbnail_url": game.get("thumbnail_url"),
    }


def main(argv: Optional[List[str]] = None) -> None:
    args = parse_args(argv)
    repo_root = find_repo_root()
    load_env_file(repo_root / ".env")

    cdn_base = __import__("os").environ.get("R2_PUBLIC_CDN_BASE_URL")
    if not cdn_base and not args.no_publish:
        die("R2_PUBLIC_CDN_BASE_URL required (set in .env) unless --no-publish")

    release_overrides = load_release_overrides(repo_root, args.game_id, args.version)
    released_at = args.released_at or release_overrides.get("released_at") or date.today().isoformat()

    builds: Dict[str, Any] = {}
    for index, platform in enumerate(args.platforms):
        executable_path = resolve_executable_path(
            platform,
            args.version,
            args.executables,
            index,
            release_overrides,
        )
        builds[platform] = build_platform_metadata(
            repo_root,
            args.game_id,
            args.version,
            platform,
            executable_path,
        )

    defaults = catalog_defaults(repo_root, args.game_id)
    register_version(
        repo_root,
        RegisterOptions(
            game_id=args.game_id,
            version=args.version,
            platform_list=args.platforms,
            builds_input=builds,
            cdn_base=(cdn_base or "https://cdn.example.com").rstrip("/"),
            released_at=released_at,
            upsert=args.patch,
            update_catalog_latest=not args.no_catalog,
            publish_r2=not args.no_publish,
            title=args.title or defaults["title"],
            description=args.description or defaults["description"],
            thumbnail_url=args.thumbnail_url or defaults["thumbnail_url"],
        ),
    )

    print(
        f"register-version: registered {args.game_id} v{args.version} "
        f"({', '.join(args.platforms)})",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
