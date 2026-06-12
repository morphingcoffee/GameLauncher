"""Shared Cloudflare R2 / rclone configuration for deploy tooling."""

from __future__ import annotations

import atexit
import os
import shutil
import subprocess
import sys
from pathlib import Path
from typing import List, Optional

RCLONE_REMOTE = "gamelauncher_r2"
RCLONE_FLAGS = ["--s3-no-check-bucket", "--exclude", "**/.DS_Store"]

ACCESS_KEY_SERVICE = os.environ.get(
    "GAME_LAUNCHER_R2_ACCESS_KEY_SERVICE", "gamelauncher-r2-access-key-id"
)
SECRET_KEY_SERVICE = os.environ.get(
    "GAME_LAUNCHER_R2_SECRET_KEY_SERVICE", "gamelauncher-r2-secret-access-key"
)
KEYCHAIN_ACCOUNT = os.environ.get("GAME_LAUNCHER_R2_KEYCHAIN_ACCOUNT", os.environ.get("USER", ""))


def find_repo_root(start: Optional[Path] = None) -> Path:
    """Return git repository root, or parent of tools/deploy when not in a repo."""
    start = start or Path(__file__).resolve().parent
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True,
            text=True,
            check=True,
        )
        return Path(result.stdout.strip())
    except (subprocess.CalledProcessError, FileNotFoundError):
        return start.parent.parent


def load_env_file(env_path: Path) -> None:
    """Load KEY=VALUE pairs from a dotenv-style file into os.environ."""
    if not env_path.is_file():
        return
    for line in env_path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key:
            os.environ.setdefault(key, value)


def _keychain_read(service: str) -> str:
    try:
        result = subprocess.run(
            [
                "security",
                "find-generic-password",
                "-a",
                KEYCHAIN_ACCOUNT,
                "-s",
                service,
                "-w",
            ],
            capture_output=True,
            text=True,
            check=True,
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        return ""
    return result.stdout.strip().strip("\r\n")


def load_keychain_credentials() -> bool:
    """Load R2 S3 keys from macOS Keychain into os.environ. Returns False on failure."""
    access_key = _keychain_read(ACCESS_KEY_SERVICE)
    secret_key = _keychain_read(SECRET_KEY_SERVICE)

    if not access_key:
        print(
            f"r2-from-keychain: no item '{ACCESS_KEY_SERVICE}' for account '{KEYCHAIN_ACCOUNT}'",
            file=sys.stderr,
        )
        print(
            f"Store with: security add-generic-password -U -a \"$USER\" -s \"{ACCESS_KEY_SERVICE}\" -w \"ACCESS_KEY_ID\"",
            file=sys.stderr,
        )
        return False

    if not secret_key:
        print(
            f"r2-from-keychain: no item '{SECRET_KEY_SERVICE}' for account '{KEYCHAIN_ACCOUNT}'",
            file=sys.stderr,
        )
        print(
            f"Store with: security add-generic-password -U -a \"$USER\" -s \"{SECRET_KEY_SERVICE}\" -w \"SECRET_ACCESS_KEY\"",
            file=sys.stderr,
        )
        return False

    if len(access_key) != 32:
        print(
            f"r2-from-keychain: access key length is {len(access_key)} (expected 32) — "
            f"re-store Keychain item '{ACCESS_KEY_SERVICE}'",
            file=sys.stderr,
        )
        return False

    if len(secret_key) < 32:
        print(
            f"r2-from-keychain: secret key looks too short ({len(secret_key)} chars) — "
            f"re-store Keychain item '{SECRET_KEY_SERVICE}'",
            file=sys.stderr,
        )
        return False

    os.environ["R2_ACCESS_KEY_ID"] = access_key
    os.environ["R2_SECRET_ACCESS_KEY"] = secret_key
    return True


def require_rclone() -> None:
    if shutil.which("rclone") is None:
        print("rclone not found — install with: brew install rclone", file=sys.stderr)
        sys.exit(1)


def configure_rclone_env() -> None:
    """Set ephemeral RCLONE_CONFIG_* env vars (no rclone.conf on disk)."""
    account_id = os.environ.get("R2_ACCOUNT_ID")
    access_key = os.environ.get("R2_ACCESS_KEY_ID")
    secret_key = os.environ.get("R2_SECRET_ACCESS_KEY")

    if not account_id:
        print("R2_ACCOUNT_ID required", file=sys.stderr)
        sys.exit(1)
    if not access_key:
        print("R2_ACCESS_KEY_ID required", file=sys.stderr)
        sys.exit(1)
    if not secret_key:
        print("R2_SECRET_ACCESS_KEY required", file=sys.stderr)
        sys.exit(1)

    endpoint = f"https://{account_id}.r2.cloudflarestorage.com"
    prefix = f"RCLONE_CONFIG_{RCLONE_REMOTE.upper()}_"
    os.environ[f"{prefix}TYPE"] = "s3"
    os.environ[f"{prefix}PROVIDER"] = "Cloudflare"
    os.environ[f"{prefix}ACCESS_KEY_ID"] = access_key
    os.environ[f"{prefix}SECRET_ACCESS_KEY"] = secret_key
    os.environ[f"{prefix}ENDPOINT"] = endpoint
    os.environ[f"{prefix}ACL"] = "private"
    os.environ[f"{prefix}NO_CHECK_BUCKET"] = "true"


def cleanup_rclone_env() -> None:
    prefix = f"RCLONE_CONFIG_{RCLONE_REMOTE.upper()}_"
    os.environ.pop(f"{prefix}SECRET_ACCESS_KEY", None)
    os.environ.pop(f"{prefix}ACCESS_KEY_ID", None)


def remote_url(bucket: str, path: str = "") -> str:
    base = f"{RCLONE_REMOTE}:{bucket}"
    if path:
        return f"{base}/{path}"
    return base


def rclone_run(args: List[str], **kwargs) -> subprocess.CompletedProcess:
    """Run rclone with standard flags."""
    cmd = ["rclone", *args, *RCLONE_FLAGS]
    return subprocess.run(cmd, **kwargs)


class R2Session:
    """Context manager: load env, configure rclone, cleanup secrets on exit."""

    def __init__(
        self,
        repo_root: Optional[Path] = None,
        use_keychain: bool = True,
    ) -> None:
        self.repo_root = repo_root or find_repo_root()
        self.use_keychain = use_keychain
        self._registered = False

    def __enter__(self) -> R2Session:
        load_env_file(self.repo_root / ".env")
        if self.use_keychain and not os.environ.get("R2_ACCESS_KEY_ID"):
            if not load_keychain_credentials():
                sys.exit(1)
        require_rclone()
        configure_rclone_env()
        if not self._registered:
            atexit.register(cleanup_rclone_env)
            self._registered = True
        return self

    def __exit__(self, *_args) -> None:
        cleanup_rclone_env()

    @property
    def bucket(self) -> str:
        bucket = os.environ.get("R2_BUCKET_NAME")
        if not bucket:
            print("Set R2_BUCKET_NAME in .env (see .env.example)", file=sys.stderr)
            sys.exit(1)
        return bucket
