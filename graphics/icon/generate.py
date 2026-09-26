#!/usr/bin/env python3
"""Generate the 白い熊 空中線 (shiroikuma-kuchusen) app icon + splash art.

House style: the Podcini glyph — a left-bulging crescent "pea pod" holding four
smiley peas, with a treble clef in the opening — retraced as pure yellow (#FFFF00)
line-art on black.

This is the single source of truth for the icon. Running it (re)writes:
  graphics/icon/ic_launcher_foreground.svg   (transparent art, for reference/preview)
  graphics/icon/ic_launcher_preview.svg      (art on black rounded square)
  app/src/main/res/drawable/ic_launcher_sk_foreground.xml   (Android VectorDrawable)
  app/src/main/res/drawable/ic_launcher_sk_monochrome.xml   (themed-icon monochrome)
  app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml        (adaptive icon)

The treble clef path is extracted from Noto Music's U+1D11E glyph via fontTools,
so no music font is needed at runtime. Circles are emitted as arc paths (Android
VectorDrawable has no <circle>). The clef transform is baked into the path data.

Rasters (mipmap fallbacks) and the splash-layer PNGs are produced by render.sh,
which rasterises this art with rsvg-convert + ImageMagick.

Run from anywhere:  python3 graphics/icon/generate.py
"""
import math
import os
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

# ---- paths --------------------------------------------------------------------
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")
NOTO_MUSIC = "/usr/share/fonts/truetype/noto/NotoMusic-Regular.ttf"

# ---- design parameters --------------------------------------------------------
YELLOW = "#FFFF00"
VIEW = 512
# crescent = (outer circle) minus (inner circle); outer-left + inner-right => bulges
# left, opens right, matching the original Podcini pod.
OCX, OR, ICX, IR = 225, 188, 345, 170
CSW = 15                       # crescent stroke width
PEA_RADII = [30, 43, 38, 30]   # four smiley peas, varied sizes, top -> bottom
PEA_GAP = 18                   # clear gap between adjacent peas (no touching)
CLEF = (300, 262, 250)         # clef centre x, centre y, height

# Fit-to-safe-zone transform. The raw art (above) is drawn left-leaning and spans
# a bounding radius of ~216 in the 512 viewport, which overflows an adaptive icon's
# 72dp safe zone (radius ~170) and gets clipped by the launcher mask. We recenter
# the glyph's bounding box on the viewport centre and scale it down to fit.
# (acx,acy) = art bbox centre; FIT_SCALE = safe_radius / bounding_radius.
# Re-measure with measure.py if you change the geometry above.
ART_CX, ART_CY = 186.8, 256.0
FIT_SCALE = 0.778

# ---- crescent + placement geometry -------------------------------------------
_d = abs(ICX - OCX)
_a = (_d * _d + OR * OR - IR * IR) / (2 * _d)
H = math.sqrt(OR * OR - _a * _a)
MX = OCX + _a * (ICX - OCX) / _d
TIP_T, TIP_B = 256 - H, 256 + H


def mid(y):
    """Pod centre-x and half-width at height y."""
    dy = y - 256
    xl = OCX - math.sqrt(max(OR * OR - dy * dy, 0))
    xr = ICX - math.sqrt(max(IR * IR - dy * dy, 0))
    return (xl + xr) / 2, (xr - xl) / 2


def _place(y1, radii, gap):
    cx, _ = mid(y1)
    centers = [(cx, y1)]
    for i in range(len(radii) - 1):
        need = radii[i] + radii[i + 1] + gap
        y = centers[-1][1]
        while y < TIP_B:
            y += 0.5
            mx2, _ = mid(y)
            if math.hypot(mx2 - centers[-1][0], y - centers[-1][1]) >= need:
                centers.append((mx2, y))
                break
        else:
            return None
    return centers


def pea_centers(radii=PEA_RADII, gap=PEA_GAP, wallmin=11, margin_min=7):
    """Pack the peas along the pod centreline, balancing top/bottom margins."""
    best = None
    y = TIP_T + radii[0] + 2
    while y < 256:
        c = _place(y, radii, gap)
        if c:
            tm = (c[0][1] - radii[0]) - TIP_T
            bm = TIP_B - (c[-1][1] + radii[-1])
            walls_ok = all(mid(cy)[1] - radii[i] - CSW / 2 >= wallmin
                           for i, (cx, cy) in enumerate(c))
            if bm >= margin_min and tm >= margin_min and walls_ok:
                if best is None or abs(tm - bm) < best[0]:
                    best = (abs(tm - bm), c)
        y += 0.5
    if best is None:
        raise SystemExit("peas do not fit with the given radii/gap")
    return best[1]


CENTERS = pea_centers()

# ---- fit transform: design coords -> final (recenter on viewport + scale) -----
_SX = FIT_SCALE
_OX = 256 - _SX * ART_CX
_OY = 256 - _SX * ART_CY


def TP(x, y):
    """Transform a point into the fitted/recentred space."""
    return _SX * x + _OX, _SX * y + _OY


def TL(v):
    """Transform a length (radius / stroke width)."""
    return _SX * v


# ---- treble clef path (baked transform) --------------------------------------
_f = TTFont(NOTO_MUSIC)
_g = _f.getBestCmap()[0x1D11E]
_gs = _f.getGlyphSet()
_FXMIN, _FYMIN, _FXMAX, _FYMAX = 50, -398, 661, 1334
_fcx, _fcy = (_FXMIN + _FXMAX) / 2, (_FYMIN + _FYMAX) / 2
_cx, _cy, _ch = CLEF
_s = _ch / (_FYMAX - _FYMIN)
_tx = _cx - _fcx * _s
_ty = _cy + _fcy * _s
_spen = SVGPathPen(_gs)
# compose the clef transform (scale + flip Y + translate) with the fit transform
_gs[_g].draw(TransformPen(_spen, (_SX * _s, 0, 0, -_SX * _s, _SX * _tx + _OX, _SX * _ty + _OY)))
CLEF_D = _spen.getCommands()


# ---- path builders -----------------------------------------------------------
def crescent_d():
    bx, by = TP(MX, 256 + H)
    tx, ty = TP(MX, 256 - H)
    rO, rI = TL(OR), TL(IR)
    return (f"M{bx:.2f},{by:.2f} A{rO:.2f},{rO:.2f} 0 1 1 {tx:.2f},{ty:.2f} "
            f"A{rI:.2f},{rI:.2f} 0 0 0 {bx:.2f},{by:.2f} Z")


def circ_d(cx, cy, r):
    return (f"M{cx - r:.2f},{cy:.2f} a{r:.2f},{r:.2f} 0 1 0 {2 * r:.2f},0 "
            f"a{r:.2f},{r:.2f} 0 1 0 {-2 * r:.2f},0 Z")


def smile_d(cx, cy, r):
    sm = 0.44 * r
    return f"M{cx - sm:.2f},{cy + 0.05 * r:.2f} Q{cx:.2f},{cy + 0.58 * r:.2f} {cx + sm:.2f},{cy + 0.05 * r:.2f}"


# ---- SVG output --------------------------------------------------------------
def svg_art():
    out = [f'<path d="{crescent_d()}" fill="none" stroke="{YELLOW}" '
           f'stroke-width="{TL(CSW):.2f}" stroke-linejoin="round" stroke-linecap="round"/>']
    for i, (cx, cy) in enumerate(CENTERS):
        r = PEA_RADII[i]
        sw = max(round(r * 0.17), 6)
        tcx, tcy = TP(cx, cy)
        tr = TL(r)
        out.append(f'<circle cx="{tcx:.2f}" cy="{tcy:.2f}" r="{tr:.2f}" fill="none" stroke="{YELLOW}" stroke-width="{TL(sw):.2f}"/>')
        ex, ey, er = 0.34 * r, cy - 0.22 * r, max(0.13 * r, 3.2)
        lx, ly = TP(cx - ex, ey)
        rx, ry = TP(cx + ex, ey)
        ter = TL(er)
        out.append(f'<circle cx="{lx:.2f}" cy="{ly:.2f}" r="{ter:.2f}" fill="{YELLOW}"/>')
        out.append(f'<circle cx="{rx:.2f}" cy="{ry:.2f}" r="{ter:.2f}" fill="{YELLOW}"/>')
        out.append(f'<path d="{smile_d(tcx, tcy, tr)}" fill="none" stroke="{YELLOW}" stroke-width="{TL(max(sw - 1, 5)):.2f}" stroke-linecap="round"/>')
    out.append(f'<g><path d="{CLEF_D}" fill="{YELLOW}"/></g>')
    return "\n  ".join(out)


def write_svgs():
    art = svg_art()
    fg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{VIEW}" height="{VIEW}" '
          f'viewBox="0 0 {VIEW} {VIEW}">\n  {art}\n</svg>\n')
    rr = VIEW * 0.235
    preview = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{VIEW}" height="{VIEW}" '
               f'viewBox="0 0 {VIEW} {VIEW}">\n'
               f'  <rect width="{VIEW}" height="{VIEW}" rx="{rr}" ry="{rr}" fill="#000"/>\n  {art}\n</svg>\n')
    open(os.path.join(HERE, "ic_launcher_foreground.svg"), "w").write(fg)
    open(os.path.join(HERE, "ic_launcher_preview.svg"), "w").write(preview)


# ---- Android VectorDrawable output -------------------------------------------
def vd_paths(col):
    p = [f'  <path android:pathData="{crescent_d()}" android:strokeColor="{col}" '
         f'android:strokeWidth="{TL(CSW):.2f}" android:strokeLineJoin="round" android:strokeLineCap="round"/>']
    for i, (cx, cy) in enumerate(CENTERS):
        r = PEA_RADII[i]
        sw = max(round(r * 0.17), 6)
        tcx, tcy = TP(cx, cy)
        tr = TL(r)
        p.append(f'  <path android:pathData="{circ_d(tcx, tcy, tr)}" android:strokeColor="{col}" android:strokeWidth="{TL(sw):.2f}"/>')
        ex, ey, er = 0.34 * r, cy - 0.22 * r, max(0.13 * r, 3.2)
        lx, ly = TP(cx - ex, ey)
        rx, ry = TP(cx + ex, ey)
        ter = TL(er)
        p.append(f'  <path android:pathData="{circ_d(lx, ly, ter)}" android:fillColor="{col}"/>')
        p.append(f'  <path android:pathData="{circ_d(rx, ry, ter)}" android:fillColor="{col}"/>')
        p.append(f'  <path android:pathData="{smile_d(tcx, tcy, tr)}" android:strokeColor="{col}" '
                 f'android:strokeWidth="{TL(max(sw - 1, 5)):.2f}" android:strokeLineCap="round"/>')
    p.append(f'  <path android:pathData="{CLEF_D}" android:fillColor="{col}"/>')
    return "\n".join(p)


def vector(col):
    return ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp" android:height="108dp"\n'
            f'    android:viewportWidth="{VIEW}" android:viewportHeight="{VIEW}">\n'
            f'{vd_paths(col)}\n</vector>\n')


def write_android():
    open(os.path.join(RES, "drawable", "ic_launcher_sk_foreground.xml"), "w").write(vector(YELLOW))
    open(os.path.join(RES, "drawable", "ic_launcher_sk_monochrome.xml"), "w").write(vector("#FF000000"))
    os.makedirs(os.path.join(RES, "mipmap-anydpi-v26"), exist_ok=True)
    open(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher.xml"), "w").write(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@android:color/black" />\n'
        '    <foreground android:drawable="@drawable/ic_launcher_sk_foreground" />\n'
        '    <monochrome android:drawable="@drawable/ic_launcher_sk_monochrome" />\n'
        '</adaptive-icon>\n')


if __name__ == "__main__":
    write_svgs()
    write_android()
    print("generated: graphics/icon/*.svg + app/src/main/res/{drawable,mipmap-anydpi-v26} icon XMLs")
    print(f"peas: {[ (round(cx),round(cy),r) for (cx,cy),r in zip(CENTERS, PEA_RADII) ]}")
