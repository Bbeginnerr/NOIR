package app.noir.core

/** Renders scan results as portable JSON and human-readable Markdown. */
object Report {

    fun json(device: Device, apps: List<AppEntry>, recommendations: List<Recommendation> = emptyList()): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"device\": {\"serial\": \"").append(esc(device.serial))
            .append("\", \"model\": \"").append(esc(device.model)).append("\"},\n")
        sb.append("  \"apps\": [\n")
        apps.forEachIndexed { i, a ->
            sb.append("    {\n")
            sb.append("      \"package\": \"").append(esc(a.pkg)).append("\",\n")
            sb.append("      \"uid\": \"").append(esc(a.uid)).append("\",\n")
            sb.append("      \"system\": ").append(a.isSystem).append(",\n")
            sb.append("      \"debuggable\": ").append(a.debuggable).append(",\n")
            sb.append("      \"installer\": ").append(a.installer?.let { "\"${esc(it)}\"" } ?: "null").append(",\n")
            sb.append("      \"category\": \"").append(esc(a.category.label)).append("\",\n")
            sb.append("      \"vendor\": ").append(a.vendor?.let { "\"${esc(it)}\"" } ?: "null").append(",\n")
            sb.append("      \"score\": ").append(a.score).append(",\n")
            sb.append("      \"permissions\": ").append(array(a.permissions)).append(",\n")
            sb.append("      \"endpoints\": ").append(array(a.endpoints)).append("\n")
            sb.append("    }").append(if (i == apps.lastIndex) "" else ",").append("\n")
        }
        sb.append("  ],\n")
        sb.append("  \"recommendations\": [\n")
        recommendations.forEachIndexed { i, r ->
            sb.append("    {\n")
            sb.append("      \"package\": \"").append(esc(r.pkg)).append("\",\n")
            sb.append("      \"action\": \"").append(esc(r.action.label)).append("\",\n")
            sb.append("      \"risk\": \"").append(esc(r.risk)).append("\",\n")
            sb.append("      \"reason\": \"").append(esc(r.reason)).append("\",\n")
            sb.append("      \"autoSafe\": ").append(r.autoSafe).append("\n")
            sb.append("    }").append(if (i == recommendations.lastIndex) "" else ",").append("\n")
        }
        sb.append("  ]\n}\n")
        return sb.toString()
    }

    fun markdown(
        device: Device,
        apps: List<AppEntry>,
        recommendations: List<Recommendation> = emptyList()
    ): String {
        val sb = StringBuilder()
        sb.append("# NOIR report\n\n")
        sb.append("- device: `").append(device.model).append("` (`").append(device.serial).append("`)\n")
        sb.append("- apps flagged: ").append(apps.size).append("\n")
        sb.append("- recommendations: ").append(recommendations.size).append("\n\n")

        sb.append("## Overview\n\n")
        sb.append("| score | package | category | vendor | system | perms | upload | cmd |\n")
        sb.append("|---:|---|---|---|:---:|---:|---:|---:|\n")
        apps.forEach { a ->
            sb.append("| ").append(a.score)
                .append(" | `").append(a.pkg).append("`")
                .append(" | ").append(a.category.label)
                .append(" | ").append(a.vendor ?: "—")
                .append(" | ").append(if (a.isSystem) "yes" else "no")
                .append(" | ").append(a.permissions.size)
                .append(" | ").append(a.uploadEndpoints.size)
                .append(" | ").append(a.commandEndpoints.size)
                .append(" |\n")
        }

        if (recommendations.isNotEmpty()) {
            sb.append("\n## Recommended actions\n\n")
            sb.append("| risk | action | package | why |\n")
            sb.append("|:---:|---|---|---|\n")
            recommendations.forEach { r ->
                sb.append("| ").append(r.risk)
                    .append(" | ").append(r.action.label)
                    .append(" | `").append(r.pkg).append("`")
                    .append(" | ").append(r.reason)
                    .append(" |\n")
            }
        }

        sb.append("\n## Details\n")
        apps.forEach { a ->
            sb.append("\n### ").append(a.pkg).append("  (").append(a.score).append(")\n\n")
            sb.append("- uid: ").append(a.uid)
            sb.append(" · system: ").append(a.isSystem)
            sb.append(" · debuggable: ").append(a.debuggable)
            sb.append(" · installer: ").append(a.installer ?: "unknown")
            sb.append(" · category: ").append(a.category.label)
            sb.append(" · vendor: ").append(a.vendor ?: "unknown").append("\n")
            if (a.permissions.isNotEmpty()) {
                sb.append("- permissions: ").append(a.permissions.joinToString(", ")).append("\n")
            }
            if (a.endpoints.isNotEmpty()) {
                sb.append("- endpoints:\n")
                a.endpoints.take(25).forEach { sb.append("  - `").append(it).append("`\n") }
            }
        }
        return sb.toString()
    }

    /** Domains only, deduplicated — convenient for Pi-hole / AdGuard / DNS blocklists. */
    fun domains(apps: List<AppEntry>): String {
        val hosts = LinkedHashSet<String>()
        apps.forEach { a ->
            a.uploadEndpoints.forEach { url ->
                val host = url.substringAfter("://").substringBefore('/').substringBefore(':').lowercase()
                if (host.isNotBlank() && !host.count { it == '.' }.let { it < 1 }) hosts.add(host)
            }
        }
        return hosts.joinToString("\n")
    }

    private fun array(items: List<String>): String =
        items.joinToString(prefix = "[", postfix = "]") { "\"${esc(it)}\"" }

    private fun esc(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u").append(c.code.toString(16).padStart(4, '0')) else sb.append(c)
            }
        }
        return sb.toString()
    }
}
