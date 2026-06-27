---
name: upstream-new-version
description: Rebase our fork onto a new upstream release of XilinJia/Podcini.A. Use when 白い熊 says a new upstream version is out, asks to update/sync to upstream, bump to the new Podcini.A release, or rebase custom onto the latest upstream. Fast-forwards the main mirror, replays our custom layer, resets the version base + build counter, and builds the new +1 via the build-apk skill.
---

# Rebase the fork onto a new upstream release

This codifies the "new upstream version" half of the fork workflow. The goal: move `main` to the
new upstream release, replay our `custom` customizations on top of it, and produce a fresh `+1` build.

> **Never `git push` or `git commit` unprompted, and never `adb install`.** Same hard rules as
> everyday development (see CLAUDE.md). After the rebase + build you stop and let 白い熊 test; you
> only `git push` when they explicitly say **"Push"**.

## Background — how versioning works here

- `VERSION_NAME` / `VERSION_CODE` in `gradle.properties` **track upstream** (XilinJia/Podcini.A).
- `BUILD_NUMBER` is **our** fork increment. It **resets to `1`** on each new upstream version.
- Fork `versionName` = `"<VERSION_NAME>+<BUILD_NUMBER>"`, `versionCode` = `VERSION_CODE * 10000 + BUILD_NUMBER`.
- `app/build.gradle.kts` derives `forkVersionName` / `forkVersionCode` from those three and exposes
  the `buildFork` task. The code namespace stays `ac.mdiq.podcini`; only the installed `APP_ID`
  (`shiroikuma.kuchusen`) and the label differ.

So when upstream's `versionCode` climbs (e.g. 60 → 61), our fork's codes for the new line
(`610001`, `610002`, …) all exceed the previous line's (`600001`, …), keeping upgrades monotonic.

There is **no patched dependency** to re-cut (unlike the Fossify forks' Commons). Podcini has no
anti-tamper, and `com.github.XilinJia:PodciniLib` is consumed straight from jitpack.

## Steps

1. **Fetch upstream:**
   - `git fetch upstream --tags`
   - Identify the new release: `git for-each-ref --sort=-creatordate --format='%(creatordate:short) %(refname:short)' refs/tags | head`,
     or check `upstream/main`. Confirm the new `versionCode` / `versionName` from upstream's
     `app/build.gradle.kts` at that point:
     `git show upstream/main:app/build.gradle.kts | grep -E 'versionCode =|versionName ='`
     (we track upstream `main`, like the memo/messeji forks; `main` HEAD is usually the latest tag
     plus at most a couple of non-version commits).

2. **Advance `main` to the new upstream release** (it mirrors upstream, no fork work lives there):
   - `git checkout main`
   - `git merge --ff-only upstream/main` (if it can't fast-forward, upstream rewrote history — STOP
     and discuss; do not force).

3. **Rebase `custom` onto the new `main`:**
   - `git checkout custom`
   - **Back up first:** `git branch custom-pre-<newver> custom` (e.g. `custom-pre-12.1.8`).
   - `git rebase main`
   - Resolve conflicts so **all** our customizations survive (see the table below). The conflict-prone
     files are `app/build.gradle.kts` (version math, applicationId, app_name/provider_authority
     resValue, signingConfig, the `buildFork` task) and `gradle.properties` (our appended fork block).
     If the rebase is large or a file we customize was refactored upstream, **STOP and plan with
     白い熊** rather than improvising — surface the conflicting commits + options.

4. **Update versioning in `gradle.properties`:**
   - Set `VERSION_NAME` / `VERSION_CODE` to the **new upstream** values.
   - **Reset `BUILD_NUMBER` to `1`.**

5. **Verify our customizations are intact** (after resolving the rebase):

   | What | Expected value | Where |
   | --- | --- | --- |
   | Installed app ID | `shiroikuma.kuchusen` | `gradle.properties` → `APP_ID`; `applicationId = project.property("APP_ID")…` in `app/build.gradle.kts` |
   | Code namespace | `ac.mdiq.podcini` (unchanged from upstream) | `app/build.gradle.kts` → `namespace` |
   | App launcher label | `白い熊 空中線` | `resValue("string", "app_name", …)` in the `release` build type (+ `… Debug` in `debug`) |
   | Provider authority | `${project.property("APP_ID")}.provider` (matches `${applicationId}.provider` in the manifest — used by `getUriForFile` in code) | `resValue("string", "provider_authority", …)` |
   | Signing | gitignored `keystore.properties` → `~/.android-keystores/shiroikuma-kuchusen.jks` (alias `kuchusen`) | `signingConfigs.releaseConfig` |
   | Fork version logic | `forkVersionName` / `forkVersionCode` + the `buildFork` task | `app/build.gradle.kts` |
   | Icon | yellow line-art on black adaptive icon (`mipmap-anydpi-v26/ic_launcher.xml` → `@android:color/black` + `ic_launcher_sk_foreground` + monochrome) | `app/src/main/res/…` |

   Sanity check the build script still evaluates:
   `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:tasks --group build` should list
   `buildFork` and report `BUILD SUCCESSFUL`.

6. **Build the new `+1`** via the **build-apk** skill
   (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew buildFork < /dev/null`); build-apk then
   delivers the APK automatically via `/after-build` (adb push if a phone is connected, else scp to
   skhw — no prompt). This is the first build of the new upstream line (`<newVersion>+1`).

7. **Stop.** Let 白い熊 test. Commit/push only on their explicit **"Push"** (force-push needed for
   `custom` since rebasing rewrites history: `git push --force-with-lease origin custom`; `main` is a
   fast-forward: `git push origin main`). Once `custom` is pushed and confirmed, the
   `custom-pre-<newver>` backup branch can be deleted (ask first).

## Notes

- Keep our changes a **small, legible layer** on top of upstream — prefer rebasing (linear history)
  over merging, so the customization set stays easy to audit and replay.
- If upstream restructures a file we customize, port our change to the new structure rather than
  forcing the old diff.
- `main` is **fast-forward only**; it never carries our work. `custom` is the development branch.

---

**Commit convention — no Claude attribution.** Never add a `Co-Authored-By: Claude …` / "Generated with Claude" trailer to commit messages or PR bodies; end the message at the last line of the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
