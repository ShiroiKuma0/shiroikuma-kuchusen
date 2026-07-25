package ac.mdiq.podcini.config.automation

import ac.mdiq.podcini.R
import ac.mdiq.podcini.config.settings.KuchusenExport
import ac.mdiq.podcini.config.settings.KuchusenExport.Cat
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "StateExportReceiver"

/** 白い熊's 保存復元 wire contract: a headless, token-gated export of this app's whole state. */
class StateExportReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val action = intent.action ?: return
        val replyAction = intent.getStringExtra("reply_action")
        val replyPackage = intent.getStringExtra("reply_package")
        val replyId = intent.getStringExtra("reply_id")
        if (replyAction.isNullOrBlank() || replyPackage.isNullOrBlank() || replyId.isNullOrBlank()) {
            Log.w(TAG, "$action without a reply channel (reply_action/reply_package/reply_id) — ignored")
            return
        }
        // Read before goAsync(): afterwards the receiver no longer owns the pending result.
        val ordered = isOrderedBroadcast
        val pending = goAsync()
        // Exactly one terminal reply per request — an async success and a synchronous error can
        // never both fire.
        val replied = AtomicBoolean(false)

        fun reply(result: String) {
            if (!replied.compareAndSet(false, true)) return
            // Unconditional: this one line is how a failing automation run is diagnosed on-device.
            Log.i(TAG, "$replyId → $result")
            try {
                // A fresh broadcast is the only channel EMUI carries reliably; no ResultReceiver,
                // no PendingIntent, no Messenger.
                app.sendBroadcast(Intent(replyAction).apply {
                    setPackage(replyPackage)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    putExtra("reply_id", replyId)
                    putExtra("result", result)
                })
                // Correct AOSP behaviour and free, but EMUI severs it between third-party apps —
                // never the only reply.
                if (ordered) pending.resultData = result
            } catch (e: Throwable) {
                Log.e(TAG, "reply broadcast failed", e)
            } finally { runCatching { pending.finish() } }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when {
                    !AutomationAuth.isEnabled(app) -> reply("ERROR:automation disabled")
                    !AutomationAuth.matches(app, intent.getStringExtra("token")) -> reply("ERROR:bad token")
                    action.endsWith(".action.LIST_CATEGORIES") -> reply(categoryList(app))
                    action.endsWith(".action.EXPORT_STATE") -> exportState(app, intent, replyPackage, replyId, ::reply)
                    else -> reply("ERROR:unknown action")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "automation request failed", e)
                reply("ERROR:${shortReason(e)}")
            }
        }
    }

    /** `id<TAB>label` per category. This app's categories are flat — no sub-options. */
    private fun categoryList(context: Context): String =
        "OK:" + Cat.entries.joinToString("\n") { "${it.id}\t${context.getString(it.labelRes)}" }

    private fun exportState(context: Context, intent: Intent, replyPackage: String, replyId: String, reply: (String) -> Unit) {
        val cats = when (val selection = selectCategories(intent.getStringExtra("items"))) {
            is Selection.Bad -> return reply("ERROR:unknown category in items: ${selection.unknown.joinToString(",")}")
            is Selection.Ok -> selection.cats
        }
        val name = KuchusenExport.exportFileName()
        val target = when (val destination = resolveTarget(context, intent.getStringExtra("path")?.trim(), name)) {
            is Destination.Failed -> return reply("ERROR:${destination.reason}")
            is Destination.Ready -> destination.target
        }

        val progress = ProgressSender(context, intent.getStringExtra("progress_action"), replyPackage, replyId)
        var counter: CountingOutputStream? = null
        KuchusenExport.export(cats, { CountingOutputStream(target.open()).also { counter = it } }) {
            progress.send(it.text, it.current, it.total, it.unit)
        }

        // The caller cannot stat the file, so both numbers are ours to compute.
        val bytes = target.length().takeIf { it > 0L } ?: counter?.count ?: 0L
        progress.sendFinal(context.getString(R.string.kuchusen_eim_prog_done, KuchusenExport.humanSize(bytes)), bytes)
        reply("OK:${target.path()}|$bytes|${KuchusenExport.humanSize(bytes)}|${cats.size} categories")
    }

    // ---- Category selection ---------------------------------------------------------------------

    private sealed class Selection {
        class Ok(val cats: Set<Cat>) : Selection()
        class Bad(val unknown: List<String>) : Selection()
    }

    /** Absent or empty `items` means everything. */
    private fun selectCategories(items: String?): Selection {
        val ids = items?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        if (ids.isEmpty()) return Selection.Ok(Cat.entries.toSet())
        val unknown = ids.filter { KuchusenExport.catById(it) == null }
        if (unknown.isNotEmpty()) return Selection.Bad(unknown)
        return Selection.Ok(ids.mapNotNull { KuchusenExport.catById(it) }.toSet())
    }

    // ---- Where the one ZIP goes -------------------------------------------------------------------

    /** The single file this run writes — either a plain path or a document in the SAF export tree. */
    private interface Target {
        fun open(): OutputStream
        fun length(): Long
        fun path(): String
    }

    private class FileTarget(private val file: File) : Target {
        override fun open(): OutputStream = file.outputStream()
        override fun length(): Long = file.length()
        override fun path(): String = file.absolutePath
    }

    private class SafTarget(private val context: Context, private val dir: DocumentFile, private val name: String) : Target {
        private var created: DocumentFile? = null
        override fun open(): OutputStream {
            val file = dir.createFile("application/zip", name) ?: throw IOException("cannot create $name in the export directory")
            created = file
            return context.contentResolver.openOutputStream(file.uri) ?: throw IOException("cannot open $name for writing")
        }
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
                return Destination.Ready(FileTarget(File(dir, name)))
            }
            Log.w(TAG, "no All-files access — ignoring path=$path")
            if (safDir == null) return Destination.Failed("no-storage-access")
        }
        return safDir?.let { Destination.Ready(SafTarget(context, it, name)) } ?: Destination.Failed("no-directory")
    }

    private fun canWriteAnywhere(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    // ---- Progress: real numbers, never a percentage -----------------------------------------------

    private class ProgressSender(private val context: Context, private val action: String?,
                                 private val replyPackage: String, private val replyId: String) {
        private var lastSent = 0L
        private var lastUnit = ""

        fun send(text: String, current: Long, total: Long, unit: String, force: Boolean = false) {
            if (action.isNullOrBlank()) return
            val now = SystemClock.elapsedRealtime()
            if (!force && now - lastSent < 500L) return      // at most one every 500 ms
            lastSent = now
            lastUnit = unit
            context.sendBroadcast(Intent(action).apply {
                setPackage(replyPackage)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                putExtra("reply_id", replyId)
                putExtra("app", context.getString(R.string.app_name))
                putExtra("text", text)
                putExtra("current", current)
                putExtra("total", total)
                putExtra("unit", unit)
            })
        }

        /** Always sent, however the throttle fell. */
        fun sendFinal(text: String, bytes: Long) =
            send(text, bytes, bytes, lastUnit.ifEmpty { context.getString(R.string.kuchusen_eim_unit_bytes) }, force = true)
    }

    // ---- Small helpers ----------------------------------------------------------------------------

    private class CountingOutputStream(private val out: OutputStream) : OutputStream() {
        var count = 0L
            private set
        override fun write(b: Int) { out.write(b); count++ }
        override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); count += len }
        override fun flush() = out.flush()
        override fun close() = out.close()
    }

    private fun shortReason(e: Throwable): String =
        (e.message ?: e.javaClass.simpleName).replace('\n', ' ').trim().take(160)

    companion object {
        /** Renders a primary-storage document URI as the path 白い熊 sees in a file manager. */
        fun absolutePathOf(uri: Uri): String = runCatching {
            val docId = DocumentsContract.getDocumentId(uri)
            if (docId.startsWith("primary:")) "/storage/emulated/0/" + docId.removePrefix("primary:")
            else uri.toString()
        }.getOrDefault(uri.toString())
    }
}
