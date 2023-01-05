package one.yufz.setproxy

data class Proxy(
    val host: String,
    val port: Int,
    val userName: String? = null,
    val password: String? = null
) {

    companion object {
        val EMPTY_PROXY = Proxy("", 0)
    }

    fun isEmpty(): Boolean {
        return host.isEmpty() || port <= 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Proxy

        if (host != other.host) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + port
        return result
    }
}