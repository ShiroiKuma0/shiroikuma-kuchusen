package ac.mdiq.podcini.activity

import ac.mdiq.podcini.PodciniApp.Companion.getAppContext
import ac.mdiq.podcini.R
import ac.mdiq.podcini.activity.MainActivity.Extras
import ac.mdiq.podcini.shared.ShareType
import ac.mdiq.podcini.sources.sourceClients
import ac.mdiq.podcini.storage.database.realm
import ac.mdiq.podcini.storage.database.upsertBlk
import ac.mdiq.podcini.storage.model.ShareLog
import ac.mdiq.podcini.storage.utils.toSafeUri
import ac.mdiq.podcini.ui.compose.ConfirmAddEpisode
import ac.mdiq.podcini.ui.compose.PodciniTheme
import ac.mdiq.podcini.utils.Logd
import ac.mdiq.podcini.utils.Loge
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.ktor.http.decodeURLQueryComponent

class ShareReceiverActivity : ComponentActivity() {
    private var sharedText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Logd(TAG, "intent: $intent")
        when {
            intent.hasExtra(ARG_FEEDURL) -> sharedText = intent.getStringExtra(ARG_FEEDURL)
            intent.action == Intent.ACTION_SEND -> sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            intent.action == Intent.ACTION_VIEW -> sharedText = intent.dataString
        }
        if (sharedText.isNullOrBlank()) {
            Loge(TAG, "feedUrl is empty or null.\n" + getString(R.string.null_value_podcast_error))
            return
        }
        val regex = Regex("""https?://[^\s'"<>]+""")
        val rawUrl = regex.find(sharedText!!)?.value
        val text = rawUrl?.toSafeUri()?.getQueryParameter("url")?.let { it.decodeURLQueryComponent() } ?: rawUrl ?: sharedText!!
        Logd(TAG, "feedUrl: $sharedText")
        val log = ShareLog(text)
        upsertBlk(log) {}

        receiveShared(text,this, true) { setContent { PodciniTheme { ConfirmAddEpisode(listOf(text), onDismissRequest = { finish() }) } } }
    }

    companion object {
        private val TAG: String = ShareReceiverActivity::class.simpleName ?: "Anonymous"

        const val ARG_FEEDURL: String = "arg.feedurl"

        fun receiveShared(sharedText: String, activity: ComponentActivity, finish: Boolean, mediaCB: ()->Unit) {
            Logd(TAG, "receiveShared sharedText: $sharedText")
            val log = realm.query(ShareLog::class).query("url == $0", sharedText).first().find()
            when {
//            plain text
//                sharedUrl.matches(Regex("^[^\\s<>/]+\$")) -> {
                sharedText.matches(Regex("^[^<>/]+$")) -> {  // include spaces
                    if (log != null)  upsertBlk(log) {it.type = ShareType.Text.name }
                    Logd(TAG, "Activity is started with text $sharedText")
                    val intent = showOnlineSearch(sharedText)
                    activity.startActivity(intent)
                    if (finish) activity.finish()
                }
                else -> {
                    val client = sourceClients.find { it.withProviderBlocking { p-> p.canHandleSharedMedia(sharedText) } == true }
                    if (client != null) {
                        // extension media
                        if (log != null) upsertBlk(log) { it.type = client.withProviderBlocking { p-> p.getShareLogType() } }
                        mediaCB()
                    } else {
                        //              podcast or other?
                        if (log != null) upsertBlk(log) { it.type = ShareType.Podcast.name }
                        Logd(TAG, "Activity is started with url $sharedText")
                        val intent = showOnlineFeed(sharedText, true)
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        activity.startActivity(intent)
                        if (finish) activity.finish()
                    }
                }
            }
        }

        fun showOnlineFeed(feedUrl: String, isShared: Boolean = false): Intent {
            val intent = Intent(getAppContext(), MainActivity::class.java).apply {
                putExtra(Extras.fragment_feed_url.name, feedUrl)
                putExtra(Extras.isShared.name, isShared)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            return intent
        }

        fun showOnlineSearch(query: String): Intent {
            val intent = Intent(getAppContext(), MainActivity::class.java).apply {
                putExtra(Extras.search_string.name, query)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            return intent
        }
    }
}
