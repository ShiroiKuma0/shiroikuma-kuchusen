package ac.mdiq.podcini.config.automation

import ac.mdiq.podcini.R
import ac.mdiq.podcini.config.settings.KuchusenExport
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

private const val TAG = "AutomationProgress"

/**
 * Progress for both automation doors — the §1 broadcast receiver and the §2a data door — from one
 * implementation.
 *
 * **One sender, deliberately.** There were two, one per door, and two implementations of the same
 * watchdog drift; the one that drifts is always the one nobody is looking at. What differs between
 * the doors is only the correlation id, so that is what this is parameterised on: it is sent as both
 * `reply_id` and `job_id`, so a single progress reader on the caller's side serves both.
 *
 * ## Real numbers, never a percentage
 *
 * 白い熊's explicit requirement. `text` is the numbers-first line read in the panel; `item` is the
 * **category id** being written right now, which is how the panel knows which row to highlight — it
 * cannot work that out from `current`, because `current` is whatever is being counted at that
 * moment (categories while walking them, feeds or bytes while writing one of them).
 *
 * ## The throttle and the heartbeat are opposite things
 *
 * They solve opposite problems and confusing them fails the two-minute rule.
 *
 * - The **throttle** caps a chatty engine at one message per 500 ms.
 * - The **heartbeat** covers an engine that is not chatty at all. 自由作業盤 treats every progress
 *   broadcast as proof this app is still alive and presumes an app silent for two minutes dead.
 *
 * This app needs both, and the heartbeat is not optional here for two independent reasons:
 *
 * 1. `KuchusenExport` reports **per category**, and its database category is one enormous step. The
 *    realm snapshot alone — `writeCopyTo`, before a single byte reaches the archive — is minutes on
 *    a real library and reports nothing at all while it runs. A correctly-implemented throttle does
 *    not help with silence.
 * 2. On the data door the archive is written into **a descriptor the caller supplied**, which may be
 *    a pipe — so a write blocks for exactly as long as 応用管理 is slow to drain it, and that stall
 *    has no relation to how much data this app holds. "My export is fast" is not sufficient
 *    reasoning there.
 *
 * The heartbeat **re-sends the last true line**. It never invents a moving number: a fabricated
 * count is worse than a repeated one, because it cannot be told apart from progress.
 */
internal class AutomationProgress(
    private val context: Context,
    private val action: String?,
    private val replyPackage: String?,
    private val correlationId: String,
) {

    private var lastSentAt = 0L
    private var last: KuchusenExport.Progress? = null
    private var beat: Thread? = null

    /**
     * `progress_action` is useless without `reply_package`, and the failure is total.
     *
     * Since API 26 an implicit broadcast is not delivered to a manifest-declared receiver **at all**,
     * so a progress broadcast without `setPackage` is not a weak progress broadcast — it is no
     * progress broadcast. Passing `null` to `setPackage` does not send one more widely; it sends one
     * nothing can hear. If there is no package to address, there is nobody to tell, so we send
     * nothing rather than shouting into a closed room.
     */
    private val live: Boolean get() = !action.isNullOrBlank() && !replyPackage.isNullOrBlank()

    @Synchronized
    fun send(p: KuchusenExport.Progress, force: Boolean = false) {
        if (!live) return
        last = p
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastSentAt < THROTTLE_MS) return
        emit(p, now)
    }

    /** Always sent, however the throttle fell — §3 requires a final message. */
    fun sendDone(bytes: Long) = send(
        KuchusenExport.Progress(
            context.getString(R.string.kuchusen_eim_prog_done, KuchusenExport.humanSize(bytes)),
            bytes, bytes, context.getString(R.string.kuchusen_eim_unit_bytes)
        ), force = true
    )

    /** Start re-sending the last true line while the export is otherwise quiet. */
    @Synchronized
    fun startHeartbeat() {
        if (!live || beat != null) return
        beat = Thread {
            runCatching {
                while (!Thread.currentThread().isInterrupted) {
                    Thread.sleep(TICK_MS)
                    resendIfQuiet()
                }
            }
        }.apply {
            isDaemon = true
            name = "kuchusen-automation-heartbeat"
            start()
        }
    }

    @Synchronized
    fun stopHeartbeat() {
        beat?.interrupt()
        beat = null
    }

    @Synchronized
    private fun resendIfQuiet() {
        val p = last ?: return          // nothing true has been said yet; inventing one is worse
        val now = SystemClock.elapsedRealtime()
        if (now - lastSentAt < HEARTBEAT_MS) return
        emit(p, now)
    }

    @Synchronized
    private fun emit(p: KuchusenExport.Progress, now: Long) {
        lastSentAt = now
        runCatching {
            context.sendBroadcast(Intent(action!!).apply {
                // Unconditional, and guaranteed non-null by `live` — see the note there.
                setPackage(replyPackage)
                // Without this a backgrounded or never-launched caller never hears us.
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                // Both ids, so one progress reader serves the receiver and the data door alike.
                putExtra("reply_id", correlationId)
                putExtra("job_id", correlationId)
                putExtra("app", context.getString(R.string.app_name))
                putExtra("item", p.item)
                putExtra("text", p.text)
                putExtra("current", p.current)
                putExtra("total", p.total)
                putExtra("unit", p.unit)
            })
        }.onFailure { Log.w(TAG, "progress broadcast failed", it) }
    }

    private companion object {
        /** At most one message every 500 ms, however chatty the engine gets. */
        const val THROTTLE_MS = 500L

        /**
         * Re-send after this much silence. §3 asks for at least one every 30 s and presumes an app
         * silent for two minutes dead, so this leaves room for a tick to be missed and still clear
         * the deadline by a wide margin.
         */
        const val HEARTBEAT_MS = 15_000L

        /** How often the heartbeat looks; the decision is [HEARTBEAT_MS], not this. */
        const val TICK_MS = 5_000L
    }
}
