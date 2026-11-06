/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class I18nTest {

    private val han = Regex("[\\u4e00-\\u9fff]")

    @Test
    fun `locale tags resolve to a supported language`() {
        assertEquals(Lang.ZH, Lang.fromTag("zh-CN"))
        assertEquals(Lang.ZH, Lang.fromTag("zh_CN.UTF-8"))
        assertEquals(Lang.EN, Lang.fromTag("en-US"))
        assertEquals(Lang.EN, Lang.fromTag("fr-FR"))
        assertEquals(null, Lang.fromTag(null))
        assertEquals(null, Lang.fromTag("  "))
    }

    @Test
    fun `english labels contain no chinese characters`() {
        val labels = Category.entries.map { it.localized(Lang.EN) } +
            DataType.entries.map { it.localized(Lang.EN) } +
            ActionType.entries.map { it.localized(Lang.EN) }
        labels.forEach { label ->
            assertFalse(han.containsMatchIn(label), "untranslated label: $label")
        }
        assertEquals("telemetry", Category.TELEMETRY.localized(Lang.EN))
        assertEquals("revoke capture", ActionType.APPOP_PROJECT_MEDIA.localized(Lang.EN))
        assertEquals("microphone", DataType.MIC.localized(Lang.EN))
    }

    @Test
    fun `english advisor reasons are translated`() {
        val app = AppEntry(
            pkg = "com.example.telemetry",
            uid = "10123",
            isSystem = true,
            debuggable = false,
            installer = null,
            permissions = listOf("INTERNET", "PACKAGE_USAGE_STATS"),
            endpoints = listOf(
                "https://tracker.example.com/upload",
                "https://stats.example.com/report"
            ),
            score = 30,
            category = Category.TELEMETRY,
            vendor = "Example"
        )

        val recommendations = Advisor.advise(listOf(app), Lang.EN)
        assertTrue(recommendations.isNotEmpty())
        recommendations.forEach { recommendation ->
            assertFalse(han.containsMatchIn(recommendation.reason), "untranslated reason: ${recommendation.reason}")
        }
    }
}
