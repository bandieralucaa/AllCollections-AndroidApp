package com.example.allcollections.core.utils.time

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ONE_MINUTE_MS = 60_000L
private const val ONE_HOUR_MS = 3_600_000L
private const val ONE_DAY_MS = 86_400_000L

/**
 * Formatta una [Date] in una stringa relativa al momento attuale.
 *
 * | Differenza          | Output esempio   |
 * |---------------------|------------------|
 * | < 1 minuto          | `"ora"`          |
 * | < 1 ora             | `"5m"`           |
 * | < 1 giorno          | `"3h"`           |
 * | >= 1 giorno         | `"14/06/25"`     |
 *
 * @param date La data da formattare.
 * @return Stringa relativa leggibile, localizzata con [Locale.getDefault].
 */
fun formatRelativeTime(date: Date): String {
    val diff = Date().time - date.time

    return when {
        diff < ONE_MINUTE_MS -> "ora"
        diff < ONE_HOUR_MS -> "${diff / ONE_MINUTE_MS}m"
        diff < ONE_DAY_MS -> "${diff / ONE_HOUR_MS}h"
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
    }
}
