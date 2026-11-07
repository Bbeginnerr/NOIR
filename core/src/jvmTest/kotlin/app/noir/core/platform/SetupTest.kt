/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core.platform

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse

class SetupTest {

    @Test
    fun `unzip rejects escaping entries`() {
        val dir = Files.createTempDirectory("noir-zip").toFile()
        val zip = File(dir, "bad.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("../escaped.txt"))
            out.write("no".toByteArray())
            out.closeEntry()
        }
        val dest = File(dir, "tools")

        assertFails { Setup.unzip(zip, dest) }
        assertFalse(File(dir, "escaped.txt").exists())
    }
}
