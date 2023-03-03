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
    }

    fun isEmpty(): Boolean {
        return host.isEmpty() || port <= 0
    }

    fun asAddress(): String {
        return "$host:$port"
    }
}