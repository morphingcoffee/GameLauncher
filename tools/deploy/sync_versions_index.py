#!/usr/bin/env python3
"""Sync git versions.json from catalog manifest and/or R2."""

from __future__ import annotations

import argparse
import json
import sys
import tempfile
from pathlib import Path

from catalog_layout import catalog_manifest_path, git_versions_path, r2_versions_object_key
from register_game_version import (
    fetch_versions_index,
    merge_catalog_builds_into_versions,
    new_versions_index,
    write_versions_index,
)
from r2_config import R2Session, find_repo_root, remote_url


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Sync manifests/games/{game_id}/versions.json from R2 and/or catalog",
    )
    parser.add_argument("game_id", help="Game identifier (e.g. krabs_v1)")
    parser.add_argument(
        "--from-r2",
        action="store_true",
        help="Fetch versions.json from R2 when git copy is missing or with --force-r2",
    )
    parser.add_argument(
        "--force-r2",
        action="store_true",
        help="Replace git versions.json with R2 copy before merging catalog",
    )
    parser.add_argument(
        "--merge-catalog",
        action="store_true",
        help="Merge inline catalog builds into the latest_version entry",
    )
    parser.add_argument(
        "--publish",
        action="store_true",
        help="Upload git versions.json to R2 after sync",
    )
    return parser.parse_args(argv)


def load_versions_from_r2(repo_root: Path, game_id: str) -> dict:
    remote_key = r2_versions_object_key(game_id)
    use_keychain = not __import__("os").environ.get("R2_ACCESS_KEY_ID")
    with R2Session(repo_root=repo_root, use_keychain=use_keychain) as session:
        remote = remote_url(session.bucket)
        with tempfile.TemporaryDirectory() as tmpdir:
            local_path = Path(tmpdir) / "versions.json"
            return fetch_versions_index(remote, remote_key, game_id, local_path)


def main(argv: list[str] | None = None) -> None:
    args = parse_args(argv)
    repo_root = find_repo_root()
    git_path = git_versions_path(repo_root, args.game_id)
    manifest_path = catalog_manifest_path(repo_root)

    if not manifest_path.is_file():
        print(f"sync-versions-index: manifest not found: {manifest_path}", file=sys.stderr)
        sys.exit(1)

    manifest = json.loads(manifest_path.read_text())
    versions_data = None

    if args.force_r2 or (args.from_r2 and not git_path.is_file()):
        versions_data = load_versions_from_r2(repo_root, args.game_id)
    elif git_path.is_file():
        versions_data = json.loads(git_path.read_text())
    elif args.from_r2:
        versions_data = load_versions_from_r2(repo_root, args.game_id)
    else:
        versions_data = new_versions_index(args.game_id)

    if args.merge_catalog:
        versions_data = merge_catalog_builds_into_versions(manifest, args.game_id, versions_data)

    write_versions_index(git_path, versions_data)
    print(f"sync-versions-index: wrote {git_path}", file=sys.stderr)

    if args.publish:
        from r2_publish_versions import main as publish_main

        publish_main([args.game_id])


if __name__ == "__main__":
    main()
