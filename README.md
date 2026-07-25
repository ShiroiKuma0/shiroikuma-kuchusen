<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="120" alt="白い熊 空中線 icon" />

# 白い熊 空中線

**A podcast & media player re-skinned into the black-yellow house style, themable live, backing itself up in one file — by hand or on command.**

A fork of [Podcini.A](https://github.com/XilinJia/Podcini.A) with **major additions**: a live whole-app theming page (colours, fonts, shapes — every change re-skins the app instantly), a one-file category backup that includes the whole listening database, a token-gated automation interface that lets another app trigger that backup headlessly, and a black-yellow line-art identity.

Installs **side-by-side** with Podcini.A (app id `shiroikuma.kuchusen`).

**📥 Latest release: [`12.4.10+5`](https://github.com/ShiroiKuma0/shiroikuma-kuchusen/releases/latest)** — [all releases & APK downloads »](https://github.com/ShiroiKuma0/shiroikuma-kuchusen/releases)

</div>

---

## 🎨 Live whole-app theming — the 白い熊 空中線 UI page

One page themes everything, live: foundation colours (background, text, secondary, accent, border) with an RGBA picker and one-click recents, font family with external `.ttf`/`.otf` import, font size and weight sliders, corner roundness and border thickness. Every slider tick re-skins the whole app the instant it moves — no apply, no restart. The black-yellow house style is the built-in default; one tap resets back to it. Long-press the drawer's settings cog — or the subscriptions screen's top-right menu button — to jump straight to the page.

## 📦 Export / Import — the whole app in one file, by category

The first section of the UI page backs the app up into a single ZIP: **Subscribed feeds, Database, Colours, Typography & fonts, Shape & borders, App settings** — all preselected. Settings travel as plain per-category JSON (plus your imported fonts); the **Database** category carries a transactionally consistent snapshot of the listening database itself — episodes, play positions, queues, ratings — so one file is a real restore point, not a settings dump. Set an export directory once and the page answers "Last export: …" every time it opens; settings imports merge (never wipe) and skip feeds you already have, while a restored database takes effect on the restart the app then insists on.

## 🤖 Backup on command — headless, token-gated

The app answers a broadcast from a sister automation app and exports itself with no UI at all: it writes the same one ZIP, optionally into a directory the caller names, reports progress as **real counts** while it works (`Category 3/6 — Colours`, `Database 24.0 MB / 61.3 MB` — never a meaningless percentage), and replies with the written path, its exact byte size and the category count. A master switch (**off** by default) and a 24-byte token — tap to copy, regenerate at will, never included in any backup — are the only gate; both rows sit inside the Export/Import section where backup lives.

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
