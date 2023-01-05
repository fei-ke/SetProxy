package one.yufz.setproxy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import one.yufz.setproxy.DeviceProxyManager
import one.yufz.setproxy.Proxy
import one.yufz.setproxy.ProxyStore

class ProxyViewModel(private val app: Application) : AndroidViewModel(app) {
    private val proxyStore = ProxyStore(app)

    val currentProxy: Flow<Proxy> = DeviceProxyManager.getCurrentProxyFlow(app)

    val proxyList: StateFlow<List<Proxy>> = proxyStore.listFlow()

    fun addProxy(proxy: Proxy) {
        proxyStore.addProxy(proxy)
    }

    fun removeProxy(proxy: Proxy) {
        proxyStore.removeProxy(proxy)
    }

    fun activateProxy(proxy: Proxy) {
        DeviceProxyManager.setProxy(app, proxy)
    }

    fun deactivateProxy() {
        DeviceProxyManager.removeProxy(app)
    }
}