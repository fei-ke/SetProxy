package one.yufz.setproxy

object Util {
    fun checkProxyFormat(text: String): Boolean {
        val regex = "^([\\w.-]+|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+)\$".toRegex()
        return regex.matches(text)
    }
}