package ac.mdiq.podcini.config.settings

import ac.mdiq.podcini.BuildConfig
import ac.mdiq.podcini.PodciniApp.Companion.getAppContext
import ac.mdiq.podcini.R
import ac.mdiq.podcini.sourcing.feed.FeedUpdater.Companion.updateFeedFull
import ac.mdiq.podcini.storage.database.appPrefsFlow
import ac.mdiq.podcini.storage.database.config
import ac.mdiq.podcini.storage.database.getFeedList
import ac.mdiq.podcini.storage.database.realm
import ac.mdiq.podcini.storage.database.upsertBlk
import ac.mdiq.podcini.storage.model.AppPrefs
import ac.mdiq.podcini.storage.model.Feed
import ac.mdiq.podcini.ui.compose.KuchusenUi
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import io.github.xilinjia.krdb.RealmConfiguration
import io.github.xilinjia.krdb.types.TypedRealmObject
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

/**
 * 白い熊 空中線 settings export/import (the Kōjiki flow).
 *
 * The export is a ZIP of plain JSON files — one per category — plus any imported font files as real
 * files under `fonts/`. No binary blobs, no serialized objects. A `manifest.json` lists the format,
 * version and the categories present. Every category is an independent file; import iterates the
 * selected categories, skips any whose file is absent, and merges — never clears — so the format is
 * future-proof by construction.
 *
 * The export directory (a SAF tree URI) lives in its own device-local prefs file, deliberately
 * outside the exported settings: a foreign device holds no permission for the URI.
 */
object KuchusenExport {
    private const val EXIMPORT_PREFS = "kuchusen_eximport"   // device-local; never exported
    private const val KEY_DIR_URI = "dir_uri"

    // 白い熊's family convention (2026-07-25): every sister app writes `<english-app-name>_<stamp>.zip`
    // into one shared directory, so the names must sort and read uniformly — no version, no infix.
    private const val FILE_PREFIX = "shiroikuma-kuchusen_"
    private const val LEGACY_FILE_PREFIX = "shiroikuma-kuchusen-"   // pre-2026-07-25 exports
    private const val FORMAT = "shiroikuma-kuchusen-export"
    private const val VERSION = 1

    /** The realm snapshot's entry name inside the zip (and of the temporary snapshot file). */
    private const val DB_ENTRY = "database.realm"

    /**
     * A selectable category; `id` is the entry name (`<id>.json`) inside the zip.
     *
     * `defaultSelected` is what an automation caller gets when it names no `items`, and what the
     * data door reports in its header. Everything this app exports is **authored** — subscriptions,
     * play state, queues, the theme 白い熊 built — so nothing here is opt-out. The flag exists
     * because the family contract puts the default on *our* side of the wire: a category that were
     * ever large, derived and re-creatable (a cover cache, a downloaded-media dump) would be added
     * here as `false` and start unticked in 自由作業盤's picker.
     */
    enum class Cat(val id: String, @param:StringRes val labelRes: Int, val defaultSelected: Boolean = true) {
        FEEDS("feeds", R.string.kuchusen_eim_cat_feeds),
        DATABASE("database", R.string.kuchusen_eim_cat_database),
        COLORS("colors", R.string.kuchusen_eim_cat_colors),
        TYPOGRAPHY("typography", R.string.kuchusen_eim_cat_typography),
        SHAPE("shape", R.string.kuchusen_eim_cat_shape),
        APP_SETTINGS("app_settings", R.string.kuchusen_eim_cat_app),
    }

    fun catById(id: String): Cat? = Cat.entries.firstOrNull { it.id == id }

    /** What an automation caller gets when it names no `items` — our recommendation, not our footprint. */
    fun defaultCats(): Set<Cat> = Cat.entries.filter { it.defaultSelected }.toSet()

    /**
     * Raised out of [export] when the caller's cancel flag comes up at a write boundary.
     *
     * A distinct type rather than a bare exception because a cancelled export and a failed one are
     * two different terminal replies: the caller answers `ERROR:cancelled` for the first and the
     * real reason for the second, and only one of them is 白い熊 pressing 中止.
     */
    class Cancelled : Exception("cancelled")

    /**
     * One progress step of an export. `text` is the numbers-first display line 白い熊 reads;
     * `current`/`total`/`unit` carry the same fact structurally, for bars and logic.
     *
     * `item` is the **category id** being written right now. 自由作業盤 draws our categories as a
     * list and highlights the one in progress; it cannot work that out from `current`, because
     * `current` is whatever we are counting at that moment — categories while we walk them, feeds or
     * bytes while we write one of them.
     */
    class Progress(val text: String, val current: Long, val total: Long, val unit: String, val item: String? = null)

    // ---- Export directory (device-local) --------------------------------------------------------

    private fun eximportPrefs(context: Context) = context.getSharedPreferences(EXIMPORT_PREFS, Context.MODE_PRIVATE)

    fun dirUri(context: Context): Uri? =
        eximportPrefs(context).getString(KEY_DIR_URI, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }

    fun setDirUri(context: Context, uri: Uri) = eximportPrefs(context).edit { putString(KEY_DIR_URI, uri.toString()) }

    fun exportDir(context: Context): DocumentFile? =
        dirUri(context)?.let { runCatching { DocumentFile.fromTreeUri(context, it) }.getOrNull() }?.takeIf { it.isDirectory }

    /** The newest export file in the directory, by modification time (legacy names included). */
    fun latestExport(context: Context): DocumentFile? =
        exportDir(context)?.let { dir ->
            runCatching {
                dir.listFiles().filter { it.isFile && isExportName(it.name) }.maxByOrNull { it.lastModified() }
            }.getOrNull()
        }

    private fun isExportName(name: String?): Boolean =
        name != null && name.endsWith(".zip") && (name.startsWith(FILE_PREFIX) || name.startsWith(LEGACY_FILE_PREFIX))

    fun formatTimestamp(epochMs: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date(epochMs))

    /** Datetime-stamped export filename, e.g. `shiroikuma-kuchusen_2026-07-25_10-00-00.zip`. */
    fun exportFileName(): String =
        FILE_PREFIX + SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date()) + ".zip"

    /** Display size for a byte count — `4.6 MB`, `1.20 GB` — as the automation reply carries it. */
    fun humanSize(bytes: Long): String {
        val k = 1024.0
        return when {
            bytes < k -> "$bytes B"
            bytes < k * k -> String.format(Locale.ROOT, "%.1f KB", bytes / k)
            bytes < k * k * k -> String.format(Locale.ROOT, "%.1f MB", bytes / (k * k))
            else -> String.format(Locale.ROOT, "%.2f GB", bytes / (k * k * k))
        }
    }

    // ---- The UI prefs partition (kuchusen_ui prefs file) ----------------------------------------

    // Exhaustive partition of the kuchusen_ui prefs keys, so no key can fall through the cracks.
    private fun isColorKey(key: String) = key.endsWith("_color") || key == "recent_colors"
    private fun isTypographyKey(key: String) = key.startsWith("font_")

    private fun uiKeyFilter(cat: Cat): ((String) -> Boolean)? = when (cat) {
        Cat.COLORS -> ::isColorKey
        Cat.TYPOGRAPHY -> ::isTypographyKey
        Cat.SHAPE -> { key -> !isColorKey(key) && !isTypographyKey(key) }
        else -> null
    }

    // ---- The upstream settings (AppPrefs, Realm) ------------------------------------------------

    /**
     * Every exportable AppPrefs field, by explicit property reference — the whole settable surface
     * of the upstream settings. Deliberately excluded: `id`, `lastVersion`, OPML restore runtime
     * state, backup timestamps, device-local folder/ringtone URIs, one-time flags and the proxy
     * password (credentials).
     */
    private val APP_PREF_FIELDS: List<KMutableProperty1<AppPrefs, *>> = listOf(
        AppPrefs::OPMLBackup,
        AppPrefs::theme, AppPrefs::themeBlack, AppPrefs::useDynamicThemes, AppPrefs::tintedColors,
        AppPrefs::useEpisodeCover, AppPrefs::showSkip, AppPrefs::showDownloadReport, AppPrefs::defaultPage,
        AppPrefs::backButtonOpensDrawer, AppPrefs::showErrorToasts, AppPrefs::printDebugLogs,
        AppPrefs::pauseOnHeadsetDisconnect, AppPrefs::unpauseOnHeadsetReconnect, AppPrefs::unpauseOnBluetoothReconnect,
        AppPrefs::hardwareForwardButton, AppPrefs::hardwarePreviousButton,
        AppPrefs::skipKeepsEpisode, AppPrefs::removeFromQueueMarkPlayed, AppPrefs::favoriteKeepsEpisode,
        AppPrefs::autoBackup, AppPrefs::autoBackupIntervall, AppPrefs::autoBackupLimit,
        AppPrefs::autoDelete, AppPrefs::autoDeleteLocal, AppPrefs::playbackSpeedArray, AppPrefs::fallbackSpeed,
        AppPrefs::useRingTone, AppPrefs::disableRingToneOnMusic,
        AppPrefs::streamOverDownload, AppPrefs::lowQualityOnMobile,
        AppPrefs::speedforwardSpeed, AppPrefs::skipforwardSpeed, AppPrefs::useAdaptiveProgressUpdate,
        AppPrefs::enqueueDownloaded, AppPrefs::fetchmediaSizes, AppPrefs::checkAvailableSpace,
        AppPrefs::autoUpdateInterval, AppPrefs::episodeCleanup, AppPrefs::episodeCacheSize,
        AppPrefs::enableAutoDl, AppPrefs::enableAutoDownloadOnBattery,
        AppPrefs::proxyType, AppPrefs::proxyHost, AppPrefs::proxyPort, AppPrefs::proxyUser,
        AppPrefs::gpodnet_notifications, AppPrefs::nextcloud_server_address,
        AppPrefs::deleteRemovesFromQueue,
        AppPrefs::playbackSpeed, AppPrefs::playbackPitch, AppPrefs::skipSilence,
        AppPrefs::fastForwardSecs, AppPrefs::rewindSecs, AppPrefs::streamingCacheSizeMB, AppPrefs::videoPlaybackMode,
        AppPrefs::content_country, AppPrefs::loadExternalApp, AppPrefs::audioQuality, AppPrefs::videoQuality,
    )

    // Values are type-tagged because JSON cannot tell Int from Long nor Float from Double.
    private fun tagValue(value: Any): JSONObject? {
        val entry = JSONObject()
        when (value) {
            is Boolean -> entry.put("t", "boolean").put("v", value)
            is Int -> entry.put("t", "int").put("v", value)
            is Long -> entry.put("t", "long").put("v", value)
            is Float -> entry.put("t", "float").put("v", value.toDouble())
            is String -> entry.put("t", "string").put("v", value)
            else -> return null
        }
        return entry
    }

    private fun untagValue(entry: JSONObject): Any? = when (entry.optString("t")) {
        "boolean" -> entry.getBoolean("v")
        "int" -> entry.getInt("v")
        "long" -> entry.getLong("v")
        "float" -> entry.getDouble("v").toFloat()
        "string" -> entry.getString("v")
        else -> null
    }

    private fun settingsJson(entries: JSONObject): String = JSONObject()
        .put("_format", FORMAT).put("_version", VERSION).put("entries", entries).toString(2)

    private fun uiSettingsJson(cat: Cat): String {
        val filter = uiKeyFilter(cat)!!
        val entries = JSONObject()
        for ((key, value) in KuchusenUi.prefsSnapshot()) {
            if (value == null || !filter(key)) continue
            val entry = tagValue(value) ?: continue
            entries.put(key, entry)
        }
        return settingsJson(entries)
    }

    private fun appSettingsJson(): String {
        val entries = JSONObject()
        val prefs = appPrefsFlow!!.value
        for (p in APP_PREF_FIELDS) {
            val value = p.get(prefs) ?: continue
            val entry = tagValue(value) ?: continue
            entries.put(p.name, entry)
        }
        // The one collection field, exported as a plain string set.
        entries.put("mobileUpdateTypes", JSONObject().put("t", "stringSet").put("v", JSONArray(prefs.mobileUpdateTypes.toList())))
        return settingsJson(entries)
    }

    private fun feedsJson(isCancelled: () -> Boolean, onProgress: (Progress) -> Unit): String {
        val arr = JSONArray()
        val context = getAppContext()
        val feeds = getFeedList().filterNot { it.isSynthetic() }
        val total = feeds.size.toLong()
        var done = 0L
        for (feed in feeds) {
            // Between feeds, never mid-write: the row we are on is finished or not started.
            if (isCancelled()) throw Cancelled()
            done++
            if (done % 20L == 0L || done == total)
                onProgress(Progress(context.getString(R.string.kuchusen_eim_prog_feeds, done, total), done, total,
                    context.getString(R.string.kuchusen_eim_unit_feeds), Cat.FEEDS.id))
            if (feed.downloadUrl.isNullOrBlank()) continue
            arr.put(JSONObject()
                .put("title", feed.title ?: "")
                .put("xmlUrl", feed.downloadUrl)
                .put("htmlUrl", feed.link ?: "")
                .put("type", feed.type ?: ""))
        }
        return JSONObject().put("_format", FORMAT).put("_version", VERSION).put("feeds", arr).toString(2)
    }

    // ---- The Realm database ---------------------------------------------------------------------

    /** Scratch directory for the realm snapshot — never reused across runs, always cleaned up. */
    private fun snapshotDir(): File = File(getAppContext().cacheDir, "kuchusen-db-snapshot")

    /**
     * A consistent, compacted copy of the live database. `writeCopyTo` takes a transactional
     * snapshot — copying the open realm file byte-wise could catch a half-written transaction.
     */
    private fun snapshotDatabase(): File {
        val dir = snapshotDir()
        dir.deleteRecursively()
        dir.mkdirs()
        @Suppress("UNCHECKED_CAST")
        val schema = config.schema as Set<KClass<out TypedRealmObject>>
        realm.writeCopyTo(RealmConfiguration.Builder(schema)
            .directory(dir.absolutePath).name(DB_ENTRY).schemaVersion(config.schemaVersion).build())
        return File(dir, DB_ENTRY)
    }

    /** Streams the realm snapshot into the zip, reporting real byte counts as it goes. */
    private fun writeDatabaseEntry(zip: ZipOutputStream, verifier: ArchiveVerifier,
                                   isCancelled: () -> Boolean, onProgress: (Progress) -> Unit) {
        val context = getAppContext()
        val snapshot = snapshotDatabase()
        try {
            val total = snapshot.length()
            val unit = context.getString(R.string.kuchusen_eim_unit_bytes)
            // NO setLevel HERE. The compression level is chosen once, before the first entry, in
            // [export] — see the note there. Changing it at this point is what silently destroyed
            // every backup this app wrote between 2026-09-04 and 2026-09-08.
            zip.putNextEntry(ZipEntry(DB_ENTRY))
            verifier.beginEntry(DB_ENTRY)
            val crc = CRC32()
            var written = 0L
            var reported = 0L
            snapshot.inputStream().buffered().use { input ->
                val buf = ByteArray(256 * 1024)
                while (true) {
                    // The realm file is the long step — minutes for a big library — so the flag is
                    // read once per buffer, between whole writes rather than inside one.
                    if (isCancelled()) throw Cancelled()
                    val n = input.read(buf)
                    if (n <= 0) break
                    crc.update(buf, 0, n)
                    zip.write(buf, 0, n)
                    written += n
                    if (written - reported >= 2L * 1024 * 1024 || written == total) {
                        reported = written
                        onProgress(Progress(context.getString(R.string.kuchusen_eim_prog_db,
                            humanSize(written), humanSize(total)), written, total, unit, Cat.DATABASE.id))
                    }
                }
            }
            zip.closeEntry()
            verifier.endEntry(crc.value, written)
        } finally { snapshotDir().deleteRecursively() }
    }

    // ---- Export ---------------------------------------------------------------------------------

    /**
     * Every byte of the archive, inflated straight back as it is written.
     *
     * This app spent four days writing backups that could not be restored and reporting success
     * every time, because nothing between the deflater and the file ever asked whether the bytes
     * could be read again. The recorded CRC and sizes were all correct — they describe what went
     * *into* the deflater, and the fault was in what came out of it. Only an inflate catches that.
     *
     * **Why in-stream and not a second pass over the finished file.** The data door writes into a
     * descriptor 応用管理 opened, which may be a pipe: there is nothing to reopen and re-read, and
     * that is the one path that actually failed. Inflating as we go verifies a pipe, a SAF document
     * and a plain file identically, in constant memory, and it names the entry that broke at the
     * moment it breaks rather than after another full pass. Inflating is several times cheaper than
     * the deflate it checks, so the archive costs a fraction more to write and stops being a
     * question mark.
     *
     * It sits directly beneath the [ZipOutputStream] and above the buffer, so `closeEntry()` is an
     * exact boundary: by the time it returns, every byte of that entry has come past here.
     */
    private class ArchiveVerifier(private val out: OutputStream) : OutputStream() {
        private val inflater = Inflater(true)
        private val crc = CRC32()
        private val scratch = ByteArray(64 * 1024)
        private var entry: String? = null
        private var produced = 0L
        private var closed = false

        /** Called after `putNextEntry`, so the local header has already gone past untouched. */
        fun beginEntry(name: String) {
            inflater.reset()
            crc.reset()
            produced = 0L
            entry = name
        }

        /**
         * Called after `closeEntry`, which has flushed the whole deflate stream and then written the
         * data descriptor. Those trailing descriptor bytes are never fed to the inflater: it has
         * already reached the end of the stream by then and [write] stops feeding it.
         */
        fun endEntry(expectedCrc: Long, expectedSize: Long) {
            val name = entry ?: return
            entry = null
            if (!inflater.finished()) fail(name, "the deflate stream never ended")
            if (produced != expectedSize) fail(name, "inflated to $produced bytes, not $expectedSize")
            if (crc.value != expectedCrc) fail(name, "checksum mismatch after inflating")
        }

        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            // The archive first, always: verification must not be able to lose a byte of it.
            out.write(b, off, len)
            val name = entry ?: return
            if (inflater.finished()) return
            inflater.setInput(b, off, len)
            try {
                while (!inflater.finished() && !inflater.needsInput()) {
                    val n = inflater.inflate(scratch)
                    if (n == 0) break   // asking for a preset dictionary we are never going to have
                    crc.update(scratch, 0, n)
                    produced += n
                }
            } catch (e: DataFormatException) {
                fail(name, e.message ?: "not a readable deflate stream")
            }
        }

        override fun flush() = out.flush()

        override fun close() {
            if (closed) return
            closed = true
            // The stream still gets closed even if the inflater is what went wrong.
            try { out.close() } finally { inflater.end() }
        }

        private fun fail(name: String, why: String): Nothing =
            throw IOException("archive verification failed on $name: $why")
    }


    private fun writeEntry(zip: ZipOutputStream, verifier: ArchiveVerifier, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        verifier.beginEntry(name)
        zip.write(bytes)
        zip.closeEntry()
        verifier.endEntry(CRC32().apply { update(bytes) }.value, bytes.size.toLong())
    }

    /**
     * Streams the selected categories into a ZIP; any failure surfaces as an exception.
     * The whole backup is this one file — the UI panel, the automation receiver and the data door
     * are thin callers of this same function, and `onProgress` is what the caller reports outward.
     *
     * `isCancelled` is polled at write boundaries only — between categories, between feeds, between
     * buffers of the realm snapshot — so a cancelled run unwinds at the next boundary rather than
     * being torn down mid-write, and raises [Cancelled] for the caller to answer. Deleting whatever
     * was half-written belongs to the caller, which is the only side that knows where it went.
     */
    fun export(cats: Set<Cat>, openOutput: () -> OutputStream, isCancelled: () -> Boolean = { false },
               onProgress: (Progress) -> Unit = {}) {
        val context = getAppContext()
        val selected = Cat.entries.filter { it in cats }
        val steps = selected.size.toLong()
        var step = 0L
        // The verifier sits directly under the zip and above the buffer, so it sees every byte the
        // ZipOutputStream emits at the moment it emits it — which is what makes `closeEntry()` an
        // exact boundary. Below the buffer those bytes would still be sitting in it.
        val verifier = ArchiveVerifier(openOutput().buffered())
        try {
            ZipOutputStream(verifier).use { zip ->
                // ---- The compression level is set HERE and never again -------------------------------
                // A realm file is large and already compact, so speed beats ratio for the archive as a
                // whole — every other entry is a few kB of JSON and loses nothing by it.
                //
                // It has to be set before the FIRST entry, and this is not a style preference. On this
                // phone, `ZipOutputStream.setLevel()` called after an entry has already been deflated
                // produces a **corrupt** next entry: zlib's deflateParams switches compression function
                // (levels 1-3 use deflate_fast, 4-9 deflate_slow) mid-archive and the entry that follows
                // is a deflate stream no inflater can read — `invalid distance too far back`. It only
                // bites an entry big enough to span more than one deflate pass, which in this archive is
                // exactly one entry: the realm snapshot. Desktop JVMs do not reproduce it, so nothing
                // short of running the export on the phone would have caught it.
                zip.setLevel(Deflater.BEST_SPEED)
                val manifest = JSONObject()
                    .put("format", FORMAT)
                    .put("version", VERSION)
                    .put("app", context.packageName)
                    .put("appVersion", BuildConfig.VERSION_NAME)
                    .put("createdTs", System.currentTimeMillis())
                    .put("categories", JSONArray(selected.map { it.id }))
                writeEntry(zip, verifier, "manifest.json", manifest.toString(2).toByteArray())
                for (cat in selected) {
                    if (isCancelled()) throw Cancelled()
                    step++
                    // `current` is the POSITION of the category being written — 「Category 4/9 — …」 means
                    // this one is number four, not that four are done.
                    onProgress(Progress(context.getString(R.string.kuchusen_eim_prog_cat, step, steps,
                        context.getString(cat.labelRes)), step, steps,
                        context.getString(R.string.kuchusen_eim_unit_cats), cat.id))
                    when (cat) {
                        Cat.FEEDS -> writeEntry(zip, verifier, "feeds.json", feedsJson(isCancelled, onProgress).toByteArray())
                        Cat.DATABASE -> writeDatabaseEntry(zip, verifier, isCancelled, onProgress)
                        Cat.COLORS, Cat.SHAPE -> writeEntry(zip, verifier, "${cat.id}.json", uiSettingsJson(cat).toByteArray())
                        Cat.TYPOGRAPHY -> {
                            writeEntry(zip, verifier, "typography.json", uiSettingsJson(cat).toByteArray())
                            for (font in KuchusenUi.fontFiles())
                                writeEntry(zip, verifier, "fonts/${font.name}", font.readBytes())
                        }
                        Cat.APP_SETTINGS -> writeEntry(zip, verifier, "app_settings.json", appSettingsJson().toByteArray())
                    }
                }
            }
        } finally {
            // Closed here as well as by the zip above, because a ZipOutputStream whose
            // finish() throws never reaches its own out.close() — and a verifier failure
            // throws exactly there. close() is idempotent.
            verifier.close()
        }
    }

    // ---- Import ---------------------------------------------------------------------------------

    private class Staged(val manifest: JSONObject, val entries: Map<String, ByteArray>, val dbFile: File?)

    private fun stagingDir(): File = File(getAppContext().cacheDir, "kuchusen-import-staging")

    /** Single streaming pass over the zip; nothing is applied until the manifest validates. */
    private fun stage(openInput: () -> InputStream): Staged {
        val entries = mutableMapOf<String, ByteArray>()
        var dbFile: File? = null
        val staging = stagingDir().also { it.deleteRecursively(); it.mkdirs() }
        ZipInputStream(openInput().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    // The database entry can be hundreds of MB — it goes to disk, never through memory.
                    if (entry.name == DB_ENTRY) dbFile = File(staging, DB_ENTRY)
                        .also { f -> f.outputStream().buffered().use { zip.copyTo(it) } }
                    else entries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val manifestBytes = entries["manifest.json"]
            ?: throw IllegalArgumentException(getAppContext().getString(R.string.kuchusen_eim_import_not_ours))
        val manifest = JSONObject(String(manifestBytes))
        if (manifest.optString("format") != FORMAT)
            throw IllegalArgumentException(getAppContext().getString(R.string.kuchusen_eim_import_not_ours))
        return Staged(manifest, entries, dbFile)
    }

    private fun readEntries(bytes: ByteArray): Map<String, Any> {
        val entriesObj = JSONObject(String(bytes)).optJSONObject("entries") ?: return emptyMap()
        val out = mutableMapOf<String, Any>()
        for (key in entriesObj.keys()) {
            val v = untagValue(entriesObj.getJSONObject(key)) ?: continue
            out[key] = v
        }
        return out
    }

    /**
     * Block until every store this import touched is on disk.
     *
     * **応用管理 force-stops this app the instant the import replies success** — `Process.killProcess`,
     * a `SIGKILL` — which is deliberate and belongs on its side: a running process writes its cached
     * `SharedPreferences` back out at orderly shutdown and would silently undo the import that just
     * happened. The consequence for us is that anything left un-flushed at that moment is simply
     * lost, and **the restore reports success over missing data** — invisible in testing, because a
     * hand-run import is followed by a normal lifecycle that flushes properly, and only the automated
     * path kills the process cold.
     *
     * The audit is "what does this restore path start asynchronously", not "where is `apply()`":
     *
     * - The UI prefs (`kuchusen_ui`) were the one asynchronous write — `androidx.core.content.edit`
     *   defaults to `commit = false`, so [KuchusenUi.importPrefValues] read as durable and was not.
     *   It now commits, and [KuchusenUi.flushPrefs] additionally lands any earlier `apply()` from a
     *   setter this path does not own.
     * - Realm is already durable on return: [upsertBlk] and `addNewFeed` both go through
     *   `realm.writeBlocking`.
     * - Font files and the restored database file are plain `File` writes. A `SIGKILL` does not lose
     *   those — the bytes are already in the kernel's hands — so they owe nothing here.
     *
     * Called from [importStaged], so the hand-run Export/Import panel gets it as well as the data
     * door. Both callers are off the main thread, so the synchronous write is free on each; a panel
     * that imported on the main thread would be trading a truncated restore for an ANR.
     */
    fun flushToDisk(context: Context) {
        KuchusenUi.flushPrefs()
        eximportPrefs(context).edit(commit = true) { }
    }

    /** What an import did: the per-category summary, and whether the database was swapped. */
    class ImportResult(val summary: String, val databaseRestored: Boolean)

    /** Applies the selected categories from a staged export; returns per-category summary lines. */
    suspend fun import(cats: Set<Cat>, openInput: () -> InputStream): ImportResult = try {
        importStaged(cats, stage(openInput))
    } finally { stagingDir().deleteRecursively() }

    private suspend fun importStaged(cats: Set<Cat>, staged: Staged): ImportResult {
        val context = getAppContext()
        val parts = mutableListOf<String>()
        var databaseRestored = false

        if (Cat.FEEDS in cats) staged.entries["feeds.json"]?.let { bytes ->
            val arr = JSONObject(String(bytes)).optJSONArray("feeds") ?: JSONArray()
            val existingUrls = getFeedList().mapNotNull { it.downloadUrl }.toSet()
            var added = 0
            var known = 0
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val xmlUrl = o.optString("xmlUrl")
                if (xmlUrl.isBlank()) continue
                if (xmlUrl in existingUrls) { known++; continue }
                val feed = Feed(xmlUrl, null, o.optString("title").ifBlank { "Unknown podcast" })
                updateFeedFull(feed, removeUnlistedItems = false)
                added++
            }
            parts.add(context.getString(R.string.kuchusen_eim_sum_feeds, added, known))
        }

        for (cat in listOf(Cat.COLORS, Cat.TYPOGRAPHY, Cat.SHAPE)) {
            if (cat !in cats) continue
            val bytes = staged.entries["${cat.id}.json"] ?: continue
            val values = readEntries(bytes)
            KuchusenUi.importPrefValues(values)
            var summary = context.getString(R.string.kuchusen_eim_sum_settings, context.getString(cat.labelRes), values.size)
            if (cat == Cat.TYPOGRAPHY) {
                var fonts = 0
                for ((name, data) in staged.entries) {
                    if (!name.startsWith("fonts/")) continue
                    KuchusenUi.writeFontFile(name.removePrefix("fonts/"), data)
                    fonts++
                }
                if (fonts > 0) summary += context.getString(R.string.kuchusen_eim_sum_fonts, fonts)
            }
            parts.add(summary)
        }

        if (Cat.APP_SETTINGS in cats) staged.entries["app_settings.json"]?.let { bytes ->
            val entriesObj = JSONObject(String(bytes)).optJSONObject("entries") ?: JSONObject()
            var applied = 0
            upsertBlk(appPrefsFlow!!.value) { managed ->
                for (p in APP_PREF_FIELDS) {
                    val entry = entriesObj.optJSONObject(p.name) ?: continue
                    val value = untagValue(entry) ?: continue
                    @Suppress("UNCHECKED_CAST")
                    runCatching { (p as KMutableProperty1<AppPrefs, Any?>).set(managed, value) }.onSuccess { applied++ }
                }
                val sets = entriesObj.optJSONObject("mobileUpdateTypes")
                if (sets != null && sets.optString("t") == "stringSet") {
                    val arr = sets.optJSONArray("v") ?: JSONArray()
                    managed.mobileUpdateTypes.clear()
                    for (i in 0 until arr.length()) managed.mobileUpdateTypes.add(arr.getString(i))
                    applied++
                }
            }
            parts.add(context.getString(R.string.kuchusen_eim_sum_settings, context.getString(Cat.APP_SETTINGS.labelRes), applied))
        }

        // The database goes last: it replaces the file every other category just wrote through,
        // and the app must restart onto the restored one.
        if (Cat.DATABASE in cats) staged.dbFile?.let { source ->
            val target = File(realm.configuration.path)
            target.delete()
            source.copyTo(target, overwrite = true)
            databaseRestored = true
            parts.add(context.getString(R.string.kuchusen_eim_sum_database, humanSize(target.length())))
        }

        // Nothing may be told this succeeded until it is actually on disk — see [flushToDisk].
        flushToDisk(context)

        val summary = if (parts.isEmpty()) context.getString(R.string.kuchusen_eim_sum_nothing) else parts.joinToString("\n")
        return ImportResult(summary, databaseRestored)
    }
}
