package com.example.allcollections.core.utils.time

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Costanti per i millisecondi nelle unità di tempo
private const val ONE_MINUTE_MS = 60_000L
private const val ONE_HOUR_MS = 3_600_000L
private const val ONE_DAY_MS = 86_400_000L

/**
 * Formatta una data in una stringa relativa al momento attuale.
 *
 * Utile per visualizzare timestamp in chat, commenti, notifiche, ecc.
 * La funzione è localizzata usando [Locale.getDefault] per il formato data.
 *
 * Regole di formattazione:
 * | Differenza (data nel passato) | Output esempio | Note |
 * |-------------------------------|----------------|------|
 * | < 1 minuto                    | "ora"          | Significa "appena ora" |
 * | 1-59 minuti                   | "5m"           | Minuti, arrotondato per difetto |
 * | 1-23 ore                      | "3h"           | Ore, arrotondato per difetto |
 * | >= 1 giorno                   | "14/06/25"     | Formato gg/MM/aa |
 *
 * **Attenzione:** Se la data è nel futuro, la funzione restituisce `"nel futuro"`.
 * Questo evita di mostrare differenze negative (es. "-5m").
 *
 * @param date La data da formattare (può essere passata o futura).
 * @return Stringa relativa localizzata, o `"nel futuro"` se la data è successiva a ora.
 *
 * @sample
 * val date = Date(System.currentTimeMillis() - 5 * ONE_MINUTE_MS)
 * println(formatRelativeTime(date)) // "5m"
 */
fun formatRelativeTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time

    // Se la data è nel futuro, restituisci un indicatore
    if (diff < 0) {
        return "nel futuro"
    }

    return when {
        diff < ONE_MINUTE_MS -> "ora"
        diff < ONE_HOUR_MS -> "${diff / ONE_MINUTE_MS}m"
        diff < ONE_DAY_MS -> "${diff / ONE_HOUR_MS}h"
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
    }
}