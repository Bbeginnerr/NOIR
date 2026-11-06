/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.cli

import app.noir.core.Category
import app.noir.core.Lang
import app.noir.core.localized
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

internal data class ReportApp(
    val pkg: String,
    val score: Int,
    val category: String,
    val permissions: List<String>,
    val endpoints: List<String>,
    val categoryKey: String? = null
)

internal data class ReportSnapshot(
    val path: String,
    val device: String,
    val tool: String,
    val generated: String,
    val apps: Map<String, ReportApp>
)

internal data class AppChange(val old: ReportApp, val new: ReportApp) {
    val pkg: String get() = new.pkg
    val scoreDelta: Int get() = new.score - old.score
    val addedPermissions: List<String> get() = new.permissions - old.permissions.toSet()
    val removedPermissions: List<String> get() = old.permissions - new.permissions.toSet()
    val addedEndpoints: List<String> get() = new.endpoints - old.endpoints.toSet()
    val removedEndpoints: List<String> get() = old.endpoints - new.endpoints.toSet()
    val changed: Boolean
        get() = scoreDelta != 0 || !sameCategory ||
            addedPermissions.isNotEmpty() || removedPermissions.isNotEmpty() ||
            addedEndpoints.isNotEmpty() || removedEndpoints.isNotEmpty()

    /** Localized labels may differ between a Chinese and an English report. */
    private val sameCategory: Boolean
        get() {
            val oldKey = stableCategory(old)
            val newKey = stableCategory(new)
            return if (oldKey != null && newKey != null) oldKey == newKey else old.category == new.category
        }
}

internal data class DiffResult(
    val old: ReportSnapshot,
    val new: ReportSnapshot,
    val added: List<ReportApp>,
    val removed: List<ReportApp>,
    val changed: List<AppChange>,
    val unchanged: Int
) {
    val regressions: Boolean
        get() = added.isNotEmpty() || changed.any {
            it.scoreDelta > 0 || it.addedPermissions.isNotEmpty() || it.addedEndpoints.isNotEmpty()
        }
}

/** Old reports have no categoryKey; map their label back to the stable enum. */
private fun stableCategory(app: ReportApp): String? =
    app.categoryKey ?: Category.entries.firstOrNull {
        it.localized(Lang.ZH) == app.category || it.localized(Lang.EN).equals(app.category, ignoreCase = true)
    }?.name

private fun JsonObject.string(key: String, fallback: String = ""): String =
    get(key)?.takeIf { !it.isJsonNull }?.asString ?: fallback

private fun JsonObject.number(key: String, fallback: Int = 0): Int =
    get(key)?.takeIf { !it.isJsonNull }?.asInt ?: fallback

private fun JsonObject.stringList(key: String): List<String> {
    val value = get(key) ?: return emptyList()
    if (!value.isJsonArray) return emptyList()
    return value.asJsonArray.mapNotNull { it.takeIf { v -> v.isJsonPrimitive }?.asString }
}

internal fun readReport(path: String): ReportSnapshot {
    val file = File(path)
    require(file.isFile) { "report not found: ${file.path}" }
    val root = JsonParser.parseString(file.readText()).asJsonObject
    val env = root.getAsJsonObject("environment") ?: JsonObject()
    val device = root.getAsJsonObject("device") ?: JsonObject()
    val apps = root.getAsJsonArray("apps") ?: JsonArray()

    val parsed = apps.map { element ->
        val app = element.asJsonObject
        ReportApp(
            pkg = app.string("package"),
            score = app.number("score"),
            category = app.string("category", "—"),
            permissions = app.stringList("permissions").sorted(),
            endpoints = app.stringList("endpoints").sorted(),
            categoryKey = app.string("categoryKey").ifBlank { null }
        )
    }.filter { it.pkg.isNotBlank() }.associateBy { it.pkg }

    val model = device.string("model")
    val serial = device.string("serial")
    val deviceLabel = when {
        model.isNotBlank() && serial.isNotBlank() -> "$model ($serial)"
        model.isNotBlank() -> model
        serial.isNotBlank() -> serial
        else -> "unknown device"
    }

    return ReportSnapshot(
        path = file.path,
        device = deviceLabel,
        tool = env.string("tool", "unknown"),
        generated = env.string("generatedAt"),
        apps = parsed
    )
}

internal fun diffReports(old: ReportSnapshot, new: ReportSnapshot): DiffResult {
    val added = new.apps.values
        .filter { it.pkg !in old.apps }
        .sortedWith(compareByDescending<ReportApp> { it.score }.thenBy { it.pkg })
    val removed = old.apps.values
        .filter { it.pkg !in new.apps }
        .sortedWith(compareByDescending<ReportApp> { it.score }.thenBy { it.pkg })
    val changes = new.apps.keys
        .filter { it in old.apps }
        .map { AppChange(old.apps.getValue(it), new.apps.getValue(it)) }
        .filter { it.changed }
        .sortedWith(
            compareByDescending<AppChange> { it.scoreDelta }
                .thenByDescending { it.addedEndpoints.size + it.addedPermissions.size }
                .thenBy { it.pkg }
        )
    val unchanged = new.apps.keys.count { it in old.apps } - changes.size
    return DiffResult(old, new, added, removed, changes, unchanged)
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

internal fun renderDiff(result: DiffResult): String = buildString {
    appendLine("NOIR diff")
    appendLine("old: ${result.old.path} · ${result.old.device} · ${result.old.generated.ifBlank { "unknown" }}")
    appendLine("new: ${result.new.path} · ${result.new.device} · ${result.new.generated.ifBlank { "unknown" }}")
    appendLine("apps: ${result.old.apps.size} -> ${result.new.apps.size}")
    appendLine("regression: ${if (result.regressions) "yes" else "no"}")
    appendLine()

    appendLine("[+] added (${result.added.size})")
    result.added.take(40).forEach { app ->
        appendLine("  + ${app.pkg}  score ${app.score}  ${app.category}")
    }
    if (result.added.size > 40) appendLine("  … ${result.added.size - 40} more")
    appendLine()

    appendLine("[-] removed (${result.removed.size})")
    result.removed.take(40).forEach { app ->
        appendLine("  - ${app.pkg}  score ${app.score}  ${app.category}")
    }
    if (result.removed.size > 40) appendLine("  … ${result.removed.size - 40} more")
    appendLine()

    appendLine("[~] changed (${result.changed.size})")
    result.changed.take(30).forEach { change ->
        appendLine(
            "  ~ ${change.pkg}  ${change.old.score} -> ${change.new.score} (${signed(change.scoreDelta)})  ${change.new.category}"
        )
        change.addedPermissions.forEach { appendLine("      + permission $it") }
        change.removedPermissions.forEach { appendLine("      - permission $it") }
        change.addedEndpoints.forEach { appendLine("      + endpoint $it") }
        change.removedEndpoints.forEach { appendLine("      - endpoint $it") }
    }
    if (result.changed.size > 30) appendLine("  … ${result.changed.size - 30} more")
    appendLine()
    appendLine("[=] unchanged (${result.unchanged})")
}

private fun snapshotJson(snapshot: ReportSnapshot): JsonObject = JsonObject().apply {
    addProperty("path", snapshot.path)
    addProperty("device", snapshot.device)
    addProperty("tool", snapshot.tool)
    addProperty("generatedAt", snapshot.generated)
    addProperty("apps", snapshot.apps.size)
}

private fun appJson(app: ReportApp): JsonObject = JsonObject().apply {
    addProperty("package", app.pkg)
    addProperty("score", app.score)
    addProperty("category", app.category)
    app.categoryKey?.let { addProperty("categoryKey", it) }
    add("permissions", JsonArray().apply { app.permissions.forEach { add(it) } })
    add("endpoints", JsonArray().apply { app.endpoints.forEach { add(it) } })
}

internal fun renderDiffJson(result: DiffResult): String {
    val root = JsonObject()
    root.add("old", snapshotJson(result.old))
    root.add("new", snapshotJson(result.new))
    root.addProperty("regression", result.regressions)
    root.addProperty("unchanged", result.unchanged)

    fun list(items: List<ReportApp>): JsonArray = JsonArray().apply {
        items.forEach { add(appJson(it)) }
    }
    root.add("added", list(result.added))
    root.add("removed", list(result.removed))
    root.add("changed", JsonArray().apply {
        result.changed.forEach { change ->
            add(JsonObject().apply {
                addProperty("package", change.pkg)
                change.old.categoryKey?.let { addProperty("oldCategoryKey", it) }
                change.new.categoryKey?.let { addProperty("newCategoryKey", it) }
                addProperty("oldScore", change.old.score)
                addProperty("newScore", change.new.score)
                addProperty("delta", change.scoreDelta)
                add("addedPermissions", JsonArray().apply { change.addedPermissions.forEach { add(it) } })
                add("removedPermissions", JsonArray().apply { change.removedPermissions.forEach { add(it) } })
                add("addedEndpoints", JsonArray().apply { change.addedEndpoints.forEach { add(it) } })
                add("removedEndpoints", JsonArray().apply { change.removedEndpoints.forEach { add(it) } })
            })
        }
    })
    return GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root)
}
