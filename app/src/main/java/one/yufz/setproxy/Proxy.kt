package one.yufz.setproxy

import org.json.JSONObject

data class Proxy(
    val host: String,
    val port: Int,
    val username: String? = null,
    val password: String? = null,
    val name: String = "",
) {

    companion object {
        val EMPTY_PROXY = Proxy("", 0)

        private const val PROXY_REGEX = """^(?:(?<username>[^:]+):(?<password>[^@]+)@)?(?<host>[^:]+):(?<port>\d+)$"""
        fun Proxy.toJson(): JSONObject {
            return JSONObject().apply {
                put("host", host)
                put("port", port)
                put("username", username)
                put("password", password)
                put("name", name)
            }
        }

        fun fromJson(json: String): Proxy {
            return fromJson(JSONObject(json))
        }

        fun fromJson(json: JSONObject): Proxy {
            return try {
                Proxy(
                    host = json.getString("host"),
                    port = json.getInt("port"),
                    username = json.optString("username"),
                    password = json.optString("password"),
                    name = json.optString("name")
                )
            } catch (e: Exception) {
                EMPTY_PROXY
            }
        }

        fun isValidProxy(address: String): Boolean {
            return PROXY_REGEX.toRegex().matches(address)
        }

        fun fromAddress(address: String): Proxy {
            val matchResult = PROXY_REGEX.toRegex().find(address)

            // 判断是否匹配成功
            if (matchResult != null) {
                val username = matchResult.groups["username"]?.value
                val password = matchResult.groups["password"]?.value
                val host = matchResult.groups["host"]?.value!!
                val port = matchResult.groups["port"]?.value!!

                return Proxy(host, port.toInt(), username, password)
            } else {
                return EMPTY_PROXY
            }
        }

        fun Proxy.toAddress(): String {
            if (username.isNullOrBlank() || password.isNullOrBlank()) {
                return "$host:$port"
            } else {
                return "$username:$password@$host:$port"
            }
        }
    }

    fun isEmpty(): Boolean {
        return host.isEmpty() || port <= 0
    }
}