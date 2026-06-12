#!/usr/bin/env python3
"""Verify R2 Keychain credentials (read + write) without affecting your shell."""

from __future__ import annotations

import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from r2_config import R2Session, remote_url, rclone_run


def fail_read() -> None:
    print("r2-test-auth: read failed.", file=sys.stderr)
    print(
        "  Check .env (R2_ACCOUNT_ID, R2_BUCKET_NAME), token bucket scope, and Keychain S3 keys.",
        file=sys.stderr,
    )
    print(
        "  SignatureDoesNotMatch → recreate token, update both Keychain items (no stray whitespace).",
        file=sys.stderr,
    )
    sys.exit(1)


def fail_write() -> None:
    print("r2-test-auth: write failed (PutObject denied).", file=sys.stderr)
    print(
        "  Token is likely read-only — recreate with Object Read & Write, "
        "then update both Keychain items.",
        file=sys.stderr,
    )
    sys.exit(1)


def main() -> None:
    probe_file: Path | None = None
    probe_key: str | None = None

    with R2Session(use_keychain=True) as session:
        bucket = session.bucket
        remote = remote_url(bucket)

        print(f"r2-test-auth: read — r2://{bucket}/", file=sys.stderr)
        lsf = subprocess.Popen(
            ["rclone", "lsf", remote, "--s3-no-check-bucket", "--max-depth", "1"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        stdout, _ = lsf.communicate()
        lsf_status = lsf.returncode
        # head closes the pipe after 20 lines; rclone may exit 141 (SIGPIPE)
        if lsf_status not in (0, 141):
            fail_read()
        lines = stdout.decode().splitlines()[:20]
        if lines:
            print("\n".join(lines), file=sys.stderr)

        probe_fd, probe_path = tempfile.mkstemp(suffix=".txt")
        os.close(probe_fd)
        probe_file = Path(probe_path)
        probe_key = f".gamelauncher-r2-probe/auth-{int(time.time())}.txt"
        probe_file.write_text("probe")

        print(f"r2-test-auth: write — {probe_key}", file=sys.stderr)
        write_result = rclone_run(
            ["copyto", str(probe_file), f"{remote}/{probe_key}"],
            capture_output=True,
        )
        if write_result.returncode != 0:
            fail_write()

        try:
            rclone_run(
                ["deletefile", f"{remote}/{probe_key}"],
                capture_output=True,
            )
        except Exception:
            pass

    if probe_file and probe_file.exists():
        probe_file.unlink()

    print("r2-test-auth: OK", file=sys.stderr)


if __name__ == "__main__":
    main()
