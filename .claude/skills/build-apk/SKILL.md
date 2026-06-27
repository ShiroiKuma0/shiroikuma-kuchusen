---
name: build-apk
description: Build the signed free release APK with the buildFork Gradle task, then deliver it automatically via the global /after-build skill (adb push if a phone is connected, else scp to skhw — no prompt). Always build first without asking for permission to build. Use whenever 白い熊 asks to build the app, build the APK, make a release build, or build and send to the phone (白い熊 空中線 / shiroikuma-kuchusen / Podcini fork).
---

# Build the free release APK and optionally send to phone

> **Never ask whether to build — just build.** When this skill applies (白い熊 asked
> to build, or you've made changes that are ready to test), run the build immediately.
> Do **not** ask "shall I build?" / "want me to run buildFork?" — that question is
> wrong. There is **no** transfer question either: after a successful build, deliver the
> APK automatically via the global **`/after-build`** skill (see below). So: always
> build, *then* let `/after-build` deliver — no prompts at all.

> **The push destination is ALWAYS `/sdcard/tmp/`.** Every `adb push` of the APK goes
> to `/sdcard/tmp/<apk name>` — **never** `/sdcard/Download/` or anywhere else. Create
> `/sdcard/tmp` if needed and push there.

> **Never run `adb install` (or `pm install`).** The build step may copy the APK to the
> phone with `adb push` — and only after confirming with 白い熊 — but **白い熊 installs
> the APK themselves** from the phone's file manager. Do not install it for them.

> **adb runs UNSANDBOXED.** Every `adb` invocation uses `dangerouslyDisableSandbox: true`
> (global hard rule in `~/.claude/CLAUDE.md`). The sandbox blocks adb's server socket.

> **Never `git commit` or `git push` on your own.** Building does not include committing.
> After building (and the optional `adb push`), 白い熊 tests the build themselves. **Only
> when 白い熊 explicitly says "Push"** do you then `git commit` the changes and
> `git push origin custom`. 白い熊's **"Push"** means *commit-and-push-to-the-fork* — it
> is unrelated to the `adb push` file copy.

> **ALWAYS end every build by delivering the APK via the global `/after-build`
> skill — never ask how to transfer it.** Mandatory for *every* successful build, even
> verification builds and even when 白い熊 didn't mention transferring. `/after-build`
> runs `/adb-check` UNSANDBOXED, then `/adb-push` to `/sdcard/tmp/` if a phone is
> connected, otherwise `/scp` to `skhw:~/tmp/`, and announces the filename. Do **not**
> ask "scp or adb push?" / "phone connected?" — invoke `/after-build` and let it decide.

## Steps

1. **Note the output filename.** Read the current version and build number:
   - `grep -E 'VERSION_NAME|VERSION_CODE|BUILD_NUMBER' gradle.properties`
   - The APK will be `shiroikuma-kuchusen_<VERSION_NAME>+<BUILD_NUMBER>_arm64-v8a.apk`,
     using the `BUILD_NUMBER` value **before** the build (the task bumps it afterward).
   - versionCode for that build = `VERSION_CODE * 10000 + BUILD_NUMBER`.

2. **Build** (needs JDK 21 — the default `java` on this machine is JDK 11):
   - `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew buildFork < /dev/null`
     (the `< /dev/null` guarantees it never blocks on stdin).
   - This runs `assembleFreeRelease`, copies the **signed arm64-v8a** APK to
     `~/tmp/<apk name>`, and auto-increments `BUILD_NUMBER` in `gradle.properties`.
   - The task prints `>>> <path>` and `>>> versionCode <n>`; use those to confirm the
     exact filename and code, and confirm `BUILD SUCCESSFUL`.
   - First build on a fresh checkout downloads the AGP / Compose / Media3 / krdb deps and
     `com.github.XilinJia:PodciniLib` from jitpack — slow once, then cached.

3. **At the end of every build, deliver the APK via `/after-build`** — no exceptions,
   no asking. As soon as the build reports `BUILD SUCCESSFUL` and the signed APK is in
   `~/tmp/`, invoke the global **`/after-build`** skill; it picks adb-push (phone
   connected) or scp-to-skhw on its own and announces what landed.

4. **What `/after-build` does** (for reference — you don't run these by hand):
   `/adb-check` lists devices UNSANDBOXED; if a phone is connected, `/adb-push` copies
   the newest `~/tmp/*.apk` to `/sdcard/tmp/`; otherwise `/scp` copies it to
   `skhw:~/tmp/`. It never runs `adb install` — 白い熊 installs manually from `/sdcard/tmp/`.

## Signing

Release signing is non-interactive: `app/build.gradle.kts` reads credentials from a
gitignored `keystore.properties` at the repo root (falling back to the upstream
`releaseStoreFile`/`releaseStorePassword`/`releaseKeyAlias`/`releaseKeyPassword` project
properties when that file is absent). This fork's own keystore is
`~/.android-keystores/shiroikuma-kuchusen.jks` (alias `kuchusen`); the password is in
`~/〇/[666] 私資料/[666][27] 暗号/android-keystores.org`. If neither `keystore.properties`
nor the fallback properties are present the build signs with placeholder values and the
APK will not install.

## Notes

- **Flavor is `free`** (no Google Play Services; uses Conscrypt — correct for the
  microG/no-GMS phone). The `play` flavor (GMS + Chromecast) is not what we ship.
- The APK is the **arm64-v8a** split. The build also produces a `universal` APK and an
  `exported-apks/` copy (upstream's export task) — ignore those; `buildFork` picks the
  arm64-v8a one and copies it to `~/tmp` under our filename.
- **No patched library is needed.** Unlike the Fossify forks, Podcini has no anti-tamper
  / signature check, so a plain identity rebrand (app id + label + provider authority)
  builds and runs clean. `com.github.XilinJia:PodciniLib` resolves from jitpack as-is.
- SDK location is the gitignored `local.properties` → `/home/shiroikuma/android-sdk`
  (has android-37 platform, build-tools 37.0.0, NDK 29.0.14206865 — all required here).

---

**Commit convention — no Claude attribution.** Never add a `Co-Authored-By: Claude …` / "Generated with Claude" trailer to commit messages or PR bodies; end the message at the last line of the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
