package com.example.allcollections.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRelativeTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "meno di un minuto fa"
        minutes < 60 -> "$minutes minuti fa"
        hours < 24 -> "$hours ore fa"
        days == 1L -> "ieri"
        days < 7 -> "$days giorni fa"
        else -> SimpleDateFormat("dd MMM yyyy", Locale("it", "IT")).format(date)
    }
}