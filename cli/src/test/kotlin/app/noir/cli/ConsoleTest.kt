/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.cli

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsoleTest {

    @Test
    fun `legacy windows console is not assumed to support ansi`() {
        assertFalse(ansiSupported(windows = true, env = emptyMap()))
    }

    @Test
    fun `windows terminals and ansi shims are detected`() {
        assertTrue(ansiSupported(windows = true, env = mapOf("WT_SESSION" to "session")))
        assertTrue(ansiSupported(windows = true, env = mapOf("TERM_PROGRAM" to "vscode")))
        assertTrue(ansiSupported(windows = true, env = mapOf("ANSICON" to "1")))
        assertTrue(ansiSupported(windows = true, env = mapOf("ConEmuANSI" to "ON")))
    }

    @Test
    fun `unix defaults to ansi but respects dumb terminals`() {
        assertTrue(ansiSupported(windows = false, env = emptyMap()))
        assertFalse(ansiSupported(windows = false, env = mapOf("TERM" to "dumb")))
    }
}
