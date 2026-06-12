"""Unit tests for shared rclone flag selection."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
DEPLOY_DIR = TESTS_DIR.parent
sys.path.insert(0, str(DEPLOY_DIR))

from r2_config import rclone_flags_for  # noqa: E402


class TestRcloneFlagsFor(unittest.TestCase):
    def test_copy_includes_ds_store_exclude(self) -> None:
        flags = rclone_flags_for(["copy", "/tmp/local", "remote:bucket/prefix"])
        self.assertIn("--s3-no-check-bucket", flags)
        self.assertIn("--exclude", flags)
        self.assertIn("**/.DS_Store", flags)

    def test_copyto_omits_filters(self) -> None:
        flags = rclone_flags_for(
            ["copyto", "remote:bucket/games/foo/versions.json", "/tmp/versions.json"]
        )
        self.assertEqual(flags, ["--s3-no-check-bucket"])
        self.assertNotIn("--exclude", flags)

    def test_lsf_omits_filters(self) -> None:
        flags = rclone_flags_for(["lsf", "remote:bucket/assets/", "--max-depth", "1"])
        self.assertEqual(flags, ["--s3-no-check-bucket"])


if __name__ == "__main__":
    unittest.main()
