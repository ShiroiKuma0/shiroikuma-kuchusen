# 白い熊 空中線 — changelog

Everything built on top of stock [Podcini.A](https://github.com/XilinJia/Podcini.A).

## 12.7.1+001 (versionCode 1060001)

Rebased onto upstream **v12.7.1** (versionCode 106), taking in **two** upstream releases at once —
12.7.0 arrived on 2026-08-18 and 12.7.1 superseded it two days later, so this fork line skips
straight to the newer one. A pure tracking release: **no new fork features**, the whole custom layer
replayed onto the new base and the build counter reset to `+001`.

> **No export needed.** The Realm schema stays at 158 — neither upstream release changes a stored
> shape. The one-way migration warning belongs to `12.6.0+001`; if you are coming from a 12.5.x
> build it still applies, so export from the UI page first.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **One collision, the recurring one.** `app/build.gradle.kts`, where upstream reasserts the version
  literals (`106` / `"12.7.1"`) while the fork keeps deriving them from `forkVersionName` /
  `forkVersionCode`. All twenty-nine commits of the custom layer applied otherwise clean — including
  `Composables.kt`, the one file both sides touch this time: upstream rewrote the toast popup's
  header row directly beneath the border and padding the house style adds to that popup, and the two
  merged without a conflict.
- **Version base moved to 12.7.1 / 106**, so this line's codes (`1060001`, `1060002`, …) all exceed
  the 12.6.3 line's (`1040001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.7.0 and 12.7.1

- **A second, slower Apple search that finds feeds the normal one hides.** "Deep search Apple
  Podcasts" joins the advanced search list: when Apple's index returns a podcast with no feed URL
  attached, it opens that podcast's Apple page and digs the real feed address out of the page
  itself. It is deliberately excluded from "Search all sources" — it costs a web fetch per hidden
  result, so it is there when the ordinary Apple search comes up short, not on every query.
- **Search results survive leaving the screen.** The find-feeds list is no longer thrown away and
  re-run when you navigate away and come back; the previous results are still there. A count of how
  many were found now sits at the top of the screen.
- **The top chart honours the country you picked, on the first load.** Opening the chart could fetch
  the list for the wrong country because the request was assembled before the stored country code
  was read; the country is now passed into the request explicitly. The country picker also stops
  refiltering on every keystroke — it waits half a second after you stop typing.
- **Subscribing and immediately leaving no longer errors.** The post-subscribe bookkeeping ran on the
  UI thread while the screen was already tearing down; it moved to a background scope.
- **Timers can only be set to the future.** Both the add- and edit-timer dialogs now check the time
  you entered and, if it has already passed, show a red "Timer can only be set to the future" line
  and stay open instead of silently accepting a timer that would never fire. A failed edit reports
  itself the same way rather than disappearing into the log, and deleting a timer closes the dialog.
- **The Timers view no longer crashes while clearing old timers.** Auto-removing expired timers from
  the Facets screen removed embedded records by identity against a list of live references, which
  could take the screen down; it is a single filtered removal now.
- **A timer firing on a sleeping device waits for its source app.** The alarm receiver was rewritten
  as a timer receiver: when external source providers are enabled it now waits for them to bind, and
  gives playback a five-second grace period, before starting the episode.
- **A timer no longer steals audio focus.** Audio focus is requested only for playback you started by
  tapping a play button, so an episode started by a timer does not interrupt whatever else is
  playing.
- **Playback start waits for the player instead of poking the service.** Starting an episode before
  the media controller has connected used to kick the playback service and hope; it now waits for the
  controller to actually report connected, then starts. Player settings that were applied before the
  player existed are applied inside the start path instead.
- **A player stuck on the same position gives up.** If the saved position does not move for ten
  consecutive checks, playback is paused rather than left spinning.
- **An open/close loop in the cache reader is fixed** — the cached-media reader closed a connection it
  was about to reuse, which could spin indefinitely.
- **Progress-save timing corrected again.** The 2%-of-duration interval is now computed without
  integer-dividing first, and the value set when you change speed is no longer overwritten back to
  the minimum every time playback starts.
- **Todos are reachable from the episode action list again.** "Add Todo" is back as an action, with
  its own icon, opening the todo editor directly. The editor itself is tidier — the notes and
  due-date fields open by tapping their labels instead of ticking checkboxes, the notes box grows to
  eight lines and opens by itself when a todo already has one — and an edited todo now actually
  refreshes on screen, where before an edit that kept the same number of todos went unnoticed.
  Episode details shows a todo's note next to its due date.
- **The toast popup gains a close button** beside the existing hold-to-keep lock, so a stack of
  messages can be dismissed at once.
- **Faster, less repetitive startup.** Cache and `.nomedia` setup collapse into one background job,
  the download manager is built on first use instead of on every launch, and first-launch work —
  version bookkeeping, sync-service wiring, the one-off feed update — is skipped when the app is
  merely being recreated after a rotation or theme change.
- **A failed Conscrypt install no longer takes startup with it.** On the GMS-free build the bundled
  TLS provider now falls back to the system provider with a log line instead of throwing.
- **Feed search results show more.** Every result now carries an author line ("Anonymous" when the
  feed gives none) and an episode count, and the feed URL is no longer truncated to one line.
- **No schema change.** Realm stays at 158. Dependencies: OkHttp 5.4.0 → 5.5.0, and Compose
  `foundation-layout` 1.12.0 is pulled in explicitly.

## 12.6.3+001 (versionCode 1040001)

Rebased onto upstream **v12.6.3** (versionCode 104), a single commit on top of 12.6.2. A pure
tracking release: **no new fork features**, the whole custom layer replayed onto the new base and
the build counter reset to `+001`.

> **No export needed.** The Realm schema stays at 158 — upstream 12.6.3 touches only playback and
> the external-source handshake, no stored shape. The one-way migration warning belongs to
> `12.6.0+001`; if you are coming from a 12.5.x build it still applies, so export from the UI page
> first.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **A clean replay.** Two collisions, both cosmetic: the recurring one in `app/build.gradle.kts`,
  where upstream reasserts the version literals (`104` / `"12.6.3"`) while the fork keeps deriving
  them from `forkVersionName` / `forkVersionCode`; and `README.md`, where upstream reworded a
  feature-list item in the section our README replaces wholesale. All twenty-seven commits of the
  custom layer applied without conflict otherwise — none of the six source files upstream touched
  overlaps anything the fork patches.
- **Version base moved to 12.6.3 / 104**, so this line's codes (`1040001`, `1040002`, …) all exceed
  the 12.6.2 line's (`1030001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.6.3

- **Media that took seconds to start should start promptly now.** MP3 index seeking is switched off,
  so opening a variable-bitrate file no longer scans the whole thing to build an exact seek index
  before the first sound — upstream names this the likely cause of the startup delays reported on
  some media. The trade-off: seeking inside a VBR file now uses the constant-bitrate approximation,
  so a jump can land a little off where an exact index would have put it.
- **Less buffering before playback begins at high speed.** The amount buffered before starting was
  multiplied by the playback speed; it is now a flat figure, so listening at 2× no longer waits for
  twice the audio to arrive first. The rebuffering and maximum-buffer targets still scale with speed.
- **Progress saving keeps up with the speed you actually listen at.** With adaptive progress updates
  on, the position is now saved every 2% of the media's duration *divided by the playback speed*, and
  the interval is recomputed the moment you change speed — the running saver picks the new value up
  on its next tick instead of keeping the one it started with. The old code took 2% of duration and
  ignored speed entirely, so at 2× it saved half as often as intended.
- **The buffer bar starts empty on a new episode.** Switching media left the previous episode's
  buffer fill on screen until the new one caught up; it is reset to zero now.
- **Shared links that only an external source app can open no longer fall through.** Sharing a URL
  now waits for the external-source providers to finish binding before asking which of them can
  handle it — previously the search could run before they were ready, and the link was rejected.
  Feed refreshes give a freshly bound provider an extra second before querying it.
- **A silent source provider can no longer hang what is waiting for it.** Waiting on the provider
  registry now gives up after ten seconds and carries on with no external clients, instead of
  blocking indefinitely on a provider app that never reports itself ready.
- **No schema or dependency changes.** Realm stays at 158; nothing in the dependency set moves.

## 12.6.2+001 (versionCode 1030001)

Rebased onto upstream **v12.6.2** (versionCode 103), a single commit on top of 12.6.1. A pure
tracking release: **no new fork features**, the whole custom layer replayed onto the new base and
the build counter reset to `+001`.

> **No export needed.** The Realm schema moves 157 → 158, but nothing structural changes — the
> deletion log's *key* changes meaning, not its shape, so there is no migration step and no data
> loss. The one-way migration warning belongs to `12.6.0+001`; if you are coming from a 12.5.x build
> it still applies, so export from the UI page first.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **A clean replay.** The only collision was the recurring one in `app/build.gradle.kts`, where
  upstream reasserts the version literals (`103` / `"12.6.2"`); the fork keeps deriving them from
  `forkVersionName` / `forkVersionCode`, with the upstream numbers living in `gradle.properties`.
  All twenty-five commits of the custom layer applied without conflict otherwise — none of the
  twelve files upstream touched overlaps anything the fork patches.
- **Version base moved to 12.6.2 / 103**, so this line's codes (`1030001`, `1030002`, …) all exceed
  the 12.6.1 line's (`1020001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.6.2

- **The 30-minute comment window finally works.** The previous release's threshold was written as a
  millisecond delta converted with `Duration.minutes`, so the intended 30 minutes was really 30 ms
  and a fresh date header was stamped on nearly every edit — the bug flagged when `12.6.1+001` was
  cut. It now converts with `.milliseconds`, and the clock is actually advanced on every addition
  instead of only when a header was written, so consecutive comments really do group under one time
  stamp. The editing dialog uses the same rule, so what it shows you matches what gets saved.
- **Deletion logs are keyed to what was deleted.** The log's primary key was `itemId / 100`, a
  leftover from when feed and episode ids were minted as `nowInMillis() * 100`; ids have long since
  been plain milliseconds, so the division produced a key that pointed at nothing. It is now the
  deleted feed's or episode's own id, and erasing an episode no longer overwrites that key with an
  unrelated fresh id. Logs written by earlier builds keep their old broken keys — they are still
  listed, they just will not match a feed by id; anything deleted from this build on will.
- **A feed's own removal history now shows on its details page.** If a feed you are looking at was
  removed before — matched by id, title, download URL or the opening of its description — the page
  shows a **"likely removed"** block above the description carrying each past log's comment, rating,
  description and removal date. It is the same block the online-feed page has always shown, and that
  one has been reordered to match it.
- **The Deletions log is sorted by when things were deleted.** It sorted by id, which — now that the
  id is the deleted item's creation time — would have ordered the list by when you *added* a feed.
  It now sorts by cancellation date, newest first.
- **Unticking "save important episodes" no longer runs the query anyway.** Removing a feed only
  evaluates `worthyEpisodes` when the box is actually ticked.
- **Housekeeping.** `queryIntentServicesCompat` moves inside `AppGatewayRegistry` as a private
  extension instead of sitting at file scope, the Deletions dialog drops a placeholder empty log in
  favour of a nullable one, both `addComment` call sites pass `addition` by name, and the `Episode`
  / `Feed` id comments are corrected to say ids increment from `nowInMillis()` rather than
  `nowInMillis() * 100`. No dependency changes.

## 12.6.1+001 (versionCode 1020001)

Rebased onto upstream **v12.6.1** (versionCode 102) — the first *finished* 12.6 tag, where the
previous release sat on the `v12.6.0-pre1` pre-release. A pure tracking release: **no new fork
features**, the whole custom layer replayed onto the new base and the build counter reset to `+001`.

> **No new migration here.** The one-way Realm migration to schema 157 landed in `12.6.0+001`; if
> you are coming from a 12.5.x build it still applies, so export from the UI page first. Upgrading
> from `12.6.0+001` needs nothing.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **A clean replay.** The only collision was the recurring one in `app/build.gradle.kts`, where
  upstream reasserts the version literals (`102` / `"12.6.1"`); the fork keeps deriving them from
  `forkVersionName` / `forkVersionCode`, with the upstream numbers living in `gradle.properties`.
  All twenty-three commits of the custom layer applied without conflict otherwise — none of the
  files the fork touches overlap with upstream's gateway rework.
- **Version base moved to 12.6.1 / 102**, so this line's codes (`1020001`, `1020002`, …) all exceed
  the 12.6.0 line's (`1010001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.6.1

- **The external-source gateway now waits until it is actually connected.** The loose
  `discoverSources()` / `getSourceClients()` pair is replaced by an `AppGatewayRegistry` object that
  holds a real state machine — `Initializing` / `Ready(clients)` / `Failed(error)` as a `StateFlow`,
  a `CompletableDeferred` behind a mutex, and an initialization latch. Scheduled feed updates call
  `awaitReadyClients()` before they start, so a refresh no longer runs against gateways that have
  not finished binding. **This is the fix the release is named for**: external clients that were
  "possibly not ready when updating feeds".
- **Binding is no longer fire-and-forget.** Each service is bound inside a cancellable coroutine
  that resumes on connect — or with `null` from a died binding, a null binding, or a failed
  `bindService` — and is retried once before it is given up on. A client that is slow to come up
  gets a second chance instead of silently dropping off the list, and cancellation unbinds cleanly.
- **External feeds are logged and updated under the right id.** `refreshFeed` now stamps the
  original feed's `id` and `title` onto the feed it builds from an external client's response, so
  the update lands on the right subscription and the log line names it correctly.
- **Superseded log entries stay out of the Logs screen.** The Shares and Downloads lists convert the
  Realm results to a plain list before de-duplicating, which makes `distinctBy` actually drop the
  older duplicates.
- **The feed Info view says something useful.** The languages line is gone; **parent volume** and
  **associated queue** take its place, the author falls back to "Anonymous" when blank as well as
  when missing, and the block loses its ragged left indent.
- **Episode comments group under one time stamp.** Adding a comment shortly after the previous one
  appends to the running block instead of stamping a fresh date header. Note upstream's threshold is
  written as a millisecond delta converted with `Duration.minutes`, so the intended 30 minutes is
  really 30 ms and the header still appears nearly every time — their bug, left as they wrote it.
- **Housekeeping.** `createChannels()` → `createNotificationChannels()`, `EPISODES_LIMIT` moves onto
  `Feed`'s companion, the "use external apps" switch writes its updated prefs back instead of
  discarding them, and `ConfigAutoDLEQ` is hoisted a level up in `FeedsSettingsScreen` (a move, not
  a rewrite — it accounts for most of that file's churn).
- **Dependencies.** Compose BOM `2026.06.01` → `2026.08.00` with the separately pinned material3
  `1.4.0` dropped in its favour, webkit `1.16.0` → `1.17.0`, navigation3 runtime/ui `1.1.5` →
  `1.1.6`, debug ui-tooling `1.11.4` → `1.12.0`.

## 12.6.0+001 (versionCode 1010001)

Rebased onto upstream **v12.6.0-pre1** (versionCode 101), folding in **v12.5.12** (100) as well —
that line was built as `12.5.12+001` but never published, so its upstream changes arrive here. A
pure tracking release — **no new fork features**; the whole custom layer replayed onto the new base
and the build counter reset to `+001`.

> **Two things to know before installing.** Upstream has so far tagged 12.6.0 only as a
> **pre-release** (`v12.6.0-pre`, `v12.6.0-pre1`); the last finished upstream tag is `v12.5.12`.
> And 12.6.0 runs a **one-way Realm migration** on first launch (schema 156 → 157), moving each
> feed's auto-download/enqueue configuration into a new embedded model. Going back to a 12.5.x
> build afterwards would mean wiping app data — export from the UI page first.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **A clean replay.** The only collision was the recurring one in `app/build.gradle.kts`, where
  upstream reasserts the version literals (`101` / `"12.6.0"`); the fork keeps deriving them from
  `forkVersionName` / `forkVersionCode`, with the upstream numbers living in `gradle.properties`.
  Every other commit in the custom layer applied without conflict — none of the files the fork
  touches overlap with upstream's auto-download rework.
- **Version base moved to 12.6.0 / 101**, so this line's codes (`1010001`, `1010002`, …) all exceed
  the 12.5.x lines' (`1000001`, `990001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.5.12

- **The Facets filter is fixed once more.** `FacetsVM` stops caching the filter in a `mutableStateOf`,
  reading it back from `facetsPrefs` on each access and reassigning `facetsPrefs` on write, with a
  separate `filterChanged` counter driving the episodes flow — `snapshotFlow` now watches
  `(facetsMode, filterChanged, sortOrder)` instead of the filter object, whose identity never changed
  when its contents did. The screen's own duplicate write into `filtersMap` goes with it.
- **Skipped and Passed behave like Played.** Setting episodes to either by hand now runs the same
  tail — auto-delete when the feed allows it, removal from queues per `deleteRemovesFromQueue` /
  `removeFromQueueMarkPlayed`, and a gpodder PLAY action when a provider is connected — and the
  ignore-episodes dialog gained that same auto-delete-and-dequeue pass. The Played branch drops
  `wifiSyncEnabledKey` from its sync condition, leaving `isProviderConnected` alone to gate the
  enqueue.
- **A new episode is no longer force-Ignored by an old duplicate.** That branch in
  `checkAndMarkDuplicates` is commented out; duplicates are still cross-linked as related, and the
  function now always reports updated.
- **Simplified Chinese integrated** — 374 lines of `values-zh-rCN`, the bulk of that release's diff.

### Inherited from upstream 12.6.0

- **Auto-download/enqueue configuration moved into its own DB model.** Everything a feed used to
  carry inline — the episode filter (`filterStringADL`, duration floor/ceiling), the sort order, the
  include/exclude/min-duration/max-duration/mark-excluded-played group and the policy code — now
  lives in an embedded `AutoDLEQ` object, and `Feed` holds a list of them. `Feed` keeps only
  `autoDownload`, `autoEnqueue`, `autoDLSoon`, `autoDLMaxEpisodes` and `countingPlayed`.
  `Feed.AutoDownloadPolicy` became a top-level `AutoDLEQPolicy`, and `FeedAutoDownloadFilter` became
  `FeedAutoDLEQFilter`. **Schema 157 migrates existing feeds** into one `AutoDLEQ` each, so per-feed
  setups survive the upgrade.
- **A second algorithm, interlaced at random.** Feed settings gained an **Enable second algorithm**
  switch that adds an independent second configuration — its own policy, filter, sort and
  include/exclude rules. The auto-download routine runs both and **interlaces the two candidate
  lists pairwise at random** rather than concatenating them, so neither algorithm starves the other.
  Both share the one episode-cache limit.
- **Feed settings rearranged**, with the automation block under its own icon, and the "current filter
  and sort" policy now hints that the *Auto downloadable* filter avoids undesired repeats.
- **The speedometer popup explains itself.** Playback speed, skip silence, rewind, fast forward,
  fallback speed, forward speed and skip speed each grew their helper text in the player popup — and
  **Settings → Playback dropped the duplicate block** that offered the same seven settings twice.
- **Feed details watches only its own feed.** `FeedDetailsVM` queries the feed by id instead of
  filtering the shared all-feeds flow, so an unrelated feed's change no longer wakes the screen; the
  global `feedsFlow` is gone and the feed monitor builds its flow inline.
- **Smaller repairs.** `Chapter` and `Timer` lose their indexed `id` field, and a `&&` that should
  have been `AND` in a Realm query string on the Logs screen is fixed.

## 12.5.11+001 (versionCode 990001)

Rebased onto upstream **v12.5.11** (versionCode 99), folding in **v12.5.10** (98) as well — upstream
cut two releases a day apart. A pure tracking release — **no new fork features**; the whole custom
layer replayed onto the new base and the build counter reset to `+001`.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **A clean replay.** The only collision was the recurring one in `app/build.gradle.kts`, where
  upstream reasserts the version literals (`99` / `"12.5.11"`); the fork keeps deriving them from
  `forkVersionName` / `forkVersionCode`, with the upstream numbers living in `gradle.properties`.
  Every other commit in the custom layer applied without conflict.
- **Version base moved to 12.5.11 / 99**, so this line's codes (`990001`, `990002`, …) all exceed the
  12.5.9 line's (`970001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.5.10

- **Duplicate handling rebalanced.** The *related* links between duplicates are now built from the
  full title-or-URL candidate set rather than the subset whose durations matched within 5 %, and an
  episode no longer links to itself — so two same-titled episodes of different lengths show up as
  related again, which 12.5.9's duration test had quietly severed. The ignore decision was pulled
  out of the loop: duplicates at or below **Again** are marked **Ignored** as before, and the
  incoming episode is marked Ignored only when one of its duplicates already was.
- **Clip recording rebuilt.** The recording flag moved out of the data source into app-wide Compose
  state, so the record button in the player turns red from the recording that is actually running
  instead of from a local timestamp. The data source now closes an already-open handle before
  reopening rather than stacking them, and closing the source no longer tears down the clip sink
  underneath a recording in progress; the fragile `forceCommitCurrentSink()` reopen is gone and the
  factory publishes its live data source. `saveClipInOriginalFormat` became the non-suspending
  `recordClip`, called straight from the player UI instead of through a coroutine, and its failure
  paths — no media item, no URI, no audio track, unsupported container — now surface as errors you
  can see, with temp files cleaned up in a `finally`.
- **A dead feed URL says so.** Building a podcast from an HTML page logs how many RSS `<link>`
  elements it found and raises a visible error when there are none — or when the fetch throws —
  instead of failing silently into the log.
- **Date parsing rewritten.** The hand-rolled RSS `pubDate` splitter and the long list of
  `SimpleDateFormat` patterns are replaced by explicit `kotlinx-datetime` format builders: optional
  seconds, four-digit or ISO offsets, English abbreviated / full / `Sept` month names, and a lookup
  table of the legacy zone abbreviations feed generators still emit (EDT/EST, CDT/CST, MDT/MST,
  PDT/PST, CEST/CET/BST, UT/UTC/GMT/Z).
- **"Search all sources" sticks.** Searching with no explicit source now resets the provider to the
  Combined searcher rather than leaving whichever single source was selected last.

### Inherited from upstream 12.5.11

- **Facets sort and filter actually apply.** Both were read straight back from preferences on every
  access, so the screen never recomposed when you changed them; they are now backed by observable
  state that is written through to preferences. The sort button is hidden in **History**, and the
  filter button in **Recorded**, **Due**, **Timers**, **Archived** and **Frozen** — the modes where
  neither meant anything.
- **Sort episodes by feed, in Queues.** Podcast-title sorting became conditional and is offered only
  on the Queues screen, which also gains **Feed score** and **Feed score count** sortings. All three
  sort in memory (the database-side ordering for them is parked as a TODO upstream).
- **Per-feed sorts follow the source.** The sort dialog now takes the feed it is sorting and asks
  that feed's source client whether it has view counts and like counts, so FeedDetails and OnlineFeed
  stop offering View count, Views per day and Like count on feeds that cannot supply them. A sort
  order missing from the offered list no longer leaves the dialog on an invalid selection.
- **Volumes are sorted by name** in the parent-volume popup in Feeds settings.
- **Single-shot playback where a list is a record, not a queue.** Episode lists can now prefer the
  one-off action buttons, and **FeedDetails History** and **Queues Bin** do: Play and Stream become
  **Play once** and **Stream once**, so replaying something out of history or the bin does not drag
  the rest of the list along behind it. The per-feed preferred action is consulted through the feed
  passed to the button rather than dug out of each episode.
- **Feed titles survive an update.** A refresh only fills in the title when the feed has none, and
  the constructor no longer blanks it with a null — so an update can no longer strip the tab suffix
  from a YouTube feed's name.
- **xmlutil 1.0.1 → 1.0.2.**

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
