#!/usr/bin/env python3
"""Register launcher release artifacts from r2_staging into manifests/manifest.json."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional

from catalog_layout import (
    catalog_manifest_path,
    repo_r2_staging_dir,
    r2_launcher_object_key,
    r2_launcher_release_staging_dir,
)
from launcher_version import (
    marketing_version,
    validate_launcher_artifact_version,
    validate_launcher_marketing_version,
)
from r2_config import find_repo_root, load_env_file

CHANNEL_ARTIFACT_TYPES: Dict[str, str] = {
    "windows-x64-msi": "msi",
    "windows-x64-portable": "zip",
    "macos-arm64-dmg": "dmg",
    "macos-x64-dmg": "dmg",
    "macos-arm64-zip": "zip",
    "macos-x64-zip": "zip",
}


def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Patch manifests/manifest.json launcher.channels from staged release artifacts",
    )
    parser.add_argument(
        "artifact_version",
        help="Full artifact version label (e.g. 0.0.1-build51)",
    )
    parser.add_argument(
        "--channel",
        action="append",
        dest="channels",
        help="Channel key to register (repeat for multiple). Default: scan all staged channels for this version.",
    )
    parser.add_argument(
        "--bump-minimum",
        action="store_true",
        help="Raise launcher_minimum_version to artifact_version",
    )
    parser.add_argument(
        "--minimum-version",
        help="Explicit launcher_minimum_version when using --bump-minimum (default: artifact_version marketing prefix)",
    )
    parser.add_argument(
        "--release-notes-url",
        default="https://github.com/morphingcoffee/GameLauncher/releases",
        help="launcher.release_notes_url value",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print manifest diff without writing",
    )
    return parser.parse_args(argv)


def die(message: str) -> None:
    print(f"register-launcher-release: {message}", file=sys.stderr)
    sys.exit(1)


def file_metadata(path: Path) -> Dict[str, Any]:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        while True:
            chunk = handle.read(1024 * 1024)
            if not chunk:
                break
            digest.update(chunk)
    return {
        "file_size_bytes": path.stat().st_size,
        "sha256": digest.hexdigest(),
    }


def discover_channels(repo_root: Path, artifact_version: str) -> List[str]:
    version_root = (
        repo_r2_staging_dir(repo_root) / "launcher" / "releases" / artifact_version
    )
    if not version_root.is_dir():
        return []
    channels: List[str] = []
    for child in sorted(version_root.iterdir()):
        if child.is_dir() and any(child.iterdir()):
            channels.append(child.name)
    return channels


def staged_artifact_file(staging_dir: Path) -> Path:
    files = [path for path in staging_dir.iterdir() if path.is_file()]
    if not files:
        die(f"no artifact file in {staging_dir}")
    if len(files) > 1:
        die(f"expected one artifact in {staging_dir}, found: {[f.name for f in files]}")
    return files[0]


def build_channel_entry(
    *,
    channel: str,
    artifact_version: str,
    artifact_path: Path,
    cdn_base: str,
) -> Dict[str, Any]:
    artifact_type = CHANNEL_ARTIFACT_TYPES.get(channel)
    if artifact_type is None:
        die(f"unknown channel key: {channel}")

    meta = file_metadata(artifact_path)
    object_key = r2_launcher_object_key(artifact_version, channel, artifact_path.name)
    return {
        "version": artifact_version,
        "artifact_type": artifact_type,
        "released_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "download_url": f"{cdn_base}/{object_key}",
        "file_size_bytes": meta["file_size_bytes"],
        "sha256": meta["sha256"],
    }


def register_launcher_release(
    repo_root: Path,
    artifact_version: str,
    channels: List[str],
    *,
    cdn_base: str,
    release_notes_url: str,
    bump_minimum: bool,
    minimum_version: Optional[str],
) -> Dict[str, Any]:
    manifest_path = catalog_manifest_path(repo_root)
    manifest = json.loads(manifest_path.read_text())

    launcher = manifest.setdefault("launcher", {})
    if not isinstance(launcher, dict):
        die("manifest launcher block must be an object")

    launcher["release_notes_url"] = release_notes_url
    channel_map: Dict[str, Any] = launcher.setdefault("channels", {})
    if not isinstance(channel_map, dict):
        die("manifest launcher.channels must be an object")

    for channel in channels:
        staging_dir = r2_launcher_release_staging_dir(repo_root, artifact_version, channel)
        if not staging_dir.is_dir():
            die(f"staging directory missing: {staging_dir}")
        artifact_path = staged_artifact_file(staging_dir)
        channel_map[channel] = build_channel_entry(
            channel=channel,
            artifact_version=artifact_version,
            artifact_path=artifact_path,
            cdn_base=cdn_base,
        )

    if bump_minimum:
        manifest["launcher_minimum_version"] = minimum_version or marketing_version(artifact_version)

    return manifest


def validate_version_inputs(
    artifact_version: str,
    *,
    bump_minimum: bool,
    minimum_version: Optional[str],
) -> None:
    try:
        validate_launcher_artifact_version(artifact_version)
    except ValueError as error:
        die(str(error))

    if minimum_version is not None:
        try:
            validate_launcher_marketing_version(minimum_version)
        except ValueError as error:
            die(f"invalid --minimum-version: {error}")

    if bump_minimum and minimum_version is None:
        try:
            validate_launcher_marketing_version(marketing_version(artifact_version))
        except ValueError as error:
            die(str(error))


def main(argv: Optional[List[str]] = None) -> None:
    args = parse_args(argv)
    repo_root = find_repo_root()
    load_env_file(repo_root / ".env")

    validate_version_inputs(
        args.artifact_version,
        bump_minimum=args.bump_minimum,
        minimum_version=args.minimum_version,
    )

    cdn_base = __import__("os").environ.get("R2_PUBLIC_CDN_BASE_URL", "").rstrip("/")
    if not cdn_base:
        die("R2_PUBLIC_CDN_BASE_URL is required")

    channels = args.channels or discover_channels(repo_root, args.artifact_version)
    if not channels:
        die(
            f"no staged launcher artifacts under r2_staging/launcher/releases/{args.artifact_version}/ "
            "(pass --channel explicitly)",
        )

    manifest = register_launcher_release(
        repo_root,
        args.artifact_version,
        channels,
        cdn_base=cdn_base,
        release_notes_url=args.release_notes_url,
        bump_minimum=args.bump_minimum,
        minimum_version=args.minimum_version,
    )

    manifest_path = catalog_manifest_path(repo_root)
    serialized = json.dumps(manifest, indent=2) + "\n"
    if args.dry_run:
        print(serialized)
        return

    manifest_path.write_text(serialized)
    print(
        f"register-launcher-release: updated {manifest_path.relative_to(repo_root)} "
        f"channels={','.join(channels)}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
