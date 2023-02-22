package one.yufz.setproxy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STOP = "one.yufz.setproxy.action_stop"

        fun createStopIntent(context: Context): Intent {
            return Intent(context, ActionReceiver::class.java).apply {
                action = ACTION_STOP
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOP -> {
                DeviceProxyManager.removeProxy(context)
            }
        }
    }
}