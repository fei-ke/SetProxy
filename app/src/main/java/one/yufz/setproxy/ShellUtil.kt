package one.yufz.setproxy

import java.io.BufferedReader
import java.io.InputStreamReader


object ShellUtil {

    fun executeCommand(command: String, onSuccess: (text: String) -> Unit, onError: (text: String) -> Unit) = try {
        val process = Runtime.getRuntime().exec(command)
        val inputStream = process.inputStream
        val errorStream = process.errorStream
        val inputReader = BufferedReader(InputStreamReader(inputStream))
        val errorReader = BufferedReader(InputStreamReader(errorStream))
        val output = StringBuilder()

        var line: String?

        while (inputReader.readLine().also { line = it } != null) {
            output.append(line)
        }

        while (errorReader.readLine().also { line = it } != null) {
            output.append(line)
        }

        val exitValue = process.waitFor()

        if (exitValue == 0) {
            onSuccess(output.toString())
        } else {
            onError(output.toString())
        }
    } catch (e: Exception) {
        onError(e.message ?: "Unknown error")
    }
}