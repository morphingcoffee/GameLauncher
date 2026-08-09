"""Unit tests for r2_catalog_check helpers (no R2)."""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

TESTS_DIR = Path(__file__).resolve().parent
DEPLOY_DIR = TESTS_DIR.parent
sys.path.insert(0, str(DEPLOY_DIR))

from r2_catalog_check import (  # noqa: E402
    WEB_PLATFORM,
    CatalogChecker,
    cdn_path_from_url,
    check_game_offline,
    check_launcher_versions,
    main,
    run_offline_check,
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


class TestOfflineCatalogCheck(unittest.TestCase):
    def _write_catalog(self, root: Path, *, include_hidden: bool = True) -> Path:
        manifests = root / "manifests"
        games_dir = manifests / "games"
        listed = games_dir / "listed_game"
        listed.mkdir(parents=True)
        build = {
            "download_url": "https://cdn.example.com/games/listed_game/v1.0.0/windows-x64/game.zip",
            "executable_path": "Game.exe",
            "file_size_bytes": 1000,
            "uncompressed_size_bytes": 2000,
            "sha256": "a" * 64,
        }
        (listed / "versions.json").write_text(
            json.dumps(
                {
                    "game_id": "listed_game",
                    "versions": [
                        {
                            "version": "1.0.0",
                            "released_at": "2026-01-01T00:00:00Z",
                            "builds": {"windows-x64": build},
                        }
                    ],
                }
            )
        )
        if include_hidden:
            hidden = games_dir / "hidden_game"
            hidden.mkdir(parents=True)
            (hidden / "versions.json").write_text(
                json.dumps(
                    {
                        "game_id": "hidden_game",
                        "versions": [
                            {
                                "version": "9.9.9",
                                "released_at": "2026-01-01T00:00:00Z",
                                "builds": {"windows-x64": build},
                            }
                        ],
                    }
                )
            )
        manifest_path = manifests / "manifest.json"
        manifest_path.write_text(
            json.dumps(
                {
                    "launcher_minimum_version": "0.0.1",
                    "games": [
                        {
                            "id": "listed_game",
                            "title": "Listed",
                            "description": "Listed game",
                            "thumbnail_url": "https://cdn.example.com/assets/listed_game/t.webp",
                            "latest_version": "1.0.0",
                            "versions_url": "https://cdn.example.com/games/listed_game/versions.json",
                            "builds": {"windows-x64": build},
                        }
                    ],
                }
            )
        )
        return manifest_path

    def test_offline_passes_for_referenced_indexes_only(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            manifest_path = self._write_catalog(root)
            checker = CatalogChecker()
            code = run_offline_check(
                checker,
                root,
                manifest_path=manifest_path,
                game_filter=None,
            )
            self.assertEqual(code, 0)
            self.assertEqual(checker.errors, 0)
            scopes = {result.scope for result in checker.results}
            self.assertIn("game:listed_game", scopes)
            self.assertNotIn("game:hidden_game", scopes)

    def test_offline_fails_when_latest_missing_from_versions(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            manifest_path = self._write_catalog(root, include_hidden=False)
            versions_path = root / "manifests" / "games" / "listed_game" / "versions.json"
            data = json.loads(versions_path.read_text())
            data["versions"][0]["version"] = "0.0.1"
            versions_path.write_text(json.dumps(data))
            checker = CatalogChecker()
            code = run_offline_check(
                checker,
                root,
                manifest_path=manifest_path,
                game_filter=None,
            )
            self.assertEqual(code, 1)
            self.assertGreater(checker.errors, 0)

    def test_offline_main_never_opens_r2_session(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            manifest_path = self._write_catalog(root)
            with mock.patch("r2_catalog_check.find_repo_root", return_value=root), mock.patch(
                "r2_catalog_check.R2Session"
            ) as session_cls, mock.patch("r2_catalog_check.load_env_file") as load_env:
                code = main(["--offline", "--manifest", str(manifest_path)])
            self.assertEqual(code, 0)
            session_cls.assert_not_called()
            load_env.assert_not_called()

    def test_check_game_offline_requires_versions_index(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            checker = CatalogChecker()
            check_game_offline(
                checker,
                {
                    "id": "missing_game",
                    "title": "Missing",
                    "latest_version": "1.0.0",
                    "versions_url": "https://cdn.example.com/games/missing_game/versions.json",
                    "builds": {},
                },
                root,
            )
            self.assertGreater(checker.errors, 0)


if __name__ == "__main__":
    unittest.main()
