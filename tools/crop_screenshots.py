#!/usr/bin/env python3
"""
Produce listing-ready screenshots from the raw device captures.

Play requires each side within 320..3840 px and the longer side to be at most twice
the shorter. The raw S22 captures are 1080x2340, i.e. 2.167 — 180 px too tall.

Cropping the system chrome fixes the ratio and improves the shots: the status bar
carries the clock, carrier and notification icons, none of which belong in a store
listing.

The crop is pinned by measurement, not taste. App content starts at y=152 on every
capture, and on 01_home the About card runs to y~2297 with the gesture pill below it,
so the bottom can only give up 40 px without clipping a card. That forces >=140 px off
the top to reach 2160, which is exactly 2:1 — the limit is "no more than twice", so
equal is fine.

    python tools/crop_screenshots.py

Reads screenshots/ (kept as the untouched raw captures) and writes store-assets/
(the listing-ready set). Not part of the Android build.
"""

from pathlib import Path

from PIL import Image

REPO = Path(__file__).resolve().parent.parent
SRC = REPO / "screenshots"
DST = REPO / "store-assets"

CROP_TOP = 140     # status bar; app content starts at y=152, leaving a 12 px margin
CROP_BOTTOM = 40   # gesture pill only; the About card ends at y~2297
JPEG_QUALITY = 95  # high enough that re-encoding is not visible
MAX_RATIO = 2.0


def main() -> int:
    DST.mkdir(parents=True, exist_ok=True)
    shots = sorted(SRC.glob("*.jpg"))
    if not shots:
        print("no screenshots found")
        return 1

    for p in shots:
        im = Image.open(p).convert("RGB")
        w, h = im.size
        out = im.crop((0, CROP_TOP, w, h - CROP_BOTTOM))
        ow, oh = out.size
        ratio = max(ow, oh) / min(ow, oh)
        target = DST / p.name
        out.save(target, "JPEG", quality=JPEG_QUALITY, optimize=True, subsampling=0)
        size_mb = target.stat().st_size / 1024 / 1024
        ok = (
            320 <= ow <= 3840
            and 320 <= oh <= 3840
            and ratio <= MAX_RATIO
            and size_mb < 8
        )
        print(
            f"{p.name:<26} {w}x{h} -> {ow}x{oh}  ratio {ratio:.4f}  "
            f"{size_mb:.2f} MB  {'PASS' if ok else 'FAIL'}"
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
