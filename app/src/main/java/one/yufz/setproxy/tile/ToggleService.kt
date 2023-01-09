package one.yufz.setproxy.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import one.yufz.setproxy.DeviceProxyManager
import one.yufz.setproxy.Proxy


@RequiresApi(Build.VERSION_CODES.N)
class ToggleService : TileService() {
    private var scope: CoroutineScope? = null

    override fun onClick() {
        super.onClick()
    }

    override fun onStartListening() {
        super.onStartListening()
        scope = MainScope()
        scope?.launch {
            DeviceProxyManager.getCurrentProxyFlow(applicationContext).collect {
                updateTileState(it)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        scope?.cancel()
        scope = null
    }

    private fun updateTileState(proxy: Proxy) {
        qsTile.state = if (proxy.isEmpty()) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        qsTile.updateTile()
    }
}