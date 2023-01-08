package one.yufz.setproxy

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    fun addProxy(proxy: Proxy) {
        proxyList = proxyList + proxy
        storeProxyList()
    }

    fun removeProxy(proxy: Proxy) {
        proxyList = proxyList - proxy
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
}