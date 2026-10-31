package app.noir.cli

import app.noir.core.Tui

/** Human-friendly text blocks: every failure should end with an action. */
object Guidance {

    fun deviceGuide(): String = """
        |╭─ 没有检测到设备 ──────────────────────────────────────────────╮
        |│                                                              │
        |│  1. 打开开发者模式                                           │
        |│     小米 / HyperOS：设置 → 我的设备 → 全部参数 → 连点「MIUI 版本」7 次
        |│     原生 Android：设置 → 关于手机 → 连点「版本号」7 次        │
        |│                                                              │
        |│  2. 开启 USB 调试                                            │
        |│     设置 → 系统 → 开发者选项 → USB 调试                      │
        |│     Xiaomi 额外开启：USB 调试（安全设置）                     │
        |│                                                              │
        |│  3. 插线后手机会弹「允许 USB 调试吗？」                       │
        |│     勾选「一律允许使用这台计算机进行调试」→ 允许              │
        |│                                                              │
        |│  4. 还不行就换一根数据线（很多线只能充电）                    │
        |╰──────────────────────────────────────────────────────────────╯
    """.trimMargin()

    fun unauthorizedGuide(): String = """
        |设备已连接，但没有授权。
        |
        |  • 看手机屏幕：应该有一个「允许 USB 调试吗？」的弹窗
        |  • 没弹窗：开发者选项 → 撤销 USB 调试授权 → 重新插拔
        |  • 仍不行：开发者选项 → 关闭再打开 USB 调试
    """.trimMargin()

    fun adbMissing(): String = """
        |  • macOS：brew install --cask android-platform-tools
        |  • Windows：scoop install adb  或  https://developer.android.com/tools/releases/platform-tools
        |  • Debian/Ubuntu：sudo apt install adb
        |  • 或者让 NOIR 自己下载：noir setup
    """.trimMargin()

    fun jadxMissing(): String = """
        |  • macOS：brew install jadx
        |  • Windows：scoop install jadx
        |  • Debian/Ubuntu：apt install jadx
        |  • 或者让 NOIR 自己下载：noir setup
    """.trimMargin()

    fun error(title: String, hint: String? = null): String {
        val total = (Tui.terminalColumns() - 2).coerceIn(50, 100)
        val inner = total - 4

        fun row(text: String) = "│ " + pad(text, inner) + " │"

        val sb = StringBuilder()
        sb.append("╭─ ERROR ").append("─".repeat(total - 10)).append("╮").append('\n')
        wrap(title, inner).forEach { sb.append(row(it)).append('\n') }
        if (hint != null) {
            sb.append('├').append("─".repeat(total - 2)).append('┤').append('\n')
            wrap(hint, inner).forEach { sb.append(row(it)).append('\n') }
        }
        sb.append('╰').append("─".repeat(total - 2)).append('╯')
        return sb.toString()
    }

    private fun wrap(text: String, width: Int): List<String> {
        val out = ArrayList<String>()
        text.lines().forEach { line ->
            val sb = StringBuilder()
            var used = 0
            for (c in line) {
                val cw = charWidth(c)
                if (used + cw > width) {
                    out.add(sb.toString())
                    sb.clear()
                    used = 0
                }
                sb.append(c)
                used += cw
            }
            out.add(sb.toString())
        }
        return out
    }

    private fun pad(s: String, width: Int) = s + " ".repeat(maxOf(0, width - width(s)))

    private fun width(s: String) = s.sumOf { charWidth(it) }

    private fun charWidth(c: Char): Int = when {
        c.code in 0x1100..0x115F -> 2
        c.code in 0x2E80..0xA4CF -> 2
        c.code in 0xAC00..0xD7A3 -> 2
        c.code in 0xF900..0xFAFF -> 2
        c.code in 0xFE30..0xFE4F -> 2
        c.code in 0xFF00..0xFF60 -> 2
        c.code in 0xFFE0..0xFFE6 -> 2
        else -> 1
    }
}
