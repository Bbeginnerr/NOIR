/*
 * NOIR — Android leak audit
 * Copyright (c) 2026 Verlintas
 * SPDX-License-Identifier: MIT
 */
package app.noir.core.platform

/** Columns available to the text UI; actual implementations may query the OS. */
expect fun terminalColumns(): Int
