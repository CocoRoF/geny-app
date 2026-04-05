package com.geny.app.core.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formats an ISO 8601 timestamp string into a human-readable relative or absolute time.
 */
fun formatTimestamp(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return ""

    return try {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )

        var date: Date? = null
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                date = sdf.parse(isoTimestamp)
                if (date != null) break
            } catch (_: Exception) {}
        }

        if (date == null) return ""

        val now = System.currentTimeMillis()
        val diff = now - date.time

        when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                timeFormat.format(date)
            }
            diff < 604_800_000 -> {
                val dayFormat = SimpleDateFormat("EEE h:mm a", Locale.getDefault())
                dayFormat.format(date)
            }
            else -> {
                val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                dateFormat.format(date)
            }
        }
    } catch (_: Exception) {
        ""
    }
}
