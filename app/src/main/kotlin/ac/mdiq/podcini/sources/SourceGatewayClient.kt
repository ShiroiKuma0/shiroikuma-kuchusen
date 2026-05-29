package ac.mdiq.podcini.sources

import ac.mdiq.podcini.PodciniApp.Companion.getAppContext
import ac.mdiq.podcini.storage.database.appPrefs
import ac.mdiq.podcini.storage.database.upsert
import ac.mdiq.podcini.storage.database.upsertBlk
import ac.mdiq.podcini.utils.Logd
import ac.mdiq.podcini.utils.Loge
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

//private var _sourceGatewayClient: SourceGatewayClient? = null
//val sourceGatewayClient: SourceGatewayClient?
//    get() {
//        if (_sourceGatewayClient != null) return _sourceGatewayClient
//        return if (appPrefs.loadExternalApp) SourceGatewayClient(getAppContext()).also { _sourceGatewayClient = it } else { null.also { _sourceGatewayClient = null } }
//    }

var sourceGatewayClient: SourceGatewayClient? = null
    get() {
        if (field == null && appPrefs.loadExternalApp) field = SourceGatewayClient(getAppContext())
        return field
    }
    set(value) {
        field = value
    }
class SourceGatewayClient(private val context: Context) {
    companion object {
        private const val TAG = "GatewayClient"
    }

    private val mutex = Mutex()

    @Volatile
    private var gateway: IPodciniGateway? = null

    @Volatile
    private var connection: ServiceConnection? = null

    @Volatile
    private var bindDeferred: CompletableDeferred<IPodciniGateway>? = null

    fun preWarm() {
        Logd(TAG, "preWarm")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                getOrBindGateway()
                Logd(TAG, "preWarm Extension gateway pre-warmed successfully.")
            } catch (e: Exception) { Loge(TAG, "preWarm failed: ${e.message}") }
        }
    }

    suspend fun <T> execute(block: suspend (IPodciniGateway) -> T): T? {
        val gateway = getOrBindGateway() ?: return null
        return block(gateway)
    }

    fun <T> executeBlocking(block: (IPodciniGateway) -> T): T? {
//        check(Looper.myLooper() != Looper.getMainLooper()) { "executeBlocking must not be called on main thread" }
        Logd(TAG, "executeBlocking")
        return runBlocking(Dispatchers.IO) {
            val gateway = getOrBindGateway()?: return@runBlocking null
            Logd(TAG, "executeBlocking got gateway")
            withContext(Dispatchers.IO) { block(gateway) }
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
            Logd(TAG, "withProviderBlocking got provider")
            block(provider)
        }
    }

    suspend fun disconnect() {
        mutex.withLock { disconnectLocked() }
    }

    private suspend fun getOrBindGateway(): IPodciniGateway? = withContext(Dispatchers.IO) {
//        showStackTrace()
//        Log.d(TAG, "getOrBindGateway")
        gateway?.let { return@withContext it }
        val deferred: CompletableDeferred<IPodciniGateway>
        mutex.withLock {
            gateway?.let { return@withContext it }
            bindDeferred?.let {
                deferred = it
                return@withLock
            }
            deferred = CompletableDeferred()
            bindDeferred = deferred
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    Logd(TAG, "onServiceConnected")
                    val remote = IPodciniGateway.Stub.asInterface(service)
                    gateway = remote
                    Logd(TAG, "onServiceConnected Service connected")
                    deferred.complete(remote)
                }
                override fun onServiceDisconnected(name: ComponentName) {
                    Logd(TAG, "Service disconnected")
                    gateway = null
                }
                override fun onBindingDied(name: ComponentName) {
                    Logd(TAG, "Binding died")
                    gateway = null
                }
                override fun onNullBinding(name: ComponentName) {
                    Logd(TAG, "Null binding")
                    gateway = null
                    if (!deferred.isCompleted) deferred.completeExceptionally(IllegalStateException("Null binding from gateway service"))
                }
            }

            this@SourceGatewayClient.connection = connection
            val intent = Intent("ac.mdiq.podcini.action.PODCINI_GATEWAY")
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveService(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveService(intent, PackageManager.MATCH_ALL)
            }
            if (resolveInfo == null) {
                Loge(TAG, "No external source provider is available. Ignored")
                sourceGatewayClient = null
                upsert(appPrefs) { p-> p.loadExternalApp = false }
//                throw IllegalStateException("No service found for gateway action. Ensure the external app is installed and its package visibility is declared in <queries>.")
                return@withContext null
            }

            val serviceInfo = resolveInfo.serviceInfo
            Logd(TAG, "getOrBindGateway exported=${serviceInfo.exported}")
            Logd(TAG, "getOrBindGateway permission=${serviceInfo.permission}")
            Logd(TAG, "getOrBindGateway Targeting Package: ${serviceInfo.packageName}")
            Logd(TAG, "getOrBindGateway Targeting Class: ${serviceInfo.name}")
            val explicitIntent = Intent("ac.mdiq.podcini.action.PODCINI_GATEWAY").apply { component = ComponentName(serviceInfo.packageName, serviceInfo.name) }
//            val success = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            val success = context.bindService(explicitIntent, connection, Context.BIND_AUTO_CREATE)
            Logd(TAG, "getOrBindGateway success: $success")
            if (!success) {
                bindDeferred = null
                throw IllegalStateException("Failed to bind")
            }
        }
        return@withContext deferred.await()
    }

    private fun disconnectLocked() {
        gateway = null
        connection?.let { try { context.unbindService(it) } catch (_: Exception) { } }

        connection = null
        bindDeferred = null
    }
}