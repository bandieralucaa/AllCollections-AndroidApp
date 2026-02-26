package com.example.allcollections.data.model
import java.util.Date


data class ChatPreview(
    val otherUserId: String,
    val lastMessage: String,
    val timestamp: Date,
    val unreadCount: Int
)