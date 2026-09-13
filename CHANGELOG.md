# 白い熊 空中線 — changelog

Everything built on top of stock [Podcini.A](https://github.com/XilinJia/Podcini.A).

## 12.10.0+001 (versionCode 1210001) — 2026-09-13

Rebased onto upstream **v12.10.0** (versionCode 121), released 2026-09-13 — taking in **five**
upstream releases at once. 12.9.2 landed on the 6th, 12.9.3 and 12.9.4 both on the 8th, 12.9.5 on
the 11th and 12.10.0 today. A tracking release: **no new fork features**, the whole custom layer
replayed onto the new base and the build counter reset to `+001`.

> **Take a fresh backup after installing.** The Realm schema moves **159 → 161**: upstream adds two
> stored shapes, `TranscriptMeta` and `CaptionCue`, and hangs them off every episode. Your existing
> database migrates forward on first launch as usual, and a backup taken with 12.9.1+003 still
> restores here — but a backup taken *after* this build carries schema 161 and will not go back into
> a 12.9.x install. The one-way migration warning from `12.6.0+001` still applies to anyone arriving
> from a 12.5.x build.
>
> **Upstream also dropped the pre-150 migration step**, which set `episodesDownloadable` on RSS and
> Atom feeds. A database that has not been opened since schema 150 — roughly 12.4.x — no longer has
> that path available. Anything kept current is unaffected.

> **Size**: 36.67 MiB, up 609 KiB on 12.9.1+003 — the caption parsers and the new player HTTP stack.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the self-verifying export, the token-gated headless export, the data door and the
  black-yellow identity all carry over untouched.
- **No porting fix needed** — the first rebase in a while that needed none. Nothing the fork patches
  was touched in a way our code reads; the single overlap, `LibraryScreen.kt`, took its nine upstream
  lines by auto-merge, and the release compiles clean with no warning of ours.
- **Two collisions, both additive rather than semantic.** `app/build.gradle.kts` absorbed the bulk of
  the upstream rework (112 lines) and conflicted twice. One is the routine version-literal clash,
  where upstream reasserts `121` / `"12.10.0"` and the fork keeps deriving them from
  `forkVersionCode` / `forkVersionName`. The other is new: 12.9.2's legacy flavors bring a
  `configurations` block that lands immediately before `dependencies {}` — exactly where the
  `buildFork` task is registered — so git saw one region rewritten twice. Both blocks are kept.
  `README.md` conflicted in two hunks and ours stands in both, which drops the note upstream added
  about its Legacy downloads.
- **The legacy flavors are not shipped.** `freeLegacy` and `playLegacy` are registered and buildable,
  but `buildFork` is unchanged: it still assembles the plain `free` release and picks the arm64-v8a
  split out of `outputs/apk/free/release`.
- **Version base moved to 12.10.0 / 121**, so this line's codes (`1210001`, `1210002`, …) all exceed
  the 12.9.1 line's (`1160001`, …) and upgrades stay monotonic. 117 through 120 are upstream releases
  we never built separately; their contents arrive here in one step.

### Inherited from upstream 12.9.2 – 12.10.0

59 files, 1483 insertions, 694 deletions — and for once none of it is packages moving: the tree that
12.9.1 rearranged stayed exactly where it was put.

**Transcripts and captions — the headline.** Podcasting 2.0 transcripts are now supported, with the
available options captured at the moment you subscribe to a feed or when it updates, and stored
against the episode. YouTube captions are supported too, fetched only when the media is actually
played. Five formats parse: WebVTT, SubRip, HTML, JSON and TTML — anything else is kept as plain
transcript text. A new `CaptionUtils.kt` (297 lines) does the parsing; `TranscriptMeta` records where
a transcript came from and in what language, `CaptionCue` holds each cue with its start, end, speaker
and text.

- **Captions play along with the audio.** In PlayerDetailed the selected transcript can be shown as
  captions, three at a time — previous, current and next — and clicking one seeks to it.
- **Transcript options are selectable** from PlayerDetailed or EpisodeInfo, and the transcript or the
  joined captions can be opened as a popup from either.
- **Sharing is consolidated.** Episode sharing now goes through one `ShareDialog`, which gains share
  transcript and share captions when the episode has them.

**Playback and networking.**

- **A user agent is now set in the player pipeline**, on both the Media3 data source and the Cronet
  HTTP engine, which resolves redirect rejections some hosts were issuing against an unidentified
  client. A new `playback/base/OKHTTP.kt` (185 lines) carries the player's own OkHttp stack.
- **Feed language changes behave better.** The player's reaction when a feed's preferred languages
  are changed has been tuned.
- **The mp4 chapter fetcher is improved**, and podcast URL type detection is fixed for some kinds of
  URL that were not being recognised when adding a podcast.

**External source clients** — the interface this fork's own automation sits beside, though it does
not use it.

- **Clients auto-reconnect.** A dedicated reconnect path with its own mutex was added; clients that
  died or were disconnected now reconnect when the app resumes.
- **Cleanup is guaranteed when a client disconnects or dies**, and the disconnect logging survives a
  `DeadObjectException` from a gateway that is already gone rather than throwing on the way out.

**Crash fixes** — 12.9.3 and 12.9.4 exist for these.

- **Fixed: crashes from accessing the player on the IO dispatcher.** Further player accesses were
  moved onto the Main dispatcher in 12.9.4 after the first pass in 12.9.3.
- **Fixed: `DeadObjectException` when toasting about an external client disconnect.**

**Elsewhere.**

- **Synthetic feed summaries update** when episodes are added to or erased from them.
- **The Logs screen checks before it acts.** In Shared error mode, clicking a log now checks the
  entry still exists and prompts.
- **Queue resolution simplified** on `Feed`: the active-queue and none cases were folded out of the
  getter, and the `queueTextExt` variant removed.

**Packaging and dependencies.**

- **A legacy flavor pair was added** (12.9.2) — the same app with dependency libs compressed, giving
  a smaller APK at the cost of a larger install and a slower first launch. We do not ship it.
- **AGP 9.4.0 and Kotlin 2.4.0 are now declared** in `settings.gradle.kts` and the root buildscript
  rather than resolved implicitly.
- Media3 1.11.0 → **1.11.1** (and media3-cast 1.10.0 → 1.11.0), PodciniLib 1.1.2 → **1.1.4**, Compose
  BOM 2026.08.00 → **2026.09.00**, Coil 3.6.1 → **3.6.2**, Okio 3.18.1 → **3.18.2**, Conscrypt 2.5.3
  → **2.7.0** (the `free` flavor's TLS provider, so ours), play-services-base 18.9.0 → 18.10.1 and
  cast-framework 22.2.0 → 22.3.1. Two dependencies are dropped outright: `ktor-client-cio` and
  `okhttp-urlconnection`.
- **PodciniLib 1.1.4 is a compatibility break for external source apps** — upstream notes that any
  external app you use with it needs updating to match.

## 12.9.1+003 (versionCode 1160003) — 2026-09-08

Same upstream base (**v12.9.1**, versionCode 116). One defect, and it was the worst kind: every
backup written since 12.9.1+002 was unrestorable, and every one of them reported success.

### The database was never actually in the backup

An archive taken by this app carried a `database.realm` that no unzip, and no restore, could read —
`invalid distance too far back`. The six other members were perfect; the one that holds the
episodes, play positions and queues was not. It was found on the other side, during a restore, after
forty other apps' payloads came back byte-perfect through the identical path.

The cause was a single line. The database entry was written at a faster compression level than the
JSON entries around it, and the level was switched **after** the first entry had already been
compressed. On this phone that mid-archive switch corrupts the *next* entry: compression levels 1–3
select a different internal compression routine than 4–9, and zlib changing routines part-way
through an archive leaves the following entry a stream nothing can decompress. It only damages an
entry large enough to span more than one compression pass — so the small JSON members were untouched
and the database, the only large one, was destroyed. Desktop Java does not reproduce it at all;
only running the export on the phone shows it.

The compression level is now chosen once, before the first entry, and never changed again.

### The export now verifies itself, and fails loudly when it cannot

The archive's recorded checksums and sizes had been correct the whole time — they describe what went
*into* the compressor, and the fault was in what came out of it. Nothing between the compressor and
the file had ever asked the only question that mattered: can these bytes be read back?

Now every byte is decompressed again as it is written, entry by entry, and each one's length and
checksum are checked at the moment it closes. The check runs in the stream rather than as a second
pass over the finished file, which is what lets it cover the data door as well — that path writes
into a descriptor the calling app opened, which may be a pipe with nothing to reopen and re-read,
and it was the path that actually failed. It costs one decompression pass, in constant memory, and
it names the entry that broke as it breaks.

An export that cannot verify itself now stops and says so — `archive verification failed on
database.realm: …` — instead of leaving behind a file that looks exactly like a backup until the day
someone needs it.

**Backups taken with 12.9.1+001 and +002 cannot be repaired.** Their database contents were never
written correctly. Take a fresh backup with this build.

## 12.9.1+002 (versionCode 1160002)

Same upstream base (**v12.9.1**, versionCode 116). This release is entirely about the backup
automation: it implements version 2 of the sister-app contract, and fixes three defects in the
version 1 code that had never run on a phone.

### Backing up a real library no longer kills the app

**This is the important one.** The headless export ran inside the broadcast receiver, holding the
broadcast open for the whole job. Android gives a receiver about ten seconds in the foreground and a
minute in the background, and `goAsync()` does not extend that — and this app's archive carries a
Realm snapshot whose `writeCopyTo` runs for **minutes** on a real subscription list. So an automated
backup of an actual library would have been declared unresponsive and killed part-way through,
leaving a half-written `.part` file, no reply, and the app that asked for it waiting forever. It
would have looked like nothing happened.

The export now runs in its own foreground service, under an ongoing notification and a partial
wakelock — the wakelock because EMUI otherwise force-releases it seconds in and quietly starves the
process, which stops a long export at no consistent point with nothing in any log. `CANCEL_EXPORT`
is routed through the exported receiver into that service, so 中止 really stops the run, deletes the
partial file and answers `ERROR:cancelled`.

### A restore that says it worked, did

Restoring the **Colours**, **Typography** and **Shape** categories wrote them asynchronously. The
app that drives a restore force-stops this one the moment it reports success — deliberately, so a
shutdown cannot write stale settings back over what was just restored — and an asynchronous write
still in flight at that moment is simply lost. A clean-phone restore could therefore have reported
success with the whole house look silently unapplied, every other category correct and nothing
reporting a failure. Those writes are synchronous now, and every preferences file the restore
touches is flushed to disk before anything is told it succeeded.

### A progress line that keeps proving the app is alive

The caller presumes an app dead after two minutes of silence, and this export goes quiet twice over:
it reports once per category, and the database snapshot is minutes of silence *before* the first
byte reaches the archive — so throttling messages could never help, because the silence is upstream
of it. A heartbeat now re-sends the last true line every fifteen seconds. It re-sends the real one
rather than inventing a moving number, because a fabricated count cannot be told apart from
progress. The two copies of the progress sender have become one, correlating on both ids so a single
reader serves both entrances.

### The data door (new)

A `ContentProvider` at `shiroikuma.kuchusen.automation` that hands the whole backup to a caller
through a **file descriptor the caller opened**, and can put one back. It identifies who is asking
three ways — an exact package name, the uid the kernel reports, and a pinned signing certificate —
because the caller supplies the destination, and "some app called `shiroikuma.*`" is not an
identity. `import` exists **only** here and never as a broadcast, since an import overwrites the
entire database. A `describe` call answers a header (version, format, what the backup contains, and
an explicitly empty list of permissions this app's restore needs) without exporting anything, so a
caller can draw a list and judge compatibility before streaming anything.

This is what makes a wiped phone recoverable: the app can be reinstalled and its subscriptions, play
positions and queues put back before it is ever launched.

### The gate ships open now

The automation switch **defaults to on** and the token is **opt-in**, moved to a second row with the
token itself hidden until it is asked for. The reasoning is the wipe: a 48-character secret pasted
from this app into another cannot survive one, and a gate that only works on an already-configured
phone is no gate for setting a phone up. The switch stays, because it is the only way to shut this
app's automation off.

Because the default inverted, all three flag writes are now synchronous. A write that never reaches
disk used to fall back to "off"; it now falls back to **on**, so a lost "turn it off" would have
quietly reopened the door.

### Worth knowing

**The first automated backup of a real library takes minutes, not seconds.** That is the database
snapshot, and it is honest now — the foreground service carries it and the progress row keeps
ticking across the silent stretch — but the row will sit on the same line for a while before the
byte counter starts moving. Nothing is stuck.

## 12.9.1+001 (versionCode 1160001)

Rebased onto upstream **v12.9.1** (versionCode 116), released 2026-09-02 — two upstream commits, one
per day: 12.9.0 on the 1st, 12.9.1 on the 2nd. A tracking release: **no new fork features**, the
whole custom layer replayed onto the new base and the build counter reset to `+001`. versionCode 115
existed upstream as 12.9.0 but was never built here, so this line starts at `1160001`.

> **The Realm schema moves 158 → 159 — this one is a one-way door.** `CurrentState` drops two fields
> (`curMediaType`, `curFeedId`), and krdb removes them on first launch without a migration block. The
> upgrade itself is safe and touches nothing you would miss — those two fields only recorded which
> media the player had loaded — but once this build has opened the database, **12.8.7+001 and earlier
> will refuse to open it**, because their config still declares schema 158. Export from the UI page
> first if you want a way back.

> **The database is compacted at launch from now on.** Upstream turned on `compactOnLaunch()`, which
> reclaims the file when more than half its allocated space is unused. The first launch of this build
> may pause a moment longer, `Podcini.realm` may shrink noticeably, and the **Database** category of
> the fork's one-file backup shrinks with it — the same content in a smaller ZIP.

> **Size**: 36.05 MiB (37,797,980 bytes), 72 bytes larger than 12.8.7+001.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **One porting fix.** Upstream dissolved its `net` package, and `updateFeedFull` — the call the
  backup's feed restore uses to re-subscribe a feed from `feeds.json` — moved from
  `storage.database` to `sourcing.feed.FeedUpdater.Companion`. Its signature is unchanged, so
  `KuchusenExport.kt`'s import is the only line that moved; the restore path itself is untouched.
  Upstream's own Podcast Addict importer now imports it exactly the same way.
- **Two collisions, both routine.** `app/build.gradle.kts` reasserts the version literals (`116` /
  `"12.9.1"`) while the fork keeps deriving them from `forkVersionName` / `forkVersionCode`.
  `NavDrawerScreen.kt`'s players toggle collided harder than usual: upstream renamed
  `activeTheatresFlow` to `activeTheatresCount` and gave the toggle a two-state glyph — a teaser
  image when one theatre is live, the launcher icon when two are. That new logic is taken as-is,
  with our yellow `ic_launcher_sk_foreground` standing in for upstream's icon and our
  `ContentScale.Fit` kept, so both survive.
- **Note for testers**: upstream now deletes the crash log whenever the running `versionName` differs
  from the last one seen. The fork's `versionName` carries the build counter (`12.9.1+001`), so that
  happens on *every* fork build, not just every upstream release — a crash from the previous build is
  gone once the new one starts.
- **Version base moved to 12.9.1 / 116**, so this line's codes (`1160001`, `1160002`, …) all exceed
  the 12.8.7 line's (`1140001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.9.0

The largest structural release the fork has taken in: 110 files, 1,332 insertions, 1,586 deletions,
most of it packages moving rather than logic changing. Nothing here is user-visible on its own, but
it is the ground the two real fixes stand on.

- **The `net` package is dissolved.** `net/download`, `net/feed`, `net/searcher` and `net/ssl` move
  under a new `sourcing` package alongside the external-source gateway (itself moved from `sources`);
  `net/sync` becomes a top-level `sync`; `net/utils/NetworkUtils` becomes `utils/NetworkUtils`; the
  sync transceiver becomes `sourcing/Transceiver`. `LocalFeeds.kt` leaves `storage/database` for
  `sourcing/feed`, and `Feeds.kt` hands 202 lines of feed-updating to `FeedUpdater`.
- **Fixed: play and pause from external controls are obeyed properly.** Restoring the player's state
  at startup moved out of the theatre objects into the playback service, where it now also re-reads
  which queue the current episode belongs to and restarts the state monitor. Previously the service
  could come up with a media loaded but the surrounding state stale, so a headset, watch, or
  notification button pressed before the app was touched could act on the wrong thing.
- **The player's status stops being a flow.** `MediaPlayerBase` held its status in a `StateFlow` that
  published transitional states; it is now a plain field, and `PlayerStatus` loses `INDETERMINATE`
  and `PREPARING` along with the `isPreparing` / `isUnknown` checks that existed to wait them out.
  `setPlayerStatus` becomes `handlePlayerStatus`, `savePlayerStatus` becomes `saveCurState`, and
  `prepare()` becomes `prepareInitialized()`.
- **Obsolete broadcasts are gone from the player.** The shutdown-playback-service receiver is no
  longer registered on either SDK path, and `MainActivityStarter` — a helper for launching the main
  activity by intent — is deleted outright.
- **Logs: subscription logs can no longer be cleared.** The clear-logs menu is hidden in the
  Deletions view and its handler removed, because upstream needs that history for subscription
  events it plans to add.
- **Logs: download entries older than 30 days are trimmed at startup**, once, when the Logs view
  model is created.
- **The crash report from the previous version is cleared** on the first launch after an update, so
  the bug-report screen no longer shows a stale stack trace from a build you already replaced.
- **The database is compacted on launch** when more than half its allocated space is unused.
- **In feed settings, "enable second algorithm" moved below the first algorithm** it modifies.
- **Two dozen settings summaries were rewritten** for clarity — auto-enqueue, auto-download, episode
  cache, episode limit, preferred languages, skip intro/outro, volume adaptation and others now say
  "feed" where they said "podcast", and describe what they do in fewer words.

### Inherited from upstream 12.9.1

A small release: one screen reworked, and the build tooling moved forward.

- **Fixed: renaming a queue, and adding or removing one, now reaches the screen.** The Queues screen
  held its queue list in Compose state and read the current queue through snapshots, so a change to
  the queue set arrived at the database but not always at the list. The list is now a live query, the
  current queue is a flow that falls back to the active queue instead of going null, and the three
  content flows — entries, recycle bin, sorted episodes — derive from it rather than from a snapshot.
  The manual counter that used to force a name refresh is gone.
- **Tooling**: AGP 9.3.2 → 9.4.0, Gradle 9.6.1 → 9.7.1, coil3 3.5.0 → 3.6.1, xmlutil 1.0.2 → 1.0.2.1.
  Builds clean on JDK 21 with the existing SDK, NDK and build-tools.

## 12.8.7+001 (versionCode 1140001)

Rebased onto upstream **v12.8.7** (versionCode 114), released 2026-08-30 — a single upstream commit,
arriving the day after 12.8.6. A tracking release: **no new fork features**, the whole custom layer
replayed onto the new base and the build counter reset to `+001`.

> **No export needed.** The Realm schema stays at 158 — upstream touches only screen code, no stored
> shape. The one-way migration warning belongs to `12.6.0+001`; if you are coming from a 12.5.x build
> it still applies, so export from the UI page first.

> **Size unchanged**: 36.05 MiB, three bytes off the previous build.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **One collision, the routine one.** `app/build.gradle.kts`, where upstream reasserts the version
  literals (`114` / `"12.8.7"`) while the fork keeps deriving them from `forkVersionName` /
  `forkVersionCode`. The other thirty-seven commits of the custom layer applied clean — `README.md`
  included, which upstream left alone this time.
- **Version base moved to 12.8.7 / 114**, so this line's codes (`1140001`, `1140002`, …) all exceed
  the 12.8.6 line's (`1130001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.8.7

A screen-layer release, entirely under `ui/screens`: no model, playback or gateway code is touched.
The theme is the flow migration 12.8.1 and 12.8.2 began, now reaching Library and Logs — both still
read Compose state where a live database flow was available — plus three small behavioural fixes.

- **The Library follows the database again.** Its subscription preferences — filter, sort, archived,
  selected languages, tags and queues — were held in a mutable Compose field that every reader and
  writer went through, with each write reassigning the whole object by hand. They now come from a
  live query, so a preference changed anywhere is reflected immediately and the feed list, the
  volume list and the four generated query strings re-evaluate off the same source instead of
  re-reading state after the fact. Replacing the shown feed set with a different set of equal length
  now refreshes too, where before only a change in count did.
- **Logs are live.** The share, download and deletion logs were fetched once when their tab was
  opened; each is now a standing query for whichever mode is selected, so entries appear as they are
  written and no longer need the screen reopened. An empty log falls back to the session view. The
  three post-delete reloads and the reload after re-handling a shared URL disappear with the fetches
  they existed to repeat.
- **The Logs screen is cheaper to scroll.** Its three filtered lists were being rebuilt on every
  recomposition inside the list body; they are now computed once per change of the log set or of the
  success/error toggle.
- **The Logs top bar swaps its icons.** The current mode's icon becomes the drawer button, and the
  title is left empty, in place of the mode icon as title and a history glyph for the drawer.
- **Fixed: the player sheet now hides when there is nothing to play.** 12.8.6 taught the sheet to
  refuse hiding unless a deliberate swipe asked for it, which also blocked the app's own attempt to
  hide it once the last media was gone; that path is now permitted explicitly.
- **Fixed: removing a feed returns to where you came from.** It navigated to the Library
  unconditionally; it now goes back one screen.
- **Auto-download EQ policy options show up for multi-feed edits.** In feed settings, the policy rows
  appeared only when the reference feed already had a policy of its own — they are now shown whenever
  more than one feed is being edited.
- **Three list flows start eagerly** — the Facets episode list, the feed-details feed and the search
  results — instead of five seconds after the last observer leaves, so returning to those screens no
  longer waits for a re-subscribe.

## 12.8.6+001 (versionCode 1130001)

Rebased onto upstream **v12.8.6** (versionCode 113), released 2026-08-29 — taking in **four**
upstream releases at once. 12.8.3 landed on the 26th, 12.8.4 on the 27th, 12.8.5 on the 28th and
12.8.6 on the 29th, one per day; none is skipped, they simply arrived faster than the fork rebased,
and the version base jumps 109 → 113 accordingly. A tracking release: **no new fork features**, the
whole custom layer replayed onto the new base and the build counter reset to `+001`.

> **No export needed.** The Realm schema stays at 158 — upstream changes no stored shape. The
> one-way migration warning belongs to `12.6.0+001`; if you are coming from a 12.5.x build it still
> applies, so export from the UI page first.

> **Size shrinks slightly**: 36.33 MiB → 36.05 MiB (−288 KiB), from Google Play's Cronet variant
> being excluded at the dependency level.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **One porting fix**, in the drawer's players toggle. Upstream reworked it in 12.8.5 — it now shuts
  the second player down explicitly when dropping back to one, and reads the new theatre count
  rather than the stale one when sizing the player. That logic is taken as-is and the fork's own
  change sits on top of it unchanged: the yellow line-art glyph in place of upstream's two
  alternating images, which is also why the fork no longer needs the value that picked between them.
- **Two collisions, both routine.** `app/build.gradle.kts`, where upstream reasserts the version
  literals (`113` / `"12.8.6"`) while the fork keeps deriving them from `forkVersionName` /
  `forkVersionCode`; and `README.md`, where upstream reworded its feature list and the fork replaces
  that whole block. The other thirty-three commits of the custom layer applied clean.
- **Version base moved to 12.8.6 / 113**, so this line's codes (`1130001`, `1130002`, …) all exceed
  the 12.8.2 line's (`1090001`, …) and upgrades stay monotonic.
- **A coincidence worth noting**: upstream recoloured the "Player UI in the drawer" strip to yellow
  on dark grey. For once its choice agrees with the house style.

### Inherited from upstream 12.8.3 – 12.8.6

No new subsystem this time. Where 12.8.1 and 12.8.2 moved preferences and playback off Compose state
onto flows, these four releases are the settling-in: the external-source gateway is made robust, the
duration correction that 12.8.2 promised is actually implemented, the player sheet stops hiding
itself, and a long-standing crash on non-Latin text is fixed.

- **External source apps connect reliably again.** The gateway registry was completing its
  readiness signal with an empty list *before* it started work, so anything waiting on it — a feed
  update in particular — was told there were no clients. That line is gone; initialization now runs
  on a supervised main-immediate scope instead of an IO one, so one failing client no longer takes
  the registry down with it, and it refuses to start twice concurrently. Teardown is unified: the
  three disconnect callbacks each used to do their own partial cleanup, and now all route through a
  single `disconnect()` that clears every field and unbinds exactly once. Binding and unbinding are
  wrapped against exceptions throughout, and the wait-for-ready call takes a timeout and reads its
  state under lock rather than racing a reassignment. Feed updates also wait 3 seconds after
  readiness instead of 1.
- **Ordinary podcasts no longer take the external path.** Feed refresh used to consult the external
  client map first and fall back to a normal download; now RSS, Atom and untyped feeds go straight
  to the normal download, and only genuinely external types ask a client — with no client meaning no
  update, rather than a podcast fetch against a source-app URL.
- **When external apps are configured but none are connected**, returning to the app now asks what
  to do: reconnect, or turn the setting off.
- **Media durations are corrected properly.** On a timeline or media-item change, a duration
  differing from the stored one by more than five seconds is written back and logged — replacing
  12.8.3's narrower "only if invalid, only at buffering" attempt. On the other side, a feed refresh
  can no longer reset the duration of an episode already in progress.
- **Fixed: crash when creating the second player.** The HTTP and Cronet engines were being rebuilt
  per player over the same cache directory; they are now created once, given per-player storage
  paths, and released when the service stops. Player shutdown is factored into one guarded routine.
- **Fixed: the Pause button in episode lists pauses the right player.** It now targets the player
  the episode is actually on — remembered from the "default / secondary" choice — instead of looping
  over both.
- **Repeat becomes a per-media action.** A new **Repeat this** entry appears when long-pressing
  Pause, setting repeat on whichever player holds the media; playback no longer ends the episode
  when repeat is on, which is the fix for repeat under both Stream and Play. The old "Repeat current
  media" checkbox leaves the speed dialog, and the long-press menu drops the options that made no
  sense while paused.
- **The player sheet stops hiding itself.** Hiding is now refused unless the deliberate swipe-to-
  dismiss gesture asked for it, and the sheet follows its target state rather than its settled one —
  so an incidental downward swipe no longer makes the player vanish. When it *is* hidden, the
  "Player UI in the drawer" strip is clickable to bring it back, and says so.
- **Fixed: crash when removing a feed** whose description begins with characters outside the basic
  plane — emoji, CJK extensions. Truncation counted UTF-16 units and could cut a surrogate pair in
  half; every place that shortens a description, title or episode name now counts code points.
- **The toast close button clears the whole queue** instead of only the first three.
- **Fixed: F-Droid builds** exclude Google Play Services' Cronet from the media3 data source, and
  renderer-level speed adjustment is disabled as too device-dependent.
- **Build**: navigation3 1.1.6 → 1.1.7, glance-appwidget 1.1.1 → 1.2.0.

## 12.8.2+001 (versionCode 1090001)

Rebased onto upstream **v12.8.2** (versionCode 109), released 2026-08-25, two days after 12.8.1.
A tracking release: **no new fork features**, the whole custom layer replayed onto the new base and
the build counter reset to `+001`.

> **No export needed.** The Realm schema stays at 158 — upstream changes no stored shape. The
> one-way migration warning belongs to `12.6.0+001`; if you are coming from a 12.5.x build it still
> applies, so export from the UI page first.

> **Size is flat**: 36.31 MiB → 36.33 MiB (+16 KiB). Last release's 7 MiB jump was Cronet arriving;
> nothing comparable lands here.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **Two porting fixes, both forced by upstream's playback rewrite** (see below). The drawer's
  players toggle wrote `activeTheatres` directly; that value is now read-only state collected from
  a flow, so the write goes to `activeTheatresFlow` instead — the fork's yellow icon in place of
  upstream's alternating one is unaffected. And `MainScreen`, where the fork's back handler exits
  the app instead of showing upstream's "no more screens" toast, needed its imports narrowed after
  upstream reworked how toasts are held.
- **One collision, the recurring one.** `app/build.gradle.kts`, where upstream reasserts the version
  literals (`109` / `"12.8.2"`) while the fork keeps deriving them from `forkVersionName` /
  `forkVersionCode`. The other thirty-one commits of the custom layer applied clean.
- **Version base moved to 12.8.2 / 109**, so this line's codes (`1090001`, `1090002`, …) all exceed
  the 12.8.1 line's (`1080001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.8.2

**The player stops being a Compose object.** Last release turned the two global preference objects
into flows; this one does the same to playback, and the change is structural. The `InTheatre`
singleton is deleted outright — its file replaced by `Theatres.kt`, its members promoted to
top-level declarations — and every piece of Compose state inside it becomes a `StateFlow`: the
active queue, the active-theatre count, and each theatre's player. The Compose runtime imports
leave the playback package entirely, which is the point: non-UI code no longer depends on the UI
toolkit to hold its state.

- **Current media is now fed by the database, not assigned by hand.** `curEpisode` becomes
  `curMediaFlow`, and setting it opens a Realm query flow scoped to that episode, cancelled and
  reopened on each change. Player status, playback speed, video mode, buffer percentage, bitrate,
  resolution, MIME type, channel count and repeat all become flows alongside it. This is upstream's
  "re-worked curMedia monitoring", and it is the largest single file change in the release.
- **Buffer progress moves onto a flow**, polled every five seconds. The old buffering-callback
  plumbing and its `BufferUpdateEvent` are gone; the player UI collects the flow directly.
- **Invalid durations get corrected in place.** When buffering completes on an episode whose stored
  duration is missing or zero, the real duration reported by the player is written back — so the
  episode list and the player UI stop showing a bad length.
- **Fixed: error playing a downloaded file.** Download progress moved off a Compose state map onto
  a flow, and the list-summary helper that computed remaining time no longer force-unwraps a player
  that may not exist — it reads playback speed from the feed map instead.
- **Speed changes in the video player reach the player UI.** The speed dialog resolves its player
  once and reads the live speed flow, so a change made in one surface shows in the other.
- **Fewer start-up flickers in the player UI**, from the same rework — the UI no longer reacts to a
  burst of separate events while the player settles.
- **"Likely removed" no longer lists duplicates** on the online-feed screen; the results are
  collected into a set.
- **Feed settings regroup** under a new *Others* heading, and editing the feed that is currently
  playing now forces a playback reset so the change takes effect immediately.
- **Toasts and session logs become flows** too, as does the recording flag and the per-feed queue
  counter. The episode-monitor machinery in the database layer — roughly a hundred lines of mutex,
  monitor registry and per-episode subscription — is deleted, superseded by the per-media flow.
- **Text-to-speech loses its Compose coupling**: its progress and speaking state are reported
  through callbacks rather than Compose state holders, and its waits use typed durations.
- **Build**: Android Gradle Plugin 9.3.1 → 9.3.2, and `media3-ui-compose` 1.11.0 joins the
  dependency set.

## 12.8.1+001 (versionCode 1080001)

Rebased onto upstream **v12.8.1** (versionCode 108), taking in **two** upstream releases at once —
12.8.0 arrived on 2026-08-22 and 12.8.1 superseded it the next day, so this fork line skips straight
to the newer one. (Upstream spent versionCode 107 on 12.8.0, which is why this line's base jumps
106 → 108.) A tracking release: **no new fork features**, the whole custom layer replayed onto the
new base and the build counter reset to `+001`.

> **No export needed.** The Realm schema stays at 158 — neither upstream release changes a stored
> shape. The one-way migration warning belongs to `12.6.0+001`; if you are coming from a 12.5.x
> build it still applies, so export from the UI page first.

> **The APK is ~7.3 MiB larger** (29.0 MiB → 36.3 MiB). Upstream moved streaming playback onto
> Google's Cronet stack and bundles the engine in the app, so the growth is upstream's, not the
> fork's. See *Streaming now speaks HTTP/3* below for what it buys.

### Fork layer

- **Nothing removed, nothing changed.** Live theming, the one-file category backup with the database
  snapshot, the token-gated headless export, the black-yellow identity and the clean-exit back
  handler all carry over untouched.
- **One porting fix, forced by upstream's flow rewrite.** Upstream replaced the global `appPrefs`
  object with a `StateFlow` (see below), and the fork's export/import code read that global in three
  places — reading app settings out, and writing them back on restore. All three now read the flow's
  current value, the same way upstream's own background code does. Behaviour is identical; the
  **App settings** export category is unaffected in both directions.
- **One collision, the recurring one.** `app/build.gradle.kts`, where upstream reasserts the version
  literals (`108` / `"12.8.1"`) while the fork keeps deriving them from `forkVersionName` /
  `forkVersionCode`. All thirty-one commits of the custom layer applied otherwise clean — including
  the five files both sides touch this time (`AppTheme.kt`, `Composables.kt`, `LibraryScreen.kt`,
  `MainScreen.kt` and `strings.xml`), every one of which merged without a conflict even where
  upstream rewrote the lines next to the fork's.
- **Version base moved to 12.8.1 / 108**, so this line's codes (`1080001`, `1080002`, …) all exceed
  the 12.7.1 line's (`1060001`, …) and upgrades stay monotonic.

### Inherited from upstream 12.8.0 and 12.8.1

- **Streaming now speaks HTTP/3.** Playback streaming has left OkHttp for Chromium's network stack:
  on Android 12+ devices with a recent network module it uses the system's own HTTP engine, and
  everywhere else a bundled Cronet engine, both with QUIC (HTTP/3), HTTP/2 and Brotli enabled and an
  8-second connect timeout. Upstream's stated aim is high-latency networks, where negotiating down
  from HTTP/3 to HTTP/1.1 automatically makes the difference between a stream that starts and one
  that stalls. **Caveat, and upstream flags it too:** only proxies **without** authentication are
  properly supported now, and upstream is explicitly asking for someone to verify proxy behaviour —
  if you stream through an authenticated proxy, test before relying on this build.
- **The theme switches live — no restart.** Changing light/dark/system used to finish the activity
  and relaunch the app. It now re-skins in place. (The fork's own theming page always worked this
  way; this brings upstream's own theme setting in line.)
- **A crashed page no longer takes the app with it.** If Android kills the WebView renderer while
  show-notes are displayed — which it does under memory pressure — the app used to die with it. The
  dead view is now detached and disposed, and the app logs the failure and carries on.
- **"Views per day" is now "Trending", and sorts in more places.** The sort option is renamed, and
  feed-based sorting (by feed title, feed score, feed score count) is offered across most Facets
  modes — Planned, Repeats, Due, Liked, Commented, Tagged — plus Trending in Feed details and
  Facets where the source supports it. This rides on a new sorting routine that sorts the fetched
  list in memory rather than in the database query, which is what makes orders the query cannot
  express possible at all.
- **Apple episode search on the Remote tab.** A new searcher queries Apple's catalogue for
  *episodes* (not shows), keeping only results whose title actually contains what you typed —
  deliberately strict, so a two-word query does not return a hundred loosely-related episodes.
- **Remote search is manual, and no longer trips over itself.** Typing no longer fires a search on
  every keystroke; the search runs when you submit the query on the Remote tab, and cached results
  come back when you return to it. Selecting or deselecting a searcher mid-search now waits for the
  running search to finish instead of mutating the result list underneath it — the fix for
  12.8.1's "issue with multiple searchers", where two searchers running at once could corrupt or
  lose results. The searcher picker is inert while a search is in flight, and the search-criteria
  menu item is hidden on the Remote tab, where it never applied.
- **Timer and due-time entry is one field.** The five separate year/month/day/hour/minute spinners
  in the timer and todo dialogs are replaced by a single `y.M.d.H.m` text field that must be
  confirmed before it takes effect. The add-timer and edit-timer dialogs are now one dialog.
- **A todo only plays the episode if you ask it to.** Setting a due time on a todo used to schedule
  playback at that time implicitly. There is now a **Notify** checkbox: no tick, no playback — the
  due time is just a due time.
- **Changing a due time cancels the old alarm.** Editing a timer, or moving a todo's due time,
  previously left the previous alarm armed — so the episode could still start at the old time. The
  prior schedule is now cancelled before the new one is set, and re-arming a timer clears its
  existing alarm first.
- **Auto-download/enqueue policy can be set for several feeds at once.** The whole policy editor —
  the policy choice and its include/exclude filters — moved from the single-feed section of feed
  settings into the multi-select section. Feed settings is reordered around it.
- **Statistics opens on today.** The Overview's date started at the Unix epoch (1 January 1970)
  instead of the current date.
- **The position saver interval resets on new media.** A guard clause meant the adaptive
  progress-save interval kept the previous episode's value when a new one started.
- **Faster, quieter cold starts.** Three background entry points — the media-button receiver, the
  download worker and the feed-refresh worker — each re-ran full app initialization on every
  invocation; all three now skip it. Pressing a headset button, or a scheduled refresh firing, no
  longer redoes the work app startup already did.
- **External-app messages name the app.** Toasts about a source-provider app connecting,
  disconnecting or failing to bind now say *which* app, and a bind failure reports the actual error
  instead of a flat "unqualified or incompatible". The "Use external apps" setting text is rewritten
  to explain what such an app actually is.
- **Wi-Fi sync is switched off** by upstream, marked as an antique implementation pending a rewrite.
  Nextcloud/gpodder sync is unaffected.
- **Under the hood:** the two global preference objects became observable flows, which is the single
  largest change in this release by line count and touches roughly forty files — behaviour is meant
  to be identical, but it is the change most likely to surface a stray bug, so it is worth knowing
  about. Two app-wide coroutine scopes replace a scattering of per-object ones. A dropped `@Stable`
  annotation and several `mutableStateOf` wrappers come off model classes that never needed them.

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
