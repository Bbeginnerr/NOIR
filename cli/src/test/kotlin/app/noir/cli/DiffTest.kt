/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.cli

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffTest {

    private fun app(
        pkg: String,
        score: Int,
        category: String = "遥测/统计",
        permissions: List<String> = listOf("INTERNET"),
        endpoints: List<String> = listOf("https://tracker.example.com/p"),
        categoryKey: String? = null
    ) = ReportApp(pkg, score, category, permissions, endpoints, categoryKey)

    private fun snapshot(path: String, apps: List<ReportApp>) = ReportSnapshot(
        path = path,
        device = "PLK110",
        tool = "NOIR test",
        generated = "2026-11-06T00:00:00Z",
        apps = apps.associateBy { it.pkg }
    )

    @Test
    fun `diff reports added removed and changed apps`() {
        val old = snapshot(
            "old",
            listOf(
                app("com.example.same", 10),
                app("com.example.gone", 20),
                app("com.example.changed", 12)
            )
        )
        val new = snapshot(
            "new",
            listOf(
                app("com.example.same", 10),
                app("com.example.changed", 18, permissions = listOf("INTERNET", "READ_SMS")),
                app("com.example.new", 31)
            )
        )

        val result = diffReports(old, new)
        assertEquals(listOf("com.example.new"), result.added.map { it.pkg })
        assertEquals(listOf("com.example.gone"), result.removed.map { it.pkg })
        assertEquals(1, result.changed.size)
        assertEquals(6, result.changed.single().scoreDelta)
        assertEquals(listOf("READ_SMS"), result.changed.single().addedPermissions)
        assertEquals(1, result.unchanged)
        assertTrue(result.regressions)
    }

    @Test
    fun `removing apps and lowering scores is not a regression`() {
        val old = snapshot("old", listOf(app("com.example.a", 30), app("com.example.b", 20)))
        val new = snapshot("new", listOf(app("com.example.a", 10)))
        val result = diffReports(old, new)
        assertEquals(1, result.removed.size)
        assertEquals(20, result.changed.single().scoreDelta.unaryMinus())
        assertTrue(!result.regressions)
    }

    @Test
    fun `a language switch is not reported as a category change`() {
        val old = snapshot("old", listOf(app("com.example.a", 12, category = "遥测/统计")))
        val new = snapshot("new", listOf(app("com.example.a", 12, category = "telemetry")))

        val result = diffReports(old, new)
        assertTrue(result.changed.isEmpty())
        assertTrue(!result.regressions)
    }

    @Test
    fun `report json can be parsed`() {
        val file = Files.createTempFile("noir-report", ".json").toFile()
        file.writeText(
            """
            {
              "environment": {"tool": "NOIR test", "generatedAt": "2026-11-06T00:00:00Z"},
              "device": {"serial": "serial", "model": "PLK110"},
              "apps": [
                {
                  "package": "com.example",
                  "score": 42,
                  "category": "系统",
                  "permissions": ["INTERNET", "READ_SMS"],
                  "endpoints": ["https://a.example.com", "https://b.example.com"]
                }
              ]
            }
            """.trimIndent()
        )
        val snapshot = readReport(file.path)
        assertEquals("PLK110 (serial)", snapshot.device)
        assertEquals(42, snapshot.apps.getValue("com.example").score)
        assertEquals(2, snapshot.apps.getValue("com.example").endpoints.size)
        file.delete()
    }

    @Test
    fun `render includes the three change groups`() {
        val result = diffReports(
            snapshot("old", listOf(app("com.example.old", 1))),
            snapshot("new", listOf(app("com.example.new", 2)))
        )
        val text = renderDiff(result)
        assertTrue(text.contains("[+] added (1)"))
        assertTrue(text.contains("[-] removed (1)"))
        assertTrue(text.contains("[~] changed (0)"))
        assertTrue(renderDiffJson(result).contains("\"regression\": true"))
    }
}
