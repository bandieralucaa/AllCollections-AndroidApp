package com.example.allcollections.core.utils.time

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Formato di default per date lontane (>7 giorni)
private val DEFAULT_DATE_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.ITALY)

/**
 * Restituisce una rappresentazione testuale "relativa" di una data rispetto all'ora corrente.
 *
 * Esempi di output:
 * - "meno di un minuto fa"
 * - "5 minuti fa"
 * - "ieri"
 * - "3 giorni fa"
 * - "12 ago 2023" (per date più vecchie di una settimana)
 *
 * @param date Data da formattare
 * @return Stringa leggibile in italiano
 */
fun formatRelativeTime(date: Date): String {
    val now = Date()
    val diffMillis = now.time - date.time

    val seconds = diffMillis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "meno di un minuto fa"
        minutes < 60 -> "$minutes minuti fa"
        hours < 24 -> "$hours ore fa"
        days == 1L -> "ieri"
        days < 7 -> "$days giorni fa"
        else -> DEFAULT_DATE_FORMAT.format(date)
    }
}
