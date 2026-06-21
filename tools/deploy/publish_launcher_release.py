#!/usr/bin/env python3
"""Upload staged launcher release artifacts to R2 and commit manifest.json."""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
from typing import List, Optional

from catalog_layout import catalog_manifest_path, r2_launcher_release_staging_dir
from r2_config import find_repo_root, load_env_file
from register_game_version import ensure_git_author
from register_launcher_release import die, register_launcher_release

DEFAULT_CHANNELS = ("windows-x64-msi", "windows-x64-portable")


def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Register launcher release, upload blobs + manifest to R2, commit catalog",
    )
    parser.add_argument(
        "artifact_version",
        help="Full artifact version label (e.g. 0.0.1-build51)",
    )
    parser.add_argument(
        "--channel",
        action="append",
        dest="channels",
        help="Channel key to publish (repeatable). Default: windows-x64-msi and windows-x64-portable",
    )
    parser.add_argument(
        "--bump-minimum",
        action="store_true",
        help="Raise launcher_minimum_version to artifact_version",
    )
    parser.add_argument(
        "--minimum-version",
        help="Explicit launcher_minimum_version when using --bump-minimum",
    )
    parser.add_argument(
        "--skip-git-push",
        action="store_true",
        help="Upload to R2 but do not commit or push manifests/manifest.json",
    )
    return parser.parse_args(argv)


def deploy_channel(repo_root: Path, artifact_version: str, channel: str) -> None:
    staging_dir = r2_launcher_release_staging_dir(repo_root, artifact_version, channel)
    if not staging_dir.is_dir():
        die(f"staging directory missing: {staging_dir}")

    remote_key = f"launcher/releases/{artifact_version}/{channel}"
    subprocess.run(
        [
            sys.executable,
            str(repo_root / "tools" / "deploy" / "r2_deploy.py"),
            "--copy",
            f"./{staging_dir.relative_to(repo_root)}",
            remote_key,
        ],
        cwd=repo_root,
        check=True,
    )


def publish_manifest(repo_root: Path) -> None:
    subprocess.run(
        [sys.executable, str(repo_root / "tools" / "deploy" / "r2_publish_manifest.py")],
        cwd=repo_root,
        check=True,
    )


def commit_manifest(repo_root: Path, artifact_version: str, *, push: bool) -> None:
    manifest_path = catalog_manifest_path(repo_root)
    if not manifest_path.is_file():
        die(f"manifest missing after register: {manifest_path}")

    rel_path = str(manifest_path.relative_to(repo_root))
    diff = subprocess.run(
        ["git", "-C", str(repo_root), "diff", "--quiet", "--", rel_path],
        check=False,
    )
    if diff.returncode == 0:
        print("publish-launcher-release: manifest unchanged — skipping git commit", file=sys.stderr)
        return

    ensure_git_author(repo_root)
    subprocess.run(["git", "-C", str(repo_root), "add", rel_path], check=True)
    subprocess.run(
        [
            "git",
            "-C",
            str(repo_root),
            "commit",
            "-m",
            f"Register launcher release {artifact_version}\n\n"
            "Update launcher channels in manifests/manifest.json.",
        ],
        check=True,
    )
    if push:
        subprocess.run(["git", "-C", str(repo_root), "push"], check=True)
        print("publish-launcher-release: committed and pushed manifest", file=sys.stderr)


def main(argv: Optional[List[str]] = None) -> None:
    args = parse_args(argv)
    repo_root = find_repo_root()
    load_env_file(repo_root / ".env")

    cdn_base = os.environ.get("R2_PUBLIC_CDN_BASE_URL", "").rstrip("/")
    if not cdn_base:
        die("R2_PUBLIC_CDN_BASE_URL is required")

    channels = args.channels or list(DEFAULT_CHANNELS)
    release_notes_url = os.environ.get(
        "LAUNCHER_RELEASE_NOTES_URL",
        "https://github.com/morphingcoffee/GameLauncher/releases",
    )

    manifest = register_launcher_release(
        repo_root,
        args.artifact_version,
        channels,
        cdn_base=cdn_base,
        release_notes_url=release_notes_url,
        bump_minimum=args.bump_minimum,
        minimum_version=args.minimum_version,
    )

    manifest_path = catalog_manifest_path(repo_root)
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
    print(f"publish-launcher-release: updated {manifest_path.relative_to(repo_root)}", file=sys.stderr)

    for channel in channels:
        deploy_channel(repo_root, args.artifact_version, channel)

    if args.skip_git_push:
        print("publish-launcher-release: --skip-git-push — skipping git commit", file=sys.stderr)
    else:
        commit_manifest(repo_root, args.artifact_version, push=True)

    publish_manifest(repo_root)
    print(
        f"publish-launcher-release: published {args.artifact_version} "
        f"channels={','.join(channels)}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
