package ac.mdiq.podcini.config.automation

import ac.mdiq.podcini.config.settings.KuchusenExport.Cat
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "StateExportReceiver"

/**
 * 白い熊's 保存復元 wire contract: a headless export of this app's whole state, the category list,
 * and the cancel that stops a run.
 *
 * The gate lives in [AutomationAuth]: since contract v2 the switch ships ON and the token is only
 * checked when 白い熊 has asked for one — a token sent to us anyway is ignored, never refused.
 *
 * This receiver is the **unauthenticated** half of the surface, deliberately: it only ever writes
 * where it was told to and reports what it did. Everything that moves data through a caller-supplied
 * descriptor — and `import`, which can overwrite this app's database — lives behind
 * [AutomationProvider], which knows who is calling.
 *
 * ## Nothing long-running happens here, and that is the point
 *
 * A manifest receiver must reach `finish()` within Android's broadcast window — about 10 s in the
 * foreground, about 60 s otherwise — and **`goAsync()` does not extend it**. Overrunning it raises
 * an ANR against this app and kills the process mid-export, leaving a half-written archive and a
 * caller waiting on a reply that can never come. This app's export takes minutes on a real library,
 * so it runs in [StateExportService] and this receiver does three cheap things only: read the gate,
 * answer the instant question, or hand over and get out.
 */
class StateExportReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val action = intent.action ?: return

        // CANCEL_EXPORT is answered first and answered here: it is fire-and-forget, carries no reply
        // channel of its own, and must be instant — the one terminal reply belongs to the export it
        // stops, which answers ERROR:cancelled through its own channel. A cancel that arrives when
        // nothing is running, or after the export finished, is a silent no-op: 自由作業盤 fires it
        // whenever 白い熊 presses 中止, without knowing how far we got.
        if (action.endsWith(".action.CANCEL_EXPORT")) {
            if (AutomationAuth.refuse(app, intent.getStringExtra("token")) != null) return
            StateExportService.requestCancel(intent.getStringExtra("reply_id"))
            return
        }

        val replyAction = intent.getStringExtra("reply_action")
        val replyPackage = intent.getStringExtra("reply_package")
        val replyId = intent.getStringExtra("reply_id")
        if (replyAction.isNullOrBlank() || replyPackage.isNullOrBlank() || replyId.isNullOrBlank()) {
            Log.w(TAG, "$action without a reply channel (reply_action/reply_package/reply_id) — ignored")
            return
        }

        // One function, one place: two checks written out at each entry point is how "disabled" and
        // "bad token" drift apart across forty-two apps.
        val refusal = AutomationAuth.refuse(app, intent.getStringExtra("token"))
        if (refusal != null) return reply(app, replyAction, replyPackage, replyId, refusal)

        when {
            // An enum walk and a handful of string lookups — instant, and answered from here.
            action.endsWith(".action.LIST_CATEGORIES") ->
                reply(app, replyAction, replyPackage, replyId, categoryList(app))

            // Hand over and return at once, leaving no ANR window open behind us. The service owns
            // everything from here: validating `items`, choosing the directory, the progress
            // broadcasts and the single terminal reply.
            action.endsWith(".action.EXPORT_STATE") ->
                runCatching { StateExportService.start(app, intent) }.onFailure {
                    // The service never started, so nothing else will ever answer this request.
                    Log.e(TAG, "could not start the export service", it)
                    reply(app, replyAction, replyPackage, replyId,
                        "ERROR:${(it.message ?: it.javaClass.simpleName).replace('\n', ' ').trim().take(160)}")
                }

            else -> reply(app, replyAction, replyPackage, replyId, "ERROR:unknown action")
        }
    }

    /**
     * `id<TAB>label` per category. This app's categories are flat — no sub-options — and everything
     * it exports is authored rather than derived, so nothing is opt-out. The positional third
     * (parent) and fourth (default) fields therefore only appear if a category is ever marked off.
     */
    private fun categoryList(context: Context): String =
        "OK:" + Cat.entries.joinToString("\n") {
            val head = "${it.id}\t${context.getString(it.labelRes)}"
            if (it.defaultSelected) head else "$head\t\toff"
        }

    /**
     * The only channel EMUI carries reliably: a fresh broadcast, addressed with `setPackage`, with
     * `FLAG_INCLUDE_STOPPED_PACKAGES` so a backgrounded or never-launched caller still hears it. No
     * `ResultReceiver`, no `PendingIntent`, no `Messenger` — EMUI will not reliably carry a live
     * binder into another app's manifest receiver, and a broadcast carrying one may be dropped.
     */
    private fun reply(context: Context, replyAction: String, replyPackage: String, replyId: String, result: String) {
        // Unconditional: this one line is how a failing automation run is diagnosed on-device.
        Log.i(TAG, "$replyId → $result")
        runCatching {
            context.sendBroadcast(Intent(replyAction).apply {
                setPackage(replyPackage)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                putExtra("reply_id", replyId)
                putExtra("result", result)
            })
            // Correct AOSP behaviour and free, but EMUI severs the result channel between
            // third-party apps — never the only reply.
            if (isOrderedBroadcast) resultData = result
        }.onFailure { Log.e(TAG, "reply broadcast failed", it) }
    }
}
