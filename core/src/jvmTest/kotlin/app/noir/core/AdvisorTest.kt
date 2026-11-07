/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core

import kotlin.test.Test
import kotlin.test.assertTrue

class AdvisorTest {

    @Test
    fun `declared media projection capability is not an automatic action`() {
        val app = AppEntry(
            pkg = "com.miui.powerkeeper",
            uid = "1000",
            isSystem = true,
            debuggable = false,
            installer = null,
            permissions = listOf("INTERNET", "FOREGROUND_SERVICE_MEDIA_PROJECTION"),
            endpoints = listOf("https://tracker.example.com/upload"),
            score = 30,
            category = Category.SECURITY,
            vendor = "Xiaomi"
        )

        val recommendations = Advisor.advise(listOf(app))
        assertTrue(recommendations.none { it.action == ActionType.APPOP_PROJECT_MEDIA })
    }

    @Test
    fun `multiple URLs on one host do not trigger a DNS recommendation`() {
        val app = AppEntry(
            pkg = "com.example.telemetry",
            uid = "10123",
            isSystem = false,
            debuggable = false,
            installer = null,
            permissions = listOf("INTERNET"),
            endpoints = listOf(
                "https://tracker.example.com/upload",
                "https://tracker.example.com/report"
            ),
            score = 10,
            category = Category.TELEMETRY,
            vendor = "Example"
        )
        val recommendations = Advisor.advise(listOf(app))
        assertTrue(recommendations.none { it.action == ActionType.BLOCK_DNS })
    }

    @Test
    fun `usage stats telemetry still gets a manual-mode recommendation`() {
        val app = AppEntry(
            pkg = "com.example.telemetry",
            uid = "10123",
            isSystem = true,
            debuggable = false,
            installer = null,
            permissions = listOf("INTERNET", "PACKAGE_USAGE_STATS"),
            endpoints = emptyList(),
            score = 12,
            category = Category.TELEMETRY,
            vendor = "Example"
        )

        val recommendations = Advisor.advise(listOf(app))
        assertTrue(recommendations.any { it.action == ActionType.APPOP_USAGE_STATS })
    }
}
