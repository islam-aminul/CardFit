#!/usr/bin/env python3
"""
Verify an exported CardFit print PDF geometrically, so true-size correctness can be
checked without a printer. A ruler on paper conflates two things — whether the app
laid the card out at the right size, and whether the printer scaled the page. This
checks the first exactly; the ruler then only has to confirm the second.

Not part of the Android build. Usage:

    python tools/verify_print_pdf.py <file.pdf> [expected_w_mm] [expected_h_mm]

Reads /MediaBox for the page size and inflates the content stream to recover the
image placement matrices (`a b c d e f cm` followed by `/X Do`), converting points
back to millimetres with the spec's pt = mm * 72 / 25.4.
"""

import re
import sys
import zlib
from pathlib import Path

PT_PER_MM = 72.0 / 25.4


def mm(pt: float) -> float:
    return pt / PT_PER_MM


def media_boxes(data: bytes) -> list[tuple[float, float, float, float]]:
    out = []
    for m in re.finditer(rb"/MediaBox\s*\[\s*([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s*\]", data):
        out.append(tuple(float(x) for x in m.groups()))
    return out


def content_streams(data: bytes) -> list[bytes]:
    """Every stream we can inflate; page content is Flate-compressed by PdfDocument."""
    streams = []
    for m in re.finditer(rb"stream\r?\n", data):
        start = m.end()
        end = data.find(b"endstream", start)
        if end == -1:
            continue
        raw = data[start:end]
        try:
            streams.append(zlib.decompress(raw))
        except zlib.error:
            continue
    return streams


def image_placements(stream: bytes) -> list[tuple[float, float, float, float]]:
    """
    (x, y_from_top, w, h) in points for every image draw.

    Pair each `/Name Do` with the nearest *preceding* `cm` rather than regexing the two
    together: PdfDocument emits an outer flip (`1 0 0 -1 0 <height> cm`) that turns the
    canvas into a y-down space, and a combined pattern happily spans from that outer
    matrix all the way to the first image's Do, hiding a card.

    Inside that flipped space a card with matrix `w 0 0 -h tx ty` occupies
    y from `ty - h` (top) to `ty` (bottom), so the top edge is `ty + d`.
    """
    text = stream.decode("latin-1", errors="replace")
    num = r"[-\d.]+"
    cms = [
        (m.start(), tuple(float(g) for g in m.groups()))
        for m in re.finditer(rf"({num})\s+({num})\s+({num})\s+({num})\s+({num})\s+({num})\s+cm", text)
    ]
    out = []
    for do in re.finditer(r"/\w+\s+Do", text):
        prior = [c for pos, c in cms if pos < do.start()]
        if not prior:
            continue
        a, b, c, d, e, f = prior[-1]
        w, h = abs(a), abs(d)
        if mm(w) < 5.0 or mm(h) < 5.0:
            continue  # hairlines / the outer flip matrix
        out.append((e, f + d, w, h))
    return out


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    path = Path(sys.argv[1])
    expected = None
    if len(sys.argv) >= 4:
        expected = (float(sys.argv[2]), float(sys.argv[3]))

    data = path.read_bytes()
    print(f"file: {path.name}  ({len(data):,} bytes)")

    boxes = media_boxes(data)
    print(f"pages: {len(boxes)}")
    for i, (x0, y0, x1, y1) in enumerate(boxes, 1):
        w_pt, h_pt = x1 - x0, y1 - y0
        print(f"  page {i}: {w_pt:.2f} x {h_pt:.2f} pt  =  {mm(w_pt):.2f} x {mm(h_pt):.2f} mm")

    placements = []
    for s in content_streams(data):
        placements.extend(image_placements(s))

    if not placements:
        print("  (no axis-aligned image placements recovered)")
        return 0

    print(f"placed images: {len(placements)}")
    placements.sort(key=lambda p: p[1])  # top-down
    page_w = mm(boxes[0][2] - boxes[0][0]) if boxes else None
    for i, (x, y, w, h) in enumerate(placements, 1):
        line = f"  #{i}: {mm(w):.2f} x {mm(h):.2f} mm  at x={mm(x):.2f} top={mm(y):.2f} mm"
        if expected:
            ew, eh = expected
            dw, dh = abs(mm(w) - ew), abs(mm(h) - eh)
            line += f"  [{'PASS' if dw <= 0.5 and dh <= 0.5 else f'FAIL off by {dw:.2f},{dh:.2f}'}]"
        if page_w is not None:
            centred = abs((mm(x) + mm(w) / 2) - page_w / 2) <= 0.5
            line += f"  h-centred: {'yes' if centred else 'NO'}"
        print(line)

    for i in range(len(placements) - 1):
        _, y_a, _, h_a = placements[i]
        _, y_b, _, _ = placements[i + 1]
        print(f"gap #{i + 1}->#{i + 2}: {mm(y_b - (y_a + h_a)):.2f} mm")

    if boxes and placements:
        page_h = mm(boxes[0][3] - boxes[0][1])
        top = mm(placements[0][1])
        bottom_edge = mm(placements[-1][1] + placements[-1][3])
        print(f"stack: top margin {top:.2f} mm, bottom margin {page_h - bottom_edge:.2f} mm "
              f"(v-centred: {'yes' if abs(top - (page_h - bottom_edge)) <= 0.5 else 'NO'})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
