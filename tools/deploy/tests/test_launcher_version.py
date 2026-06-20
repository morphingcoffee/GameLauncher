"""Unit tests for launcher_version validation."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
DEPLOY_DIR = TESTS_DIR.parent
sys.path.insert(0, str(DEPLOY_DIR))

from launcher_version import (  # noqa: E402
    marketing_version,
    parse_launcher_version,
    validate_launcher_artifact_version,
    validate_launcher_marketing_version,
)


class TestParseLauncherVersion(unittest.TestCase):
    def test_full_artifact_version(self) -> None:
        parsed = parse_launcher_version("0.0.1-build51")
        self.assertEqual((parsed.major, parsed.minor, parsed.patch, parsed.build_number), (0, 0, 1, 51))

    def test_marketing_only(self) -> None:
        parsed = parse_launcher_version("0.0.1")
        self.assertEqual(parsed.build_number, 0)

    def test_dev_suffix(self) -> None:
        parsed = parse_launcher_version("0.0.1-dev")
        self.assertEqual(parsed.build_number, 0)

    def test_rejects_short_form(self) -> None:
        with self.assertRaises(ValueError):
            parse_launcher_version("1.0")

    def test_rejects_single_segment(self) -> None:
        with self.assertRaises(ValueError):
            parse_launcher_version("1")

    def test_rejects_invalid_build_suffix(self) -> None:
        with self.assertRaises(ValueError):
            parse_launcher_version("0.0.1-buildx")

    def test_rejects_blank(self) -> None:
        with self.assertRaises(ValueError):
            parse_launcher_version("   ")


class TestValidateLauncherVersion(unittest.TestCase):
    def test_marketing_accepts_minimum(self) -> None:
        validate_launcher_marketing_version("0.0.1")

    def test_marketing_rejects_malformed(self) -> None:
        with self.assertRaises(ValueError):
            validate_launcher_marketing_version("1.0")

    def test_artifact_requires_build_suffix(self) -> None:
        validate_launcher_artifact_version("0.0.1-build1")

    def test_artifact_rejects_marketing_only(self) -> None:
        with self.assertRaises(ValueError):
            validate_launcher_artifact_version("0.0.1")

    def test_marketing_version_strips_build(self) -> None:
        self.assertEqual(marketing_version("0.0.2-build99"), "0.0.2")


if __name__ == "__main__":
    unittest.main()
