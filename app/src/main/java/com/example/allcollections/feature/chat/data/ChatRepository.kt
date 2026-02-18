package com.example.allcollections.feature.chat.data

import com.example.allcollections.data.model.ChatMessage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class ChatRepository(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val CHATS_COLLECTION = "chats"
        private const val MESSAGES_COLLECTION = "messages"
    }

    /**
     * Genera un ID univoco per una chat tra due utenti
     */
    private fun generateChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_$userId2"
        } else {
            "${userId2}_$userId1"
        }
    }

    /**
     * Ottiene i messaggi di una chat specifica
     */
    fun getMessages(userId1: String, userId2: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatId = generateChatId(userId1, userId2)

        val listener = firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(messages).isSuccess
            }

        awaitClose { listener.remove() }
    }

    /**
     * Invia un messaggio e aggiorna i metadati della chat
     */
    suspend fun sendMessage(message: ChatMessage) {
        val chatId = generateChatId(message.senderId, message.receiverId)
        val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)

        chatRef.set(
            mapOf(
                "participants" to listOf(message.senderId, message.receiverId),
                "lastMessage" to message.text,
                "timestamp" to message.timestamp,
                "unreadCount_${message.receiverId}" to FieldValue.increment(1)
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()

        chatRef.collection(MESSAGES_COLLECTION).add(message).await()
    }

    /**
     * Azzera il contatore messaggi non letti quando si apre la chat
     */
    suspend fun resetUnreadCount(userId: String, otherUserId: String) {
        val chatId = generateChatId(userId, otherUserId)
        try {
            firestore.collection(CHATS_COLLECTION).document(chatId)
                .update("unreadCount_$userId", 0)
                .await()
        } catch (e: Exception) {
            // Il documento potrebbe non esistere ancora
        }
    }

    /**
     * Segna come letti i messaggi
     */
    suspend fun markMessagesAsRead(userId: String, otherUserId: String) {
        val chatId = generateChatId(userId, otherUserId)

        val snapshot = firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("read", false)
            .get()
            .await()

        if (snapshot.documents.isNotEmpty()) {
            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
        }
    }

    /**
     * Elimina l'intera chat tra due utenti
     */
    suspend fun deleteChat(userId1: String, userId2: String) {
        val chatId = generateChatId(userId1, userId2)

        val messagesSnapshot = firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .get()
            .await()

        if (messagesSnapshot.documents.isNotEmpty()) {
            val batch = firestore.batch()
            messagesSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }

        firestore.collection(CHATS_COLLECTION).document(chatId).delete().await()
    }

    /**
     * Ottiene le ultime chat dell'utente
     */
    fun getRecentChats(userId: String): Flow<List<ChatPreview>> = callbackFlow {
        val listener = firestore.collection(CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: return@mapNotNull null
                    val participants = doc.get("participants") as? List<*> ?: return@mapNotNull null
                    val otherUserId = participants.firstOrNull { it != userId }?.toString() ?: return@mapNotNull null
                    val unreadCount = (doc.getLong("unreadCount_$userId") ?: 0).toInt()

                    ChatPreview(otherUserId, lastMessage, timestamp, unreadCount)
                } ?: emptyList()

                trySend(chats).isSuccess
            }

        awaitClose { listener.remove() }
    }
}

data class ChatPreview(
    val otherUserId: String,
    val lastMessage: String,
    val timestamp: Date,
    val unreadCount: Int
)