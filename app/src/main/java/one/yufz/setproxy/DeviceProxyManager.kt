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
    private var currentProxy: Proxy? = null

    fun getCurrentProxyFlow(context: Context): Flow<Proxy> = callbackFlow {
        val onChange: () -> Unit = { trySendBlocking(getCurrentProxy(context)) }

        onChange()

        val observer = ObserverWrap(onChange)

        registerContentObserver(context, Settings.Global.getUriFor(Settings.Global.HTTP_PROXY), observer)

        awaitClose { unregisterContentObserver(context, observer) }
    }

    fun getCurrentProxy(context: Context): Proxy {
        val httpProxy = Settings.Global.getString(context.contentResolver, Settings.Global.HTTP_PROXY)
        return if (httpProxy == null) {
            Proxy.EMPTY_PROXY
        } else {
            if (currentProxy?.asAddress() == httpProxy) {
                return currentProxy!!
            } else {
                val (host, port) = httpProxy.split(":")
                Proxy(host, port.toIntOrNull() ?: 0)
            }
        }
    }

    fun setProxy(context: Context, proxy: Proxy) {
        currentProxy = proxy

        Settings.Global.putString(context.contentResolver, Settings.Global.HTTP_PROXY, "${proxy.host}:${proxy.port}")

        NotificationManager.showNotification(context, proxy)
    }

    fun removeProxy(context: Context) {
        currentProxy = null
        Settings.Global.putString(context.contentResolver, Settings.Global.HTTP_PROXY, ":0")
        NotificationManager.cancelNotification(context)
    }

    fun checkStatus(context: Context) {
        val proxy = getCurrentProxy(context)
        if (!proxy.isEmpty()) {
            NotificationManager.showNotification(context, proxy)
        }
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