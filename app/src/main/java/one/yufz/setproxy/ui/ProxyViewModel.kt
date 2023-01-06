package one.yufz.setproxy.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import one.yufz.setproxy.DeviceProxyManager
import one.yufz.setproxy.Proxy
import one.yufz.setproxy.ProxyStore

class ProxyViewModel(private val app: Application) : AndroidViewModel(app) {
    private val proxyStore = ProxyStore(app)

    val currentProxy: Flow<Proxy> = DeviceProxyManager.getCurrentProxyFlow(app)

    val proxyList: StateFlow<List<Proxy>> = proxyStore.listFlow()

    private val _requestPermission = MutableStateFlow(false)

    val requestPermission: Flow<Boolean> = _requestPermission

    fun addProxy(proxy: Proxy) {
        proxyStore.addProxy(proxy)
    }

    fun removeProxy(proxy: Proxy) {
        proxyStore.removeProxy(proxy)
    }

    fun activateProxy(proxy: Proxy) {
        if (checkPermission()) {
            DeviceProxyManager.setProxy(app, proxy)
        }
    }

    fun deactivateProxy() {
        if (checkPermission()) {
            DeviceProxyManager.removeProxy(app)
        }
    }

    private fun checkPermission(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            app.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
        } else {
            app.checkPermission(Manifest.permission.WRITE_SECURE_SETTINGS, android.os.Process.myPid(), android.os.Process.myUid())
        } == PackageManager.PERMISSION_GRANTED

        _requestPermission.value = !granted

        Log.i("PermissionRequire", "adb shell pm grant one.yufz.setproxy android.permission.WRITE_SECURE_SETTINGS")

        return granted
    }

    fun cancelRequestPermission() {
        _requestPermission.value = false
    }
}