package ac.mdiq.podcini.config

import ac.mdiq.podcini.playback.releaseAController
import ac.mdiq.podcini.shared.PodciniHttpClient.configProxy
import ac.mdiq.podcini.sourcing.ssl.SslProviderInstaller
import ac.mdiq.podcini.storage.database.cancelAppPrefs
import ac.mdiq.podcini.storage.database.cancelMonitorFeeds
import ac.mdiq.podcini.storage.database.cancelQueuesMonitor
import ac.mdiq.podcini.storage.database.getRealmInstance
import ac.mdiq.podcini.storage.database.initAppPrefs
import ac.mdiq.podcini.storage.database.initQueues
import ac.mdiq.podcini.storage.database.monitorFeeds
import ac.mdiq.podcini.storage.database.monitorQueues
import ac.mdiq.podcini.storage.database.proxyConfig
import ac.mdiq.podcini.storage.model.cancelMonitorVolumes
import ac.mdiq.podcini.storage.model.createVolumes
import ac.mdiq.podcini.storage.model.monitorVolumes
import ac.mdiq.podcini.storage.utils.setupStorage
import ac.mdiq.podcini.utils.NetworkUtils.cancelMonitorNetwork
import ac.mdiq.podcini.utils.NetworkUtils.monitorNetwork
import ac.mdiq.podcini.utils.timeIt
import kotlinx.coroutines.flow.MutableStateFlow


object AppConfig {

    val isInitialized =  MutableStateFlow(false)
    private var initializing = false

    private val initLock = Any()

    @Synchronized
    fun initialize() {
        synchronized(initLock) {
            if (isInitialized.value || initializing) return
            initializing = true
        }

        try {
            getRealmInstance()
            initAppPrefs()

            monitorNetwork()

            setupStorage()
            createVolumes()
            initQueues()

            timeIt("ClientConfigurator Init started ")

            SslProviderInstaller.install()

            configProxy(proxyConfig)
            createNotificationChannels()

            timeIt("ClientConfigurator Init ends ")

            isInitialized.value = true
        } finally { synchronized(initLock) { initializing = false } }
    }

    fun startLiveMonitor() {
        monitorFeeds()
        monitorVolumes()
        monitorQueues()
    }

    fun destroy() {
        cancelMonitorNetwork()
        releaseAController()
        cancelQueuesMonitor()
        cancelMonitorFeeds()
        cancelMonitorVolumes()
        cancelAppPrefs()
    }
}
