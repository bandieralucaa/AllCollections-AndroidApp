package com.example.allcollections.notification

import com.example.allcollections.profile.UserData

data class NotificationItem(
    val user: UserData,
    val timestamp: String,
    val read: Boolean,
    val notificationId: String
)
