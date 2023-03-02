package one.yufz.setproxy

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import one.yufz.setproxy.Proxy.Companion.EMPTY_PROXY
import one.yufz.setproxy.Proxy.Companion.toJson
import org.json.JSONArray

class ProxyStore(private val context: Context) {
    companion object {
        private const val KEY_PROXY_LIST = "proxy_list"
        private const val KEY_CURRENT_PROXY = "current_proxy"
    }

    private val prefs = context.getSharedPreferences("proxy", Context.MODE_PRIVATE)

    private var proxyList: List<Proxy> = getProxyList()

    private val proxyListFlow = MutableStateFlow(proxyList)

    private val currentProxy = MutableStateFlow(getCurrentProxy())

    fun addProxy(proxy: Proxy) {
        if (!proxy.isEmpty()) {
            proxyList = proxyList + proxy
            storeProxyList()
        }
    }

    fun removeProxy(proxy: Proxy) {
        proxyList = proxyList - proxy

        if (proxy == currentProxy.value) {
            setCurrentProxy(EMPTY_PROXY)
        }

        storeProxyList()
    }

    fun replaceProxy(oldProxy: Proxy, newProxy: Proxy) {
        proxyList = proxyList - oldProxy + newProxy

        if (oldProxy == currentProxy.value) {
            setCurrentProxy(newProxy)
        }

        storeProxyList()
    }

    private fun getProxyList(): List<Proxy> {
        val jsonString = prefs.getString(KEY_PROXY_LIST, "[]")
        val array = JSONArray(jsonString)
        val list = ArrayList<Proxy>(array.length())
        for (i in 0 until array.length()) {
            list.add(Proxy.fromJson(array.getJSONObject(i)))
        }
        return list
    }

    private fun storeProxyList() {
        val jsonArray = JSONArray()

        proxyList.forEachIndexed { index, proxy ->
            jsonArray.put(index, proxy.toJson())
        }
        prefs.edit {
            putString(KEY_PROXY_LIST, jsonArray.toString())
        }

        proxyListFlow.value = proxyList
    }

    fun listFlow(): StateFlow<List<Proxy>> {
        return proxyListFlow
    }

    fun setCurrentProxy(proxy: Proxy) {
        if (proxy == currentProxy.value) {
            return
        }

        prefs.edit {
            putString(KEY_CURRENT_PROXY, proxy.toJson().toString())
        }
        if (!proxyList.contains(proxy)) {
            addProxy(proxy)
        }
        currentProxy.value = proxy
    }

    fun getCurrentProxy(): Proxy {
        val jsonString = prefs.getString(KEY_CURRENT_PROXY, null)
        return if (jsonString != null) {
            Proxy.fromJson(jsonString)
        } else {
            Proxy.EMPTY_PROXY
        }
    }

    fun currentProxyFlow(): StateFlow<Proxy> {
        return currentProxy
    }
}