#!/usr/bin/env python3
"""Generate WiX installer banner/dialog BMPs for Game Launcher.

WiX UI stretches these bitmaps across the full control width.

Banner (493×58): jpackage WixUI places the step title at x≈15 and description at
x≈25 (width≈280) on the bitmap — keep x=0..310 a clean light panel. Industrial
art lives on the far right only.

Dialog (493×312): left industrial column + right light panel for welcome copy.

Writes to sibling `jpackage/` (launcher palette — see LauncherColors.kt):
  installer-banner.bmp  493×58
  installer-dialog.bmp  493×312
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

# Launcher industrial palette
BG = (13, 17, 23)  # #0D1117
SURFACE = (22, 27, 34)  # #161B22
PANEL = (31, 41, 51)  # #1F2933
ACCENT = (77, 217, 255)  # #4DD9FF
PRIMARY = (108, 158, 255)  # #6C9EFF
ON_TEXT = (230, 237, 243)  # #E6EDF3
LIGHT_TOP = (250, 251, 253)  # #FAFBFD
LIGHT_BOTTOM = (241, 245, 249)  # #F1F5F9

BANNER_W, BANNER_H = 493, 58
DIALOG_W, DIALOG_H = 493, 312

# WixUI title (~x=15, w≈200) + description (~x=25, w≈280) must sit on light pixels.
BANNER_LIGHT_W = 312
BANNER_ART_X = BANNER_LIGHT_W
DIALOG_ART_W = 178

# Monospace candidates for PIL (generator may run on macOS, Linux, or Windows).
_MONO_FONT_CANDIDATES: tuple[tuple[str, int | None], ...] = (
    ("/System/Library/Fonts/Menlo.ttc", 0),
    ("/System/Library/Fonts/Supplemental/Courier New.ttf", None),
    ("/Library/Fonts/Courier New.ttf", None),
    ("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf", None),
    ("/usr/share/fonts/TTF/DejaVuSansMono.ttf", None),
    ("C:\\Windows\\Fonts\\consola.ttf", None),
    ("C:\\Windows\\Fonts\\lucon.ttf", None),
)

DIALOG_TITLE_LINES: tuple[tuple[str, tuple[int, int, int]], ...] = (
    ("MC", ACCENT),
    (".GAME", ON_TEXT),
    (".LAUNCHER", ON_TEXT),
)


def _lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def _lerp_rgb(c1: tuple[int, int, int], c2: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return (
        int(_lerp(c1[0], c2[0], t)),
        int(_lerp(c1[1], c2[1], t)),
        int(_lerp(c1[2], c2[2], t)),
    )


def _fill_vertical_gradient(
    pixels: list[list[tuple[int, int, int]]],
    x0: int,
    x1: int,
    y0: int,
    y1: int,
    top: tuple[int, int, int],
    bottom: tuple[int, int, int],
) -> None:
    height = y1 - y0
    for y in range(y0, y1):
        t = (y - y0) / max(height - 1, 1)
        color = _lerp_rgb(top, bottom, t)
        for x in range(x0, x1):
            pixels[y][x] = color


def _fill_radial_glow(
    pixels: list[list[tuple[int, int, int]]],
    cx: float,
    cy: float,
    radius: float,
    color: tuple[int, int, int],
    peak_alpha: float,
    x0: int,
    x1: int,
    y0: int,
    y1: int,
) -> None:
    for y in range(y0, y1):
        for x in range(x0, x1):
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx * dx + dy * dy)
            t = max(0.0, 1.0 - dist / radius)
            alpha = peak_alpha * (t * t)
            base = pixels[y][x]
            pixels[y][x] = _lerp_rgb(base, color, alpha)


def _draw_scan_grid(
    pixels: list[list[tuple[int, int, int]]],
    x0: int,
    x1: int,
    y0: int,
    y1: int,
    spacing: int = 6,
    strength: float = 0.07,
) -> None:
    for y in range(y0, y1):
        if y % spacing != 0:
            continue
        for x in range(x0, x1):
            base = pixels[y][x]
            pixels[y][x] = _lerp_rgb(base, PANEL, strength)
    for x in range(x0, x1):
        if x % spacing != 0:
            continue
        for y in range(y0, y1):
            base = pixels[y][x]
            pixels[y][x] = _lerp_rgb(base, PANEL, strength * 0.65)


def _draw_horizontal_rule(
    pixels: list[list[tuple[int, int, int]]],
    y: int,
    x0: int,
    x1: int,
    color: tuple[int, int, int],
    strength: float = 0.85,
) -> None:
    if y < 0 or y >= len(pixels):
        return
    for x in range(x0, min(x1, len(pixels[0]))):
        pixels[y][x] = _lerp_rgb(pixels[y][x], color, strength)


def _draw_vertical_rule(
    pixels: list[list[tuple[int, int, int]]],
    x: int,
    y0: int,
    y1: int,
    color: tuple[int, int, int],
    strength: float = 0.9,
) -> None:
    for y in range(y0, min(y1, len(pixels))):
        if x < 0 or x >= len(pixels[0]):
            continue
        pixels[y][x] = _lerp_rgb(pixels[y][x], color, strength)


def _fill_light_panel(
    pixels: list[list[tuple[int, int, int]]],
    x0: int,
    x1: int,
    y0: int,
    y1: int,
) -> None:
    _fill_vertical_gradient(pixels, x0, x1, y0, y1, LIGHT_TOP, LIGHT_BOTTOM)


def _paint_industrial_art(
    pixels: list[list[tuple[int, int, int]]],
    art_width: int,
    height: int,
    x_offset: int = 0,
) -> None:
    x0 = x_offset
    x1 = x_offset + art_width
    _fill_vertical_gradient(pixels, x0, x1, 0, height, PANEL, BG)
    _fill_radial_glow(
        pixels,
        x_offset + art_width * 0.35,
        height * 0.28,
        min(art_width, 120),
        PRIMARY,
        0.28,
        x0,
        x1,
        0,
        height,
    )
    _fill_radial_glow(
        pixels,
        x_offset + art_width * 0.72,
        height * 0.72,
        min(art_width, 100),
        ACCENT,
        0.18,
        x0,
        x1,
        0,
        height,
    )
    _draw_scan_grid(pixels, x0, x1, 0, height, spacing=7, strength=0.09)
    _draw_vertical_rule(pixels, x_offset + 3, 0, height, ACCENT, 0.75)
    _draw_vertical_rule(pixels, x_offset + 4, 0, height, ACCENT, 0.35)


def _pixels_to_image(pixels: list[list[tuple[int, int, int]]], width: int, height: int) -> Image.Image:
    flat = bytes(channel for row in pixels for pixel in row for channel in pixel)
    return Image.frombytes("RGB", (width, height), flat)


def _load_mono_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for path, index in _MONO_FONT_CANDIDATES:
        font_path = Path(path)
        if not font_path.exists():
            continue
        try:
            if index is None:
                return ImageFont.truetype(str(font_path), size)
            return ImageFont.truetype(str(font_path), size, index=index)
        except OSError:
            continue
    return ImageFont.load_default()


def _draw_dialog_branding(img: Image.Image) -> None:
    """Stacked MC.GAME.LAUNCHER title on the left art column (matches AppHeader MonoLabel)."""
    draw = ImageDraw.Draw(img)
    font_title = _load_mono_font(11)
    font_label = _load_mono_font(9)

    line_h = 14
    block_h = len(DIALOG_TITLE_LINES) * line_h
    label_gap = 10
    label_text = "· SETUP ·"
    label_bbox = draw.textbbox((0, 0), label_text, font=font_label)
    label_h = label_bbox[3] - label_bbox[1]
    total_h = block_h + label_gap + label_h

    x = 14
    y_start = (DIALOG_H - total_h) // 2 + 6

    rule_x0 = 11
    rule_x1 = DIALOG_ART_W - 10
    rule_top = y_start - 9
    rule_bottom = y_start + block_h + 6
    draw.line((rule_x0, rule_top, rule_x1, rule_top), fill=_lerp_rgb(ACCENT, BG, 0.35))
    draw.line((rule_x0, rule_bottom, rule_x1, rule_bottom), fill=_lerp_rgb(PANEL, BG, 0.25))

    glow = _lerp_rgb(BG, PRIMARY, 0.45)
    y = y_start
    for text, color in DIALOG_TITLE_LINES:
        draw.text((x + 1, y + 1), text, fill=glow, font=font_title)
        draw.text((x, y), text, fill=color, font=font_title)
        y += line_h

    muted = _lerp_rgb(ON_TEXT, BG, 0.52)
    draw.text((x, y + label_gap), label_text, fill=muted, font=font_label)


def _make_banner() -> Image.Image:
    pixels: list[list[tuple[int, int, int]]] = [
        [BG for _ in range(BANNER_W)] for _ in range(BANNER_H)
    ]

    # Text zone first — full light field for title + subtitle.
    _fill_light_panel(pixels, 0, BANNER_LIGHT_W, 0, BANNER_H)

    # Hairline under text only (neutral, not cyan — avoids crowding subtitle at y≈23).
    _draw_horizontal_rule(pixels, BANNER_H - 1, 0, BANNER_LIGHT_W, (226, 232, 240), 0.55)

    # Industrial panel on the far right (out of text bounds).
    art_width = BANNER_W - BANNER_ART_X
    _paint_industrial_art(pixels, art_width, BANNER_H, x_offset=BANNER_ART_X)
    _draw_horizontal_rule(pixels, BANNER_H - 2, BANNER_ART_X, BANNER_W, ACCENT, 0.85)
    _draw_horizontal_rule(pixels, BANNER_H - 4, BANNER_ART_X, BANNER_W, PRIMARY, 0.22)

    # Separator between text field and art.
    sep_x = BANNER_ART_X
    for y in range(BANNER_H):
        pixels[y][sep_x] = _lerp_rgb(pixels[y][sep_x], PANEL, 0.45)
        if sep_x + 1 < BANNER_W:
            pixels[y][sep_x + 1] = _lerp_rgb(pixels[y][sep_x + 1], LIGHT_BOTTOM, 0.35)

    return _pixels_to_image(pixels, BANNER_W, BANNER_H)


def _make_dialog() -> Image.Image:
    pixels: list[list[tuple[int, int, int]]] = [
        [BG for _ in range(DIALOG_W)] for _ in range(DIALOG_H)
    ]
    _paint_industrial_art(pixels, DIALOG_ART_W, DIALOG_H)
    _fill_light_panel(pixels, DIALOG_ART_W, DIALOG_W, 0, DIALOG_H)

    # Bottom vignette on art column only
    for y in range(int(DIALOG_H * 0.72), DIALOG_H):
        t = (y - DIALOG_H * 0.72) / (DIALOG_H * 0.28)
        for x in range(0, DIALOG_ART_W):
            pixels[y][x] = _lerp_rgb(pixels[y][x], BG, t * 0.45)

    # Soft separator between art and text panel
    sep_x = DIALOG_ART_W
    for y in range(DIALOG_H):
        pixels[y][sep_x] = _lerp_rgb(pixels[y][sep_x], PANEL, 0.35)
        if sep_x + 1 < DIALOG_W:
            pixels[y][sep_x + 1] = _lerp_rgb(pixels[y][sep_x + 1], LIGHT_BOTTOM, 0.4)

    img = _pixels_to_image(pixels, DIALOG_W, DIALOG_H)
    _draw_dialog_branding(img)
    return img


def main() -> None:
    out_dir = Path(__file__).resolve().parent / "jpackage"
    out_dir.mkdir(parents=True, exist_ok=True)

    banner_path = out_dir / "installer-banner.bmp"
    dialog_path = out_dir / "installer-dialog.bmp"

    _make_banner().save(banner_path, format="BMP")
    _make_dialog().save(dialog_path, format="BMP")

    print(f"Wrote {banner_path}")
    print(f"Wrote {dialog_path}")


if __name__ == "__main__":
    main()
