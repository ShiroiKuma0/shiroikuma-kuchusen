package ac.mdiq.podcini.config.automation

import ac.mdiq.podcini.R
import ac.mdiq.podcini.config.settings.KuchusenExport
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "AutomationDataService"

/**
 * Where a data export or import started at [AutomationProvider] actually runs.
 *
 * ## Why a foreground service and not the provider call
 *
 * The call returns in milliseconds; this can run for minutes — a podcast library's realm snapshot is
 * the biggest single thing this app owns. Two hard reasons it cannot be done anywhere cheaper:
 *
 * - **A binder call holds the caller.** 応用管理 is drawing a list; a multi-minute synchronous call
 *   would freeze its UI, report no progress, and refuse cancellation.
 * - **A backgrounded app writing for minutes is frozen mid-stream on this phone**, which yields a
 *   truncated archive underneath a success reply — the worst possible failure, because it is
 *   indistinguishable from a good backup until the day it is restored (応用管理, 2026-09-04).
 *
 * On EMUI a foreground service is still not enough on its own: the system force-releases the app's
 * partial wakelock seconds in and then starves the process, so the run stops part-way with no crash,
 * no ANR and no log. Hence the wakelock below, taken for the whole job and released in the same
 * `finally` that stops the service.
 *
 * ## The descriptor
 *
 * Already duplicated by [AutomationProvider] before it got here, because the original belongs to the
 * binder transaction and is closed the moment `call()` returns. This service owns the copy and
 * closes it in a `finally` — leaking one would hold the caller's file open indefinitely, and the
 * caller cannot checksum or encrypt a file that is still open.
 */
class AutomationDataService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * The ordering here is a recipe, not four independent rules, and only this order satisfies all
     * of them. Read the extras defensively → go foreground → drain the handover → *then* the early
     * returns. Each numbered step below says what breaks if it moves.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ---- 1. The extras, read defensively off a nullable intent --------------------------------
        // Read first because the notification needs `importing`, and because reading the job id and
        // returning on it is the natural way to write this method — which is exactly how a service
        // that has already covered the stale-job-id branch keeps its crash on a null intent.
        val importing = intent?.getBooleanExtra(EXTRA_IMPORTING, false) == true
        val jobId = intent?.getStringExtra(EXTRA_JOB)

        // ---- 2. Foreground, as the FIRST statement that touches the platform ----------------------
        // Once a caller has invoked startForegroundService the platform REQUIRES startForeground
        // within the window, whatever this service then decides, and enforces that by killing the
        // process with ForegroundServiceDidNotStartInTimeException. So the early returns are the
        // dangerous ones: **a caller retrying with a stale job id would kill the very app being
        // backed up**. Guarded rather than trusted, because by now the start may itself be refused
        // under the API 31+ background-start rules.
        runCatching { startForeground(AutomationNotifications.ID_DATA, notification(importing)) }
            .onFailure { Log.e(TAG, "startForeground refused", it) }

        // ---- 3. Drain the handover, unconditionally ----------------------------------------------
        // After the foreground call and outside its guard, so that a throw up there cannot land
        // BEFORE the descriptor is taken out of the map and leave it sitting there held open. The
        // two obligations pull in opposite directions — the handover wants an owner before anything
        // that can throw, the foreground call wants to precede anything that can return — and this
        // is the one ordering that satisfies both.
        val fd = jobId?.let { HANDOVER.remove(it) }

        // ---- 4. Only now, the early returns -------------------------------------------------------
        // A stale or already-claimed job id stops **silently**. The instinct is to answer
        // "ERROR:unknown job"; do not. That id's request has already had its one terminal reply, and
        // a second one would break the single-reply rule the whole contract rests on.
        if (jobId == null || fd == null) {
            Log.i(TAG, "nothing to run for job=$jobId — stopping silently")
            return stop(startId)
        }

        val replyAction = intent.getStringExtra(AutomationProvider.KEY_REPLY_ACTION)
        val replyPackage = intent.getStringExtra(AutomationProvider.KEY_REPLY_PACKAGE)
        val progressAction = intent.getStringExtra(AutomationProvider.KEY_PROGRESS_ACTION)
        val items = intent.getStringExtra(AutomationProvider.KEY_ITEMS)

        val replied = AtomicBoolean(false)
        // A `val` lambda rather than a local `fun`: co-located with an anonymous object capturing a
        // local `var`, the two shapes together have killed `lintVitalAnalyzeRelease` elsewhere in
        // the family, ten minutes into a release build and long after the code compiled.
        val reply: (String) -> Unit = { result ->
            // Exactly one terminal answer per job, whatever path got here — a synchronous failure
            // and an asynchronous success must never both fire.
            if (replied.compareAndSet(false, true)) {
                AutomationJobs.finish(jobId)
                Log.i(TAG, "$jobId → $result")
                if (!replyAction.isNullOrEmpty() && !replyPackage.isNullOrEmpty()) {
                    runCatching {
                        sendBroadcast(Intent(replyAction).apply {
                            setPackage(replyPackage)
                            // Without this a caller that has been backgrounded never hears the
                            // answer, and on a clean phone it may not have been launched at all.
                            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                            putExtra(AutomationProvider.KEY_JOB_ID, jobId)
                            // The same id under the receiver's name too, so one reply reader on the
                            // caller's side serves both doors.
                            putExtra("reply_id", jobId)
                            putExtra(AutomationProvider.KEY_RESULT, result)
                        })
                    }.onFailure { Log.e(TAG, "reply broadcast failed", it) }
                }
            }
        }

        // ---- 5. Hand the descriptor to the coroutine, and close it on every path that is not that -
        // The window is between draining the map above and the coroutine taking ownership; one flag
        // covers every way out of it, which one guard per failure does not.
        var handedOff = false
        try {
            val wakelock = getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:automation-data")
                ?.apply { setReferenceCounted(false); acquire(WAKELOCK_TIMEOUT_MS) }

            val progress = AutomationProgress(this, progressAction, replyPackage, jobId)
            scope.launch {
                try {
                    if (importing) runImport(fd, items, reply)
                    else runExport(jobId, fd, items, progress, reply)
                } catch (e: KuchusenExport.Cancelled) {
                    reply("ERROR:cancelled")
                } catch (t: Throwable) {
                    Log.e(TAG, "automation data job failed", t)
                    reply("ERROR:${shortReason(t)}")
                } finally {
                    progress.stopHeartbeat()
                    // A leaked descriptor holds the caller's file open, and it cannot checksum or
                    // encrypt a file that is still open. Idempotent — the stream wrappers close it
                    // too.
                    runCatching { fd.close() }
                    runCatching { if (wakelock?.isHeld == true) wakelock.release() }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }
            handedOff = true
        } catch (t: Throwable) {
            Log.e(TAG, "could not start the job", t)
            reply("ERROR:${shortReason(t)}")
        } finally {
            if (!handedOff) {
                runCatching { fd.close() }
                AutomationJobs.finish(jobId)
                stop(startId)
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Write the backup into the caller's descriptor, counting the bytes on the way past.
     *
     * Counted rather than stat'ed afterwards: the caller owns the file and we may not be able to see
     * it at all — it can be an anonymous pipe, or a descriptor into a directory this app cannot list.
     */
    private fun runExport(jobId: String, fd: ParcelFileDescriptor, items: String?,
                          progress: AutomationProgress, reply: (String) -> Unit) {
        val cats = resolveExport(items) ?: return reply("ERROR:unknown category in items: $items")
        // The descriptor may be a pipe, so a write blocks for as long as the caller is slow to drain
        // it — a stall with no relation to how much data this app holds.
        progress.startHeartbeat()
        val counter = CountingOutputStream()
        ParcelFileDescriptor.AutoCloseOutputStream(fd).use { out ->
            counter.wrap(out)
            KuchusenExport.export(cats, { counter }, { AutomationJobs.isCancelled(jobId) }) { progress.send(it) }
        }
        progress.sendDone(counter.count)
        reply("OK:${counter.count}|${KuchusenExport.humanSize(counter.count)}|${cats.size} categories")
    }

    /**
     * Stream the archive straight into the importer.
     *
     * Deliberately not read into memory first, unlike the family reference: this app's archive
     * carries a whole realm snapshot and can be hundreds of megabytes. [KuchusenExport.import] makes
     * one streaming pass, stages the database entry to disk rather than the heap, and applies
     * nothing until the manifest validates — so a truncated descriptor fails before it can
     * half-restore the app.
     */
    private suspend fun runImport(fd: ParcelFileDescriptor, items: String?, reply: (String) -> Unit) {
        val result = KuchusenExport.import(resolveImport(items)) {
            ParcelFileDescriptor.AutoCloseInputStream(fd)
        }
        // The caller force-stops us straight after this — `Process.killProcess`, a SIGKILL. That is
        // deliberate and belongs on its side: a running process writes its cached SharedPreferences
        // back out at orderly shutdown and would silently undo the import that just happened, and
        // here it would also be holding the old realm file open underneath the one just restored.
        // What it means for us is that nothing may be told this succeeded until it is on disk —
        // which is why KuchusenExport.import flushes every store it touched before it returns.
        reply("OK:" + result.summary.replace('\n', ' ').trim().take(400))
    }

    /** Absent or empty `items` means our DEFAULT set, which is not the same as everything. */
    private fun resolveExport(items: String?): Set<KuchusenExport.Cat>? {
        if (items.isNullOrBlank()) return KuchusenExport.defaultCats()
        val wanted = items.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val found = wanted.mapNotNull { KuchusenExport.catById(it) }
        return if (found.size == wanted.size) found.toSet() else null
    }

    /**
     * An import asks for everything by default, not for the default set.
     *
     * The two differ on purpose: an export with no `items` should write what this app *recommends*,
     * while a restore with no `items` should put back whatever the archive actually holds. The
     * importer skips every category the file does not carry, so asking for all of them restores
     * exactly the file and nothing more.
     */
    private fun resolveImport(items: String?): Set<KuchusenExport.Cat> {
        if (items.isNullOrBlank()) return KuchusenExport.Cat.entries.toSet()
        return items.split(',').mapNotNull { KuchusenExport.catById(it.trim()) }.toSet()
            .ifEmpty { KuchusenExport.Cat.entries.toSet() }
    }

    /** Counts what goes past on its way into the caller's descriptor. */
    private class CountingOutputStream : OutputStream() {
        private var out: OutputStream? = null
        var count = 0L
            private set

        fun wrap(stream: OutputStream) { out = stream }
        override fun write(b: Int) { out!!.write(b); count++ }
        override fun write(b: ByteArray, off: Int, len: Int) { out!!.write(b, off, len); count += len }
        override fun flush() { out?.flush() }
    }

    private fun notification(importing: Boolean) = AutomationNotifications.build(
        this, if (importing) R.string.kuchusen_auto_notif_import else R.string.kuchusen_auto_notif_export)

    private fun shortReason(e: Throwable): String =
        (e.message ?: e.javaClass.simpleName).replace('\n', ' ').trim().take(160)

    /**
     * Every bail-out runs after the foreground call now, so this has to undo it — otherwise a
     * stale job id leaves a live notification and a foreground service behind.
     */
    private fun stop(startId: Int): Int {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_JOB = "job"
        private const val EXTRA_IMPORTING = "importing"

        /** A ceiling, not a budget: the wakelock is released in the job's `finally` long before this. */
        private const val WAKELOCK_TIMEOUT_MS = 30L * 60L * 1000L

        /**
         * The descriptor's way across, because an Intent is the wrong vehicle for one.
         *
         * A `ParcelFileDescriptor` in an Intent extra is duplicated by the system on delivery and
         * the copy's lifetime stops being ours to reason about. Handing it through a map keyed by
         * the job id keeps exactly one open descriptor with exactly one owner — the service, which
         * closes it in a `finally`.
         */
        private val HANDOVER = ConcurrentHashMap<String, ParcelFileDescriptor>()

        fun start(context: Context, jobId: String, fd: ParcelFileDescriptor, importing: Boolean, extras: Bundle?) {
            HANDOVER[jobId] = fd
            runCatching {
                context.startForegroundService(Intent(context, AutomationDataService::class.java).apply {
                    putExtra(EXTRA_JOB, jobId)
                    putExtra(EXTRA_IMPORTING, importing)
                    putExtra(AutomationProvider.KEY_ITEMS, extras?.getString(AutomationProvider.KEY_ITEMS))
                    putExtra(AutomationProvider.KEY_REPLY_ACTION, extras?.getString(AutomationProvider.KEY_REPLY_ACTION))
                    putExtra(AutomationProvider.KEY_REPLY_PACKAGE, extras?.getString(AutomationProvider.KEY_REPLY_PACKAGE))
                    putExtra(AutomationProvider.KEY_PROGRESS_ACTION, extras?.getString(AutomationProvider.KEY_PROGRESS_ACTION))
                })
            }.onFailure {
                // Nothing will ever come for it — do not leave the descriptor in the map.
                HANDOVER.remove(jobId)
                throw it
            }
        }
    }
}
