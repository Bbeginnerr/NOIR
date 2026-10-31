package app.noir.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportTest {

    @Test
    fun `domains exports only upload hosts and deduplicates them`() {
        val entry = AppEntry(
            pkg = "com.example",
            uid = "1",
            isSystem = false,
            debuggable = false,
            installer = null,
            permissions = listOf("INTERNET"),
            endpoints = listOf(
                "https://a.example.com/upload",
                "https://a.example.com/anything",
                "https://b.example.com/collect/data",
                "https://c.example.com/config"
            ),
            score = 5
        )
        assertEquals(listOf("a.example.com", "b.example.com"), Report.domains(listOf(entry)).lines())
    }

    @Test
    fun `json escapes quotes and control characters`() {
        val entry = AppEntry(
            pkg = "com.\"weird\"",
            uid = "1",
            isSystem = false,
            debuggable = false,
            installer = null,
            permissions = emptyList(),
            endpoints = listOf("https://a.example.com/x"),
            score = 0
        )
        val json = Report.json(Device("s", "device"), listOf(entry))
        assertTrue(json.contains("com.\\\"weird\\\""))
        assertTrue(json.contains("\"apps\""))
    }

    @Test
    fun `markdown contains the report table header`() {
        val entry = AppEntry(
            pkg = "com.example",
            uid = "1",
            isSystem = true,
            debuggable = false,
            installer = "com.android.vending",
            permissions = listOf("INTERNET"),
            endpoints = emptyList(),
            score = 3,
            category = Category.SYSTEM
        )
        val md = Report.markdown(Device("s", "device", "Pixel"), listOf(entry))
        assertTrue(md.startsWith("# NOIR report"))
        assertTrue(md.contains("| score | package |"))
        assertTrue(md.contains("`com.example`"))
    }
}
