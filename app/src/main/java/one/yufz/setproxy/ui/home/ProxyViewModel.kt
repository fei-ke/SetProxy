package one.yufz.setproxy.ui.home

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import one.yufz.setproxy.DeviceProxyManager
import one.yufz.setproxy.NotificationManager
import one.yufz.setproxy.Permission
import one.yufz.setproxy.Proxy
import one.yufz.setproxy.Proxy.Companion.toAddress
import one.yufz.setproxy.ProxyStore
import one.yufz.setproxy.ShellUtil

class ProxyViewModel(private val app: Application) : AndroidViewModel(app) {
    private val proxyStore = ProxyStore(app)

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> get() = _uiState

    private val currentUiState: HomeUiState get() = _uiState.value

    init {
        // Device proxy may be changed outside of this app
        // So we update the current proxy on every change
        DeviceProxyManager.getActivatedProxyFlow(app).onEach { activatedAddress ->
            if (activatedAddress != proxyStore.getCurrentProxy().toAddress()) {
                val proxy = proxyStore.getProxyList()
                    .find { it.toAddress() == activatedAddress }
                    ?: Proxy.fromAddress(activatedAddress)

                if (!proxy.isEmpty()) {
                    proxyStore.setCurrentProxy(proxy)
                    NotificationManager.showNotification(app, proxy)
                }
            }
        }.launchIn(viewModelScope)

        combine(DeviceProxyManager.getActivatedProxyFlow(app), proxyStore.currentProxyFlow(), proxyStore.listFlow()) { currentActivated, current, proxyList ->
            val isActivated = Proxy.isValidProxy(currentActivated)
            updateUiState(currentUiState.copy(currentProxy = current, isActivated = isActivated, proxyList = proxyList))
        }.launchIn(viewModelScope)
    }

    private fun updateUiState(uiState: HomeUiState) {
        _uiState.value = uiState
    }

    fun addProxy(proxy: Proxy) {
        proxyStore.addProxy(proxy)
    }

    fun removeProxy(proxy: Proxy) {
        if (DeviceProxyManager.getActivatedProxy(app) == proxy.toAddress()) {
            deactivateProxy()
        }
        proxyStore.removeProxy(proxy)
    }

    fun replaceProxy(oldProxy: Proxy, newProxy: Proxy) {
        proxyStore.replaceProxy(oldProxy, newProxy)

        if (DeviceProxyManager.getActivatedProxy(app) == oldProxy.toAddress()) {
            setCurrentProxy(newProxy, true)
        }
    }

    fun setCurrentProxy(proxy: Proxy, active: Boolean) {
        proxyStore.setCurrentProxy(proxy)
        if (active && checkPermission()) {
            DeviceProxyManager.activateProxy(app, proxy)
        }
    }


    fun copyProxy(proxy: Proxy) {
        addProxy(proxy.copy(name = "Copy of " + proxy.name))
    }

    fun deactivateProxy() {
        if (checkPermission()) {
            DeviceProxyManager.deactivateProxy(app)
        }
    }

    fun requestEditProxy(proxy: Proxy) {
        updateUiState(currentUiState.copy(requestEditProxy = proxy))
    }

    fun cancelEditProxy() {
        updateUiState(currentUiState.copy(requestEditProxy = null))
    }

    fun requestAddProxy() {
        updateUiState(currentUiState.copy(requestAddProxy = true))
    }

    fun cancelAddProxy() {
        updateUiState(currentUiState.copy(requestAddProxy = false))
    }

    private fun checkPermission(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            app.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
        } else {
            app.checkPermission(Manifest.permission.WRITE_SECURE_SETTINGS, android.os.Process.myPid(), android.os.Process.myUid())
        } == PackageManager.PERMISSION_GRANTED

        updateUiState(currentUiState.copy(requestingPermission = !granted))

        if (!granted) {
            Log.i("PermissionRequire", Permission.ADB_COMMAND)
        }
        return granted
    }

    fun cancelRequestPermission() {
        updateUiState(currentUiState.copy(requestingPermission = false))
    }

    fun requestPermissionUseRoot() {
        ShellUtil.executeCommand(Permission.SU_COMMAND, onSuccess = {}) {
            Toast.makeText(app, it, Toast.LENGTH_SHORT).show()
        }
    }
}