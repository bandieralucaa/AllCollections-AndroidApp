package com.example.allcollections.feature.chat.data

import com.example.allcollections.data.model.ChatMessage
import com.google.firebase.Timestamp
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

    private fun generateChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_$userId2" else "${userId2}_$userId1"
    }

    fun getMessages(userId1: String, userId2: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatId = generateChatId(userId1, userId2)
        val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)

        val listener = chatRef
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()
                    return@addSnapshotListener
                }

                // Leggi il timestamp di eliminazione per userId1
                firestore.collection(CHATS_COLLECTION).document(chatId).get()
                    .addOnSuccessListener { doc ->
                        val deletedAt = doc.getTimestamp("deletedAt_$userId1")

                        val messages = snapshot?.documents?.mapNotNull { msgDoc ->
                            val message = msgDoc.toObject(ChatMessage::class.java)?.copy(id = msgDoc.id)
                                ?: return@mapNotNull null

                            // Filtra i messaggi precedenti all'eliminazione
                            if (deletedAt != null && message.timestamp <= deletedAt) {
                                return@mapNotNull null
                            }

                            message
                        } ?: emptyList()

                        trySend(messages).isSuccess
                    }
            }

        awaitClose { listener.remove() }
    }

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

        // Rimuove solo il receiver da deletedFor così la chat riappare nella sua lista,
        // ma lascia intatto deletedAt_receiver così i vecchi messaggi restano nascosti
        chatRef.update(
            mapOf(
                "deletedFor" to FieldValue.arrayRemove(message.receiverId)
            )
        ).await()

        chatRef.collection(MESSAGES_COLLECTION).add(message).await()
    }

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

    suspend fun deleteChat(userId1: String, userId2: String) {
        val chatId = generateChatId(userId1, userId2)
        val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)

        val doc = chatRef.get().await()
        val deletedFor = doc.get("deletedFor") as? List<*> ?: emptyList<String>()

        if (deletedFor.contains(userId2)) {
            // L'altro utente ha già eliminato → elimina davvero tutto
            val messagesSnapshot = chatRef.collection(MESSAGES_COLLECTION).get().await()
            if (messagesSnapshot.documents.isNotEmpty()) {
                val batch = firestore.batch()
                messagesSnapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            chatRef.delete().await()
        } else {
            // Solo io elimino → aggiungo il mio ID a deletedFor e salvo il timestamp
            chatRef.update(
                mapOf(
                    "deletedFor" to FieldValue.arrayUnion(userId1),
                    "deletedAt_$userId1" to Timestamp.now()
                )
            ).await()
        }
    }

    fun getRecentChats(userId: String): Flow<List<ChatPreview>> = callbackFlow {
        val listener = firestore.collection(CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    // Filtra le chat eliminate dal lato dell'utente
                    val deletedFor = doc.get("deletedFor") as? List<*> ?: emptyList<Any>()
                    if (deletedFor.contains(userId)) return@mapNotNull null

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