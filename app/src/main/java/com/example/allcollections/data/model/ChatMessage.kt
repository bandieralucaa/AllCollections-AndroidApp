package com.example.allcollections.data.model

import com.google.firebase.Timestamp

/**
 * Modello dati per un messaggio chat 1-1
 */
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false
) {
    companion object {
        fun create(
            senderId: String,
            receiverId: String,
            text: String
        ) = ChatMessage(
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            timestamp = Timestamp.now(),
            read = false
        )
    }
}