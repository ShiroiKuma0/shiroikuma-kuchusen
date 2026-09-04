package ac.mdiq.podcini.config.automation

import ac.mdiq.podcini.R
import ac.mdiq.podcini.config.settings.KuchusenExport
import ac.mdiq.podcini.config.settings.KuchusenExport.Cat
import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "StateExportService"

/**
 * Where the §1 `EXPORT_STATE` export actually runs.
 *
 * ## Why this is not in the receiver
 *
 * **`goAsync()` does not extend the broadcast timeout.** A manifest receiver — `PendingResult` held
 * or not — must reach `finish()` within Android's broadcast window: about 10 s with the app in the
 * foreground, about 60 s when it is not. Overrun it and the system raises an ANR against *this* app
 * and kills the process **mid-export**: nothing replies, the archive is left half-written, and
 * 自由作業盤 waits for an answer that can never come.
 *
 * That is not a theoretical risk here, it is the ordinary case. This app's archive carries a realm
 * snapshot, and `KuchusenExport.snapshotDatabase` — `writeCopyTo`, before a single byte reaches the
 * zip — is minutes on a real library. `goAsync()` alone is only ever acceptable for an export that
 * *cannot* exceed a few seconds, which rules this one out at the first subscription.
 *
 * So the receiver checks the gate and hands over; everything below happens here, with the terminal
 * reply, the progress broadcasts and the cancel all living on this side.
 *
 * ## And a foreground service is still not quite enough on this phone
 *
 * On EMUI the system force-releases the app's partial wakelock seconds in and then starves the
 * process, so a long run stops part-way at no consistent point, with no crash, no ANR and no log —
 * `dumpsys power` shows the wakelock under "Force Released WakeLocks". Hence the wakelock taken for
 * the whole job and released in the same `finally` that stops the service.
 */
class StateExportService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ---- 1. Extras first, defensively off a nullable intent -----------------------------------
        val replyAction = intent?.getStringExtra(EXTRA_REPLY_ACTION)
        val replyPackage = intent?.getStringExtra(EXTRA_REPLY_PACKAGE)
        val replyId = intent?.getStringExtra(EXTRA_REPLY_ID)

        // ---- 2. Foreground before anything that can return ----------------------------------------
        // Once ContextCompat.startForegroundService has been called the platform requires
        // startForeground within the window whatever this service decides, and kills the process
        // with ForegroundServiceDidNotStartInTimeException if it does not come. Guarded, because the
        // start itself can be refused under the API 31+ background-start rules.
        runCatching { startForeground(AutomationNotifications.ID_EXPORT, notification()) }
            .onFailure { Log.e(TAG, "startForeground refused", it) }

        // ---- 3. Now the early returns -------------------------------------------------------------
        // No reply channel means nobody to answer, so this stops silently rather than inventing one.
        if (replyAction.isNullOrBlank() || replyPackage.isNullOrBlank() || replyId.isNullOrBlank()) {
            Log.w(TAG, "export request without a reply channel — stopping silently")
            return stop(startId)
        }

        val replied = AtomicBoolean(false)
        val reply: (String) -> Unit = { result ->
            // Exactly one terminal reply per request: an async success, a synchronous error and a
            // cancel that raced them can never fire twice between them.
            if (replied.compareAndSet(false, true)) {
                // Unconditional: this one line is how a failing automation run is diagnosed on-device.
                Log.i(TAG, "$replyId → $result")
                runCatching {
                    // A fresh broadcast is the only channel EMUI carries reliably — no
                    // ResultReceiver, no PendingIntent, no Messenger.
                    sendBroadcast(Intent(replyAction).apply {
                        setPackage(replyPackage)
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                        putExtra("reply_id", replyId)
                        putExtra("result", result)
                    })
                }.onFailure { Log.e(TAG, "reply broadcast failed", it) }
            }
        }

        val run = Run(replyId)
        // Process-local and released in a `finally`, never persisted: a persisted flag wedges the
        // app for good after one crash, and every later request would answer "already running".
        if (!RUNNING.compareAndSet(null, run)) {
            reply("ERROR:export already running")
            return stop(startId)
        }

        val path = intent.getStringExtra(EXTRA_PATH)?.trim()
        val items = intent.getStringExtra(EXTRA_ITEMS)
        val progress = AutomationProgress(this, intent.getStringExtra(EXTRA_PROGRESS_ACTION), replyPackage, replyId)

        var handedOff = false
        try {
            val wakelock = getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:automation-export")
                ?.apply { setReferenceCounted(false); acquire(WAKELOCK_TIMEOUT_MS) }

            scope.launch {
                try {
                    exportState(run, path, items, progress, reply)
                } catch (t: Throwable) {
                    Log.e(TAG, "automation export failed", t)
                    reply("ERROR:${shortReason(t)}")
                } finally {
                    progress.stopHeartbeat()
                    runCatching { if (wakelock?.isHeld == true) wakelock.release() }
                    RUNNING.compareAndSet(run, null)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }
            handedOff = true
        } catch (t: Throwable) {
            Log.e(TAG, "could not start the export", t)
            reply("ERROR:${shortReason(t)}")
        } finally {
            if (!handedOff) {
                RUNNING.compareAndSet(run, null)
                stop(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun exportState(run: Run, path: String?, items: String?,
                            progress: AutomationProgress, reply: (String) -> Unit) {
        val cats = when (val selection = selectCategories(items)) {
            is Selection.Bad -> return reply("ERROR:unknown category in items: ${selection.unknown.joinToString(",")}")
            is Selection.Ok -> selection.cats
        }

        val name = KuchusenExport.exportFileName()
        val target = when (val destination = resolveTarget(this, path, name)) {
            is Destination.Failed -> return reply("ERROR:${destination.reason}")
            is Destination.Ready -> destination.target
        }

        // The realm snapshot is one enormous silent step, so the caller needs proof of life across
        // it — 自由作業盤 presumes an app silent for two minutes dead.
        progress.startHeartbeat()
        var counter: CountingOutputStream? = null
        try {
            KuchusenExport.export(cats, { CountingOutputStream(target.open()).also { counter = it } },
                { run.cancelled }) { progress.send(it) }
        } catch (e: Throwable) {
            // Cancelled or failed, the obligation is the same: the backup directory is left exactly
            // as it was found — no short archive, no stray part file, nothing for a later restore to
            // mistake for the latest backup.
            runCatching { target.discard() }
            if (e is KuchusenExport.Cancelled) return reply("ERROR:cancelled")
            throw e
        }
        // Only now does the archive get its real name. A failure past this point must not delete it —
        // the backup is complete and 白い熊 would rather have it than not.
        target.commit()

        // The caller cannot stat the file, so both numbers are ours to compute.
        val bytes = target.length().takeIf { it > 0L } ?: counter?.count ?: 0L
        progress.sendDone(bytes)
        reply("OK:${target.path()}|$bytes|${KuchusenExport.humanSize(bytes)}|${cats.size} categories")
    }

    // ---- Category selection -----------------------------------------------------------------------

    private sealed class Selection {
        class Ok(val cats: Set<Cat>) : Selection()
        class Bad(val unknown: List<String>) : Selection()
    }

    /**
     * Absent or empty `items` means our DEFAULT set — the categories we report as `on` from
     * `LIST_CATEGORIES` — which is not the same thing as everything. An app 白い熊 has never picked
     * items for exports what it recommends, not its entire footprint.
     */
    private fun selectCategories(items: String?): Selection {
        val ids = items?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        if (ids.isEmpty()) return Selection.Ok(KuchusenExport.defaultCats())
        val unknown = ids.filter { KuchusenExport.catById(it) == null }
        if (unknown.isNotEmpty()) return Selection.Bad(unknown)
        return Selection.Ok(ids.mapNotNull { KuchusenExport.catById(it) }.toSet())
    }

    // ---- Where the one ZIP goes -------------------------------------------------------------------

    /** The single file this run writes — either a plain path or a document in the SAF export tree. */
    private interface Target {
        fun open(): OutputStream
        /** Give the finished archive its real name. */
        fun commit()
        /** Remove whatever was half-written. Safe to call when nothing was opened. */
        fun discard()
        fun length(): Long
        fun path(): String
    }

    /**
     * Written to `<name>.part` and renamed only once the archive is closed and complete.
     *
     * A killed or cancelled export otherwise leaves a file indistinguishable from a real backup
     * until someone tries to restore it — and 白い熊 keeps every app's backups in one directory
     * sorted by date, so a truncated one silently becomes "the latest backup" of this app.
     */
    private class FileTarget(dir: File, name: String) : Target {
        private val finalFile = File(dir, name)
        private val partFile = File(dir, "$name.part")
        override fun open(): OutputStream = partFile.outputStream()
        override fun commit() {
            // Same directory, so the same filesystem: a rename here is atomic and cannot fail for
            // any ordinary reason. If it somehow does, say where the complete archive is rather
            // than spend a second pass copying hundreds of megabytes.
            if (!partFile.renameTo(finalFile)) throw IOException("export complete but stuck at ${partFile.absolutePath}")
        }
        override fun discard() { partFile.delete() }
        override fun length(): Long = finalFile.length()
        override fun path(): String = finalFile.absolutePath
    }

    /**
     * The SAF tree deliberately does NOT get a `.part` dance.
     *
     * A `DocumentsProvider` derives a document's name from the MIME type it was created with and
     * will happily turn `…zip.part` into `…zip.part.zip`, then do it again on rename — so the guard
     * would cost the very thing it protects: a predictable file name. Deleting the created document
     * on cancel or failure reaches the same end state, which is that the directory is left as found.
     */
    private class SafTarget(private val context: Context, private val dir: DocumentFile, private val name: String) : Target {
        private var created: DocumentFile? = null
        override fun open(): OutputStream {
            val file = dir.createFile("application/zip", name) ?: throw IOException("cannot create $name in the export directory")
            created = file
            return context.contentResolver.openOutputStream(file.uri) ?: throw IOException("cannot open $name for writing")
        }
        override fun commit() = Unit
        override fun discard() { runCatching { created?.delete() } }
        override fun length(): Long = created?.length() ?: 0L
        override fun path(): String = created?.let { absolutePathOf(it.uri) } ?: dir.uri.toString()
    }

    private sealed class Destination {
        class Ready(val target: Target) : Destination()
        class Failed(val reason: String) : Destination()
    }

    /**
     * Directory precedence: the `path` extra → the app's configured export directory → an error.
     * `path` needs All-files access; without it we fall back to the configured SAF directory,
     * exactly as the contract prescribes, and only fail when there is none.
     */
    private fun resolveTarget(context: Context, path: String?, name: String): Destination {
        val safDir = KuchusenExport.exportDir(context)
        if (!path.isNullOrEmpty()) {
            if (canWriteAnywhere(context)) {
                val dir = File(path)
                if (!dir.isDirectory && !dir.mkdirs()) throw IOException("cannot create directory $path")
                return Destination.Ready(FileTarget(dir, name))
            }
            Log.w(TAG, "no All-files access — ignoring path=$path")
            if (safDir == null) return Destination.Failed("no-storage-access")
        }
        return safDir?.let { Destination.Ready(SafTarget(context, it, name)) } ?: Destination.Failed("no-directory")
    }

    /**
     * Check the grant, do not discover it by failing. Declaring `MANAGE_EXTERNAL_STORAGE` is not
     * holding it, and `ERROR:no-storage-access` is the exact string 自由作業盤 keys on to offer a
     * 「全ファイルアクセスを許可」 button on the failed row.
     */
    private fun canWriteAnywhere(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    // ---- Small helpers ----------------------------------------------------------------------------

    /** The export in flight, and the flag its write loop watches. */
    private class Run(val replyId: String) {
        @Volatile var cancelled = false
    }

    private class CountingOutputStream(private val out: OutputStream) : OutputStream() {
        var count = 0L
            private set
        override fun write(b: Int) { out.write(b); count++ }
        override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); count += len }
        override fun flush() = out.flush()
        override fun close() = out.close()
    }

    private fun notification() =
        AutomationNotifications.build(this, R.string.kuchusen_auto_notif_export)

    private fun shortReason(e: Throwable): String =
        (e.message ?: e.javaClass.simpleName).replace('\n', ' ').trim().take(160)

    /**
     * Undo the foreground state a bail-out left behind — but only when nothing else is using it.
     * A second request bouncing off the busy guard must not strip the notification from the export
     * still running underneath it.
     */
    private fun stop(startId: Int): Int {
        if (RUNNING.get() == null) stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_PATH = "path"
        private const val EXTRA_ITEMS = "items"
        private const val EXTRA_PROGRESS_ACTION = "progress_action"
        private const val EXTRA_REPLY_ACTION = "reply_action"
        private const val EXTRA_REPLY_PACKAGE = "reply_package"
        private const val EXTRA_REPLY_ID = "reply_id"

        /** A ceiling, not a budget: released in the job's `finally` long before this. */
        private const val WAKELOCK_TIMEOUT_MS = 30L * 60L * 1000L

        /**
         * At most one export at a time, so a cancel without a `reply_id` is unambiguous.
         *
         * Deliberately process-local and never persisted — a persisted "in progress" flag survives
         * the crash that stranded it and wedges the app for good.
         */
        private val RUNNING = AtomicReference<Run?>(null)

        /** Hand a validated request over. The gate has already been checked by the receiver. */
        fun start(context: Context, request: Intent) {
            ContextCompat.startForegroundService(context,
                Intent(context, StateExportService::class.java).apply {
                    putExtra(EXTRA_PATH, request.getStringExtra(EXTRA_PATH))
                    putExtra(EXTRA_ITEMS, request.getStringExtra(EXTRA_ITEMS))
                    putExtra(EXTRA_PROGRESS_ACTION, request.getStringExtra(EXTRA_PROGRESS_ACTION))
                    putExtra(EXTRA_REPLY_ACTION, request.getStringExtra(EXTRA_REPLY_ACTION))
                    putExtra(EXTRA_REPLY_PACKAGE, request.getStringExtra(EXTRA_REPLY_PACKAGE))
                    putExtra(EXTRA_REPLY_ID, request.getStringExtra(EXTRA_REPLY_ID))
                })
        }

        /**
         * Signal the running export to unwind at its next write boundary.
         *
         * A no-op when nothing is running or when the id names a different run — a cancel arriving
         * after the export finished is the normal race, not an error, and 自由作業盤 fires one
         * whenever 白い熊 presses 中止 without knowing how far we got. It sends no reply of its own:
         * the one terminal reply belongs to the export it stops, which answers `ERROR:cancelled`.
         */
        fun requestCancel(replyId: String?) {
            val run = RUNNING.get() ?: return
            // Absent `reply_id` means "the export you are running", which is unambiguous because
            // only one is ever allowed at a time.
            if (replyId.isNullOrBlank() || replyId == run.replyId) {
                Log.i(TAG, "cancel requested for ${run.replyId}")
                run.cancelled = true
            }
        }

        /** Renders a primary-storage document URI as the path 白い熊 sees in a file manager. */
        fun absolutePathOf(uri: Uri): String = runCatching {
            val docId = DocumentsContract.getDocumentId(uri)
            if (docId.startsWith("primary:")) "/storage/emulated/0/" + docId.removePrefix("primary:")
            else uri.toString()
        }.getOrDefault(uri.toString())
    }
}
