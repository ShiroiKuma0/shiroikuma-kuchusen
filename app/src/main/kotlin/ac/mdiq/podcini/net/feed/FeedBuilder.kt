package ac.mdiq.podcini.net.feed

import ac.mdiq.podcini.net.download.DownloadRequest.Companion.requestFor
import ac.mdiq.podcini.net.download.Downloader
import ac.mdiq.podcini.net.download.Downloader.Companion.downloaderFor
import ac.mdiq.podcini.shared.PodciniHttpClient.getKtorClient
import ac.mdiq.podcini.shared.FeedIPC
import ac.mdiq.podcini.shared.prepareUrl
import ac.mdiq.podcini.storage.database.addNewFeed
import ac.mdiq.podcini.storage.database.feedByIdentityOrID
import ac.mdiq.podcini.storage.model.Feed
import ac.mdiq.podcini.storage.model.toFeed
import ac.mdiq.podcini.utils.Logd
import ac.mdiq.podcini.utils.Loge
import ac.mdiq.podcini.utils.Logs
import ac.mdiq.podcini.utils.Logt
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequest
import io.github.xilinjia.krdb.ext.toRealmList
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.min

private const val TAG = "FeedBuilder"

class FeedBuilder(val showError: (String?, String)->Unit) {
    var selectedDownloadUrl: String? = null
    private var downloader: Downloader? = null

    suspend fun buildPodcast(url: String, username: String?, password: String?, handleFeed: (Feed, Map<String, String>)->Unit) {
        Logd(TAG, "buildPodcast: $url")
        suspend fun detectPodcastFeedType(url: String): String? {
            return try {
                val response = getKtorClient().get(url) {
                    expectSuccess = false
                    header(HttpHeaders.Range, "bytes=0-1024")
                }
                val type = response.contentType()?.withoutParameters()?.toString()?.lowercase()
                Logd(TAG, "Feed content type: $type")
                when {
                    type == null -> null
                    "xml" in type || "rss" in type || "atom" in type -> "XML"
                    "html" in type -> "HTML"
                    else -> type
                }
            } catch (e: Exception) {
                Loge(TAG, "Feed detection failed: ${e.message}")
                null
            }
        }
        when (val urlType = detectPodcastFeedType(url)) {
            "HTML" -> {
                try {
                    val doc = Ksoup.parseGetRequest(url)
                    val linkElements = doc.select("link[type=application/rss+xml]")
                    //                TODO: should show all as options
                    for (element in linkElements) {
                        val rssUrl = element.attr("href")
                        Logd(TAG, "buildPodcast RSS URL: $rssUrl")
                        buildPodcast(rssUrl, username, password) { feed, map -> handleFeed(feed, map) }
                    }
                    return
                } catch (e: Throwable) { Loge(TAG, "buildPodcast error: ${e.message}")}
            }
            "XML" -> {}
            else -> {
                Loge(TAG, "buildPodcast unknown url type $urlType")
                showError("buildPodcast unknown url type $urlType", "")
                return
            }
        }
        selectedDownloadUrl = prepareUrl(url)
        val request = requestFor(Feed(selectedDownloadUrl, null)).withAuthentication(username, password).withInitiatedByUser(true).build()
        try {
            downloader = downloaderFor(request)
            downloader?.download { source ->
                //                        Logd(TAG, "buildPodcast destination: $destination")
                val feed = Feed(selectedDownloadUrl, null)
                feed.isBuilding = true
                val result = PodcastHandler.parseFeed(source, feed)
                feed.episodesDownloadable = true
                feed.isBuilding = false
                if (result != null) withContext(Dispatchers.Main) { handleFeed(result.feed, result.alternateFeedUrls) }
            }
        } catch (e: Throwable) {
            Logs(TAG, e)
            withContext(Dispatchers.Main) { showError("buildPodcast ${e.message}", "") }
        }
    }
}

suspend fun subscribe(feed: FeedIPC) {
    subscribe(feed.toFeed())
}

suspend fun subscribe(feed: Feed) {
    while (feed.isBuilding) delay(200)
    feed.id = 0L
    if (feed.limitEpisodesCount > 0) {
        val sz = feed.episodes.size
        if (sz > 0) feed.episodes = feed.episodes.subList(0, min(sz, feed.limitEpisodesCount)).toRealmList()
    }
    for (item in feed.episodes) {
        item.id = 0L
        item.feedId = null
        item.origFeedlink = null
        item.origFeeddownloadUrl = null
        item.origFeedTitle = null
    }
    if (feedByIdentityOrID(feed) == null) addNewFeed(feed)
    else Logt(TAG, "feed already exists: ${feed.title}")
}
