package one.yufz.setproxy

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BackgroundService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 123
        private const val NOTIFICATION_CHANNEL = "status"

        private const val ACTION_PAUSE = "one.yufz.setproxy.pause"
        private const val ACTION_STOP = "one.yufz.setproxy.stop"

        fun wakeService(context: Context) {
            val intent = Intent(context, BackgroundService::class.java)
            context.startService(intent)
        }

        fun createPauseIntent(context: Context): Intent {
            return Intent(context, BackgroundService::class.java).apply {
                action = ACTION_PAUSE
            }
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, BackgroundService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }

    private val mainScope = MainScope()
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        DeviceProxyManager.getCurrentProxyFlow(this).onEach {
            if (!it.isEmpty()) {
                showNotification(this, it)
            } else {
                cancelNotification(this)
            }
        }.launchIn(mainScope)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (action != null) {
            handleAction(action)
        }
        return START_STICKY
    }

    private fun handleAction(action: String) {
        when (action) {
            ACTION_PAUSE -> {
                DeviceProxyManager.setProxy(this, Proxy.EMPTY_PROXY)
            }
            ACTION_STOP -> {
                DeviceProxyManager.setProxy(this, Proxy.EMPTY_PROXY)
            }
        }
    }

    private fun showNotification(context: Context, proxy: Proxy) {
        val nm = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channel = NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(getString(R.string.notification_channel_status))
            .build()

        nm.createNotificationChannel(channel)

        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText("${proxy.host}:${proxy.port}")
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.notification_icon)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_pause,
                getString(R.string.notification_stop),
                PendingIntent.getService(context, 0, createPauseIntent(this), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_ID)
    }
}