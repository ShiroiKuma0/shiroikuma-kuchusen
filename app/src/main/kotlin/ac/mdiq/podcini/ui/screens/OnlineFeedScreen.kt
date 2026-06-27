package ac.mdiq.podcini.ui.screens

import ac.mdiq.podcini.PodciniApp.Companion.getAppContext
import ac.mdiq.podcini.R
import ac.mdiq.podcini.net.download.EpisodeAdrDLManager
import ac.mdiq.podcini.net.feed.CombinedSearcher
import ac.mdiq.podcini.net.feed.FeedBuilder
import ac.mdiq.podcini.net.feed.FeedUrlNotFoundException
import ac.mdiq.podcini.net.feed.PodcastSearcherRegistry
import ac.mdiq.podcini.net.feed.subscribe
import ac.mdiq.podcini.net.utils.NetworkUtils.getFinalRedirectedUrl
import ac.mdiq.podcini.playback.base.InTheatre.actQueue
import ac.mdiq.podcini.shared.EpisodeIPC
import ac.mdiq.podcini.shared.FeedSearchResult
import ac.mdiq.podcini.shared.getEntityId
import ac.mdiq.podcini.sources.EPISODE_BATCH_SIZE
import ac.mdiq.podcini.sources.SourceGatewayClient
import ac.mdiq.podcini.sources.isExtFeed
import ac.mdiq.podcini.sources.sourceClients
import ac.mdiq.podcini.storage.database.EPISODES_LIMIT
import ac.mdiq.podcini.storage.database.allFeeds
import ac.mdiq.podcini.storage.database.appPrefs
import ac.mdiq.podcini.storage.database.getFeed
import ac.mdiq.podcini.storage.database.realm
import ac.mdiq.podcini.storage.database.runOnIOScope
import ac.mdiq.podcini.storage.database.upsert
import ac.mdiq.podcini.storage.database.upsertBlk
import ac.mdiq.podcini.storage.model.Episode
import ac.mdiq.podcini.storage.model.Feed
import ac.mdiq.podcini.storage.model.FeedType
import ac.mdiq.podcini.storage.model.ShareLog
import ac.mdiq.podcini.storage.model.SubscriptionLog
import ac.mdiq.podcini.storage.model.SubscriptionLog.Companion.feedLogsMap
import ac.mdiq.podcini.storage.model.tmpQueue
import ac.mdiq.podcini.storage.model.toFeed
import ac.mdiq.podcini.storage.specs.Rating.Companion.fromCode
import ac.mdiq.podcini.ui.actions.ButtonTypes
import ac.mdiq.podcini.ui.actions.SwipeActions
import ac.mdiq.podcini.ui.compose.CustomTextStyles
import ac.mdiq.podcini.ui.compose.EpisodeLazyColumn
import ac.mdiq.podcini.ui.compose.EpisodeScreen
import ac.mdiq.podcini.ui.compose.InforBar
import ac.mdiq.podcini.ui.compose.NumberEditor
import ac.mdiq.podcini.ui.compose.episodeForInfo
import ac.mdiq.podcini.ui.compose.textColor
import ac.mdiq.podcini.ui.utils.HtmlToPlainText
import ac.mdiq.podcini.utils.Logd
import ac.mdiq.podcini.utils.Loge
import ac.mdiq.podcini.utils.Logs
import ac.mdiq.podcini.utils.formatAbbrev
import ac.mdiq.podcini.utils.timeIt
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnlineFeedVM(url: String = "", source: String = "", shared: Boolean = false): ViewModel() {
    var feedSource: String = ""
    internal var feedUrl: String = ""
    internal var isShared: Boolean = false

    internal var urlToLog: String = ""
    internal var feedBuilder: FeedBuilder
    internal var showTabsDialog by mutableStateOf(false)

    internal var showEpisodes by mutableStateOf(false)
    internal var showFeedDisplay by mutableStateOf(false)
    internal var showProgress by mutableStateOf(true)
    internal var autoDownloadChecked by mutableStateOf(false)
    internal var limitEpisodesCount by mutableIntStateOf(0)
    internal var enableSubscribe by mutableStateOf(true)
    internal var enableEpisodes by mutableStateOf(true)
    internal var subButTextRes by mutableIntStateOf(R.string.subscribe_label)

    var numEpisodes by mutableIntStateOf(0)

    internal var selectedDownloadUrl: String? = null

    internal var feedOptions: List<String?> = listOf()

    internal var feedId: Long = 0L

    internal var infoBarText = mutableStateOf("")

    internal var episodes = mutableListOf<Episode>()

    internal var feed by mutableStateOf<Feed?>(null)
    internal var username: String? = null
    internal var password: String? = null

    internal var isPaused = false
    internal var didPressSubscribe = false
    internal var isFeedFoundBySearch = false

    var relatedFeeds by mutableStateOf<List<FeedSearchResult>>(listOf())

    internal var showNoPodcastFoundDialog by mutableStateOf(false)
    internal var errorMessage by mutableStateOf("")
    internal var errorDetails by mutableStateOf("")

    var sLog by mutableStateOf<SubscriptionLog?>(null)

    var gatewayClient: SourceGatewayClient? = null

    init {
        timeIt("$TAG start of init")
        feedUrl = url
        feedSource = source
        isShared = shared
        for (f in allFeeds) {
            if (isSameFeed(f, selectedDownloadUrl, feed?.title, feed?.author)) {
                feedId = f.id
                break
            }
        }

        Logd(TAG, "OnlineFeedVM init feedUrl: $feedUrl")

        val showError = { message: String?, details: String ->
            errorMessage = message ?: "No message"
            errorDetails = details
        }
        gatewayClient = sourceClients.find { it.withProviderBlocking { p-> p.canHandleFeed(feedUrl) } == true }

        val defaultCapable = gatewayClient != null
        feedBuilder = FeedBuilder(showError)
        if (feedUrl.isEmpty()) {
            Loge(TAG, "feedUrl is null.")
            showNoPodcastFoundDialog = true
        } else {
            Logd(TAG, "Activity was started with url $feedUrl")
            showProgress = true
            // Remove subscribeonandroid.com from feed URL in order to subscribe to the actual feed URL
            if (feedUrl.contains("subscribeonandroid.com")) feedUrl = feedUrl.replaceFirst("((www.)?(subscribeonandroid.com/))".toRegex(), "")
            suspend fun handleClientFeeds() {
                feedOptions = gatewayClient?.withProvider { it.feedsTitlesAtUrl(url) } ?: listOf()
                val feedOptions_ = feedOptions.filter { it != "playlists" && it != "shorts" }
                when {
                    feedOptions_.size > 1 -> {
                        showTabsDialog = true
                        feedOptions.forEach { Logd(TAG, "feedOptions: $it") }
                    }
                    feedOptions_.size == 1 -> {
                        val fipc = gatewayClient?.withProvider { it.buildFeed(url, 0) }
                        if (fipc != null) {
                            val eList = mutableListOf<EpisodeIPC>()
                            var episodes = gatewayClient?.withProvider { it.getEpisodes(EPISODE_BATCH_SIZE) } ?: listOf()
                            while (episodes.isNotEmpty()) {
                                eList.addAll(episodes)
                                numEpisodes = eList.size
                                if (limitEpisodesCount in 1..<numEpisodes || numEpisodes > EPISODES_LIMIT) break
                                Logd(TAG, "Subscribing eList: ${eList.size}")
                                episodes = gatewayClient?.withProvider { it.getEpisodes(EPISODE_BATCH_SIZE) } ?: listOf()
                            }
                            fipc.episodes = eList
                            handleFeed(fipc.toFeed())
                        } else Loge(TAG, "Building feed failed")
                    }
                    else -> Loge(TAG, "No feed found")
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                urlToLog = feedUrl
                try {
                    val urlString = PodcastSearcherRegistry.lookupUrl(feedUrl)
                    Logd(TAG, "lookupUrlAndBuild: urlString: $urlString")
                    if (defaultCapable) handleClientFeeds()
                    else feedBuilder.buildPodcast(getFinalRedirectedUrl(urlString), username, password) { feed_, _ -> handleFeed(feed_) }
                } catch (error: FeedUrlNotFoundException) {
                    Logd(TAG, "lookupUrlAndBuild in error, trying to Retrieve FeedUrl By Search")
                    var url: String? = null
                    val searcher = CombinedSearcher()
                    val query = "${error.trackName} ${error.artistName}"
                    val results = searcher.search(query)
                    if (results.isEmpty()) return@launch
                    for (result in results) {
                        if (result.feedUrl != null && result.author != null && result.author.equals(error.artistName, ignoreCase = true)
                            && result.title.equals(error.trackName, ignoreCase = true)) {
                            url = result.feedUrl
                            break
                        }
                    }
                    if (url != null) {
                        urlToLog = url
                        Logd(TAG, "Successfully retrieve feed url: $url")
                        isFeedFoundBySearch = true
                        if (defaultCapable) handleClientFeeds()
                        else feedBuilder.buildPodcast(getFinalRedirectedUrl(url), username, password) { feed_, _ -> handleFeed(feed_) }
                    } else {
                        withContext(Dispatchers.Main) { showNoPodcastFoundDialog = true }
                        Logd(TAG, "Failed to retrieve feed url")
                    }
                } catch (e: Throwable) {
                    Logs(TAG, e)
                    withContext(Dispatchers.Main) { showNoPodcastFoundDialog = true }
                }
            }
        }
        timeIt("$TAG end of init")
    }
    internal fun handleFeed(feed_: Feed) {
        Logd(TAG, "handleFeed feed_.title: ${feed_.title}")
        selectedDownloadUrl = feedBuilder.selectedDownloadUrl
        feed = feed_
        numEpisodes = feed_.episodes.size
        if (isShared) {
            val log = realm.query(ShareLog::class).query("url == $0", urlToLog).first().find()
            if (log != null) upsertBlk(log) {
                it.title = feed_.title
                it.author = feed_.author
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val fl = CombinedSearcher::class.java.getDeclaredConstructor().newInstance().search("${feed?.author} podcasts")
            withContext(Dispatchers.Main) { if (fl.isNotEmpty()) relatedFeeds = fl }
        }
        showProgress = false
        showFeedDisplay = true
        if (isFeedFoundBySearch) Loge(TAG, getAppContext().getString(R.string.no_feed_url_podcast_found_by_search))
        handleUpdatedFeedStatus()
    }

    fun isSameFeed(feed: Feed, url: String?, title: String?, author: String?): Boolean {
        if (url == null) return false
        return if (isExtFeed(url)) (feed.downloadUrl == url || (feed.title == title && feed.author == author))
        else feed.downloadUrl == url
    }

    internal fun showEpisodes() {
        if (feed == null) return
        if (episodes.isEmpty()) {
            episodes = feed!!.episodes
            infoBarText.value = "${episodes.size} episodes"

            Logd(TAG, "showEpisodes ${episodes.size}")
            if (episodes.isEmpty()) return
            episodes.sortByDescending { it.pubDate }
            for (episode in episodes) {
                episode.id = getEntityId()
                episode.origFeedlink = feed!!.link
                episode.origFeeddownloadUrl = feed!!.downloadUrl
                episode.origFeedTitle = feed!!.title
            }
        }
        showEpisodes = true
    }

    internal fun handleUpdatedFeedStatus() {
        val dli = EpisodeAdrDLManager.manager
        if (dli == null || selectedDownloadUrl == null) return

        when {
            dli.isDownloading(selectedDownloadUrl!!) -> {
                enableSubscribe = false
                subButTextRes = R.string.subscribe_label
            }
            feedId != 0L -> {
                enableSubscribe = true
                subButTextRes = R.string.open
                if (didPressSubscribe) {
                    didPressSubscribe = false
                    val feedExisting = getFeed(feedId, true)?: return
                    if (feedSource == "VistaGuide") {
                        feedExisting.prefStreamOverDownload = true
                        feedExisting.autoDownload = false
                    } else if (appPrefs.enableAutoDl) feedExisting.autoDownload = autoDownloadChecked
                    if (username != null) {
                        feedExisting.username = username
                        feedExisting.password = password
                    }
                    runOnIOScope { upsert(feedExisting) {} }
                }
            }
            else -> {
                enableSubscribe = true
                subButTextRes = R.string.subscribe_label
            }
        }
    }

    override fun onCleared() {
        episodes.clear()
    }
}

@ExperimentalMaterial3Api
@Composable
fun OnlineFeedScreen(url: String = "", source: String = "", shared: Boolean = false) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawerController = LocalDrawerController.current
    val context by rememberUpdatedState(LocalContext.current)

    val vm: OnlineFeedVM = viewModel(key = url, factory = viewModelFactory { initializer { OnlineFeedVM(url, source, shared) } })

    var swipeActions by remember { mutableStateOf(SwipeActions(TAG, false)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> Logd(TAG, "feedUrl: ${vm.feedUrl}")
                Lifecycle.Event.ON_START -> {
                    vm.isPaused = false
                    vm.infoBarText.value = "${vm.episodes.size} episodes"
                }
                Lifecycle.Event.ON_STOP -> vm.isPaused = true
                Lifecycle.Event.ON_DESTROY -> {}
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(vm.showEpisodes, episodeForInfo) {
        if (vm.showEpisodes || episodeForInfo != null) handleBackSubScreens.add(TAG)
        else handleBackSubScreens.remove(TAG)
        onDispose { handleBackSubScreens.remove(TAG) }
    }

    BackHandler(enabled = handleBackSubScreens.contains(TAG)) {
        when {
            episodeForInfo != null -> episodeForInfo = null
            else -> vm.showEpisodes = false
        }
    }

    @Composable
    fun ShowTabsDialog(onDismissRequest: () -> Unit, handleFeed: (Feed) -> Unit) {
        val ytTabsMap = remember { mutableStateMapOf<Int, String>() }
        AlertDialog(modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.extraLarge), onDismissRequest = { onDismissRequest() },
            title = { Text(stringResource(R.string.choose_tab), style = CustomTextStyles.titleCustom) },
            text = {
                Column {
                    val selectedId = remember { mutableStateOf<Int?>(null) }
                    for (i in vm.feedOptions.indices) {
                        val urlEnd = vm.feedOptions[i]
                        if (!urlEnd.isNullOrBlank() && urlEnd != "playlists" && urlEnd != "shorts") Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 30.dp)) {
                                var checked by remember { mutableStateOf(false) }
                                val isChecked = ytTabsMap.contains(i)   // TODO: better enable multi-select
                                Checkbox(checked = selectedId.value == i, onCheckedChange = {
                                    selectedId.value = if (selectedId.value == i) null else i
                                    checked = it
                                    if (checked) ytTabsMap[i] = urlEnd else ytTabsMap.remove(i)
                                })
                                Text(text = urlEnd, style = MaterialTheme.typography.bodyLarge.merge(), modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (i in ytTabsMap.keys) {
                            Logd(TAG, "Subscribing $i ${vm.feedOptions[i]} ${ytTabsMap[i]}")
                            val endUrl = ytTabsMap[i] ?: continue
                            val fipc = vm.gatewayClient?.withProvider { it.buildFeed(url, i) }
                            if (fipc != null) {
                                fipc.title = "${fipc.title}: $endUrl"
                                val eList = mutableListOf<EpisodeIPC>()
                                var episodes = vm.gatewayClient?.withProvider { it.getEpisodes(EPISODE_BATCH_SIZE) }?: listOf()
                                while (episodes.isNotEmpty()) {
                                    eList.addAll(episodes)
                                    vm.numEpisodes = eList.size
                                    if (vm.limitEpisodesCount in 1..<vm.numEpisodes || vm.numEpisodes > EPISODES_LIMIT) break
                                    Logd(TAG, "Subscribing eList: ${eList.size}")
                                    episodes = vm.gatewayClient?.withProvider { it.getEpisodes(EPISODE_BATCH_SIZE) }?: listOf()
                                }
                                fipc.episodes = eList
                                handleFeed(fipc.toFeed())
                            } else Loge(TAG, "Subscribe feed failed")
                        }
                    }
                    onDismissRequest()
                }) { Text(text = stringResource(R.string.confirm_label)) }
            },
            dismissButton = { TextButton(onClick = { onDismissRequest() }) { Text(stringResource(R.string.cancel_label)) } }
        )
    }

    if (vm.showTabsDialog) ShowTabsDialog(onDismissRequest = { vm.showTabsDialog = false }) { feed -> vm.handleFeed(feed) }

    if (vm.showNoPodcastFoundDialog) AlertDialog(modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.extraLarge), onDismissRequest = { vm.showNoPodcastFoundDialog = false },
        title = { Text(stringResource(R.string.error_label)) },
        text = { Text(stringResource(R.string.null_value_podcast_error)) },
        confirmButton = { TextButton(onClick = { vm.showNoPodcastFoundDialog = false }) { Text("OK") } })

    if (vm.errorMessage.isNotBlank()) Loge(TAG, "${vm.errorMessage}\n${vm.errorDetails}")

    swipeActions.ActionOptionsDialog()

    if (episodeForInfo != null) EpisodeScreen(episodeForInfo!!)
    else Scaffold(topBar = {
        Box {
            TopAppBar(title = { Text(text = "Online feed") }, navigationIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back or drawer",  modifier = Modifier.padding(7.dp).clickable {
                if (vm.showEpisodes) vm.showEpisodes = false
                else if (!navBack()) drawerController?.open()
            }) } )
            HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), thickness = DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }) { innerPadding ->
        if (vm.showEpisodes) Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(start = 5.dp, end = 5.dp).background(MaterialTheme.colorScheme.surface)) {
            InforBar(swipeActions) { Text(vm.infoBarText.value, style = MaterialTheme.typography.bodyMedium) }
            EpisodeLazyColumn(vm.episodes.toList(), isExternal = true, swipeActions = swipeActions,
                actionButtonCB = { _, type -> if (type in listOf(ButtonTypes.PLAY, ButtonTypes.PLAY_LOCAL, ButtonTypes.STREAM)) actQueue = tmpQueue() })
        } else Column(modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp).background(MaterialTheme.colorScheme.surface)) {
            ConstraintLayout(modifier = Modifier.fillMaxWidth().height(110.dp).background(MaterialTheme.colorScheme.surface)) {
                val (coverImage, taColumn, buttons) = createRefs()
                AsyncImage(model = vm.feed?.imageUrl ?: "", contentDescription = "coverImage", error = painterResource(R.drawable.ic_launcher_foreground),
                    modifier = Modifier.width(80.dp).height(80.dp).constrainAs(coverImage) {
                        centerVerticallyTo(parent)
                        start.linkTo(parent.start)
                    })
                Column(Modifier.padding(start = 5.dp).constrainAs(taColumn) {
                    top.linkTo(parent.top)
                    start.linkTo(coverImage.end)
                }) {
                    Text(vm.feed?.title ?: "No title", color = textColor, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(vm.feed?.author ?: "", color = textColor, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.constrainAs(buttons) {
                    start.linkTo(coverImage.end)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }) {
                    Spacer(modifier = Modifier.weight(0.2f))
                    if (vm.showFeedDisplay && vm.enableSubscribe) Button(onClick = {
                        if (vm.feed == null) return@Button
                        if (vm.feedId != 0L) {
                            if (vm.isShared) {
                                val log = realm.query(ShareLog::class).query("url == $0", vm.feedUrl).first().find()
                                if (log != null) upsertBlk(log) { it.status = ShareLog.Status.EXISTING.ordinal }
                            }
                            navTo(FeedDetails(feedId=vm.feedId, modeName=FeedScreenMode.Info.name))
                        } else {
                            vm.enableSubscribe = false
                            vm.enableEpisodes = false
                            CoroutineScope(Dispatchers.IO).launch {
                                if (vm.limitEpisodesCount > 0) vm.feed?.limitEpisodesCount = vm.limitEpisodesCount
                                subscribe(vm.feed!!)
                                if (vm.isShared) {
                                    val log = realm.query(ShareLog::class).query("url == $0", vm.feedUrl).first().find()
                                    if (log != null) upsertBlk(log) { it.status = ShareLog.Status.SUCCESS.ordinal }
                                }
                                withContext(Dispatchers.Main) {
                                    vm.feedId = vm.feed?.id ?: 0L
                                    vm.enableSubscribe = true
                                    vm.subButTextRes = R.string.open
                                    vm.didPressSubscribe = true
                                    vm.handleUpdatedFeedStatus()
                                }
                            }
                        }
                    }) { Text(stringResource(vm.subButTextRes)) }
                    Spacer(modifier = Modifier.weight(0.1f))
                    when {
                        vm.showEpisodes -> Button(onClick = { vm.showEpisodes = false }) { Text(stringResource(R.string.feed)) }
                        vm.enableEpisodes && vm.feed != null -> Button(onClick = { vm.showEpisodes() }) { Text(stringResource(R.string.episodes_label)) }
                        else -> {}
                    }
                    Spacer(modifier = Modifier.weight(0.2f))
                }
            }
            Column(Modifier.border(1.dp, MaterialTheme.colorScheme.tertiary)) {
//                    TODO: alternate_urls_spinner
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.limit_episodes_to), modifier = Modifier.weight(0.5f))
                    NumberEditor(vm.limitEpisodesCount, label = "0 = unlimited", nz = false, instant = false, modifier = Modifier.weight(0.5f)) {
                        Logd(TAG, "limitEpisodesCount: $it")
                        vm.limitEpisodesCount = it
                    }
                }
                val isAudoDL = remember(vm.feed) { vm.feed?.type in listOf(FeedType.RSS.name, FeedType.ATOM.name) }
                if (appPrefs.enableAutoDl && isAudoDL) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vm.autoDownloadChecked, onCheckedChange = { vm.autoDownloadChecked = it })
                    Text(text = stringResource(R.string.auto_download_label), style = MaterialTheme.typography.bodyMedium.merge(), color = textColor, modifier = Modifier.padding(start = 16.dp))
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp)) {
                val sLog = remember { feedLogsMap!![vm.feed?.downloadUrl ?: ""] ?: feedLogsMap!![vm.feed?.title ?: ""] }
                if (sLog != null) {
                    val commentTextState = remember(sLog.comment) { TextFieldValue(sLog.comment) }
                    val cancelDate = remember { formatAbbrev(sLog.cancelDate) }
                    val ratingRes = remember { fromCode(sLog.rating).res }
                    if (commentTextState.text.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 15.dp, top = 10.dp, bottom = 5.dp)) {
                            Icon(imageVector = ImageVector.vectorResource(ratingRes), tint = MaterialTheme.colorScheme.tertiary, contentDescription = null)
                            Text(stringResource(R.string.comments), color = MaterialTheme.colorScheme.primary, style = CustomTextStyles.titleCustom, modifier = Modifier.padding(start = 5.dp))
                        }
                        Text(commentTextState.text, color = textColor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 15.dp, bottom = 10.dp))
                        Text(stringResource(R.string.cancelled_on_label) + ": " + cancelDate, color = textColor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 15.dp, bottom = 10.dp))
                    }
                }
                Text("${vm.numEpisodes} episodes", color = textColor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 5.dp, bottom = 10.dp))
                Text(stringResource(R.string.description_label), color = textColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp, bottom = 4.dp))
                Text(HtmlToPlainText.getPlainText(vm.feed?.description ?: ""), color = textColor, style = MaterialTheme.typography.bodyMedium)
                if (!vm.feed?.episodes.isNullOrEmpty()) {
                    Text(stringResource(R.string.recent_episode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp, bottom = 4.dp))
                    Text(vm.feed?.episodes[0]?.title ?: "", color = textColor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 5.dp, bottom = 4.dp))
                }

                Text(stringResource(R.string.feeds_related_to_author), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp).clickable {
                        searchOnline(query = "${vm.feed?.author} podcasts")
                        navTo(FindFeeds)
                    })
                LazyRow(state = rememberLazyListState(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(vm.relatedFeeds) { feed ->
                        AsyncImage(model = ImageRequest.Builder(context).data(feed.imageUrl).memoryCachePolicy(CachePolicy.ENABLED).build(), placeholder = painterResource(R.drawable.ic_launcher_foreground), error = painterResource(R.drawable.ic_launcher_foreground), contentDescription = "imgvCover", modifier = Modifier.width(100.dp).height(100.dp).clickable {
                            navTo(OnlineFeed(url=feed.feedUrl?:"", source=feed.source)) })
                    }
                }
                val info = remember(vm.feed) {
                    if (vm.feed == null) return@remember ""
                    val languageString = vm.feed!!.langSet.joinToString(" ")
                    "$languageString ${vm.feed!!.type.orEmpty()} ${vm.feed!!.lastUpdate.orEmpty()}"
                }
                Text(info, color = textColor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                Text(vm.feed?.link ?: "", color = textColor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 5.dp, bottom = 4.dp))
                Text(vm.feed?.downloadUrl ?: "", color = textColor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 5.dp, bottom = 4.dp))
            }
        }
        if (vm.showProgress) Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            CircularProgressIndicator(progress = {0.6f}, strokeWidth = 10.dp, color = textColor, modifier = Modifier.size(50.dp).align(Alignment.Center))
        }
    }
}

private val TAG: String = Screens.OnlineFeed.name
