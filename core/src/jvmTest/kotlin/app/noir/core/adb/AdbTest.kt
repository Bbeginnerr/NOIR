/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core.adb

import app.noir.core.platform.Platform
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdbTest {

    private fun script(body: String): File {
        val file = File(Files.createTempDirectory("noir-adb").toFile(), "fake-adb")
        file.writeText("#!/bin/sh\n$body\n")
        file.setExecutable(true)
        return file
    }

    @Test
    fun `exec timeout fires while the process is silent`() {
        if (Platform.isWindows) return
        val adb = Adb(script("sleep 5").absolutePath)
        val started = System.currentTimeMillis()
        val result = adb.exec(emptyList(), timeoutSec = 1)
        val elapsed = System.currentTimeMillis() - started

        assertTrue(result.timedOut, "process should have timed out")
        assertTrue(elapsed < 4_000, "timeout took too long: ${elapsed}ms")
    }

    @Test
    fun `device list ignores adb daemon startup lines`() {
        if (Platform.isWindows) return
        val adb = Adb(
            script(
                "printf '* daemon not running; starting now at tcp:5037\\n" +
                    "* daemon started successfully\\n" +
                    "List of devices attached\\n" +
                    "serial\\tdevice model:Pixel product:husky\\n'"
            ).absolutePath
        )

        val devices = adb.devices()
        assertEquals(1, devices.size)
        assertEquals("serial", devices.single().serial)
        assertEquals("device", devices.single().state)
        assertEquals("Pixel", devices.single().model)
        assertEquals("husky", devices.single().product)
    }
}
