#!/usr/bin/env python3
"""Validate R2 env vars, credentials, and bucket connectivity."""

from __future__ import annotations

import os
import re
import subprocess
import sys
import tempfile
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional, Sequence, Tuple

from r2_config import (
    RCLONE_REMOTE,
    find_repo_root,
    load_env_file,
    load_keychain_credentials,
    rclone_flags_for,
    require_rclone,
)

PROBE_PREFIX = ".gamelauncher-r2-probe"

ACCOUNT_ID_RE = re.compile(r"^[0-9a-f]{32}$")
BUCKET_NAME_RE = re.compile(r"^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$")


@dataclass
class CheckResult:
    name: str
    passed: bool
    detail: str = ""


class Checker:
    def __init__(self) -> None:
        self.results: List[CheckResult] = []

    def record(self, name: str, passed: bool, detail: str = "") -> None:
        self.results.append(CheckResult(name, passed, detail))

    def ok(self, name: str, detail: str = "") -> None:
        self.record(name, True, detail)

    def fail(self, name: str, detail: str = "") -> None:
        self.record(name, False, detail)

    @property
    def passed(self) -> bool:
        return all(result.passed for result in self.results)


def _use_color() -> bool:
    return sys.stderr.isatty() and os.environ.get("NO_COLOR") is None


def _color(text: str, code: str) -> str:
    if not _use_color():
        return text
    return f"{code}{text}\033[0m"


def _status(passed: bool) -> str:
    if passed:
        return _color("PASS", "\033[32m")
    return _color("FAIL", "\033[31m")


def _require_non_empty(checker: Checker, name: str, value: Optional[str]) -> bool:
    if value and value.strip():
        checker.ok(f"{name} set")
        return True
    checker.fail(f"{name} set", "missing or empty")
    return False


def validate_access_key(checker: Checker, name: str, value: Optional[str]) -> bool:
    if not _require_non_empty(checker, name, value):
        return False
    assert value is not None
    if len(value) == 32:
        checker.ok(f"{name} format", "32 characters")
        return True
    checker.fail(f"{name} format", f"expected 32 characters, got {len(value)}")
    return False


def validate_secret_key(checker: Checker, name: str, value: Optional[str]) -> bool:
    if not _require_non_empty(checker, name, value):
        return False
    assert value is not None
    if len(value) >= 32:
        checker.ok(f"{name} format", f"{len(value)} characters")
        return True
    checker.fail(f"{name} format", f"expected at least 32 characters, got {len(value)}")
    return False


def validate_account_id(checker: Checker, value: Optional[str]) -> bool:
    if not _require_non_empty(checker, "R2_ACCOUNT_ID", value):
        return False
    assert value is not None
    if ACCOUNT_ID_RE.match(value):
        checker.ok("R2_ACCOUNT_ID format", "32 hex characters")
        return True
    checker.fail("R2_ACCOUNT_ID format", "expected 32 lowercase hex characters")
    return False


def validate_bucket_name(checker: Checker, value: Optional[str]) -> bool:
    if not _require_non_empty(checker, "R2_BUCKET_NAME", value):
        return False
    assert value is not None
    if BUCKET_NAME_RE.match(value):
        checker.ok("R2_BUCKET_NAME format")
        return True
    checker.fail(
        "R2_BUCKET_NAME format",
        "expected 3-63 chars, lowercase letters, digits, and hyphens",
    )
    return False


def validate_cdn_url(checker: Checker, value: Optional[str]) -> bool:
    if not _require_non_empty(checker, "R2_PUBLIC_CDN_BASE_URL", value):
        return False
    assert value is not None
    if not value.startswith("https://"):
        checker.fail("R2_PUBLIC_CDN_BASE_URL format", "must start with https://")
        return False
    if value.endswith("/"):
        checker.fail("R2_PUBLIC_CDN_BASE_URL format", "must not have a trailing slash")
        return False
    checker.ok("R2_PUBLIC_CDN_BASE_URL format")
    return True


def run_format_checks(checker: Checker) -> bool:
    ok = True
    ok = validate_account_id(checker, os.environ.get("R2_ACCOUNT_ID")) and ok
    ok = validate_bucket_name(checker, os.environ.get("R2_BUCKET_NAME")) and ok
    ok = validate_cdn_url(checker, os.environ.get("R2_PUBLIC_CDN_BASE_URL")) and ok
    ok = validate_access_key(checker, "R2_ACCESS_KEY_ID", os.environ.get("R2_ACCESS_KEY_ID")) and ok
    ok = validate_secret_key(
        checker,
        "R2_SECRET_ACCESS_KEY",
        os.environ.get("R2_SECRET_ACCESS_KEY"),
    ) and ok
    return ok


def configure_ephemeral_remote(
    remote_name: str,
    access_key_id: str,
    secret_access_key: str,
    account_id: str,
) -> None:
    endpoint = f"https://{account_id}.r2.cloudflarestorage.com"
    prefix = f"RCLONE_CONFIG_{remote_name.upper()}_"
    os.environ[f"{prefix}TYPE"] = "s3"
    os.environ[f"{prefix}PROVIDER"] = "Cloudflare"
    os.environ[f"{prefix}ACCESS_KEY_ID"] = access_key_id
    os.environ[f"{prefix}SECRET_ACCESS_KEY"] = secret_access_key
    os.environ[f"{prefix}ENDPOINT"] = endpoint
    os.environ[f"{prefix}ACL"] = "private"
    os.environ[f"{prefix}NO_CHECK_BUCKET"] = "true"


def cleanup_ephemeral_remote(remote_name: str) -> None:
    prefix = f"RCLONE_CONFIG_{remote_name.upper()}_"
    os.environ.pop(f"{prefix}SECRET_ACCESS_KEY", None)
    os.environ.pop(f"{prefix}ACCESS_KEY_ID", None)


def remote_path(remote_name: str, bucket: str, key: str = "") -> str:
    base = f"{remote_name}:{bucket}"
    if key:
        return f"{base}/{key}"
    return base


def rclone_with_remote(remote_name: str, args: Sequence[str], **kwargs) -> subprocess.CompletedProcess:
    cmd = ["rclone", *args, *rclone_flags_for(list(args))]
    return subprocess.run(cmd, **kwargs)


def probe_key(suffix: str = "env-check") -> str:
    return f"{PROBE_PREFIX}/{suffix}-{uuid.uuid4()}.txt"


def probe_read(
    checker: Checker,
    label: str,
    remote_name: str,
    bucket: str,
    prefix: str,
) -> None:
    target = remote_path(remote_name, bucket, prefix)
    result = rclone_with_remote(
        remote_name,
        ["lsf", target, "--max-depth", "1"],
        capture_output=True,
        text=True,
    )
    if result.returncode in (0, 141):
        checker.ok(f"{label} read", prefix or "(bucket root)")
        return
    detail = (result.stderr or result.stdout or "list failed").strip().splitlines()
    checker.fail(f"{label} read", detail[0] if detail else "list failed")


def probe_write(
    checker: Checker,
    label: str,
    remote_name: str,
    bucket: str,
    key: str,
    local_path: Path,
    *,
    dry_run: bool = False,
) -> None:
    args = ["copyto", str(local_path), remote_path(remote_name, bucket, key)]
    if dry_run:
        args.append("--dry-run")
    result = rclone_with_remote(
        remote_name,
        args,
        capture_output=True,
        text=True,
    )
    if result.returncode == 0:
        checker.ok(f"{label} write", key)
        return
    detail = (result.stderr or result.stdout or "write failed").strip().splitlines()
    checker.fail(f"{label} write", detail[0] if detail else "write failed")


def probe_delete(
    checker: Checker,
    label: str,
    remote_name: str,
    bucket: str,
    key: str,
) -> bool:
    result = rclone_with_remote(
        remote_name,
        ["deletefile", remote_path(remote_name, bucket, key)],
        capture_output=True,
        text=True,
    )
    if result.returncode == 0:
        checker.ok(f"{label} delete", key)
        return True
    detail = (result.stderr or result.stdout or "delete failed").strip().splitlines()
    checker.fail(f"{label} delete", detail[0] if detail else "delete failed")
    return False


def cleanup_probe(
    remote_name: str,
    bucket: str,
    key: str,
) -> None:
    result = rclone_with_remote(
        remote_name,
        ["deletefile", remote_path(remote_name, bucket, key)],
        capture_output=True,
        text=True,
    )
    if result.returncode == 0:
        print(f"r2-env-check: cleaned up {key}", file=sys.stderr)
        return
    print(
        f"r2-env-check: cleanup skipped for {key} (delete denied or object missing)",
        file=sys.stderr,
    )


def run_r2_probes(
    checker: Checker,
    remote_name: str,
    bucket: str,
    probe_file: Path,
) -> List[str]:
    """Connectivity probes for a single bucket-scoped token."""
    created_keys: List[str] = []
    label = "r2"

    probe_read(checker, label, remote_name, bucket, "games/")
    probe_read(checker, label, remote_name, bucket, "assets/")
    probe_read(checker, label, remote_name, bucket, "manifest.json")

    game_key = f"games/{probe_key('write')}"
    probe_write(checker, label, remote_name, bucket, game_key, probe_file)
    if checker.results[-1].passed:
        created_keys.append(game_key)

    probe_write(
        checker,
        label,
        remote_name,
        bucket,
        "manifest.json",
        probe_file,
        dry_run=True,
    )

    surviving_keys: List[str] = []
    for key in created_keys:
        if not probe_delete(checker, label, remote_name, bucket, key):
            surviving_keys.append(key)

    return surviving_keys


def print_report(checker: Checker) -> None:
    print("\nr2-env-check", file=sys.stderr)
    print("-" * 72, file=sys.stderr)
    for result in checker.results:
        line = f"{_status(result.passed)}  {result.name}"
        if result.detail:
            line = f"{line} — {result.detail}"
        print(line, file=sys.stderr)
    print("-" * 72, file=sys.stderr)
    if checker.passed:
        print(_color("r2-env-check: OK", "\033[32m"), file=sys.stderr)
    else:
        print(_color("r2-env-check: FAILED", "\033[31m"), file=sys.stderr)


def load_credentials(repo_root: Path, checker: Checker) -> bool:
    load_env_file(repo_root / ".env")
    if not os.environ.get("R2_ACCESS_KEY_ID"):
        if not load_keychain_credentials():
            checker.fail("Keychain credentials", "see tools/deploy/README.md")
            return False
        checker.ok("Keychain credentials")
    elif not os.environ.get("GITHUB_ACTIONS"):
        checker.ok("R2 credentials from environment")
    return True


def main() -> int:
    repo_root = find_repo_root()
    checker = Checker()

    if not load_credentials(repo_root, checker):
        print_report(checker)
        return 1

    if not run_format_checks(checker):
        print_report(checker)
        return 1

    require_rclone()

    account_id = os.environ["R2_ACCOUNT_ID"]
    bucket = os.environ["R2_BUCKET_NAME"]
    access_key = os.environ["R2_ACCESS_KEY_ID"]
    secret_key = os.environ["R2_SECRET_ACCESS_KEY"]

    configure_ephemeral_remote(RCLONE_REMOTE, access_key, secret_key, account_id)

    probe_fd, probe_path = tempfile.mkstemp(suffix=".txt")
    os.close(probe_fd)
    probe_file = Path(probe_path)
    probe_file.write_text("r2-env-check probe")

    created_keys: List[Tuple[str, str]] = []
    try:
        keys = run_r2_probes(checker, RCLONE_REMOTE, bucket, probe_file)
        created_keys.extend((RCLONE_REMOTE, key) for key in keys)
    finally:
        cleanup_ephemeral_remote(RCLONE_REMOTE)
        if probe_file.exists():
            probe_file.unlink()

    for remote_name, key in created_keys:
        cleanup_probe(remote_name, bucket, key)

    print_report(checker)
    return 0 if checker.passed else 1


if __name__ == "__main__":
    sys.exit(main())
