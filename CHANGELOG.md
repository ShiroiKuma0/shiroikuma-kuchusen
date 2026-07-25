# 白い熊 空中線 — changelog

Everything built on top of stock [Podcini.A](https://github.com/XilinJia/Podcini.A).

## 12.4.10+5 (versionCode 820005)

Implements 白い熊's **保存復元** state-export contract, and turns the category backup into a full restore point.

### Major features

- **Headless, token-gated state export.** An exported broadcast receiver answers two actions —
  `shiroikuma.kuchusen.action.EXPORT_STATE` and `shiroikuma.kuchusen.action.LIST_CATEGORIES` — with no
  Activity and no user interaction, so a sister automation app can back this app up unattended. The
  receiver declares **no `android:permission`** (the caller cannot hold one): the automation token is the
  entire gate, checked on every request together with the master switch, before any work is done.
  - `LIST_CATEGORIES` replies `OK:` plus one `id<TAB>label` line per exportable category, the ids being
    exactly the ones accepted in `items` and exactly the entry names used inside the ZIP.
  - `EXPORT_STATE` accepts `token`, `path` (an absolute directory that overrides the app's own configured
    one, created if missing), `items` (comma-separated category ids; absent or empty means everything),
    `progress_action`, and the `reply_action` / `reply_package` / `reply_id` reply channel. Directory
    precedence is `path` → the configured export directory → `ERROR:no-directory`.
  - The reply is **always a fresh broadcast** carrying `reply_id` and `result`, with
    `FLAG_INCLUDE_STOPPED_PACKAGES` so a backgrounded caller still hears it — never a `ResultReceiver`,
    `PendingIntent` or `Messenger`, and never the ordered-broadcast result alone (it is still set, but only
    when the broadcast really was ordered). Success reads
    `OK:<absolute path>|<bytes>|<human size>|<n> categories`; both numbers are computed by this app, since
    the caller cannot stat the file.
  - **Exactly one terminal reply per request**, guarded by an `AtomicBoolean`, so an async success and a
    synchronous error can never both fire. Distinct errors for `automation disabled`, `bad token`,
    `no-directory`, `no-storage-access` and `unknown category in items: …` — they debug differently.
  - The work runs under `goAsync()` on a background dispatcher, and every reply is written to logcat
    unconditionally as `StateExportReceiver: <reply_id> → <result>`, because the app's own log helpers are
    gated behind a debug preference and would otherwise hide a failing automation run.
- **Progress broadcasts with real numbers, never a percentage** — `Category 3/6 — Colours`,
  `Feeds 120/312`, `Database 24.0 MB / 61.3 MB` — each carrying the structured `current` / `total` / `unit`
  extras alongside the display line and the app's label, throttled to at most one every 500 ms with a final
  one always sent at completion. The Export/Import panel shows the same progress for manual exports.
- **The database is now part of the backup.** A new **Database** category writes a transactionally
  consistent, compacted snapshot of the Realm database — episodes, play positions, queues, ratings —
  taken with `writeCopyTo` rather than a byte copy of the live file, which could catch a half-written
  transaction. One request still produces **exactly one ZIP**: never a second file, never a split by
  category, never a companion `.json` next to the archive.
  - On import the database entry is **streamed to disk, never through memory** (it can be hundreds of MB),
    and applied **last**, after every other category has finished writing through the database it replaces.
  - A restored database offers only **Restart now** — "Later" would leave the app running against a file it
    no longer has open.
- **Automation controls inside the Export/Import section** of the 白い熊 空中線 UI page, directly below the
  existing export rows (never a section of their own): a **master switch, off by default**, and a **token
  row** showing the token abbreviated (`80922d8c…4c49a87c`), copying the full value to the clipboard on tap
  with a confirmation toast, and carrying a **Regenerate** action that warns pasted copies must be updated.
- **Automation token infrastructure**: 24 random bytes from `SecureRandom`, hex-encoded, generated lazily on
  first read so the row always shows a value, and compared **constant-time** — a length-or-prefix leak is
  enough to walk a token out. It lives in its own device-local preferences file that no export category
  touches, so the token can never travel inside a backup ZIP.

### Fixes & behavior

- **Family backup file-name convention** (白い熊, 2026-07-25): every backup this app writes — from the
  automation path *and* from the Export/Import panel — is now named
  `shiroikuma-kuchusen_<yyyy-MM-dd_HH-mm-ss>.zip`, with no version, no `-export` infix and no other
  decoration, so all apps' backups sort and read uniformly in one shared directory. The "last export"
  lookup still recognises the previous `shiroikuma-kuchusen-export_…` names.
- The export manifest now also records the fork's `appVersion`.
- The database entry is deflated at `BEST_SPEED`: a realm file is large and already compact, so the time
  saved matters more than the ratio.

### Packaging & identity

- Declares **`MANAGE_EXTERNAL_STORAGE`**, so an automation run can honour the directory its caller names.
  Without the grant the `path` extra is ignored in favour of the configured SAF directory (and only fails
  with `no-storage-access` when there is none); a warning row under the token opens the grant screen while
  the permission is missing.

## 12.4.10+4 (versionCode 820004)

Rebased onto upstream **v12.4.10** (versionCode 82) — the fork layer replayed from the 12.1.7 line.

### Major features

- **Export / Import (settings backup)** as the first section of the 白い熊 空中線 UI page:
  - A **settable export directory** (SAF folder picker; stored device-locally and deliberately never exported, since a foreign device holds no permission for the URI). With no directory set, export falls back to a save-as picker.
  - The directory is **queried every time the page opens** for the newest export; the row summary and the panel status line answer "Last export: 2026-07-25 10:44:12", or warn when no directory or no export exists yet.
  - **Category checkboxes, all preselected**, with a Select-all master: **Subscribed feeds** (first), **Colours**, **Typography & fonts**, **Shape & borders**, **App settings** — the whole settable surface of the app, split logically.
  - The export file is a **ZIP of plain per-category JSON files** — type-tagged values, no binary blobs — plus imported font files under `fonts/` and a `manifest.json` (format, version, app id, timestamp, categories). Filename is datetime-stamped: `shiroikuma-kuchusen-export_YYYY-MM-DD_HH-mm-ss.zip`.
  - **Import merges, never clears**: categories are independent files, absent ones are skipped, unknown keys are tolerated. Feeds are matched by URL — already-subscribed ones are skipped and counted; new ones are subscribed with a full refresh. UI settings apply live on import (no restart needed to see them); app settings are written into the Realm-backed preferences; fonts are restored (basename-only, no path traversal).
  - The **subscribed-feeds category** exports title/xmlUrl/htmlUrl/type per feed (synthetic feeds excluded) — OPML-equivalent, as JSON.
  - The **app-settings category** covers the upstream preferences surface by explicit property references, with credentials (proxy password) and device-local values (folder/ringtone URIs, runtime state, timestamps) excluded.
- **Success dialogs & the auto-close chain**: successful export shows a black, yellow-bordered "✓ Export finished" dialog; import shows "✓ Import finished" with **Later** / **Restart now** (Restart relaunches the app). Acknowledging success (OK / Later) closes the whole chain — the info dialog, the Export/Import panel beneath it, and the UI settings page down to the app's main screen, with the settings backstack reset. Failures ("Export failed: …", "Import failed: …", "No categories selected.") show as red inline status and leave the panel open for a retry.
- **ArcaneChat-style action row** in the panel: round-pill outline buttons — Cancel alone on the left, Import and Export grouped on the right; pills disable and dim while an export/import runs.

### UI & theming

- **白い熊 空中線 UI page** (top of Settings): live whole-app theming — foundation colours (background, text, secondary text, accent, border) with an RGBA colour picker, live preview swatch and one-click recent colours; font family with external `.ttf`/`.otf` import (each option rendered in its own glyphs); font size (50–200 %) and weight (100–900) sliders; corner roundness and border thickness sliders bottoming out at 0. Every change re-skins the whole app instantly via a Compose-state-backed store; one tap resets to the house style.
- **kxkb-style page look**: 20 sp bold accent section headings underlined exactly as wide as their text (2.5 dp bar), hairline full-width separators between sections, and a matching 17 sp sub-heading style (1.5 dp underline).
- **Black menus and dialog surfaces**: all Material `surfaceContainer*` colour roles follow the house background, so dropdown menus (e.g. the subscriptions screen's overflow menu) and dialogs render on true black instead of Material grey.
- **Quick access to the UI page**: long-press the drawer's settings cog, or the subscriptions screen's top-right menu button (a short tap still opens the menu).
- **Toasts in house style**: black background, yellow text, yellow border, instead of a filled accent background.
- **Drawer "moon" icons** (next to Settings and the bottom banner) use the fork's yellow traced glyph instead of the upstream colour artwork.
- The default look out of the box is the **black-yellow house style** — baked into the defaults, no seeding step.

### Fixes & behavior

- Back on the home screen with nothing left to pop now **finishes the activity** to exit cleanly, instead of showing a "No more screens back" toast and sticking.
- Removed the upstream "unofficial version / GPL" startup notice.

### Packaging & identity

- App id **`shiroikuma.kuchusen`** (installs side-by-side with upstream Podcini.A), launcher label **白い熊 空中線**, provider authority `shiroikuma.kuchusen.provider`; the code namespace stays `ac.mdiq.podcini`.
- **Launcher icon**: the upstream glyph re-traced as pure-yellow (`#FFFF00`) line art on black — adaptive icon with monochrome layer, safe-zone fitted; the splash screen retraced to match.
- **Fork versioning**: `versionName = <upstream>+<build>` (`12.4.10+4`), `versionCode = <upstream>*10000 + build` (`820004`) — upgrades stay monotonic across upstream rebases.
- Signed release builds via the fork's own keystore; `buildFork` Gradle task assembles the arm64-v8a `free` flavour APK and auto-increments the build counter.
