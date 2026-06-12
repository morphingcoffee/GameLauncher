#!/usr/bin/env python3
"""Upload manifests/manifest.json to R2 (live catalog)."""

from __future__ import annotations

import sys
from pathlib import Path

from r2_config import R2Session, find_repo_root, remote_url, rclone_run


def main() -> None:
    repo_root = find_repo_root()
    manifest_path = repo_root / "manifests" / "manifest.json"
    if not manifest_path.is_file():
        print(f"r2-publish-manifest: not found: {manifest_path}", file=sys.stderr)
        sys.exit(1)

    use_keychain = not __import__("os").environ.get("R2_ACCESS_KEY_ID")
    with R2Session(repo_root=repo_root, use_keychain=use_keychain) as session:
        remote = remote_url(session.bucket)
        print(f"r2-publish-manifest: {manifest_path} -> r2://{session.bucket}/manifest.json", file=sys.stderr)
        rclone_run(
            ["copyto", str(manifest_path), f"{remote}/manifest.json"],
            check=True,
        )


if __name__ == "__main__":
    main()
