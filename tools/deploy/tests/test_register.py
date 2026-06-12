"""Unit tests for register_game_version manifest logic (no rclone/R2)."""

from __future__ import annotations

import json
import subprocess
import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
DEPLOY_DIR = TESTS_DIR.parent
sys.path.insert(0, str(DEPLOY_DIR))

from register_game_version import (  # noqa: E402
    append_version_entry,
    enrich_builds_with_urls,
    find_game_index,
    is_object_not_found,
    new_versions_index,
    parse_platform_list,
    update_catalog_manifest,
    update_game_in_manifest,
    validate_platforms,
)

FIXTURES = TESTS_DIR / "fixtures"
CDN = "https://cdn.example.com"


class TestEnrichBuilds(unittest.TestCase):
    def test_adds_download_urls(self) -> None:
        builds = {
            "macos-arm64": {
                "executable_path": "Game.app/Contents/MacOS/Game",
                "file_size_bytes": 12345,
                "sha256": "deadbeef",
            }
        }
        result = enrich_builds_with_urls(builds, "cool_game", "1.2.0", CDN)
        self.assertEqual(
            result["macos-arm64"]["download_url"],
            "https://cdn.example.com/games/cool_game/v1.2.0/macos-arm64/game.zip",
        )
        self.assertEqual(result["macos-arm64"]["sha256"], "deadbeef")


class TestPlatformValidation(unittest.TestCase):
    def test_parse_platform_list(self) -> None:
        self.assertEqual(parse_platform_list("macos-arm64, windows-x64"), ["macos-arm64", "windows-x64"])

    def test_validate_platforms_ok(self) -> None:
        builds = {"macos-arm64": {"sha256": "x"}}
        validate_platforms(["macos-arm64"], builds)

    def test_validate_platforms_unknown(self) -> None:
        builds = {"linux-x64": {"sha256": "x"}}
        with self.assertRaises(SystemExit):
            validate_platforms(["linux-x64"], builds)


class TestVersionsIndex(unittest.TestCase):
    def test_is_object_not_found(self) -> None:
        missing = subprocess.CompletedProcess(
            args=[],
            returncode=1,
            stderr="ERROR : Object not found",
        )
        self.assertTrue(is_object_not_found(missing))

        auth_error = subprocess.CompletedProcess(
            args=[],
            returncode=1,
            stderr="ERROR : Access Denied",
        )
        self.assertFalse(is_object_not_found(auth_error))

    def test_append_version_entry(self) -> None:
        data = new_versions_index("cool_game")
        builds = {"macos-arm64": {"sha256": "x", "download_url": "https://cdn.example.com/x"}}
        updated = append_version_entry(data, "1.2.0", "2025-06-01", builds)
        self.assertEqual(len(updated["versions"]), 1)
        self.assertEqual(updated["versions"][0]["version"], "1.2.0")

    def test_duplicate_version_exits(self) -> None:
        data = new_versions_index("cool_game")
        builds = {"macos-arm64": {"sha256": "x"}}
        data = append_version_entry(data, "1.0.0", "2025-01-01", builds)
        with self.assertRaises(SystemExit):
            append_version_entry(data, "1.0.0", "2025-01-02", builds)


class TestManifestUpdates(unittest.TestCase):
    def _load_manifest(self) -> dict:
        return json.loads((FIXTURES / "manifest.json").read_text())

    def test_find_game_index(self) -> None:
        manifest = self._load_manifest()
        self.assertEqual(find_game_index(manifest, "cool_game"), 0)
        self.assertIsNone(find_game_index(manifest, "missing"))

    def test_add_new_game(self) -> None:
        manifest = json.loads((FIXTURES / "manifest.json").read_text())
        manifest["games"] = []
        builds = enrich_builds_with_urls(
            {"windows-x64": {"executable_path": "Game.exe", "file_size_bytes": 1, "sha256": "x"}},
            "new_game",
            "1.0.0",
            CDN,
        )
        updated = update_catalog_manifest(
            manifest,
            "new_game",
            "1.0.0",
            f"{CDN}/games/new_game/versions.json",
            builds,
            title="New Game",
            description="Desc",
            thumbnail_url=f"{CDN}/assets/new_game/thumbnail.webp",
        )
        self.assertEqual(len(updated["games"]), 1)
        self.assertEqual(updated["games"][0]["id"], "new_game")

    def test_update_existing_game_merges_builds(self) -> None:
        manifest = self._load_manifest()
        new_builds = enrich_builds_with_urls(
            {
                "windows-x64": {
                    "executable_path": "Game.exe",
                    "file_size_bytes": 2000,
                    "sha256": "win",
                }
            },
            "cool_game",
            "1.2.0",
            CDN,
        )
        updated = update_catalog_manifest(
            manifest,
            "cool_game",
            "1.2.0",
            f"{CDN}/games/cool_game/versions.json",
            new_builds,
        )
        game = updated["games"][0]
        self.assertEqual(game["latest_version"], "1.2.0")
        self.assertIn("macos-arm64", game["builds"])
        self.assertIn("windows-x64", game["builds"])

    def test_update_existing_game_thumbnail(self) -> None:
        manifest = self._load_manifest()
        builds = enrich_builds_with_urls(
            {"macos-arm64": {"executable_path": "x", "file_size_bytes": 1, "sha256": "y"}},
            "cool_game",
            "1.1.0",
            CDN,
        )
        updated = update_game_in_manifest(
            manifest,
            "cool_game",
            "1.1.0",
            f"{CDN}/games/cool_game/versions.json",
            builds,
            thumbnail_url="https://cdn.example.com/assets/cool_game/new-thumb.webp",
        )
        self.assertEqual(
            updated["games"][0]["thumbnail_url"],
            "https://cdn.example.com/assets/cool_game/new-thumb.webp",
        )


if __name__ == "__main__":
    unittest.main()
