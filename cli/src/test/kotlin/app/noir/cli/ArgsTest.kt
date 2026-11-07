/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.cli

import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArgsTest {

    @Test
    fun `boolean false values do not enable destructive flags`() {
        val args = Args(
            listOf(
                "--apply=false",
                "--purge=false",
                "--yes=false",
                "--fail-on-regression=false",
                "--quiet=true",
                "--verbose=false"
            )
        )
        assertFalse(args.flag("apply"))
        assertFalse(args.flag("purge"))
        assertFalse(args.flag("yes"))
        assertFalse(args.flag("fail-on-regression"))
        assertTrue(args.flag("quiet"))
        assertFalse(args.flag("verbose"))
    }

    @Test
    fun `unknown options and missing values fail before execution`() {
        assertFails { Args(listOf("--aply")) }
        assertFails { Args(listOf("--serial")) }
        assertFails { Args(listOf("--out", "--yes")) }
    }

    @Test
    fun `inline values are parsed`() {
        val args = Args(listOf("--serial=abc", "--limit=10", "--apply"))
        assertTrue(args.flag("apply"))
        assertTrue(args.value("serial") == "abc")
        assertTrue(args.value("limit") == "10")
    }
}
