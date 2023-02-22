package one.yufz.setproxy.ui.home

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import one.yufz.setproxy.DeviceProxyManager
import one.yufz.setproxy.Proxy
import one.yufz.setproxy.ProxyStore

class ProxyViewModel(private val app: Application) : AndroidViewModel(app) {
    private val proxyStore = ProxyStore(app)

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> get() = _uiState

    private val currentUiState: HomeUiState get() = _uiState.value

    init {
        // Device proxy may be changed outside of this app
        // So we update the current proxy on every change
        DeviceProxyManager.getCurrentProxyFlow(app).onEach {
            if (!it.isEmpty()) {
                proxyStore.setCurrentProxy(it)
            }
        }.launchIn(viewModelScope)

        combine(DeviceProxyManager.getCurrentProxyFlow(app), proxyStore.currentProxyFlow(), proxyStore.listFlow()) { currentActivated, current, proxyList ->
            updateUiState(currentUiState.copy(currentProxy = current, isActivated = !currentActivated.isEmpty(), proxyList = proxyList))
        }.launchIn(viewModelScope)
    }

    private fun updateUiState(uiState: HomeUiState) {
        _uiState.value = uiState
    }

    fun addProxy(proxy: Proxy) {
        proxyStore.addProxy(proxy)
    }

    fun removeProxy(proxy: Proxy) {
        if (DeviceProxyManager.getCurrentProxy(app) == proxy) {
            deactivateProxy()
        }
        proxyStore.removeProxy(proxy)
    }

    fun setCurrentProxy(proxy: Proxy, active: Boolean) {
        proxyStore.setCurrentProxy(proxy)
        if (active && checkPermission()) {
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

        updateUiState(currentUiState.copy(requestingPermission = !granted))

        Log.i("PermissionRequire", "adb shell pm grant one.yufz.setproxy android.permission.WRITE_SECURE_SETTINGS")

        return granted
    }

    fun cancelRequestPermission() {
        updateUiState(currentUiState.copy(requestingPermission = false))
    }
}