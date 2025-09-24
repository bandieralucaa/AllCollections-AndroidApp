package com.example.allcollections.notification

import com.example.allcollections.profile.UserData
import java.util.Date

data class NotificationItem(
    val user: UserData,
    val timestamp: Date,
    val read: Boolean,
    val notificationId: String,
    val type: String,
    val collectionId: String? = null,
    val collectionName: String? = null,
    val commentText: String? = null
)
