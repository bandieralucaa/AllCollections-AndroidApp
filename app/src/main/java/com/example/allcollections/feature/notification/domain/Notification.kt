package com.example.allcollections.feature.notification.domain

import com.example.allcollections.data.model.UserData
import java.util.Date

data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val timestamp: Date = Date(),
    val read: Boolean = false,
    val data: NotificationData = NotificationData(),
    val sender: UserData? = null
) {
    val isSystem: Boolean get() = senderId == "system"

    val formattedTime: String
        get() = formatRelativeTime(timestamp)

    fun markAsRead() = copy(read = true)
}

data class NotificationData(
    val collectionId: String? = null,
    val collectionName: String? = null,
    val itemId: String? = null,
    val itemDescription: String? = null,
    val commentText: String? = null,
    val pushTitle: String? = null,
    val pushMessage: String? = null
)

fun formatRelativeTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time

    return when {
        diff < 60000 -> "ora"
        diff < 3600000 -> "${diff / 60000} minuti fa"
        diff < 86400000 -> "${diff / 3600000} ore fa"
        else -> java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date)
    }
}