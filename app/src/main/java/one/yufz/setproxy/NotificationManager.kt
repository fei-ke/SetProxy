package one.yufz.setproxy

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationManager {
    private const val NOTIFICATION_ID = 123
    private const val NOTIFICATION_CHANNEL = "status"

    fun showNotification(context: Context, proxy: Proxy) {
        val nm = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channel = NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.notification_channel_status))
            .build()

        nm.createNotificationChannel(channel)

        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setContentTitle(context.getString(R.string.notification_title) + ": ${proxy.name}")
            .setContentText("${proxy.host}:${proxy.port}")
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.notification_icon)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_stop,
                context.getString(R.string.notification_stop),
                PendingIntent.getBroadcast(context, 0, ActionReceiver.createStopIntent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context)
            .cancel(NOTIFICATION_ID)
    }
}