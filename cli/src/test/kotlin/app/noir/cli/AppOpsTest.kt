/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.cli

import kotlin.test.Test
import kotlin.test.assertEquals

class AppOpsTest {

    @Test
    fun `parses explicit appop mode`() {
        assertEquals("ignore", parseAppOpMode("PROJECT_MEDIA: ignore", "PROJECT_MEDIA"))
        assertEquals("default", parseAppOpMode("GET_USAGE_STATS: default; time=+1d", "GET_USAGE_STATS"))
    }

    @Test
    fun `parses effective default mode`() {
        assertEquals(
            "ignore",
            parseAppOpMode("No operations.\nDefault mode: ignore", "PROJECT_MEDIA")
        )
        assertEquals(
            "default",
            parseAppOpMode("No operations.\nDefault mode: default", "GET_USAGE_STATS")
        )
    }
}
