/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core.adb

import app.noir.core.apk.ApkScan
import java.io.File

data class PackagePath(val pkg: String, val path: String)

/**
 * Collects URL strings from every installed APK.
 *
 * The fast path runs entirely on the device (unzip + strings), so only a few KB
 * cross the USB link. If the device lacks those toybox tools, it falls back to
 * pulling each APK and scanning it locally.
 */
object DeviceScan {

    private const val FALLBACK_SUFFIX = "!FALLBACK"

    fun packagePaths(adb: Adb, serial: String): Map<String, String> {
        val out = adb.shell(serial, "pm list packages -f").output
        val map = HashMap<String, String>()
        out.lineSequence().forEach { line ->
            if (!line.startsWith("package:")) return@forEach
            val body = line.removePrefix("package:")
            val idx = body.lastIndexOf('=')
            if (idx <= 0) return@forEach
            val path = body.substring(0, idx)
            val pkg = body.substring(idx + 1).trim()
            if (pkg.isNotEmpty()) map[pkg] = path
        }
        return map
    }

    fun hasDeviceTools(adb: Adb, serial: String): Boolean {
        val out = adb.shell(serial, "which unzip strings").output
        return out.contains("unzip") && out.contains("strings")
    }

    fun urls(
        adb: Adb,
        serial: String,
        targets: List<PackagePath>,
        workDir: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Map<String, List<String>> {
        if (targets.isEmpty()) return emptyMap()
        workDir.mkdirs()

        if (!hasDeviceTools(adb, serial)) {
            return urlsByPulling(adb, serial, targets, workDir, onProgress)
        }

        val result = HashMap<String, List<String>>()
        val pendingFallback = ArrayList<String>()
        val chunks = targets.chunked(8)

        val script = File(workDir, "scan.sh")
        script.writeText(
            """
            #!/system/bin/sh
            OUT=/data/local/tmp/noir_urls.txt
            : > ${'$'}OUT
            TMP=/data/local/tmp/noir_dex.bin
            while read p f; do
              # Explicit entry names: some vendor `unzip` builds do not expand 'classes*.dex'.
              : > ${'$'}TMP
              for d in classes.dex classes2.dex classes3.dex classes4.dex classes5.dex \
                       classes6.dex classes7.dex classes8.dex classes9.dex classes10.dex \
                       classes11.dex classes12.dex classes13.dex classes14.dex classes15.dex; do
                unzip -p "${'$'}f" "${'$'}d" 2>/dev/null >> ${'$'}TMP
              done
              if [ ! -s ${'$'}TMP ]; then
                # Nothing extractable: ask the host to pull and parse this APK.
                echo "${'$'}p!FALLBACK" >> ${'$'}OUT
              else
                u=${'$'}(strings < ${'$'}TMP | grep -oE 'https?://[A-Za-z0-9._~:/?#@!&()*+,;=%-]+' \
                    | sort -u | head -400 | tr '\n' ' ')
                [ -n "${'$'}u" ] && echo "${'$'}p|${'$'}u" >> ${'$'}OUT
              fi
              rm -f ${'$'}TMP
            done < /data/local/tmp/noir_targets.txt
            echo "#DONE" >> ${'$'}OUT
            """.trimIndent()
        )
        adb.push(serial, script, "/data/local/tmp/noir_scan.sh")

        chunks.forEachIndexed { chunkIndex, chunk ->
            val list = File(workDir, "targets.txt")
            list.writeText(chunk.joinToString("\n", postfix = "\n") { "${it.pkg} ${it.path}" })
            adb.push(serial, list, "/data/local/tmp/noir_targets.txt")
            adb.shell(serial, "sh /data/local/tmp/noir_scan.sh", timeoutSec = 0)

            val local = File(workDir, "urls.txt")
            adb.pull(serial, "/data/local/tmp/noir_urls.txt", local)

            if (local.exists()) {
                local.readLines().forEach { line ->
                    if (line.endsWith(FALLBACK_SUFFIX)) {
                        pendingFallback.add(line.removeSuffix(FALLBACK_SUFFIX))
                        return@forEach
                    }
                    if (!line.contains('|')) return@forEach
                    val pkg = line.substringBefore('|')
                    val found = line.substringAfter('|').split(' ').filter { it.isNotBlank() }
                    if (found.isNotEmpty()) result[pkg] = found
                }
            }

            onProgress(minOf((chunkIndex + 1) * 8, targets.size), targets.size)
        }

        if (pendingFallback.isNotEmpty()) {
            println("  ! ${'$'}{pendingFallback.size} APK(s) could not be read by the device's unzip; pulling instead")
            val fallbackTargets = targets.filter { it.pkg in pendingFallback }
            result.putAll(urlsByPulling(adb, serial, fallbackTargets, workDir, onProgress))
        }

        return result
    }

    private fun urlsByPulling(
        adb: Adb,
        serial: String,
        targets: List<PackagePath>,
        workDir: File,
        onProgress: (Int, Int) -> Unit
    ): Map<String, List<String>> {
        val result = HashMap<String, List<String>>()
        val apkDir = File(workDir, "apks").apply { mkdirs() }
        targets.forEachIndexed { index, target ->
            val local = File(apkDir, "${target.pkg}.apk")
            adb.pull(serial, target.path, local)
            if (local.exists()) {
                val found = runCatching { ApkScan.urls(local) }.getOrDefault(emptyList())
                if (found.isNotEmpty()) result[target.pkg] = found
            }
            onProgress(index + 1, targets.size)
        }
        return result
    }
}
