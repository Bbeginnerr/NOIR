/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core

import java.io.File

/**
 * Remembers endpoint extraction results so repeat scans only touch new APKs.
 * Keyed by package + APK path; Android changes the path whenever the app updates.
 */
object Cache {

    private val dir = File(System.getProperty("user.home"), ".noir").apply { mkdirs() }
    private val file = File(dir, "endpoints.tsv")
    private val map = HashMap<String, String>()

    init {
        if (file.exists()) {
            runCatching {
                file.forEachLine { line ->
                    val sep = line.indexOf('\t')
                    if (sep > 0) map[line.substring(0, sep)] = line.substring(sep + 1)
                }
            }
        }
    }

    private fun key(pkg: String, path: String) = "$pkg|$path"

    fun get(pkg: String, path: String): List<String>? {
        val k = key(pkg, path)
        if (!map.containsKey(k)) return null
        return map[k].orEmpty().split(' ').filter { it.isNotBlank() }
    }

    fun put(pkg: String, path: String, urls: List<String>) {
        map[key(pkg, path)] = urls.joinToString(" ")
    }

    fun flush() {
        runCatching {
            file.writeText(map.entries.joinToString("\n") { "${it.key}\t${it.value}" })
        }
    }
}
