package one.yufz.setproxy

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProxyStore(private val context: Context) {
    companion object {
        private fun Proxy.toPrefsString(): String {
            return "$host\n$port\n&$userName\n$password"
        }

        private fun String.toProxy(): Proxy {
            val (host, port, userName, password) = split("\n")
            return Proxy(host, port.toInt(), userName, password)
        }
    }

    private val prefs = context.getSharedPreferences("proxy", Context.MODE_PRIVATE)

    private var proxyList: List<Proxy> = getProxyList()

    private val proxyListFlow = MutableStateFlow(proxyList)

    fun addProxy(proxy: Proxy) {
        proxyList = proxyList + proxy
        storeProxyList()
    }

    fun removeProxy(proxy: Proxy) {
        proxyList = proxyList - proxy
        storeProxyList()
    }

    private fun getProxyList(): List<Proxy> {
        return prefs.all.toSortedMap().values.map { it.toString().toProxy() }
    }

    private fun storeProxyList() {
        prefs.edit {
            clear()
            proxyList.forEachIndexed { index, proxy ->
                putString(index.toString(), proxy.toPrefsString())
            }
        }

        proxyListFlow.value = proxyList
    }

    fun listFlow(): StateFlow<List<Proxy>> {
        return proxyListFlow
    }
}