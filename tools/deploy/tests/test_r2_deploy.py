"""Unit tests for r2_deploy argument parsing and dry-run logic."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
DEPLOY_DIR = TESTS_DIR.parent
sys.path.insert(0, str(DEPLOY_DIR))

from r2_deploy import extract_dry_run_deletes, parse_args  # noqa: E402


class TestParseArgs(unittest.TestCase):
    def test_copy_mode(self) -> None:
        args = parse_args(["--copy", "/tmp/build", "games/foo/v1/macos-arm64"])
        self.assertTrue(args.copy)
        self.assertFalse(args.allow_deletes)
        self.assertEqual(args.local_dir, Path("/tmp/build"))
        self.assertEqual(args.remote_prefix, "games/foo/v1/macos-arm64")

    def test_sync_with_allow_deletes(self) -> None:
        args = parse_args(["--allow-deletes", "/tmp/dist", "releases/v1"])
        self.assertFalse(args.copy)
        self.assertTrue(args.allow_deletes)
        self.assertEqual(args.remote_prefix, "releases/v1")

    def test_local_dir_only(self) -> None:
        args = parse_args(["/tmp/dist"])
        self.assertEqual(args.remote_prefix, "")


class TestDryRunDeletes(unittest.TestCase):
    def test_extract_paths(self) -> None:
        log = (
            "NOTICE: games/foo/old.zip: Skipped delete as --dry-run is set\n"
            "NOTICE: games/foo/dir: Skipped remove directory as --dry-run is set\n"
            "other line\n"
        )
        paths = extract_dry_run_deletes(log)
        self.assertEqual(paths, ["games/foo/old.zip", "games/foo/dir"])

    def test_no_deletes(self) -> None:
        self.assertEqual(extract_dry_run_deletes("uploading file\n"), [])


if __name__ == "__main__":
    unittest.main()
