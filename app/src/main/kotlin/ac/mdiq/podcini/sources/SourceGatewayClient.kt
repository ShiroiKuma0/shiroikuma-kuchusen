package ac.mdiq.podcini.sources

import ac.mdiq.podcini.PodciniApp.Companion.getAppContext
import ac.mdiq.podcini.storage.model.FeedType
import ac.mdiq.podcini.storage.database.appPrefs
import ac.mdiq.podcini.storage.database.upsert
import ac.mdiq.podcini.utils.Logd
import ac.mdiq.podcini.utils.Loge
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "GatewayClient"


var sourceClients: List<SourceGatewayClient> = listOf()

val typeClientMap = mutableMapOf<FeedType, SourceGatewayClient>()

fun getSourceClient(feedType: FeedType): SourceGatewayClient? {
    return typeClientMap[feedType]
}

fun isExtFeed(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    for (client in sourceClients) if (client.withProviderBlocking { it.canHandleFeed(url) } == true) return true
    return false
}
fun clientsHaveMultiQ(): Boolean {
    for (client in sourceClients) if (client.withProviderBlocking { it.haveMultiQualities() } == true) return true
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
    val intent = Intent("ac.mdiq.podcini.action.PODCINI_GATEWAY")
    val resolveInfos = context.packageManager.queryIntentServicesCompat(intent, PackageManager.MATCH_ALL)
    if (resolveInfos.isEmpty()) {
        Loge(TAG, "No external source provider is available. Ignored")
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
        val explicitIntent = Intent("ac.mdiq.podcini.action.PODCINI_GATEWAY").apply { component = ComponentName(serviceInfo.packageName, serviceInfo.name) } //            val success = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        val client = SourceGatewayClient()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Logd(TAG, "onServiceConnected")
                val remote = IPodciniGateway.Stub.asInterface(service)
                client.gateway = remote
                client.connection = this
                Logd(TAG, "onServiceConnected Service connected")
            }
            override fun onServiceDisconnected(name: ComponentName) {
                Logd(TAG, "Service disconnected")
                client.gateway = null
                client.connection = null
            }
            override fun onBindingDied(name: ComponentName) {
                Logd(TAG, "Binding died")
                client.gateway = null
                client.connection = null
            }
            override fun onNullBinding(name: ComponentName) {
                Logd(TAG, "Null binding")
                client.gateway = null
                client.connection = null
            }
        }
        val success = context.bindService(explicitIntent, connection, Context.BIND_AUTO_CREATE)
        if (success) clients.add(client)
    }
    return clients
}

class SourceGatewayClient() {
    private val mutex = Mutex()

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