/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RulesTest {

    @Test
    fun `upload detection ignores query strings`() {
        assertTrue(Rules.isUpload("https://tracker.example.com/stats"))
        assertTrue(Rules.isUpload("https://tracker.example.com/logs"))
        assertTrue(Rules.isUpload("https://tracker.example.com/tracking"))
        assertTrue(Rules.isUpload("https://tracker.example.com/analytics"))
        assertFalse(Rules.isUpload("https://cdn.example.com/page?next=https://tracker.example/report"))
    }

    @Test
    fun `coarse location and legacy storage are sensitive`() {
        assertTrue("ACCESS_COARSE_LOCATION" in Rules.SENSITIVE)
        assertTrue("READ_EXTERNAL_STORAGE" in Rules.SENSITIVE)
        assertTrue("READ_MEDIA_AUDIO" in Rules.SENSITIVE)
    }
}
