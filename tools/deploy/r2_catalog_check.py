#!/usr/bin/env python3
"""Validate live R2 catalog: manifest, per-game versions.json, and build objects."""

from __future__ import annotations

import argparse
import json
import re
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Sequence, Set
from urllib.parse import urlparse

from catalog_layout import (
    catalog_manifest_path,
    git_versions_path,
    r2_versions_object_key,
)
from register_game_version import VALID_PLATFORMS, is_object_not_found
from r2_config import R2Session, find_repo_root, load_env_file, remote_url, rclone_run

SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
REQUIRED_BUILD_FIELDS = ("download_url", "executable_path", "file_size_bytes", "sha256")


@dataclass
class CheckResult:
    scope: str
    name: str
    passed: bool
    detail: str = ""
    severity: str = "error"  # error | warn


@dataclass
class CatalogChecker:
    results: List[CheckResult] = field(default_factory=list)

    def ok(self, scope: str, name: str, detail: str = "") -> None:
        self.results.append(CheckResult(scope, name, True, detail))

    def fail(self, scope: str, name: str, detail: str = "") -> None:
        self.results.append(CheckResult(scope, name, False, detail, "error"))

    def warn(self, scope: str, name: str, detail: str = "") -> None:
        self.results.append(CheckResult(scope, name, False, detail, "warn"))

    @property
    def errors(self) -> int:
        return sum(1 for r in self.results if not r.passed and r.severity == "error")

    @property
    def warnings(self) -> int:
        return sum(1 for r in self.results if not r.passed and r.severity == "warn")

    @property
    def passed(self) -> bool:
        return self.errors == 0


def cdn_path_from_url(cdn_base: str, url: str) -> Optional[str]:
    base = cdn_base.rstrip("/")
    if not url.startswith(f"{base}/"):
        return None
    return url[len(base) + 1 :]


def validate_build_metadata(
    checker: CatalogChecker,
    scope: str,
    platform: str,
    build: Dict[str, Any],
) -> None:
    label = f"{scope} build[{platform}]"
    if platform not in VALID_PLATFORMS:
        checker.warn(label, f"non-standard platform key: {platform}")

    for key in REQUIRED_BUILD_FIELDS:
        if key not in build or build[key] in (None, ""):
            checker.fail(label, f"missing {key}")
            return

    size = build.get("file_size_bytes")
    if not isinstance(size, int) or size <= 0:
        checker.fail(label, f"invalid file_size_bytes: {size!r}")
    else:
        checker.ok(label, "required metadata present")

    sha = build.get("sha256")
    if not isinstance(sha, str) or not SHA256_RE.match(sha):
        checker.fail(f"{label} sha256", "expected 64-char lowercase hex")
    else:
        checker.ok(label, "sha256 valid")

    if not build.get("uncompressed_size_bytes"):
        checker.fail(label, "missing uncompressed_size_bytes")


def remote_object_size(remote: str, object_key: str) -> Optional[int]:
    import subprocess

    result = rclone_run(
        ["lsjson", f"{remote}/{object_key}"],
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        return None
    try:
        entries = json.loads(result.stdout or "[]")
    except json.JSONDecodeError:
        return None
    if not entries:
        return None
    size = entries[0].get("Size")
    return int(size) if isinstance(size, int) else None


def remote_object_exists(remote: str, object_key: str) -> bool:
    return remote_object_size(remote, object_key) is not None


def fetch_remote_json(remote: str, object_key: str, local_path: Path) -> Optional[Dict[str, Any]]:
    import subprocess

    copy_result = rclone_run(
        ["copyto", f"{remote}/{object_key}", str(local_path)],
        capture_output=True,
        text=True,
    )
    if copy_result.returncode == 0 and local_path.is_file():
        return json.loads(local_path.read_text())
    if is_object_not_found(copy_result):
        return None
    detail = (copy_result.stderr or copy_result.stdout or "unknown error").strip()
    raise RuntimeError(f"failed to fetch {object_key}: {detail}")


def check_build_object(
    checker: CatalogChecker,
    scope: str,
    platform: str,
    build: Dict[str, Any],
    remote: str,
    cdn_base: str,
) -> None:
    label = f"{scope} object[{platform}]"
    download_url = build.get("download_url")
    if not isinstance(download_url, str) or not download_url:
        return

    object_key = cdn_path_from_url(cdn_base, download_url)
    if object_key is None:
        parsed = urlparse(download_url)
        object_key = parsed.path.lstrip("/")
        checker.warn(label, f"download_url host differs from CDN base; using path {object_key}")

    if not remote_object_exists(remote, object_key):
        checker.fail(label, f"missing on R2: {object_key}")
        return

    checker.ok(label, object_key)

    expected_size = build.get("file_size_bytes")
    actual_size = remote_object_size(remote, object_key)
    if isinstance(expected_size, int) and actual_size is not None and expected_size != actual_size:
        checker.fail(
            f"{label} size",
            f"file_size_bytes={expected_size} but R2 object is {actual_size} bytes",
        )
    elif actual_size is not None:
        checker.ok(f"{label} size", f"{actual_size} bytes")


def check_thumbnail(
    checker: CatalogChecker,
    game_id: str,
    thumbnail_url: Optional[str],
    remote: str,
    cdn_base: str,
) -> None:
    scope = f"game:{game_id}"
    if not thumbnail_url:
        checker.warn(scope, "missing thumbnail_url")
        return

    object_key = cdn_path_from_url(cdn_base, thumbnail_url)
    if object_key is None:
        checker.warn(f"{scope} thumbnail", "URL does not match R2_PUBLIC_CDN_BASE_URL")
        return

    if remote_object_exists(remote, object_key):
        checker.ok(f"{scope} thumbnail", object_key)
    else:
        checker.fail(f"{scope} thumbnail", f"missing on R2: {object_key}")


def check_versions_index(
    checker: CatalogChecker,
    game_id: str,
    versions_data: Dict[str, Any],
    remote: str,
    cdn_base: str,
    *,
    check_objects: bool,
) -> None:
    scope = f"game:{game_id}"
    if versions_data.get("game_id") != game_id:
        checker.fail(
            scope,
            f"versions.json game_id mismatch: {versions_data.get('game_id')!r}",
        )
        return
    checker.ok(f"{scope} versions.json", "game_id matches")

    versions = versions_data.get("versions")
    if not isinstance(versions, list) or not versions:
        checker.fail(f"{scope} versions", "empty or missing versions array")
        return

    checker.ok(f"{scope} versions", f"{len(versions)} entr{'y' if len(versions) == 1 else 'ies'}")

    for entry in versions:
        version = entry.get("version")
        if not version:
            checker.fail(scope, "version entry missing version field")
            continue
        entry_scope = f"{scope} v{version}"
        if not entry.get("released_at"):
            checker.warn(entry_scope, "missing released_at")

        builds = entry.get("builds")
        if not isinstance(builds, dict) or not builds:
            checker.fail(entry_scope, "missing builds")
            continue

        for platform, build in builds.items():
            if not isinstance(build, dict):
                checker.fail(entry_scope, f"build[{platform}] is not an object")
                continue
            validate_build_metadata(checker, entry_scope, platform, build)
            if check_objects:
                check_build_object(checker, entry_scope, platform, build, remote, cdn_base)


def compare_json_documents(
    checker: CatalogChecker,
    scope: str,
    label: str,
    left: Dict[str, Any],
    right: Dict[str, Any],
) -> None:
    if left == right:
        checker.ok(scope, f"{label} matches git")
        return
    checker.warn(scope, f"{label} differs from git copy")


def check_game(
    checker: CatalogChecker,
    game: Dict[str, Any],
    remote: str,
    cdn_base: str,
    repo_root: Path,
    *,
    check_objects: bool,
    compare_git: bool,
    tmpdir: Path,
) -> None:
    game_id = game.get("id")
    if not game_id:
        checker.fail("manifest", "game entry missing id")
        return

    scope = f"game:{game_id}"
    for field_name in ("title", "latest_version", "versions_url"):
        if not game.get(field_name):
            checker.fail(scope, f"catalog missing {field_name}")

    latest_version = game.get("latest_version")
    catalog_builds = game.get("builds") or {}
    if catalog_builds:
        checker.ok(f"{scope} catalog builds", f"{len(catalog_builds)} platform(s)")
        for platform, build in catalog_builds.items():
            if isinstance(build, dict):
                validate_build_metadata(checker, f"{scope} catalog", platform, build)
                if check_objects:
                    check_build_object(
                        checker,
                        f"{scope} catalog",
                        platform,
                        build,
                        remote,
                        cdn_base,
                    )
    else:
        checker.warn(f"{scope} catalog builds", "no inline builds in manifest")

    check_thumbnail(checker, game_id, game.get("thumbnail_url"), remote, cdn_base)

    versions_key = r2_versions_object_key(game_id)
    versions_path = tmpdir / f"{game_id}-versions.json"
    try:
        versions_data = fetch_remote_json(remote, versions_key, versions_path)
    except RuntimeError as exc:
        checker.fail(scope, str(exc))
        return

    if versions_data is None:
        checker.fail(scope, f"missing R2 object: {versions_key}")
        return

    checker.ok(scope, f"R2 {versions_key} present")
    check_versions_index(
        checker,
        game_id,
        versions_data,
        remote,
        cdn_base,
        check_objects=check_objects,
    )

    if latest_version and isinstance(versions_data.get("versions"), list):
        version_numbers = {entry.get("version") for entry in versions_data["versions"]}
        if latest_version not in version_numbers:
            checker.fail(
                scope,
                f"catalog latest_version {latest_version} not found in versions.json",
            )
        else:
            checker.ok(scope, f"latest_version {latest_version} in versions.json")

        latest_entry = next(
            (entry for entry in versions_data["versions"] if entry.get("version") == latest_version),
            None,
        )
        if latest_entry and catalog_builds:
            remote_builds = latest_entry.get("builds") or {}
            for platform, catalog_build in catalog_builds.items():
                remote_build = remote_builds.get(platform)
                if remote_build is None:
                    checker.warn(
                        scope,
                        f"catalog build[{platform}] not in versions.json for {latest_version}",
                    )
                elif remote_build != catalog_build:
                    checker.warn(
                        scope,
                        f"catalog build[{platform}] differs from versions.json for {latest_version}",
                    )
                else:
                    checker.ok(
                        scope,
                        f"catalog build[{platform}] matches versions.json",
                    )

    if compare_git:
        git_path = git_versions_path(repo_root, game_id)
        if git_path.is_file():
            git_versions = json.loads(git_path.read_text())
            compare_json_documents(checker, scope, "versions.json", git_versions, versions_data)
        else:
            checker.warn(scope, f"no git copy at {git_path.relative_to(repo_root)}")


def check_manifest_structure(checker: CatalogChecker, manifest: Dict[str, Any]) -> List[Dict[str, Any]]:
    if manifest.get("schema_version") != 1:
        checker.warn("manifest", f"unexpected schema_version: {manifest.get('schema_version')!r}")
    else:
        checker.ok("manifest", "schema_version=1")

    games = manifest.get("games")
    if not isinstance(games, list):
        checker.fail("manifest", "games must be an array")
        return []
    if not games:
        checker.warn("manifest", "no games in catalog")
    else:
        checker.ok("manifest", f"{len(games)} game(s)")
    return games


def filter_games(games: Sequence[Dict[str, Any]], only: Optional[Set[str]]) -> List[Dict[str, Any]]:
    if not only:
        return list(games)
    filtered = [game for game in games if game.get("id") in only]
    missing = only - {game.get("id") for game in filtered}
    return filtered


def parse_args(argv: Optional[Sequence[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate R2 catalog integrity (manifest, versions.json, build zips)",
    )
    parser.add_argument(
        "--manifest",
        type=Path,
        help="Validate a local manifest file instead of live R2 manifest.json",
    )
    parser.add_argument(
        "--game",
        action="append",
        dest="games",
        metavar="GAME_ID",
        help="Limit checks to one or more game ids (repeatable)",
    )
    parser.add_argument(
        "--skip-objects",
        action="store_true",
        help="Skip R2 existence/size checks for zips and thumbnails",
    )
    parser.add_argument(
        "--compare-git",
        action="store_true",
        help="Compare live R2 JSON with git manifests/games/*/versions.json",
    )
    parser.add_argument(
        "--compare-git-manifest",
        action="store_true",
        help="Compare live R2 manifest.json with git manifests/manifest.json",
    )
    return parser.parse_args(argv)


def _use_color() -> bool:
    import os

    return sys.stderr.isatty() and os.environ.get("NO_COLOR") is None


def _color(text: str, code: str) -> str:
    if not _use_color():
        return text
    return f"{code}{text}\033[0m"


def print_report(checker: CatalogChecker) -> None:
    print("\nr2-catalog-check", file=sys.stderr)
    print("-" * 72, file=sys.stderr)
    for result in checker.results:
        if result.passed:
            status = _color("PASS", "\033[32m")
        elif result.severity == "warn":
            status = _color("WARN", "\033[33m")
        else:
            status = _color("FAIL", "\033[31m")
        line = f"{status}  [{result.scope}] {result.name}"
        if result.detail:
            line = f"{line} — {result.detail}"
        print(line, file=sys.stderr)
    print("-" * 72, file=sys.stderr)
    summary = f"errors={checker.errors} warnings={checker.warnings}"
    if checker.passed:
        if checker.warnings:
            print(_color(f"r2-catalog-check: OK ({summary})", "\033[33m"), file=sys.stderr)
        else:
            print(_color(f"r2-catalog-check: OK ({summary})", "\033[32m"), file=sys.stderr)
    else:
        print(_color(f"r2-catalog-check: FAILED ({summary})", "\033[31m"), file=sys.stderr)


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = parse_args(argv)
    repo_root = find_repo_root()
    load_env_file(repo_root / ".env")
    checker = CatalogChecker()
    game_filter = set(args.games) if args.games else None

    import os

    cdn_base = os.environ.get("R2_PUBLIC_CDN_BASE_URL", "").rstrip("/")
    if not cdn_base and not args.skip_objects:
        checker.fail("config", "R2_PUBLIC_CDN_BASE_URL required (set in .env)")
        print_report(checker)
        return 1

    use_keychain = not os.environ.get("R2_ACCESS_KEY_ID")
    with R2Session(repo_root=repo_root, use_keychain=use_keychain) as session:
        remote = remote_url(session.bucket)

        with tempfile.TemporaryDirectory() as tmp:
            tmpdir = Path(tmp)

            if args.manifest:
                if not args.manifest.is_file():
                    checker.fail("manifest", f"not found: {args.manifest}")
                    print_report(checker)
                    return 1
                manifest = json.loads(args.manifest.read_text())
                checker.ok("manifest", f"loaded local {args.manifest}")
            else:
                manifest_path = tmpdir / "manifest.json"
                try:
                    manifest = fetch_remote_json(remote, "manifest.json", manifest_path)
                except RuntimeError as exc:
                    checker.fail("manifest", str(exc))
                    print_report(checker)
                    return 1
                if manifest is None:
                    checker.fail("manifest", "missing R2 object: manifest.json")
                    print_report(checker)
                    return 1
                checker.ok("manifest", "live R2 manifest.json loaded")

            if args.compare_git_manifest and not args.manifest:
                git_manifest_path = catalog_manifest_path(repo_root)
                if git_manifest_path.is_file():
                    git_manifest = json.loads(git_manifest_path.read_text())
                    compare_json_documents(
                        checker,
                        "manifest",
                        "manifest.json",
                        git_manifest,
                        manifest,
                    )
                else:
                    checker.warn("manifest", "git manifests/manifest.json not found")

            games = check_manifest_structure(checker, manifest)
            games = filter_games(games, game_filter)

            if game_filter:
                missing = game_filter - {game.get("id") for game in games}
                for game_id in sorted(missing):
                    checker.fail(f"game:{game_id}", "not listed in manifest")

            for game in games:
                check_game(
                    checker,
                    game,
                    remote,
                    cdn_base,
                    repo_root,
                    check_objects=not args.skip_objects,
                    compare_git=args.compare_git,
                    tmpdir=tmpdir,
                )

    print_report(checker)
    return 0 if checker.passed else 1


if __name__ == "__main__":
    sys.exit(main())
