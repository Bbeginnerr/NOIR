/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
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

    fun exec(args: List<String>, timeoutSec: Long = 180): ExecResult =
        runProcess(args, timeoutSec, null)

    /**
     * Runs a command and delivers stdout line by line. Used by the device-side
     * scan so the progress bar can advance once per APK.
     */
    fun execStreaming(
        args: List<String>,
        timeoutSec: Long = 0,
        onLine: (String) -> Unit
    ): ExecResult = runProcess(args, timeoutSec, onLine)

    /**
     * Drains stdout on a daemon thread while the caller enforces the deadline.
     * Reading to EOF before waitFor() would make the timeout ineffective for a
     * process that hangs without closing its pipe.
     */
    private fun runProcess(
        args: List<String>,
        timeoutSec: Long,
        onLine: ((String) -> Unit)?
    ): ExecResult {
        val process = ProcessBuilder(Exec.argv(bin, args))
            .redirectErrorStream(true)
            .start()
        val output = StringBuilder()
        val reader = Thread {
            try {
                process.inputStream.bufferedReader().use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        synchronized(output) { output.append(line).append('\n') }
                        try {
                            onLine?.invoke(line)
                        } catch (_: Exception) {
                            // A progress callback must not kill the stream reader.
                        }
                    }
                }
            } catch (_: Exception) {
                // Process killed on timeout, or the pipe was closed.
            }
        }
        reader.isDaemon = true
        reader.start()

        val finished = if (timeoutSec <= 0) {
            process.waitFor()
            true
        } else {
            process.waitFor(timeoutSec, TimeUnit.SECONDS)
        }
        if (!finished) {
            process.destroyForcibly()
            reader.join(2_000)
            return ExecResult(-1, synchronized(output) { output.toString() }, timedOut = true)
        }
        reader.join(2_000)
        return ExecResult(process.exitValue(), synchronized(output) { output.toString() })
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
            .map { it.trim() }
            .filter {
                it.isNotEmpty() &&
                    !it.startsWith("*") &&
                    !it.startsWith("List of devices") &&
                    !it.startsWith("adb server")
            }
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
