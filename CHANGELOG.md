# 白い熊 空中線 — changelog

Everything built on top of stock [Podcini.A](https://github.com/XilinJia/Podcini.A).

## 12.5.9+001 (versionCode 970001)

Rebased onto upstream **v12.5.9** (versionCode 97). A pure tracking release — **no new fork
features**; the whole custom layer replayed onto the new base and the build counter reset to `+001`.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **A clean replay.** The only collision was the recurring one in `app/build.gradle.kts`, where
  upstream reasserts the version literals (`97` / `"12.5.9"`); the fork keeps deriving them from
  `forkVersionName` / `forkVersionCode`, with the upstream numbers living in `gradle.properties`.
  Every other commit in the custom layer applied without conflict.
- **Version base moved to 12.5.9 / 97**, so this line's codes (`970001`, `970002`, …) all exceed the
  12.5.8 line's (`960001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.5.9

- **OnlineFeed knows a feed you already have.** The existence check was split in two: a strict
  match on download URL, and a fuzzy match on title, author, domain and description prefix. A feed
  found by URL opens with **Open** and the "Limit episodes" row gives way to the episode count of
  the copy you already hold; a feed recognised by content but living at a *different* URL now
  offers **Update url**, which rewrites the subscription's download URL to the new one and opens
  it — previously the same podcast under a moved URL looked like a stranger and could be subscribed
  twice. The fuzzy match no longer applies only to external-source feeds.
- **Duplicate detection also compares durations.** Marking duplicates as Ignored used to key on
  title-or-URL alone; the candidates are now kept only when both durations are known and within 5 %
  of each other, so two genuinely different episodes sharing a title survive.
- **EpisodeInfo shows the last-played date** above the details when the episode has ever been
  played.
- **Feeds received from another device land in the current volume.** The wifi transceiver's feed
  receiver stopped clearing the frozen flag on arrival and simply assigns the receiving volume.
- **Sharing an unsupported URL now says so.** `ShareReceiverActivity` builds the episode itself and
  writes an explicit Shares-log error ("Can not build episode" / "client is null") instead of
  failing silently into logcat; `addClientEpisode` became the narrower `addToFeed`.
- **Library's language filter resets on feed churn.** The selected-languages preference now follows
  the *size of the known language set* rather than the feed count, and bumps the feeds-filtered
  counter, so adding or removing feeds re-evaluates the filter instead of leaving a stale selection.
- **Media3 1.10.1 → 1.11.0.** Assorted refactoring alongside: `System.currentTimeMillis()` calls in
  the player, sync service and timer dialog moved to the app's own `nowInMillis()`, and
  `EpisodeScreen`'s header was restructured.

## 12.5.8+001 (versionCode 960001)

Rebased onto upstream **v12.5.8** (versionCode 96). A pure tracking release — **no new fork
features**; the whole custom layer replayed onto the new base and the build counter reset to `+001`.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **A clean replay.** The only collision was the recurring one in `app/build.gradle.kts`, where
  upstream reasserts the version literals (`96` / `"12.5.8"`); the fork keeps deriving them from
  `forkVersionName` / `forkVersionCode`, with the upstream numbers living in `gradle.properties`.
  Every other commit in the custom layer applied without conflict.
- **Version base moved to 12.5.8 / 96**, so this line's codes (`960001`, `960002`, …) all exceed the
  12.5.7 line's (`950001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.5.8

- **External audio no longer bypasses the player cache.** Media3Player used to build a second,
  separate `CacheDataSource` chain when playing a stream chosen through the client-side audio-spec
  path, so that media was fetched outside the cache the rest of playback uses. The player now holds
  its single `SegmentSavingDataSourceFactory` as a field and hands that same factory to the
  client-side source, so external audio streams through the shared cache like everything else.
  HTTP transfer and cache-hit logging was added alongside it.
- **Logs screen.** Superseded Shares entries are hidden — the list is now deduplicated by URL, so a
  feed shared repeatedly shows only its most recent entry. In the Downloads details popup a failed
  download now offers **both** actions: **Open**, previously shown only for successes, and **Retry**
  — the old **Redo**, renamed and moved so it appears on failures.
- **OnlineFeed texts are selectable.** The feed description, episode titles and the removal/cancel
  log block sit inside a selection container, so a podcast's details can be copied out by hand.

## 12.5.7+001 (versionCode 950001)

Rebased onto upstream **v12.5.7** (versionCode 95). A pure tracking release — **no new fork
features**; the whole custom layer replayed onto the new base and the build counter reset to `+001`.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **The UI-page shortcut survived upstream's Library rework.** Upstream rewrote a large part of
  `LibraryScreen`; the fork's long-press on the subscriptions screen's top-right menu button — which
  jumps straight to the 白い熊 空中線 UI page, short tap still opening the menu — sits on the same
  icon as before.
- **Version base moved to 12.5.7 / 95**, so this line's codes (`950001`, `950002`, …) all exceed the
  12.5.6 line's (`940001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.5.7

- **Logs screen, further amended.** The mode title is now an icon; Session, Downloads and Shares
  modes gained a switch to show success- or error-only entries; Downloads mode hides past superseded
  items and moves the **Redo** action into the details dialog; the details dialogs were reformatted
  (a plain "Reason:" line replaces the old run-on technical-reason/file-URL block); **Clear logs**
  moved into the menu; and EpisodeInfo opened from the Logs screen no longer carries a Close icon.
- **Persisted enum values no longer ride on `ordinal`.** Enums stored in the database — `ShareLog.Status`,
  the download `RequestType` behind `DownloadResult.feedfileType` — now carry explicit integer codes
  instead of their declaration order, so reordering or inserting a constant upstream can no longer
  silently reinterpret existing rows. `RequestTye` was also corrected to `RequestType`, and
  `ShareType.Podcast` renamed to `Feed`.
- The custom-media-folder warning now says it falls back to the internal media folder rather than
  that it is "currently using" one.

## 12.5.6+002 (versionCode 940002)

Rebased onto upstream **v12.5.6** (versionCode 94) — twelve upstream releases on from the 12.4.10
base — and the fork's build counter switched to zero-padded numbering.

### Fork layer

- **Toasts re-styled onto upstream's new toast stack.** Upstream removed the single-message
  `CustomToast` in favour of `CommonToast`, which queues messages instead of overriding them, shows
  up to three at once, and carries a lock toggle that holds them on screen for up to a minute. The
  house style was ported onto that new composable rather than the deleted one, so toasts keep the
  black background, yellow border and yellow text **and** gain the queueing behaviour. Confirm
  dialogs are likewise queued upstream and shown one at a time, which the fork inherits unchanged.
- **The clean-exit back handler survives the `toastMassege` → `toastMessages` rename.** Back on the
  home screen with nothing left to pop still finishes the activity instead of reporting "No more
  screens back".
- **Build counter is now zero-padded to three digits** — `versionName` reads `12.5.6+002`, and the
  APK it names is `shiroikuma-kuchusen_12.5.6+002_arm64-v8a.apk`. Unpadded counters sort wrongly as
  text (`+10` lands before `+3`), burying the newest build in the middle of a file list; three digits
  holds up to `+999`, which the `versionCode` multiplier already capped it at. The padding is **text
  only** — `versionCode` keeps the plain integer (`94 * 10000 + 2 = 940002`), so upgrades stay
  monotonic across the switch. Builds and releases made before the switch keep their names; nothing
  is retagged or renamed.

### Inherited from upstream 12.4.11 → 12.5.6

- **Stream selection.** A `StreamChanger` popup in PlayerDetailed picks locale, codec and bitrate for
  audio, and codec, resolution and protocol for video; a "Prefer muxed video" feed option falls back
  to the muxed stream, and resolution is shown while playing video.
- **Due media.** A new **Due** mode in the Facets screen collects all Again/Forever media past due, a
  daily reminder surfaces them, auto-download/enqueue no longer queues them, and a "Set due date"
  swipe action was added for Again, Forever and Later media.
- **Logs screen.** Shares-mode logs turn stale Success entries into Missing on click and reopen the
  share dialog from Error/Missing ones; Downloads-mode Success entries show details in a popup;
  "Subscriptions" became "Deletions".
- **OnlineFeed and external apps.** Better handling of feeds shared from other apps (no more
  double-open), prior cancellation logs surfaced, preserved/frozen feeds shown, and PeerTube video
  sources fixed together with PeerPop.
- **Playback.** Player buffer sizing tuned for faster start, force-video/audio-only switching fixed,
  preferred locale/codec/bitrate reset per episode, and a toast about language availability when no
  eligible audio stream exists.
- Assorted fixes: feed-settings URL edits now persist, Library feed-origin filters amended and a
  crash fixed, Search tolerates special characters and records all queries in history.

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
