<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="120" alt="白い熊 空中線 icon" />

# 白い熊 空中線

**A podcast & media player re-skinned into the black-yellow house style, themable live, with one-tap settings backup.**

A fork of [Podcini.A](https://github.com/XilinJia/Podcini.A) with **major additions**: a live whole-app theming page (colours, fonts, shapes — every change re-skins the app instantly), category-based Export/Import of subscriptions and every setting, and a black-yellow line-art identity.

Installs **side-by-side** with Podcini.A (app id `shiroikuma.kuchusen`).

**📥 Latest release: [`12.4.10+4`](https://github.com/ShiroiKuma0/shiroikuma-kuchusen/releases/latest)** — [all releases & APK downloads »](https://github.com/ShiroiKuma0/shiroikuma-kuchusen/releases)

</div>

---

## 🎨 Live whole-app theming — the 白い熊 空中線 UI page

One page themes everything, live: foundation colours (background, text, secondary, accent, border) with an RGBA picker and one-click recents, font family with external `.ttf`/`.otf` import, font size and weight sliders, corner roundness and border thickness. Every slider tick re-skins the whole app the instant it moves — no apply, no restart. The black-yellow house style is the built-in default; one tap resets back to it. Long-press the drawer's settings cog — or the subscriptions screen's top-right menu button — to jump straight to the page.

## 📦 Export / Import — subscriptions and every setting, by category

The first section of the UI page backs up the whole app into a plain ZIP of per-category JSON files (plus your imported fonts): **Subscribed feeds, Colours, Typography & fonts, Shape & borders, App settings** — all preselected. Set an export directory once and the page answers "Last export: …" every time it opens; imports merge (never wipe) and skip feeds you already have. Round-pill dialogs in house style; acknowledging a successful run closes the whole chain back down to the main screen.

## 🖤 Black-yellow identity, everywhere

The launcher icon is the Podcini glyph re-traced as pure-yellow line art on black (adaptive + monochrome), the splash matches, menus and dialogs sit on true black, and toasts follow the house style — black, yellow text, yellow border.

---

## Built on Podcini.A

A fork of [Podcini.A](https://github.com/XilinJia/Podcini.A) by XilinJia (app id `shiroikuma.kuchusen`, so it coexists with the official build). Podcini.A is an open-source, extensible podcast and media instrument handling podcasts, local media, and external source providers — all of its functionality is inherited here. The code remains under [GPL-3.0](LICENSE).

## Building

```bash
git clone git@github.com:ShiroiKuma0/shiroikuma-kuchusen.git
cd shiroikuma-kuchusen
# needs JDK 21 and the Android SDK (platform 37, build-tools 37.0.0, NDK 29.0.14206865)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew buildFork
```

`buildFork` assembles the signed arm64-v8a `free` release APK as `shiroikuma-kuchusen_<version>+<build>_arm64-v8a.apk` and bumps the build counter. Development happens on the `custom` branch; `main` mirrors upstream.
