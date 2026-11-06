/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core

/**
 * UI and report language.
 *
 * Chinese is the original interface. English is selected from the Android
 * device locale, the host locale, or an explicit `--lang` flag. Reports keep a
 * stable machine-readable key alongside every localized label so a diff can
 * compare a Chinese report with an English one.
 */
enum class Lang {
    ZH,
    EN;

    companion object {
        /** Maps a BCP-47 / POSIX locale tag to a supported language. */
        fun fromTag(tag: String?): Lang? {
            val value = tag?.trim()?.lowercase()?.replace('_', '-') ?: return null
            if (value.isBlank()) return null
            return if (value.startsWith("zh")) ZH else EN
        }
    }
}

fun Category.localized(lang: Lang): String = when (lang) {
    Lang.ZH -> label
    Lang.EN -> when (this) {
        Category.TELEMETRY -> "telemetry"
        Category.ADS -> "ads"
        Category.SECURITY -> "security"
        Category.AI_VOICE -> "AI/voice"
        Category.LOCATION -> "location"
        Category.PUSH -> "push"
        Category.APP_STORE -> "app store"
        Category.INPUT_METHOD -> "input"
        Category.PAYMENT -> "payment"
        Category.FINANCE -> "finance"
        Category.SHOPPING -> "shopping"
        Category.MEDIA -> "media"
        Category.SOCIAL -> "social"
        Category.GAME -> "games"
        Category.TOOL -> "tools"
        Category.CARRIER -> "carrier"
        Category.SYSTEM -> "system"
        Category.OTHER -> "other"
    }
}

fun DataType.localized(lang: Lang): String = when (lang) {
    Lang.ZH -> label
    Lang.EN -> when (this) {
        DataType.LOCATION -> "location"
        DataType.CONTACTS -> "contacts"
        DataType.SMS -> "SMS"
        DataType.CALLS -> "call log"
        DataType.MIC -> "microphone"
        DataType.CAMERA -> "camera"
        DataType.SCREEN -> "screen"
        DataType.APPS -> "app list"
        DataType.IDENTITY -> "device IDs"
        DataType.FILES -> "files/photos"
        DataType.CALENDAR -> "calendar"
        DataType.ACCOUNTS -> "accounts"
    }
}

fun ActionType.localized(lang: Lang): String = when (lang) {
    Lang.ZH -> label
    Lang.EN -> when (this) {
        ActionType.APPOP_PROJECT_MEDIA -> "revoke capture"
        ActionType.APPOP_USAGE_STATS -> "block usage"
        ActionType.UNINSTALL_USER -> "uninstall user"
        ActionType.REVIEW -> "manual review"
        ActionType.BLOCK_DNS -> "block DNS"
    }
}
