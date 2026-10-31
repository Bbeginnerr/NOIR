/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core

enum class Category(val label: String) {
    TELEMETRY("遥测/统计"),
    ADS("广告"),
    SECURITY("安全/杀毒"),
    AI_VOICE("AI/语音"),
    LOCATION("定位/地图"),
    PUSH("推送"),
    APP_STORE("应用商店"),
    INPUT_METHOD("输入法"),
    PAYMENT("支付"),
    FINANCE("金融"),
    SHOPPING("购物"),
    MEDIA("影音"),
    SOCIAL("社交"),
    GAME("游戏"),
    TOOL("工具"),
    CARRIER("运营商"),
    SYSTEM("系统"),
    OTHER("其他")
}

/**
 * Cheap, explainable classification: package name + endpoint domains.
 *
 * Deliberately conservative: keyword matching uses token boundaries so that
 * `statementservice` is not mistaken for telemetry. System apps fall back to
 * SYSTEM rather than OTHER, which keeps the exposure chart readable.
 */
object Classifier {

    private val TELEMETRY = Regex(
        "(^|[./_-])(analytics|track|tracker|tracking|stat|stats|beacon|telemetry|misight|onetrack|logging)([./_-]|$)"
    )
    private val ADS = Regex(
        "(^|[./_-])(ads?|advert|advertising|doubleclick|pangle|sigmob)([./_-]|$)"
    )

    private val KNOWN: Map<String, Category> = mapOf(
        "com.miui.analytics" to Category.TELEMETRY,
        "com.miui.misightservice" to Category.TELEMETRY,
        "com.xiaomi.security.onetrack" to Category.TELEMETRY,
        "com.miui.powerinsight" to Category.TELEMETRY,
        "com.miui.securitycenter" to Category.SECURITY,
        "com.miui.securitycore" to Category.SECURITY,
        "com.miui.securityadd" to Category.SECURITY,
        "com.miui.securitymanager" to Category.SECURITY,
        "com.miui.guardprovider" to Category.SECURITY,
        "com.bsp.catchlog" to Category.SECURITY,
        "com.miui.cleanmaster" to Category.SECURITY,
        "com.miui.systemadsolution" to Category.ADS,
        "com.xiaomi.market" to Category.APP_STORE,
        "com.android.vending" to Category.APP_STORE,
        "com.mobiletools.systemhelper" to Category.CARRIER,
        "com.xiaomi.xmsf" to Category.PUSH,
        "com.xiaomi.digitalkey" to Category.SYSTEM,
        "com.miui.screenrecorder" to Category.SYSTEM,
        "com.android.systemui" to Category.SYSTEM,
        "com.tencent.mm" to Category.SOCIAL,
        "com.tencent.mobileqq" to Category.SOCIAL,
        "com.eg.android.alipaygphone" to Category.FINANCE,
        "com.taobao.taobao" to Category.SHOPPING,
        "com.jingdong.app.mall" to Category.SHOPPING,
        "com.xunmeng.pinduoduo" to Category.SHOPPING,
        "tv.danmaku.bili" to Category.MEDIA,
        "com.google.android.youtube" to Category.MEDIA
    )

    private val VENDORS: List<Pair<String, List<String>>> = listOf(
        "Xiaomi" to listOf("xiaomi", "miui", "mi.com", "mijia", "miui.com"),
        "Google" to listOf("google", "gstatic", "android.com", "doubleclick"),
        "Tencent" to listOf("qq.com", "tencent", "weixin", "wechat"),
        "Alibaba" to listOf("alibaba", "aliyun", "taobao", "tmall", "amap.com", "ucweb"),
        "Baidu" to listOf("baidu", "bdstatic"),
        "ByteDance" to listOf("bytedance", "zijieapi", "toutiao", "pangle"),
        "Meta" to listOf("facebook", "instagram", "fb.com"),
        "Sogou" to listOf("sogou"),
        "iFlytek" to listOf("iflytek", "voicecloud"),
        "UnionPay" to listOf("unionpay", "95516"),
        "China Unicom" to listOf("wo.com.cn", "10010"),
        "Amazon" to listOf("amazonaws", "amazon-adsystem"),
        "Umeng" to listOf("umeng"),
        "Firebase" to listOf("firebase", "crashlytics"),
        "Mihoyo" to listOf("mihoyo"),
        "NetEase" to listOf("netease", "163.com")
    )

    fun vendor(pkg: String, endpoints: List<String>): String? {
        val haystack = (pkg + " " + endpoints.joinToString(" ")).lowercase()
        return VENDORS.firstOrNull { (_, keys) -> keys.any { haystack.contains(it) } }?.first
    }

    fun category(
        pkg: String,
        endpoints: List<String>,
        permissions: List<String>,
        isSystem: Boolean = false
    ): Category {
        KNOWN[pkg.lowercase()]?.let { return it }

        val name = pkg.lowercase()
        val urls = endpoints.joinToString(" ").lowercase()
        val both = "$name $urls"

        fun nameHas(vararg keys: String) = keys.any { name.contains(it) }
        fun urlHas(vararg keys: String) = keys.any { urls.contains(it) }

        val matched = when {
            nameHas("dm.wo.com.cn", ".simactivate", "carrier") || urlHas("dm.wo.com.cn") -> Category.CARRIER
            nameHas("inputmethod", ".ime", "sogou", "iflytek") -> Category.INPUT_METHOD
            nameHas("market", "vending", "appstore") -> Category.APP_STORE
            nameHas("pay", "wallet", "unionpay", "mipay", ".bank") -> Category.FINANCE
            nameHas("taobao", "tmall", "jingdong", "pinduoduo", "idlefish", "meituan", "dianping", ".shop") -> Category.SHOPPING
            nameHas("bili", "youtube", "netflix", "spotify", "music", "video", "douyin", "kuaishou", "iqiyi", "youku") -> Category.MEDIA
            nameHas("browser", "chrome", "fileexplorer", "translator", "wps", "notes", "calculator", "recorder", "camera") -> Category.TOOL
            nameHas("migame", "gamecenter", "minigame", "roblox", "mihoyo", "tmgp", "game") -> Category.GAME
            nameHas("tencent.mm", "whatsapp", "instagram", "telegram", "twitter", "facebook") -> Category.SOCIAL
            TELEMETRY.containsMatchIn(both) -> Category.TELEMETRY
            ADS.containsMatchIn(both) -> Category.ADS
            nameHas("security", "guard", "antivirus", "virus", "clean", "catchlog") -> Category.SECURITY
            nameHas("voice", "asr", "speech", "xiaoai", "xiaomixiaoai", "ai.engine", "aiasst") -> Category.AI_VOICE
            nameHas("location", "lbs", "gps", "map", "amap", "metoknlp") -> Category.LOCATION
            nameHas("push", "xmsf", "firebase", "umeng", "getui", "jpush") -> Category.PUSH
            nameHas("systemui", ".settings", ".home", "launcher") -> Category.SYSTEM
            else -> null
        }

        return matched ?: if (isSystem) Category.SYSTEM else Category.OTHER
    }
}
