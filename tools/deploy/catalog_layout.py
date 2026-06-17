"""Repository layout for catalog JSON vs local R2 upload staging."""

from __future__ import annotations

from pathlib import Path

# Gitignored local upload tree — mirrors R2 object key prefixes (games/, assets/).
R2_STAGING_DIR_NAME = "r2_staging"

# Git-tracked version history and release overrides.
GIT_GAMES_DIR = Path("manifests") / "games"


def repo_r2_staging_dir(repo_root: Path) -> Path:
    return repo_root / R2_STAGING_DIR_NAME


def r2_staging_zip_path(repo_root: Path, game_id: str, version: str, platform: str) -> Path:
    return (
        repo_r2_staging_dir(repo_root)
        / "games"
        / game_id
        / f"v{version}"
        / platform
        / "game.zip"
    )


def r2_staging_assets_dir(repo_root: Path, game_id: str) -> Path:
    return repo_r2_staging_dir(repo_root) / "assets" / game_id


def catalog_manifest_path(repo_root: Path) -> Path:
    return repo_root / "manifests" / "manifest.json"


def git_game_dir(repo_root: Path, game_id: str) -> Path:
    return repo_root / GIT_GAMES_DIR / game_id


def git_versions_path(repo_root: Path, game_id: str) -> Path:
    return git_game_dir(repo_root, game_id) / "versions.json"


def git_release_path(repo_root: Path, game_id: str, version: str) -> Path:
    return git_game_dir(repo_root, game_id) / "releases" / f"{version}.json"


def r2_zip_object_key(game_id: str, version: str, platform: str) -> str:
    return f"games/{game_id}/v{version}/{platform}/game.zip"


def r2_versions_object_key(game_id: str) -> str:
    return f"games/{game_id}/versions.json"


def r2_assets_prefix(game_id: str) -> str:
    return f"assets/{game_id}"
