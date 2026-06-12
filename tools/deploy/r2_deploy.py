#!/usr/bin/env python3
"""Sync or copy a local directory to Cloudflare R2 via rclone."""

from __future__ import annotations

import argparse
import re
import sys
import tempfile
from pathlib import Path

from r2_config import R2Session, remote_url, rclone_run

DRY_RUN_DELETE_PATTERN = re.compile(
    r"Skipped (delete|remove directory) as --dry-run is set"
)
NOTICE_PATH_PATTERN = re.compile(r"^.*NOTICE: ([^:]+):")


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="r2_deploy.py",
        description="Upload a local directory to Cloudflare R2 via rclone.",
        epilog=(
            "Prefer --copy for game binaries (append-only versioned prefixes). "
            "Default sync mirrors local to remote and may delete remote objects."
        ),
    )
    parser.add_argument(
        "--copy",
        action="store_true",
        help="Upload only (rclone copy). Does not delete remote objects.",
    )
    parser.add_argument(
        "--allow-deletes",
        action="store_true",
        help="When syncing, proceed if dry-run shows remote deletes.",
    )
    parser.add_argument(
        "local_dir",
        type=Path,
        help="Directory to upload",
    )
    parser.add_argument(
        "remote_prefix",
        nargs="?",
        default="",
        help="Optional path inside the bucket (no leading slash)",
    )
    return parser.parse_args(argv)


def extract_dry_run_deletes(dry_log: str) -> list[str]:
    paths: list[str] = []
    for line in dry_log.splitlines():
        if DRY_RUN_DELETE_PATTERN.search(line):
            match = NOTICE_PATH_PATTERN.match(line)
            if match:
                paths.append(match.group(1).strip())
    return paths


def run_copy(local_dir: Path, remote: str) -> None:
    print("r2-deploy: copy (no remote deletes)...", file=sys.stderr)
    rclone_run(
        [
            "copy",
            str(local_dir),
            remote,
            "--progress",
            "--retries",
            "5",
            "--retries-sleep",
            "5s",
            "--low-level-retries",
            "10",
            "--stats-one-line",
            "--stats",
            "5s",
        ],
        check=True,
    )


def run_sync(local_dir: Path, remote: str, allow_deletes: bool) -> None:
    dry_log_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(mode="w+", delete=False, suffix=".log") as dry_file:
            dry_log_path = Path(dry_file.name)

        print("r2-deploy: dry-run (checking for remote deletes)...", file=sys.stderr)
        dry_result = rclone_run(
            [
                "sync",
                str(local_dir),
                remote,
                "--dry-run",
                "-v",
                "--stats-one-line",
            ],
            capture_output=True,
            text=True,
        )
        dry_log = dry_result.stdout + dry_result.stderr
        if dry_log_path:
            dry_log_path.write_text(dry_log)

        if dry_result.returncode != 0:
            print("r2-deploy: dry-run failed", file=sys.stderr)
            if dry_log:
                print("r2-deploy: dry-run output:", file=sys.stderr)
                print(dry_log, file=sys.stderr)
            sys.exit(1)

        deletes = extract_dry_run_deletes(dry_log)
        if deletes:
            print(
                f"r2-deploy: WARNING — sync would DELETE remote files/dirs "
                f"not present in {local_dir}:",
                file=sys.stderr,
            )
            for path in deletes:
                print(f"  {path}", file=sys.stderr)
            if not allow_deletes:
                print(
                    "r2-deploy: aborted — re-run with --allow-deletes to proceed",
                    file=sys.stderr,
                )
                sys.exit(1)
            print("r2-deploy: --allow-deletes set, continuing with sync", file=sys.stderr)

        rclone_run(
            [
                "sync",
                str(local_dir),
                remote,
                "--progress",
                "--retries",
                "5",
                "--retries-sleep",
                "5s",
                "--low-level-retries",
                "10",
                "--stats-one-line",
                "--stats",
                "5s",
            ],
            check=True,
        )
    finally:
        if dry_log_path and dry_log_path.exists():
            dry_log_path.unlink()


def main(argv: list[str] | None = None) -> None:
    args = parse_args(argv)
    local_dir = args.local_dir

    if not local_dir.is_dir():
        print(f"r2-deploy: local directory not found: {local_dir}", file=sys.stderr)
        sys.exit(1)

    mode = "copy" if args.copy else "sync"

    with R2Session(use_keychain=True) as session:
        bucket = session.bucket
        remote = remote_url(bucket, args.remote_prefix)
        prefix_display = args.remote_prefix or ""
        print(
            f"r2-deploy: {local_dir} -> r2://{bucket}/{prefix_display} ({mode})",
            file=sys.stderr,
        )

        if args.copy:
            run_copy(local_dir, remote)
        else:
            run_sync(local_dir, remote, args.allow_deletes)


if __name__ == "__main__":
    main()
