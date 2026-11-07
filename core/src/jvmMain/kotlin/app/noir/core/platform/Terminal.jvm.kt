/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core.platform

actual fun terminalColumns(): Int =
    System.getenv("COLUMNS")?.toIntOrNull()?.takeIf { it in 40..400 } ?: 80
