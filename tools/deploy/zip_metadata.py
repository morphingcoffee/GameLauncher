"""Derive build metadata from a local game.zip staging file."""

from __future__ import annotations

import hashlib
import sys
import zipfile
from pathlib import Path
from typing import Any, Dict


def die(message: str) -> None:
    print(f"zip-metadata: {message}", file=sys.stderr)
    sys.exit(1)


def zip_build_metadata(zip_path: Path) -> Dict[str, Any]:
    """Return file_size_bytes, uncompressed_size_bytes, and sha256 for a zip file."""
    if not zip_path.is_file():
        die(f"zip not found: {zip_path}")

    file_size_bytes = zip_path.stat().st_size
    digest = hashlib.sha256()
    uncompressed_size_bytes = 0

    with zip_path.open("rb") as handle:
        while True:
            chunk = handle.read(1024 * 1024)
            if not chunk:
                break
            digest.update(chunk)

    with zipfile.ZipFile(zip_path) as archive:
        for info in archive.infolist():
            uncompressed_size_bytes += info.file_size

    return {
        "file_size_bytes": file_size_bytes,
        "uncompressed_size_bytes": uncompressed_size_bytes,
        "sha256": digest.hexdigest(),
    }
