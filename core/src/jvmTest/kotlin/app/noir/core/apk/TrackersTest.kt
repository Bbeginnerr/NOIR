/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core.apk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackersTest {

    @Test
    fun `endpoint matching respects domain boundaries`() {
        assertTrue(Trackers.fromEndpoints(listOf("https://cdn.umeng.com/upload")).contains("Umeng"))
        assertTrue(Trackers.fromEndpoints(listOf("https://notumeng.com/upload")).isEmpty())
        assertTrue(Trackers.fromEndpoints(listOf("https://example.com/?ref=qq.com")).isEmpty())
    }

    @Test
    fun `known firebase hosts are detected`() {
        assertEquals(setOf("Firebase"), Trackers.fromEndpoints(listOf("https://firebase-settings.crashlytics.com/x")))
    }
}
