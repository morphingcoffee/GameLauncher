"""Unit tests for r2_catalog_check helpers (no R2)."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
DEPLOY_DIR = TESTS_DIR.parent
sys.path.insert(0, str(DEPLOY_DIR))

from r2_catalog_check import (  # noqa: E402
    CatalogChecker,
    cdn_path_from_url,
    validate_build_metadata,
)


class TestCdnPathFromUrl(unittest.TestCase):
    def test_strips_cdn_base(self) -> None:
        url = "https://cdn.example.com/games/foo/v1.0.0/windows-x64/game.zip"
        self.assertEqual(
            cdn_path_from_url("https://cdn.example.com", url),
            "games/foo/v1.0.0/windows-x64/game.zip",
        )

    def test_mismatch_returns_none(self) -> None:
        self.assertIsNone(
            cdn_path_from_url(
                "https://cdn.example.com",
                "https://other.example.com/games/foo/game.zip",
            )
        )


class TestValidateBuildMetadata(unittest.TestCase):
    def test_valid_build_passes(self) -> None:
        checker = CatalogChecker()
        validate_build_metadata(
            checker,
            "v1.0.0",
            "windows-x64",
            {
                "download_url": "https://cdn.example.com/games/x/v1.0.0/windows-x64/game.zip",
                "executable_path": "Game.exe",
                "file_size_bytes": 1000,
                "uncompressed_size_bytes": 2000,
                "sha256": "a" * 64,
            },
        )
        self.assertEqual(checker.errors, 0)

    def test_missing_sha256_fails(self) -> None:
        checker = CatalogChecker()
        validate_build_metadata(
            checker,
            "v1.0.0",
            "windows-x64",
            {
                "download_url": "https://cdn.example.com/x.zip",
                "executable_path": "Game.exe",
                "file_size_bytes": 1000,
                "sha256": "too-short",
            },
        )
        self.assertGreater(checker.errors, 0)

    def test_missing_uncompressed_size_fails(self) -> None:
        checker = CatalogChecker()
        validate_build_metadata(
            checker,
            "v1.0.0",
            "windows-x64",
            {
                "download_url": "https://cdn.example.com/x.zip",
                "executable_path": "Game.exe",
                "file_size_bytes": 1000,
                "sha256": "a" * 64,
            },
        )
        self.assertGreater(checker.errors, 0)


if __name__ == "__main__":
    unittest.main()
