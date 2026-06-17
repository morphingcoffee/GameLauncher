"""Tests for upsert and catalog merge helpers."""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
DEPLOY_DIR = TESTS_DIR.parent
sys.path.insert(0, str(DEPLOY_DIR))

from register_game_version import (  # noqa: E402
    merge_catalog_builds_into_versions,
    merge_platform_builds,
    new_versions_index,
    upsert_version_entry,
)
from zip_metadata import zip_build_metadata  # noqa: E402


class TestUpsertVersionEntry(unittest.TestCase):
    def test_inserts_when_missing(self) -> None:
        data = new_versions_index("cool_game")
        builds = {"windows-x64": {"sha256": "abc", "file_size_bytes": 100}}
        updated = upsert_version_entry(data, "1.0.0", "2026-01-01", builds)
        self.assertEqual(updated["versions"][0]["version"], "1.0.0")
        self.assertEqual(updated["versions"][0]["builds"]["windows-x64"]["sha256"], "abc")

    def test_merges_existing_platform_builds(self) -> None:
        data = new_versions_index("cool_game")
        initial = {"windows-x64": {"sha256": "old", "file_size_bytes": 100}}
        data = upsert_version_entry(data, "1.0.0", "2026-01-01", initial)
        patch = {
            "windows-x64": {
                "uncompressed_size_bytes": 250,
            }
        }
        updated = upsert_version_entry(data, "1.0.0", "2026-01-02", patch)
        build = updated["versions"][0]["builds"]["windows-x64"]
        self.assertEqual(build["sha256"], "old")
        self.assertEqual(build["uncompressed_size_bytes"], 250)
        self.assertEqual(updated["versions"][0]["released_at"], "2026-01-02")


class TestMergeCatalogBuilds(unittest.TestCase):
    def test_merges_latest_version_builds_from_catalog(self) -> None:
        manifest = {
            "games": [
                {
                    "id": "cool_game",
                    "latest_version": "1.0.0",
                    "builds": {
                        "windows-x64": {
                            "file_size_bytes": 100,
                            "uncompressed_size_bytes": 250,
                            "sha256": "new",
                        }
                    },
                }
            ]
        }
        versions = new_versions_index("cool_game")
        versions = upsert_version_entry(
            versions,
            "1.0.0",
            "2026-01-01",
            {"windows-x64": {"sha256": "old", "file_size_bytes": 99}},
        )
        merged = merge_catalog_builds_into_versions(manifest, "cool_game", versions)
        build = merged["versions"][0]["builds"]["windows-x64"]
        self.assertEqual(build["sha256"], "new")
        self.assertEqual(build["uncompressed_size_bytes"], 250)


class TestMergePlatformBuilds(unittest.TestCase):
    def test_incoming_overwrites_conflicts(self) -> None:
        merged = merge_platform_builds(
            {"windows-x64": {"sha256": "a", "file_size_bytes": 1}},
            {"windows-x64": {"uncompressed_size_bytes": 9}},
        )
        self.assertEqual(merged["windows-x64"]["sha256"], "a")
        self.assertEqual(merged["windows-x64"]["uncompressed_size_bytes"], 9)


class TestZipMetadata(unittest.TestCase):
    def test_reads_zip_sizes_and_hash(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            zip_path = Path(tmpdir) / "game.zip"
            with zipfile.ZipFile(zip_path, "w") as archive:
                archive.writestr("hello.txt", "hello world")
            meta = zip_build_metadata(zip_path)
            self.assertGreater(meta["file_size_bytes"], 0)
            self.assertEqual(meta["uncompressed_size_bytes"], len("hello world"))
            self.assertEqual(len(meta["sha256"]), 64)


if __name__ == "__main__":
    unittest.main()
