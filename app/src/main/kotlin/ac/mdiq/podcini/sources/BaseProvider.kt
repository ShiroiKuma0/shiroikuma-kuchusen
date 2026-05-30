package ac.mdiq.podcini.sources

import ac.mdiq.podcini.shared.AudioSpec
import ac.mdiq.podcini.shared.EpisodeIPC
import ac.mdiq.podcini.shared.FeedIPC
import ac.mdiq.podcini.storage.model.FeedType
import ac.mdiq.podcini.shared.VideoSpec

class BaseProvider: Provider.Stub() {

    override fun feedType(): String = ""    // TODO

    override fun haveMultiQualities(): Boolean = false

    override fun canHandleFeed(url: String): Boolean = false

    override fun canHandleUrl(url: String): Boolean = false

    override fun haveViewCount(): Boolean = false
    override fun haveLikeCount(): Boolean = false

    override fun buildEpisode(url: String): EpisodeIPC? = null

    override fun getEpisodeDescription(url: String): String? = null

    override fun searcherTAG(): String = ""

    override fun isFeedAutoDownloadable(urlString: String): Boolean = true

    override fun canHandleSharedMedia(urlString: String): Boolean = false

    override fun getShareLogType(): String? = null

    override fun feedDomains(): List<String> = listOf()
    override fun getAudioSpecs(media: EpisodeIPC): List<AudioSpec> = listOf()

    override fun getVideoOnlySpecs(media: EpisodeIPC): List<VideoSpec> = listOf()

    override fun getVideoSpecs(media: EpisodeIPC):  List<VideoSpec> = listOf()

    override fun feedsAtUrl(url_: String): List<String> = listOf()

    override fun buildFeed(url: String, feedSource: String, index: Int): FeedIPC? = null

    override fun getEpisodes(total: Int): List<EpisodeIPC> = listOf()

    override fun downloadFeed(url: String, lastUpdateTime: Long, fullUpdate: Boolean, limitEpisodesCount: Int): FeedIPC? = null
}