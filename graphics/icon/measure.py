#!/usr/bin/env python3
"""Verify the icon art fits an adaptive-icon safe zone (so the launcher mask
doesn't clip it).

Renders the generated foreground SVG and reports the maximum radius of any
opaque pixel from the viewport centre, against the adaptive-icon safe radii:
  - 72dp safe zone   -> radius 170.7 in the 512 viewport (recommended max)
  - 66dp guaranteed  -> radius 156.4 (never clipped on any mask)

Run after generate.py:  python3 graphics/icon/measure.py

To RE-FIT after changing the glyph geometry in generate.py:
  1. temporarily set FIT_SCALE = 1.0 in generate.py and run it,
  2. run this script — it prints the raw bbox centre + bounding radius,
  3. set ART_CX/ART_CY to that centre and FIT_SCALE = 170/bounding_radius,
  4. run generate.py + render.sh again and re-check here.
"""
import os
import subprocess
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
FG = os.path.join(HERE, "ic_launcher_foreground.svg")
N = 1024  # render size
SCALE = 512.0 / N

png = os.path.join(HERE, ".measure.png")
subprocess.run(["rsvg-convert", "-w", str(N), "-h", str(N), FG, "-o", png], check=True)
im = np.array(Image.open(png))
os.remove(png)
opaque = im[..., 3] > 16
ys, xs = np.where(opaque)
xs, ys = xs * SCALE, ys * SCALE
x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
acx, acy = (x0 + x1) / 2, (y0 + y1) / 2
r_centre = np.sqrt((xs - 256) ** 2 + (ys - 256) ** 2).max()
r_bbox = np.sqrt((xs - acx) ** 2 + (ys - acy) ** 2).max()

print(f"bbox        x[{x0:.1f},{x1:.1f}] y[{y0:.1f},{y1:.1f}]  centre=({acx:.1f},{acy:.1f})")
print(f"radius from viewport centre (256,256) = {r_centre:.1f}")
print(f"radius from bbox centre               = {r_bbox:.1f}")
print(f"72dp safe = 170.7   66dp guaranteed = 156.4")
ok = r_centre <= 170.7
print("FIT: OK (within 72dp safe zone)" if ok else "FIT: TOO BIG — will clip; reduce FIT_SCALE")
sys.exit(0 if ok else 1)
