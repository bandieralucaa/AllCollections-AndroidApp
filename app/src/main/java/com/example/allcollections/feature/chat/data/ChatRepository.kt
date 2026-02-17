package com.example.allcollections.feature.chat.data

import com.example.allcollections.data.model.ChatMessage
import com.google.firebase.Timestamp
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
     * Invia un messaggio
     */
    suspend fun sendMessage(message: ChatMessage) {
        val chatId = generateChatId(message.senderId, message.receiverId)

        firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .add(message)
            .await()
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

        // Prima elimina tutti i messaggi nella subcollection
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

        // Poi elimina il documento principale della chat (opzionale)
        // firestore.collection(CHATS_COLLECTION).document(chatId).delete().await()
    }

    /**
     * Ottiene le ultime chat dell'utente
     */
    fun getRecentChats(userId: String): Flow<List<ChatPreview>> = callbackFlow {
        // Query per messaggi RICEVUTI
        val receivedListener = firestore.collectionGroup(MESSAGES_COLLECTION)
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { receivedSnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // Query per messaggi INVIATI
                firestore.collectionGroup(MESSAGES_COLLECTION)
                    .whereEqualTo("senderId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { sentSnapshot ->

                        val chatMap = mutableMapOf<String, ChatPreview>()

                        // Processa messaggi RICEVUTI
                        receivedSnapshot?.documents?.forEach { doc ->
                            val message = doc.toObject(ChatMessage::class.java)
                            message?.let {
                                val otherUserId = it.senderId
                                chatMap[otherUserId] = ChatPreview(
                                    otherUserId = otherUserId,
                                    lastMessage = it.text,
                                    timestamp = it.timestamp.toDate(),
                                    unreadCount = (chatMap[otherUserId]?.unreadCount ?: 0) + if (!it.read) 1 else 0
                                )
                            }
                        }

                        // Processa messaggi INVIATI
                        sentSnapshot.documents.forEach { doc ->
                            val message = doc.toObject(ChatMessage::class.java)
                            message?.let {
                                val otherUserId = it.receiverId

                                if (!chatMap.containsKey(otherUserId)) {
                                    chatMap[otherUserId] = ChatPreview(
                                        otherUserId = otherUserId,
                                        lastMessage = it.text,
                                        timestamp = it.timestamp.toDate(),
                                        unreadCount = 0
                                    )
                                } else {
                                    val existing = chatMap[otherUserId]!!
                                    if (it.timestamp.toDate() > existing.timestamp) {
                                        chatMap[otherUserId] = existing.copy(
                                            lastMessage = it.text,
                                            timestamp = it.timestamp.toDate()
                                        )
                                    }
                                }
                            }
                        }

                        val result = chatMap.values.sortedByDescending { it.timestamp }
                        trySend(result).isSuccess
                    }
            }

        awaitClose { receivedListener.remove() }
    }
}

data class ChatPreview(
    val otherUserId: String,
    val lastMessage: String,
    val timestamp: Date,
    val unreadCount: Int
)