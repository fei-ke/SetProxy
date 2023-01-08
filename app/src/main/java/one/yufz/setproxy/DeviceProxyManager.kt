package one.yufz.setproxy

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object DeviceProxyManager {
    private const val NOTIFICATION_ID = 123
    private const val NOTIFICATION_CHANNEL = "status"

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

        showNotification(context, proxy)
    }

    fun removeProxy(context: Context) {
        Settings.Global.putString(context.contentResolver, Settings.Global.HTTP_PROXY, ":0")
        cancelNotification(context)
    }

    private fun showNotification(context: Context, proxy: Proxy) {
        val nm = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channel = NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName("Status")
            .build()

        nm.createNotificationChannel(channel)

        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setContentTitle("Current Proxy")
            .setContentText("${proxy.host}:${proxy.port}")
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_outline_lan)
            .setOngoing(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_ID)
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