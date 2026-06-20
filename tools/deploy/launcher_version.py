"""Launcher version parsing — mirrors :core:model VersionComparator contract."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional


@dataclass(frozen=True)
class ParsedLauncherVersion:
    major: int
    minor: int
    patch: int
    build_number: int


def parse_launcher_version(raw: str) -> ParsedLauncherVersion:
    """Parse major.minor.patch with optional -buildN suffix (missing build = 0)."""
    trimmed = raw.strip()
    if not trimmed:
        raise ValueError("version must not be blank")

    without_dev = trimmed.removesuffix("-dev")
    build_index = without_dev.find("-build")
    if build_index >= 0:
        marketing_part = without_dev[:build_index]
        build_suffix = without_dev[build_index + len("-build") :]
        build_number_text: Optional[str] = build_suffix
    else:
        marketing_part = without_dev
        build_number_text = None

    segments = marketing_part.split(".")
    if len(segments) != 3:
        raise ValueError(f"expected major.minor.patch version: {raw!r}")

    try:
        major = int(segments[0])
        minor = int(segments[1])
        patch = int(segments[2])
    except ValueError as error:
        raise ValueError(f"invalid numeric segment in version: {raw!r}") from error

    if build_number_text is None:
        build_number = 0
    else:
        if not build_number_text.isdigit():
            raise ValueError(f"invalid build suffix in version: {raw!r}")
        build_number = int(build_number_text)

    return ParsedLauncherVersion(
        major=major,
        minor=minor,
        patch=patch,
        build_number=build_number,
    )


def validate_launcher_marketing_version(raw: str) -> None:
    """Validate launcher_minimum_version (major.minor.patch; build suffix optional)."""
    parse_launcher_version(raw)


def validate_launcher_artifact_version(raw: str) -> None:
    """Validate staged artifact / channel version (major.minor.patch-buildN required)."""
    parsed = parse_launcher_version(raw)
    if parsed.build_number == 0 and "-build" not in raw.strip().removesuffix("-dev"):
        raise ValueError(f"artifact version must include -buildN suffix: {raw!r}")


def marketing_version(artifact_version: str) -> str:
    """Return major.minor.patch prefix from a full artifact version label."""
    validate_launcher_artifact_version(artifact_version)
    without_dev = artifact_version.strip().removesuffix("-dev")
    build_index = without_dev.find("-build")
    if build_index >= 0:
        return without_dev[:build_index]
    return without_dev
