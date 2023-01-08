package one.yufz.setproxy

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object DeviceProxyManager {
    fun getCurrentProxyFlow(context: Context): Flow<Proxy> = callbackFlow {
        val onChange: () -> Unit = { trySendBlocking(getCurrentProxy(context)) }

        onChange()

        val observer = ObserverWrap(onChange)

        registerContentObserver(context, Settings.Global.getUriFor(Settings.Global.HTTP_PROXY), observer)

        awaitClose { unregisterContentObserver(context, observer) }
    }

    private fun getCurrentProxy(context: Context): Proxy {
        val httpProxy = Settings.Global.getString(context.contentResolver, Settings.Global.HTTP_PROXY)
        return if (httpProxy == null) {
            Proxy.EMPTY_PROXY
        } else {
            val (host, port) = httpProxy.split(":")
            Proxy(host, port.toIntOrNull() ?: 0)
        }
    }

    fun setProxy(context: Context, proxy: Proxy) {
        Settings.Global.putString(context.contentResolver, Settings.Global.HTTP_PROXY, "${proxy.host}:${proxy.port}")

        BackgroundService.wakeService(context)
    }

    fun removeProxy(context: Context) {
        Settings.Global.putString(context.contentResolver, Settings.Global.HTTP_PROXY, ":0")
    }

    private fun registerContentObserver(context: Context, uri: Uri, observer: ObserverWrap) {
        context.contentResolver.registerContentObserver(uri, false, observer)
    }

    private fun unregisterContentObserver(context: Context, observer: ObserverWrap) {
        context.contentResolver.unregisterContentObserver(observer)
    }

    private class ObserverWrap(val observer: () -> Unit) : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            observer()
        }
    }
}