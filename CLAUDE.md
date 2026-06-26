# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**白い熊 空中線** — a personal fork of [Podcini.A](https://github.com/XilinJia/Podcini.A) (XilinJia),
an open-source podcast/media player for Android, written in Kotlin + Jetpack Compose, targeting
Android API 26–37.

This repository (`ShiroiKuma0/shiroikuma-kuchusen`) is a fork. We track upstream
(`XilinJia/Podcini.A`) and layer our own customizations on top of it.

## Fork Workflow — READ THIS FIRST

This is the most important section. The whole point of this repo is to maintain a small set of
customizations on top of upstream and rebuild as upstream releases new versions.

### Git remotes & branches

- `origin` → `git@github.com:ShiroiKuma0/shiroikuma-kuchusen.git` — our fork (push here).
- `upstream` → `https://github.com/XilinJia/Podcini.A.git` — the original (read-only, for rebasing).
- **`main`** mirrors upstream's `main` (fast-forward only). We do **not** develop on it.
- **`custom`** is our development branch. **All our work lives here.** This is the default working branch.

### Our customizations (what makes this a fork)

| What | Value | Where |
| --- | --- | --- |
| Installed app ID | `shiroikuma.kuchusen` | `gradle.properties` → `APP_ID` (read by `applicationId` in `app/build.gradle.kts`) |
| Code namespace | `ac.mdiq.podcini` (unchanged from upstream) | `app/build.gradle.kts` → `namespace` |
| App launcher label | `白い熊 空中線` | `resValue("string", "app_name", …)` in the `release` build type of `app/build.gradle.kts` |
| Provider authority | `${APP_ID}.provider` (= `shiroikuma.kuchusen.provider`; must match `${applicationId}.provider` in the manifest, which `FileProvider.getUriForFile` reads via `R.string.provider_authority`) | `resValue("string", "provider_authority", …)` |
| Signing | own keystore, gitignored config | `keystore.properties` → `~/.android-keystores/shiroikuma-kuchusen.jks` (alias `kuchusen`) |
| App icon | yellow `#FFFF00` line-art on black (our style) | adaptive icon under `app/src/main/res/` (see Icon below) |

The app ID is deliberately changed so this fork installs **alongside** upstream Podcini.A without
conflict. The namespace is intentionally kept as `ac.mdiq.podcini` so `R`/`BuildConfig` and all
source packages remain unchanged — only the installed package id, label, and icon differ.

### Versioning & APK naming

We base our version on upstream and add a fork increment (`BUILD_NUMBER`).

- `VERSION_NAME` / `VERSION_CODE` in `gradle.properties` **track upstream** (currently `12.1.7` / `60`).
- `BUILD_NUMBER` is **our** increment. It starts at `1` and bumps by `1` on every build.
- Fork `versionName` = `"<VERSION_NAME>+<BUILD_NUMBER>"` (e.g. `12.1.7+1`).
- Fork `versionCode` = `VERSION_CODE * 10000 + BUILD_NUMBER` (e.g. `60 * 10000 + 1 = 600001`).
- Output APK filename = `shiroikuma-kuchusen_<VERSION_NAME>+<BUILD_NUMBER>_arm64-v8a.apk`
  (e.g. `shiroikuma-kuchusen_12.1.7+1_arm64-v8a.apk`).

So the first build is `+1` (`600001`), the next build is `+2` (`600002`), and so on. On a new upstream
version, `BUILD_NUMBER` resets to `1` (see the **upstream-new-version** skill).

### Building

Requires **JDK 21** (the default `java` on this machine is JDK 11) and the **Android SDK** (`sdk.dir`
in the gitignored `local.properties` → `/home/shiroikuma/android-sdk`; needs the android-37 platform,
build-tools 37.0.0, and NDK 29.0.14206865 — all present on this host).

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew buildFork
```

`buildFork` (defined in `app/build.gradle.kts`):
1. builds `assembleFreeRelease` (signed, via `keystore.properties`),
2. copies the **arm64-v8a** APK to `~/tmp/shiroikuma-kuchusen_<version>_arm64-v8a.apk`,
3. **auto-increments `BUILD_NUMBER`** in `gradle.properties` for the next build.

See the **build-apk** skill for the full build-and-transfer procedure (scp to skhw / adb push).

### Rebasing onto a new upstream release

When 白い熊 says a new upstream version is out, follow the **upstream-new-version** skill. In short:
1. `git fetch upstream --tags`.
2. Fast-forward `main` to the new upstream release.
3. Back up `custom`, then rebase it onto `main`, preserving every customization in the table above.
4. Set `VERSION_NAME` / `VERSION_CODE` to the new upstream values and **reset `BUILD_NUMBER` to `1`**.
5. Build the new `+1` with `./gradlew buildFork`; continue further changes as `+2`, `+3`, …

### HARD RULES (do not violate)

- **Never install APKs to the phone automatically.** After building, **ask** (via `AskUserQuestion`)
  how to transfer — scp to skhw (first choice) or `adb push` to `/sdcard/tmp/`. 白い熊 installs it
  manually from there. Do **not** use `adb install`.
- **adb runs UNSANDBOXED** (`dangerouslyDisableSandbox: true`) — global hard rule in
  `~/.claude/CLAUDE.md`; the sandbox blocks adb's server socket.
- **Never commit or push on your own.** Develop and build, let 白い熊 test, and **only commit/push
  when 白い熊 explicitly says "Push"**. Push goes to `origin` (`custom` branch; `main` is a
  fast-forward of upstream).

## Build Commands

```bash
./gradlew buildFork              # Our fork build: free release → ~/tmp + bump BUILD_NUMBER (use this)
./gradlew assembleFreeRelease    # Build free release APK only (signed via keystore.properties)
./gradlew assembleFreeDebug      # Build debug APK (app id gets .debug suffix)
```

**Product flavors:** `free` (no Google Play Services; Conscrypt) and `play` (GMS + Chromecast).
We ship **`free`** (the microG/no-GMS phone). ABI split: `arm64-v8a` + a `universal` APK; we deploy
the arm64-v8a one.

## Icon (our-style black-yellow trace)

Our launcher icon is the upstream glyph rendered as **yellow `#FFFF00` line-art on a black
background**, as an adaptive icon — the same house style as the other shiroikuma forks (e.g.
shiroikuma-messeji). It lives in `app/src/main/res/`:
- `mipmap-anydpi-v26/ic_launcher.xml` — `<background>@android:color/black</background>` +
  `<foreground>@drawable/ic_launcher_sk_foreground</foreground>` + a `<monochrome>`.
- `drawable/ic_launcher_sk_foreground.xml` — the traced vector: `fillColor="#00000000"` (transparent)
  + `strokeColor="#FFFF00"` (pure yellow). minSdk is 26, so the adaptive icon is used on every device.

## Architecture (upstream Podcini.A)

Single `:app` module. Kotlin + Jetpack Compose UI; Realm (krdb) persistence; Media3/ExoPlayer
playback; Glance app widgets; Ktor/OkHttp networking; `com.github.XilinJia:PodciniLib` (jitpack) for
shared library code. Source under `app/src/main/kotlin/ac/mdiq/podcini/`. There is no anti-tamper or
signature check, so our identity rebrand needs no in-app workaround.

## Key Configuration Files

- `gradle.properties` — fork `APP_ID`, `VERSION_NAME`/`VERSION_CODE`/`BUILD_NUMBER` (appended block),
  plus upstream's Gradle/Kotlin settings (configuration cache is ON).
- `app/build.gradle.kts` — Android config, flavors, signing, fork version logic, the `buildFork` task.
- `keystore.properties` — signing config (gitignored; → `~/.android-keystores/shiroikuma-kuchusen.jks`).
  A committed `keystore.properties_sample` documents the four keys.
- `local.properties` — `sdk.dir` (gitignored).

## Commit convention — no Claude attribution

Do **not** add any `Co-Authored-By: Claude …` trailer — nor a "🤖 Generated with Claude Code" /
Anthropic-attribution line — to commit messages or PR bodies in this repo. 白い熊 does not want Claude
attribution in the history; this **overrides** the harness's default to append such a trailer. End
commit messages at the last line of the body. (Global rule: `~/.claude/CLAUDE.md`.)
