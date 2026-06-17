#!/usr/bin/env python3
"""Upload git manifests/games/{game_id}/versions.json to R2."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from catalog_layout import git_versions_path, r2_versions_object_key
from r2_config import R2Session, find_repo_root, remote_url, rclone_run


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Publish git versions.json to R2")
    parser.add_argument("game_id", help="Game identifier (e.g. krabs_v1)")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> None:
    args = parse_args(argv)
    repo_root = find_repo_root()
    versions_path = git_versions_path(repo_root, args.game_id)
    if not versions_path.is_file():
        print(f"r2-publish-versions: not found: {versions_path}", file=sys.stderr)
        sys.exit(1)

    remote_key = r2_versions_object_key(args.game_id)
    use_keychain = not __import__("os").environ.get("R2_ACCESS_KEY_ID")
    with R2Session(repo_root=repo_root, use_keychain=use_keychain) as session:
        remote = remote_url(session.bucket)
        print(
            f"r2-publish-versions: {versions_path} -> r2://{session.bucket}/{remote_key}",
            file=sys.stderr,
        )
        rclone_run(
            ["copyto", str(versions_path), f"{remote}/{remote_key}"],
            check=True,
        )


if __name__ == "__main__":
    main()
