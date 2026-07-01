package ac.mdiq.podcini.sources

import ac.mdiq.podcini.PodciniApp.Companion.getAppContext
import ac.mdiq.podcini.R
import ac.mdiq.podcini.net.feed.PodcastSearcherRegistry.searcherInfos
import ac.mdiq.podcini.shared.FeedSearchResult
import ac.mdiq.podcini.shared.FeedSearcher
import ac.mdiq.podcini.shared.PROVIDER_API_VERSION
import ac.mdiq.podcini.shared.ProviderAttrs
import ac.mdiq.podcini.storage.database.appPrefs
import ac.mdiq.podcini.storage.database.upsert
import ac.mdiq.podcini.storage.model.FeedType
import ac.mdiq.podcini.utils.Logd
import ac.mdiq.podcini.utils.Loge
import ac.mdiq.podcini.utils.Logt
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "GatewayClient"

const val EPISODE_BATCH_SIZE = 100

var sourceClients: List<SourceGatewayClient> = listOf()

val typeClientMap = mutableMapOf<String, SourceGatewayClient>()

fun getSourceClient(name: String): SourceGatewayClient? {
    return typeClientMap[name]
}

fun isExtFeed(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    for (client in sourceClients) if (client.withProviderBlocking { it.canHandleFeed(url) } == true) return true
    return false
}
fun clientsHaveMultiQ(): Boolean {
    for (client in sourceClients) if (client.attributes?.hasMultiQualities == true) return true
    return false
}

fun clientshaveViewCounts(): Boolean {
    for (client in sourceClients) if (client.attributes?.hasViewCount == true) return true
    return false
}
fun clientshaveLikeCounts(): Boolean {
    for (client in sourceClients) if (client.attributes?.hasLikeCount == true) return true
    return false
}

fun PackageManager.queryIntentServicesCompat(intent: Intent, flags: Int): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        queryIntentServices(intent, flags)
    }
}

fun discoverSources(loadExternal: Boolean) {
    if (!loadExternal) sourceClients = listOf()
    else CoroutineScope(Dispatchers.IO).launch { sourceClients = getSourceClients() }
}

suspend fun getSourceClients(): List<SourceGatewayClient> {
    val context = getAppContext()
    searcherInfos.clear()
    val intent = Intent("ac.mdiq.podcini.action.PODCINI_GATEWAY")
    val resolveInfos = context.packageManager.queryIntentServicesCompat(intent, PackageManager.MATCH_ALL)
    if (resolveInfos.isEmpty()) {
        Loge(TAG, "No external source provider is available. Setting '${context.getString(R.string.pref_use_external_app)}' is turned off")
        upsert(appPrefs) { p-> p.loadExternalApp = false }
        return listOf()
    }

    val clients = mutableListOf<SourceGatewayClient>()
    for (resolveInfo in resolveInfos) {
        val serviceInfo = resolveInfo.serviceInfo
        Logd(TAG, "getSourceClients exported=${serviceInfo.exported}")
        Logd(TAG, "getSourceClients permission=${serviceInfo.permission}")
        Logd(TAG, "getSourceClients Targeting Package: ${serviceInfo.packageName}")
        Logd(TAG, "getSourceClients Targeting Class: ${serviceInfo.name}")
        val explicitIntent = Intent("ac.mdiq.podcini.action.PODCINI_GATEWAY").apply { component = ComponentName(serviceInfo.packageName, serviceInfo.name) }

        val client = SourceGatewayClient()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
//                Logt(TAG, "onServiceConnected")
                val remote = IPodciniGateway.Stub.asInterface(service)
                val attr = remote.attributes
                Logd(TAG, "onServiceConnected name: ${attr.name} type: ${attr.feedType} api: ${attr.apiVersion} $PROVIDER_API_VERSION")
                searcherInfos.clear()
                val recognized = attr.feedType in FeedType.entries.map { it.name }
                val versionMatched = attr.apiVersion == PROVIDER_API_VERSION
                if (recognized && versionMatched) {
                    client.attributes = attr
                    client.gateway = remote
                    client.connection = this
                    val aidlSearchProvider = client.gateway?.getSearchProvider()
                    if (aidlSearchProvider != null) client.feedSearcher = GatewaySearcherAdapter(aidlSearchProvider)
                    typeClientMap[attr.name] = client
                    Logt(TAG, "External service ${attr.name} connected")
                } else {
                    if (recognized && !versionMatched) Loge(TAG, "External service ${attr.name} is not a compatible version, rejected.")
                    else Loge(TAG, "External service ${attr.name} not qualified, rejected.")
                    clients.remove(client)
                }
            }
            override fun onServiceDisconnected(name: ComponentName) {
                Logt(TAG, "Service disconnected")
                searcherInfos.clear()
                client.attributes = null
                client.gateway = null
                client.feedSearcher = null
                clients.remove(client)
                client.connection = null
            }
            override fun onBindingDied(name: ComponentName) {
                Logt(TAG, "Binding died, trying to rebind service")
                searcherInfos.clear()
                client.attributes = null
                client.gateway = null
                client.feedSearcher = null
                clients.remove(client)
                context.unbindService(this)
                client.connection = null

                val success = context.bindService(explicitIntent, this, Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT)
                if (success) clients.add(client)
            }
            override fun onNullBinding(name: ComponentName) {
                Logt(TAG, "Service not bond: null binding")
                searcherInfos.clear()
                client.attributes = null
                client.gateway = null
                client.feedSearcher = null
                clients.remove(client)
                context.unbindService(this)
                client.connection = null
            }
        }

        val success = context.bindService(explicitIntent, connection, Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT)
        if (success) clients.add(client)
    }
    return clients
}

class SourceGatewayClient() {
    private val mutex = Mutex()

    var attributes: ProviderAttrs? = null

    var feedSearcher: FeedSearcher? = null

    @Volatile
    var gateway: IPodciniGateway? = null

    @Volatile
    var connection: ServiceConnection? = null

    @Volatile
    private var bindDeferred: CompletableDeferred<IPodciniGateway>? = null

    suspend fun <T> execute(block: suspend (IPodciniGateway) -> T): T? {
        if (gateway == null) return null
        return block(gateway!!)
    }

    fun <T> executeBlocking(block: (IPodciniGateway) -> T): T? {
        Logd(TAG, "executeBlocking")
        return runBlocking(Dispatchers.IO) {
            if (gateway == null) return@runBlocking null
            withContext(Dispatchers.IO) { block(gateway!!) }
        }
    }

    suspend fun <T> withProvider(block: suspend (Provider) -> T): T? {
        return execute { gateway ->
            val provider = gateway.provider ?: throw IllegalStateException("Extension does not provide Provider support")
            block(provider)
        }
    }

    fun <T> withProviderBlocking(block: (Provider) -> T): T? {
//        Logs(TAG, "withProviderBlocking")
        return executeBlocking { gateway ->
            val provider = gateway.provider ?: throw IllegalStateException("Extension does not provide Provider support")
            block(provider)
        }
    }

    suspend fun disconnect() {
        mutex.withLock { disconnectLocked() }
    }

    private fun disconnectLocked() {
        gateway = null
        connection?.let { try { getAppContext().unbindService(it) } catch (_: Exception) { } }
        connection = null
        bindDeferred = null
    }
}

class GatewaySearcherAdapter(private val aidlProvider: IFeedSearchProvider) : FeedSearcher {
    override val name: String?
        get() = try { aidlProvider.name } catch (e: RemoteException) { null }

    override fun urlNeedsLookup(url: String): Boolean {
        return try { aidlProvider.urlNeedsLookup(url) } catch (e: RemoteException) { false }
    }
    override suspend fun search(query: String): List<FeedSearchResult> = withContext(Dispatchers.IO) {
        try { aidlProvider.search(query) ?: emptyList() } catch (e: RemoteException) { emptyList() }
    }
    override suspend fun lookupUrl(url: String): String = withContext(Dispatchers.IO) {
        try { aidlProvider.lookupUrl(url) ?: url } catch (e: RemoteException) { url }
    }
}