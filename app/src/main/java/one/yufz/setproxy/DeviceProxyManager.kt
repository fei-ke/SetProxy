package one.yufz.setproxy

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

object DeviceProxyManager {
    fun getActivatedProxyFlow(context: Context): Flow<String> = callbackFlow {
        val onChange: () -> Unit = { trySendBlocking(getActivatedProxy(context)) }

        onChange()

        val observer = ObserverWrap(onChange)

        registerContentObserver(context, Settings.Global.getUriFor(Settings.Global.HTTP_PROXY), observer)

        awaitClose { unregisterContentObserver(context, observer) }
    }

    fun getActivatedProxy(context: Context): String {
        return Settings.Global.getString(context.contentResolver, Settings.Global.HTTP_PROXY) ?: ""
    }

    fun activateProxy(context: Context, proxy: Proxy) {
        Settings.Global.putString(context.contentResolver, Settings.Global.HTTP_PROXY, "${proxy.host}:${proxy.port}")
        NotificationManager.showNotification(context, proxy)
    }

    fun deactivateProxy(context: Context) {
        Settings.Global.putString(context.contentResolver, Settings.Global.HTTP_PROXY, ":0")
        NotificationManager.cancelNotification(context)
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