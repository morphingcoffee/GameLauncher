"""Unit tests for r2_env_check format validation (no rclone/R2 required)."""

from __future__ import annotations

import os
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

TESTS_DIR = Path(__file__).resolve().parent
DEPLOY_DIR = TESTS_DIR.parent
sys.path.insert(0, str(DEPLOY_DIR))

from r2_env_check import (  # noqa: E402
    Checker,
    probe_key,
    run_format_checks,
    validate_account_id,
    validate_bucket_name,
    validate_cdn_url,
)


class TestFormatValidation(unittest.TestCase):
    def test_account_id_valid(self) -> None:
        checker = Checker()
        self.assertTrue(
            validate_account_id(checker, "0123456789abcdef0123456789abcdef")
        )

    def test_account_id_invalid(self) -> None:
        checker = Checker()
        self.assertFalse(validate_account_id(checker, "not-a-valid-account-id"))
        self.assertFalse(checker.passed)

    def test_bucket_name_valid(self) -> None:
        checker = Checker()
        self.assertTrue(validate_bucket_name(checker, "gamelauncher-dev"))

    def test_bucket_name_invalid(self) -> None:
        checker = Checker()
        self.assertFalse(validate_bucket_name(checker, "Bad_Bucket"))
        self.assertFalse(checker.passed)

    def test_cdn_url_valid(self) -> None:
        checker = Checker()
        self.assertTrue(validate_cdn_url(checker, "https://cdn.example.com"))

    def test_cdn_url_rejects_trailing_slash(self) -> None:
        checker = Checker()
        self.assertFalse(validate_cdn_url(checker, "https://cdn.example.com/"))
        self.assertFalse(checker.passed)

    def test_probe_key_is_unique(self) -> None:
        self.assertNotEqual(probe_key(), probe_key())
        self.assertIn("env-check-", probe_key())


class TestFormatChecks(unittest.TestCase):
    @patch.dict(
        os.environ,
        {
            "R2_ACCOUNT_ID": "0123456789abcdef0123456789abcdef",
            "R2_BUCKET_NAME": "gamelauncher-dev",
            "R2_PUBLIC_CDN_BASE_URL": "https://cdn.example.com",
            "R2_ACCESS_KEY_ID": "a" * 32,
            "R2_SECRET_ACCESS_KEY": "b" * 40,
        },
        clear=True,
    )
    def test_format_checks_pass(self) -> None:
        checker = Checker()
        self.assertTrue(run_format_checks(checker))
        self.assertTrue(checker.passed)


if __name__ == "__main__":
    unittest.main()
