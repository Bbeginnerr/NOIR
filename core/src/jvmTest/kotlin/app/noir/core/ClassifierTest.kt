package app.noir.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassifierTest {

    @Test
    fun `known packages win over keyword guesses`() {
        assertEquals(Category.TELEMETRY, Classifier.category("com.miui.analytics", emptyList(), emptyList()))
        assertEquals(Category.SECURITY, Classifier.category("com.miui.securitycenter", emptyList(), emptyList()))
        assertEquals(Category.SOCIAL, Classifier.category("com.tencent.mm", emptyList(), emptyList()))
        assertEquals(Category.FINANCE, Classifier.category("com.eg.android.AlipayGphone", emptyList(), emptyList()))
        assertEquals(Category.SHOPPING, Classifier.category("com.taobao.taobao", emptyList(), emptyList()))
    }

    @Test
    fun `token boundaries prevent statementservice being called telemetry`() {
        val category = Classifier.category("com.android.statementservice", emptyList(), emptyList(), isSystem = true)
        assertEquals(Category.SYSTEM, category)

        val qti = Classifier.category("com.qti.qualcomm.datastatusnotification", emptyList(), emptyList(), isSystem = true)
        assertEquals(Category.SYSTEM, qti)
    }

    @Test
    fun `telemetry is detected from endpoint paths`() {
        val category = Classifier.category(
            "com.example.unknown",
            listOf("https://tracking.example.com/track/v1"),
            emptyList()
        )
        assertEquals(Category.TELEMETRY, category)
    }

    @Test
    fun `unknown system packages fall back to system instead of other`() {
        assertEquals(Category.SYSTEM, Classifier.category("com.unknown.system", emptyList(), emptyList(), isSystem = true))
        assertEquals(Category.OTHER, Classifier.category("com.unknown.user", emptyList(), emptyList(), isSystem = false))
    }

    @Test
    fun `vendor detection uses domains`() {
        assertEquals("Xiaomi", Classifier.vendor("com.example", listOf("https://api.sec.miui.com/x")))
        assertEquals("Alibaba", Classifier.vendor("com.example", listOf("https://cgicol.amap.com/x")))
    }

    @Test
    fun `scoring rewards upload and command endpoints`() {
        val base = Rules.score(listOf("INTERNET", "READ_SMS"), emptyList(), false, false)
        val withUpload = Rules.score(listOf("INTERNET", "READ_SMS"), listOf("https://a.example.com/upload"), false, false)
        val withCommand = Rules.score(listOf("INTERNET", "READ_SMS"), listOf("https://a.example.com/api/command/query"), false, false)
        assertTrue(withUpload > base)
        assertTrue(withCommand > base)
    }
}
