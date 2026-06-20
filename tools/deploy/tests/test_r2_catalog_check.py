"""Unit tests for r2_catalog_check helpers (no R2)."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
DEPLOY_DIR = TESTS_DIR.parent
sys.path.insert(0, str(DEPLOY_DIR))

from r2_catalog_check import (  # noqa: E402
    WEB_PLATFORM,
    CatalogChecker,
    cdn_path_from_url,
    check_launcher_versions,
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

    def test_web_build_accepts_zero_size_and_empty_sha(self) -> None:
        checker = CatalogChecker()
        validate_build_metadata(
            checker,
            "game:game_gallery v1.0.0",
            WEB_PLATFORM,
            {
                "download_url": "https://morphingcoffee.github.io/apps/games/",
                "executable_path": "",
                "file_size_bytes": 0,
                "sha256": "",
            },
        )
        self.assertEqual(checker.errors, 0)

    def test_web_build_rejects_missing_download_url(self) -> None:
        checker = CatalogChecker()
        validate_build_metadata(
            checker,
            "game:game_gallery v1.0.0",
            WEB_PLATFORM,
            {
                "download_url": "",
                "executable_path": "",
                "file_size_bytes": 0,
                "sha256": "",
            },
        )
        self.assertEqual(checker.errors, 1)


class TestLauncherVersionChecks(unittest.TestCase):
    def test_valid_launcher_versions_pass(self) -> None:
        checker = CatalogChecker()
        check_launcher_versions(
            checker,
            {
                "launcher_minimum_version": "0.0.1",
                "launcher": {
                    "channels": {
                        "windows-x64-msi": {"version": "0.0.1-build51"},
                    },
                },
            },
        )
        self.assertEqual(checker.errors, 0)

    def test_invalid_minimum_version_fails(self) -> None:
        checker = CatalogChecker()
        check_launcher_versions(checker, {"launcher_minimum_version": "1.0"})
        self.assertGreater(checker.errors, 0)

    def test_channel_version_without_build_suffix_fails(self) -> None:
        checker = CatalogChecker()
        check_launcher_versions(
            checker,
            {
                "launcher_minimum_version": "0.0.1",
                "launcher": {
                    "channels": {
                        "windows-x64-msi": {"version": "0.0.1"},
                    },
                },
            },
        )
        self.assertGreater(checker.errors, 0)


if __name__ == "__main__":
    unittest.main()
