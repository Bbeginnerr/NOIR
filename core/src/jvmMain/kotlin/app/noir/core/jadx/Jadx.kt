/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core.jadx

import app.noir.core.platform.Exec
import java.io.File
import java.util.concurrent.TimeUnit

data class JadxHit(val file: String, val line: Int, val text: String)

/** Drives the external `jadx` CLI and greps its output. */
class Jadx(private val bin: String = System.getenv("JADX") ?: "jadx") {

    fun available(): Boolean = try {
        run(listOf("--version"), 15) == 0
    } catch (e: Exception) {
        false
    }

    fun decompile(apk: File, outDir: File, noRes: Boolean = true, timeoutSec: Long = 1_800): Int {
        outDir.mkdirs()
        val args = mutableListOf("-d", outDir.absolutePath)
        if (noRes) args += "--no-res"
        args += apk.absolutePath
        return run(args, timeoutSec)
    }

    /** Runs jadx with a real deadline; a hung decompiler must not hang NOIR. */
    private fun run(args: List<String>, timeoutSec: Long): Int {
        val process = ProcessBuilder(Exec.argv(bin, args)).redirectErrorStream(true).start()
        val reader = Thread {
            try {
                process.inputStream.bufferedReader().use { stream ->
                    while (stream.readLine() != null) {
                        // Drain stdout/stderr so the child cannot block on a full pipe.
                    }
                }
            } catch (_: Exception) {
                // Process was destroyed after the deadline.
            }
        }
        reader.isDaemon = true
        reader.start()
        val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            reader.join(2_000)
            return -1
        }
        reader.join(2_000)
        return process.exitValue()
    }

    fun grep(dir: File, patterns: List<String>, maxHits: Int = 200): List<JadxHit> {
        val hits = ArrayList<JadxHit>()
        if (!dir.exists()) return hits

        val regexes = patterns.map { Regex(it, RegexOption.IGNORE_CASE) }
        val files = dir.walkTopDown()
            .filter { it.isFile && (it.extension == "java" || it.extension == "kt") }
            .toList()

        for (file in files) {
            if (hits.size >= maxHits) break
            val lines = try {
                file.readLines()
            } catch (e: Exception) {
                continue
            }
            for ((index, line) in lines.withIndex()) {
                if (hits.size >= maxHits) break
                if (regexes.any { it.containsMatchIn(line) }) {
                    hits.add(JadxHit(file.relativeTo(dir).path, index + 1, line.trim().take(220)))
                }
            }
        }
        return hits
    }
}
