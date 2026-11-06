/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core

enum class ActionType(val label: String) {
    APPOP_PROJECT_MEDIA("撤销常驻录屏授权"),
    APPOP_USAGE_STATS("禁止读取使用统计"),
    UNINSTALL_USER("对当前用户卸载"),
    REVIEW("人工复核"),
    BLOCK_DNS("DNS 拦截")
}

data class Recommendation(
    val pkg: String,
    val action: ActionType,
    val risk: String,
    val reason: String,
    val autoSafe: Boolean
)

/**
 * Turns findings into a small, ordered set of actions.
 *
 * Deliberate restraints:
 *  - declaring screen-capture capability is not treated as an action: the
 *    PROJECT_MEDIA app-op only controls a permanent grant, not recording itself;
 *  - NOIR never proposes uninstalling a user app. Those become REVIEW, because
 *    "WeChat uploads a lot" is not a reason to uninstall WeChat.
 */
object Advisor {

    private val CORE = setOf(
        "com.miui.securitycenter",
        "com.miui.guardprovider",
        "com.xiaomi.xmsf",
        "com.xiaomi.digitalkey",
        "com.android.systemui",
        "com.miui.home",
        "com.android.settings",
        "com.android.phone",
        "com.android.mms",
        "com.android.incallui"
    )

    fun advise(apps: List<AppEntry>): List<Recommendation> {
        val out = ArrayList<Recommendation>()

        apps.forEach { app ->
            if (app.pkg == "android") return@forEach

            if ("PACKAGE_USAGE_STATS" in app.permissions &&
                app.category == Category.TELEMETRY &&
                app.isSystem
            ) {
                out += Recommendation(
                    app.pkg,
                    ActionType.APPOP_USAGE_STATS,
                    "MED",
                    "遥测类系统应用可读取应用使用统计",
                    autoSafe = true
                )
            }

            if ((app.category == Category.TELEMETRY || app.category == Category.ADS) && app.pkg !in CORE) {
                out += Recommendation(
                    app.pkg,
                    ActionType.REVIEW,
                    if (app.isSystem) "HIGH" else "MED",
                    if (app.isSystem) "遥测/广告类系统组件，无法卸载，建议 DNS 层拦截" else "遥测/广告密度高，评估后决定是否保留",
                    autoSafe = false
                )
            }

            if (app.uploadEndpoints.size >= 2 &&
                app.category in setOf(Category.TELEMETRY, Category.LOCATION, Category.ADS)
            ) {
                out += Recommendation(
                    app.pkg,
                    ActionType.BLOCK_DNS,
                    "LOW",
                    "存在多个上报端点，可在 DNS 层拦截",
                    autoSafe = false
                )
            }
        }

        return out
            .distinctBy { it.pkg to it.action }
            .sortedWith(compareBy({ riskRank(it.risk) }, { it.action.ordinal }))
    }

    private fun riskRank(risk: String): Int = when (risk) {
        "HIGH" -> 0
        "MED" -> 1
        else -> 2
    }
}
