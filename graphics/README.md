# graphics — 白い熊 空中線 icon & splash source

House style: the Podcini glyph — a left-bulging crescent "pea pod" holding four
smiley peas, with a treble clef in the opening — retraced as pure yellow
(`#FFFF00`) line-art on black. Same family as the other shiroikuma forks
(e.g. shiroikuma-messeji's yellow line-art icon).

## Files

- `icon/generate.py` — **the single source of truth.** Pure Python (+ fontTools).
  Builds the glyph and writes:
  - `icon/ic_launcher_foreground.svg` — transparent art (for reference / rasterising).
  - `icon/ic_launcher_preview.svg` — art on a black rounded square.
  - `app/src/main/res/drawable/ic_launcher_sk_foreground.xml` — Android VectorDrawable.
  - `app/src/main/res/drawable/ic_launcher_sk_monochrome.xml` — themed-icon monochrome.
  - `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — the adaptive icon.

  The treble clef is extracted from Noto Music's U+1D11E glyph via fontTools (no
  music font is needed at runtime); circles are emitted as arc paths (Android
  VectorDrawable has no `<circle>`), and the clef transform is baked into the path.

- `icon/render.sh` — rasterises the SVG art with `rsvg-convert` + ImageMagick into:
  - `app/src/main/res/mipmap-*/ic_launcher.png` — legacy/raster fallbacks (minSdk is
    26, so the adaptive icon is used on every device; these are just fallbacks).
  - `app/src/main/res/drawable-nodpi/launcher_animate_{bg,wave1,wave2}.png` — the
    three splash layers (our glyph at dim / mid / full yellow).
  - `icon/ic_launcher_preview.png` — a reference render.

## Regenerating after a design change

```bash
python3 graphics/icon/generate.py     # rewrites the SVGs + Android vector XMLs
bash    graphics/icon/render.sh        # rewrites the raster mipmaps + splash layers
```

Tunable parameters live at the top of `generate.py` (crescent geometry, pea radii
+ gap, clef size/position) and the splash opacities in `render.sh`.

## Splash

The Android-12 splash (`Theme.Podcini.Splash` in `res/values/styles.xml`) uses
`windowSplashScreenBackground = @android:color/black` and the
`windowSplashScreenAnimatedIcon = @drawable/launcher_animate` frame animation,
which pulses the glyph full → mid → dim → mid (looping) — a yellow glow on black.
