#!/usr/bin/env python3
"""
One-off generator for CardFit launcher rasters and Play Store listing assets.

NOT part of the Android build. Nothing here is wired into Gradle and nothing it
produces beyond res/mipmap-* ships inside the APK. Run manually:

    python tools/generate_store_assets.py

Every shape and colour below is transcribed from the real vector sources, not
redesigned:
  * app/src/main/res/drawable/ic_launcher_background.xml  - flat #F4F6FB, no gradient
  * app/src/main/res/drawable/ic_launcher_foreground.xml  - mark, group(1.35, 27.88, 27)
  * app/src/main/res/mipmap-anydpi/ic_launcher.xml        - legacy tile, group(1.9, 17.24, 16)

The mark ("voice arcs") in the group's local 40-unit space is:
  focal dot   filled circle centre (12,20) r 2.6                       #1E2150
  inner arc   right semicircle centre (12,20) r 5     stroke 2.6 round #14B8A6
  middle arc  right semicircle centre (12,20) r 10.5  stroke 2.6 round #2A2F6B
  outer arc   right semicircle centre (12,20) r 16    stroke 2.6 round #5DCAA5
Visual bounds are x 9.4..29.3, y 2.7..37.3, so the mark's visual centre sits at
local (19.35, 20) - to the right of the arc centre (12, 20), because the arcs
open rightward.
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

REPO = Path(__file__).resolve().parent.parent
RES = REPO / "app" / "src" / "main" / "res"
FONT_DIR = RES / "font"
STORE = REPO / "store-assets"

# --- colours, exactly as in the vector sources -------------------------------
PAPER = "#F4F6FB"      # ic_launcher_background fill
DOT = "#1E2150"        # focal dot
ARC_INNER = "#14B8A6"  # r 5
ARC_MIDDLE = "#2A2F6B"  # r 10.5
ARC_OUTER = "#5DCAA5"  # r 16
TEXT_MUTED = "#2A2F6B"

# --- mark geometry in the group's local 40-unit space ------------------------
ARC_CX, ARC_CY = 12.0, 20.0
VISUAL_CX = 19.35          # centre of the mark's ink, not of its arcs
RADII = (5.0, 10.5, 16.0)
ARC_COLOURS = (ARC_INNER, ARC_MIDDLE, ARC_OUTER)
STROKE = 2.6
DOT_R = 2.6

# --- the two group transforms actually used by the app -----------------------
LEGACY_GROUP = (1.9, 17.24, 16.0)    # mipmap-anydpi/ic_launcher.xml
ADAPTIVE_GROUP = (1.35, 27.88, 27.0)  # ic_launcher_foreground.xml
VIEWPORT = 108.0
LEGACY_CORNER_R = 25.5               # rx in the legacy tile path

SS = 4  # supersample factor; Pillow has no antialiased draw, so render big and downscale


def draw_mark(d: ImageDraw.ImageDraw, cx: float, cy: float, scale: float) -> None:
    """The voice-arcs mark centred on the arc centre (cx, cy), at `scale` px per local unit."""
    stroke = STROKE * scale
    for radius, colour in zip(RADII, ARC_COLOURS):
        r = radius * scale
        # Pillow strokes inward from the bbox, so push the bbox out by half the
        # stroke to put the centreline on the true radius.
        outer = r + stroke / 2.0
        d.arc(
            [cx - outer, cy - outer, cx + outer, cy + outer],
            start=-90, end=90, fill=colour, width=max(1, round(stroke)),
        )
        # Round caps: the vectors set strokeLineCap="round"; Pillow has no cap style.
        cap = stroke / 2.0
        for end_y in (cy - r, cy + r):
            d.ellipse([cx - cap, end_y - cap, cx + cap, end_y + cap], fill=colour)
    dot = DOT_R * scale
    d.ellipse([cx - dot, cy - dot, cx + dot, cy + dot], fill=DOT)


def render_icon(size: int, group: tuple[float, float, float], shape: str) -> Image.Image:
    """One launcher icon. `shape` is 'rounded' (legacy tile), 'circle', or 'square' (full bleed)."""
    big = size * SS
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    k = big / VIEWPORT

    if shape == "rounded":
        d.rounded_rectangle([0, 0, big - 1, big - 1], radius=LEGACY_CORNER_R * k, fill=PAPER)
    elif shape == "circle":
        d.ellipse([0, 0, big - 1, big - 1], fill=PAPER)
    else:
        d.rectangle([0, 0, big - 1, big - 1], fill=PAPER)

    gs, tx, ty = group
    draw_mark(d, (tx + gs * ARC_CX) * k, (ty + gs * ARC_CY) * k, gs * k)
    return img.resize((size, size), Image.LANCZOS)


def write_launcher_rasters() -> list[str]:
    """Legacy PNG fallbacks: adaptive icons only render on API 26+, minSdk is 24."""
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    written = []
    for density, px in densities.items():
        out_dir = RES / f"mipmap-{density}"
        out_dir.mkdir(parents=True, exist_ok=True)
        for name, shape in (("ic_launcher", "rounded"), ("ic_launcher_round", "circle")):
            path = out_dir / f"{name}.png"
            render_icon(px, LEGACY_GROUP, shape).save(path, "PNG")
            written.append(f"{path.relative_to(REPO)} ({px}x{px})")
    return written


def write_play_icon() -> str:
    """512x512 32-bit PNG, full square and unmasked - Play applies its own mask."""
    STORE.mkdir(parents=True, exist_ok=True)
    path = STORE / "play_icon_512x512.png"
    # The adaptive composition (full-bleed paper + foreground mark) is what an
    # API 26+ device actually shows before masking.
    render_icon(512, ADAPTIVE_GROUP, "square").save(path, "PNG")
    return str(path.relative_to(REPO))


def fitted_font(path: Path, text: str, max_width: int, start: int, floor: int = 12):
    """Largest size at or below `start` whose rendered `text` fits `max_width`."""
    size = start
    while size > floor:
        font = ImageFont.truetype(str(path), size)
        if font.getbbox(text)[2] <= max_width:
            return font
        size -= 2
    return ImageFont.truetype(str(path), floor)


def write_feature_graphic() -> str:
    """1024x500 24-bit PNG (no alpha), same flat palette and motif as the icon."""
    STORE.mkdir(parents=True, exist_ok=True)
    w, h = 1024, 500
    big = Image.new("RGB", (w * SS, h * SS), PAPER)
    d = ImageDraw.Draw(big)

    margin_x, margin_y = int(w * 0.05), int(h * 0.05)  # 5% safe area on every edge

    # Mark on the left. Sized so its ink height (2*(r_outer + stroke/2)) clears the
    # vertical safe area comfortably.
    scale = 8.125                      # r_outer = 16*8.125 = 130px
    visual_cx, visual_cy = 232.0, h / 2.0
    arc_cx = visual_cx - (VISUAL_CX - ARC_CX) * scale
    draw_mark(d, arc_cx * SS, visual_cy * SS, scale * SS)

    # Wordmark + tagline in the app's own brand faces, read straight off res/font.
    text_x = 396
    avail = w - text_x - margin_x
    wordmark_font = fitted_font(FONT_DIR / "space_grotesk_semibold.ttf", "CardFit", avail * SS, 118 * SS)
    tagline = "Scan, size and print your documents — fully offline."
    tagline_font = fitted_font(FONT_DIR / "inter_regular.ttf", tagline, avail * SS, 40 * SS)

    # Baselines chosen so the text block's optical centre matches the mark's, which is
    # centred on the canvas; otherwise the right side reads as top-heavy.
    d.text((text_x * SS, 236 * SS), "CardFit", font=wordmark_font, fill=DOT, anchor="ls")
    # Teal accent rule, the icon's inner-arc colour.
    d.rounded_rectangle(
        [text_x * SS, 264 * SS, (text_x + 96) * SS, 272 * SS],
        radius=4 * SS, fill=ARC_INNER,
    )
    d.text((text_x * SS, 336 * SS), tagline, font=tagline_font, fill=TEXT_MUTED, anchor="ls")

    path = STORE / "play_feature_graphic_1024x500.png"
    big.resize((w, h), Image.LANCZOS).save(path, "PNG")  # RGB mode -> 24-bit, no alpha
    return str(path.relative_to(REPO))


if __name__ == "__main__":
    print("Launcher rasters:")
    for line in write_launcher_rasters():
        print("  ", line)
    print("Play icon:        ", write_play_icon())
    print("Feature graphic:  ", write_feature_graphic())
