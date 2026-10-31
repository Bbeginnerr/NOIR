package app.noir.core.adb

import app.noir.core.Device
import app.noir.core.platform.Exec
import java.io.File
import java.util.concurrent.TimeUnit

data class ExecResult(val code: Int, val output: String, val timedOut: Boolean = false) {
    val ok: Boolean get() = code == 0 && !timedOut
}

/** Thin wrapper around the platform `adb` binary. */
class Adb(private val bin: String = System.getenv("ADB") ?: "adb") {

    fun exec(args: List<String>, timeoutSec: Long = 180): ExecResult {
        val process = ProcessBuilder(Exec.argv(bin, args))
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val finished = if (timeoutSec <= 0) {
            process.waitFor()
            true
        } else {
            process.waitFor(timeoutSec, TimeUnit.SECONDS)
        }
        if (!finished) {
            process.destroyForcibly()
            return ExecResult(-1, output, timedOut = true)
        }
        return ExecResult(process.exitValue(), output)
    }

    fun shell(serial: String?, command: String, timeoutSec: Long = 180): ExecResult {
        val args = mutableListOf<String>()
        if (serial != null) {
            args += "-s"
            args += serial
        }
        args += "shell"
        args += command
        return exec(args, timeoutSec)
    }

    fun devices(): List<Device> {
        val out = exec(listOf("devices", "-l"), timeoutSec = 30).output
        return out.lineSequence()
            .drop(1)
            .filter { it.isNotBlank() && !it.startsWith("*") }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
                if (parts.size < 2) return@mapNotNull null
                var model = ""
                var product = ""
                parts.drop(2).forEach { field ->
                    if (field.startsWith("model:")) model = field.removePrefix("model:")
                    if (field.startsWith("product:")) product = field.removePrefix("product:")
                }
                Device(serial = parts[0], state = parts[1], model = model, product = product)
            }
            .toList()
    }

    fun push(serial: String, local: File, remote: String): ExecResult =
        exec(listOf("-s", serial, "push", local.absolutePath, remote), timeoutSec = 120)

    fun pull(serial: String, remote: String, local: File): ExecResult {
        local.parentFile?.mkdirs()
        return exec(listOf("-s", serial, "pull", remote, local.absolutePath), timeoutSec = 600)
    }
}
