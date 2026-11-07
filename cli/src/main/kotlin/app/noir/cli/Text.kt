/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.cli

import app.noir.core.Device
import app.noir.core.Lang
import app.noir.core.adb.Adb
import java.util.Locale

/**
 * Current interface language.
 *
 * Starts from the host locale, then switches to the Android device locale as
 * soon as a device is available. An explicit `--lang` or `NOIR_LANG` pins it.
 */
internal var LANG: Lang = Lang.ZH

/** True when the user pinned a language; the device must not override it. */
internal var LANG_LOCKED: Boolean = false

/** Ask renderers for plain ASCII borders, bars and ellipses. */
internal var ASCII: Boolean = false

/** Picks the Chinese or English string for the current interface. */
internal fun t(zh: String, en: String): String = if (LANG == Lang.EN) en else zh

internal fun initLanguage(args: List<String>) {
    val requested = rawOption(args, "--lang") ?: System.getenv("NOIR_LANG")
    val value = requested?.trim()?.lowercase()?.replace('_', '-')
    when {
        value.isNullOrEmpty() || value == "auto" -> {
            LANG = detectHostLanguage()
            LANG_LOCKED = false
        }
        value.startsWith("zh") -> {
            LANG = Lang.ZH
            LANG_LOCKED = true
        }
        value.startsWith("en") -> {
            LANG = Lang.EN
            LANG_LOCKED = true
        }
        else -> {
            // Any other explicit locale gets the English interface.
            LANG = Lang.fromTag(value) ?: detectHostLanguage()
            LANG_LOCKED = true
        }
    }
}

internal fun detectHostLanguage(
    env: Map<String, String> = System.getenv(),
    locale: Locale = Locale.getDefault()
): Lang {
    val tag = listOf("LC_ALL", "LC_MESSAGES", "LANG")
        .asSequence()
        .mapNotNull { env[it]?.trim() }
        .firstOrNull { value ->
            value.isNotEmpty() &&
                !value.equals("C", ignoreCase = true) &&
                !value.startsWith("C.", ignoreCase = true) &&
                !value.equals("POSIX", ignoreCase = true)
        }
        ?: locale.toLanguageTag()
    return Lang.fromTag(tag) ?: Lang.ZH
}

/**
 * Reads the Android UI locale. `persist.sys.locale` is the current setting;
 * the product properties are a fallback on older builds.
 */
internal fun detectDeviceLanguage(adb: Adb, device: Device): Lang? {
    val output = adb.shell(
        device.serial,
        "getprop persist.sys.locale; " +
            "getprop ro.product.locale; " +
            "getprop ro.product.locale.language; " +
            "settings get system system_locales",
        timeoutSec = 15
    ).output
    val localeTag = Regex("^[a-zA-Z]{2,3}([-_][a-zA-Z0-9]+)*$")
    val tag = output.lineSequence()
        .flatMap { it.split(',').asSequence() }
        .map { it.trim().trim('[', ']') }
        .firstOrNull { it.isNotEmpty() && !it.equals("null", ignoreCase = true) && localeTag.matches(it) }
        ?: return null
    return Lang.fromTag(tag)
}

/** Switches to the device language unless the user pinned one. */
internal fun applyDeviceLanguage(adb: Adb, device: Device) {
    if (LANG_LOCKED) return
    detectDeviceLanguage(adb, device)?.let { LANG = it }
}

private fun rawOption(args: List<String>, name: String): String? {
    args.forEachIndexed { index, arg ->
        if (arg == name && index + 1 < args.size && !args[index + 1].startsWith("--")) {
            return args[index + 1]
        }
        if (arg.startsWith("$name=")) return arg.substringAfter('=')
    }
    return null
}
