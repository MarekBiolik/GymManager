package com.gymmanager.core.data.supabase

/**
 * Minimal ISO-8601 / Postgres timestamptz ↔ epoch millis helpers for hello-sync DTOs.
 * Accepts trailing Z or numeric offset; falls back to 0 on unparseable values.
 */
internal fun parseTimestamptzToEpochMs(value: String): Long {
    // kotlinx-datetime Instant.parse accepts ISO-8601 with Z or offset.
    return try {
        kotlinx.datetime.Instant.parse(normalizeTimestamptz(value)).toEpochMilliseconds()
    } catch (_: Exception) {
        0L
    }
}

internal fun epochMsToTimestamptz(epochMs: Long): String =
    kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs).toString()

private fun normalizeTimestamptz(value: String): String {
    val trimmed = value.trim()
    // Postgres often returns "2024-01-01 12:00:00+00" — Instant wants T separator.
    return if (trimmed.length > 10 && trimmed[10] == ' ') {
        trimmed.replaceFirst(' ', 'T')
    } else {
        trimmed
    }
}
