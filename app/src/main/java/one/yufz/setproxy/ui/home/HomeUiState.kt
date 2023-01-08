package one.yufz.setproxy.ui.home

import one.yufz.setproxy.Proxy

data class HomeUiState(
    val requestingPermission: Boolean = false,
    val currentProxy: Proxy = Proxy.EMPTY_PROXY,
    val proxyList: List<Proxy> = emptyList()
)