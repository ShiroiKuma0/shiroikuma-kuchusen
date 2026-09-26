#!/usr/bin/env bash
# Rasterise the 白い熊 空中線 icon art into the Android resources.
#
# Prereqs: rsvg-convert (librsvg). The vector SVG sources are produced by
# generate.py — run that first if you changed the design.
#
# Produces:
#   app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png   (legacy/raster fallback)
#   graphics/icon/ic_launcher_preview.png                          (reference render)
#
# Run from anywhere:  bash graphics/icon/render.sh
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
RES="$ROOT/app/src/main/res"
PREVIEW="$HERE/ic_launcher_preview.svg"

# --- launcher icon raster fallbacks (rounded black + art) ---
declare -A DENS=( [mdpi]=48 [hdpi]=72 [xhdpi]=96 [xxhdpi]=144 [xxxhdpi]=192 )
for dpi in "${!DENS[@]}"; do
  rsvg-convert -w "${DENS[$dpi]}" -h "${DENS[$dpi]}" "$PREVIEW" -o "$RES/mipmap-$dpi/ic_launcher.png"
done

# --- reference preview ---
rsvg-convert -w 512 -h 512 "$PREVIEW" -o "$HERE/ic_launcher_preview.png"

echo "rendered mipmaps and preview into app/src/main/res + graphics/icon"
