#!/usr/bin/env bash
# Rasterise the 白い熊 空中線 icon art into the Android resources.
#
# Prereqs: rsvg-convert (librsvg) + ImageMagick (magick). The vector SVG sources
# are produced by generate.py — run that first if you changed the design.
#
# Produces:
#   app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png   (legacy/raster fallback)
#   app/src/main/res/drawable-nodpi/launcher_animate_{bg,wave1,wave2}.png  (splash layers)
#   graphics/icon/ic_launcher_preview.png                          (reference render)
#
# The splash (windowSplashScreenAnimatedIcon = launcher_animate) is a 3-layer
# frame animation that pulses the glyph; we render our glyph at three opacities
# (dim -> mid -> full) so it glows yellow on the black splash background.
#
# Run from anywhere:  bash graphics/icon/render.sh
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
RES="$ROOT/app/src/main/res"
FG="$HERE/ic_launcher_foreground.svg"
PREVIEW="$HERE/ic_launcher_preview.svg"

# --- launcher icon raster fallbacks (rounded black + art) ---
declare -A DENS=( [mdpi]=48 [hdpi]=72 [xhdpi]=96 [xxhdpi]=144 [xxxhdpi]=192 )
for dpi in "${!DENS[@]}"; do
  rsvg-convert -w "${DENS[$dpi]}" -h "${DENS[$dpi]}" "$PREVIEW" -o "$RES/mipmap-$dpi/ic_launcher.png"
done

# --- reference preview ---
rsvg-convert -w 512 -h 512 "$PREVIEW" -o "$HERE/ic_launcher_preview.png"

# --- splash layers: full-yellow glyph + two dimmer copies (transparent bg) ---
SPLASH_PX=700
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
rsvg-convert -w "$SPLASH_PX" -h "$SPLASH_PX" "$FG" -o "$TMP/full.png"
# wave2 = full intensity (top layer, shown when the icon is fully "lit")
cp "$TMP/full.png" "$RES/drawable-nodpi/launcher_animate_wave2.png"
# wave1 = mid intensity
magick "$TMP/full.png" -channel A -evaluate multiply 0.60 +channel "$RES/drawable-nodpi/launcher_animate_wave1.png"
# bg = dim base (shown alone in the dimmest frame)
magick "$TMP/full.png" -channel A -evaluate multiply 0.28 +channel "$RES/drawable-nodpi/launcher_animate_bg.png"

echo "rendered mipmaps, splash layers, and preview into app/src/main/res + graphics/icon"
