package com.example.allcollections.feature.chat.data

import com.example.allcollections.data.model.ChatMessage
import com.example.allcollections.data.model.ChatPreview
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository per la gestione delle chat in tempo reale su Firestore.
 *
 * Ogni conversazione è identificata da un ID deterministico generato dai due
 * userId ordinati alfabeticamente (`userId1_userId2`), garantendo unicità
 * indipendentemente dall'ordine in cui i due utenti avviano la chat.
 *
 * Supporta eliminazione logica per singolo utente: quando un utente elimina
 * la chat, essa viene nascosta solo per lui (campo `deletedFor`); la chat viene
 * fisicamente eliminata solo quando entrambi gli utenti l'hanno eliminata.
 *
 * @param firestore Istanza di FirebaseFirestore iniettata tramite Koin.
 */
class ChatRepository(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val CHATS_COLLECTION = "chats"
        private const val MESSAGES_COLLECTION = "messages"
    }

    /**
     * Genera un ID chat deterministico dai due userId.
     *
     * Ordina i due ID alfabeticamente e li unisce con `_`, così che
     * `generateChatId("A","B") == generateChatId("B","A")`.
     */
    private fun generateChatId(userId1: String, userId2: String): String =
        if (userId1 < userId2) "${userId1}_$userId2" else "${userId2}_$userId1"

    /**
     * Osserva in tempo reale i messaggi di una conversazione.
     *
     * Filtra i messaggi precedenti alla data di eliminazione logica
     * ([deletedAt_userId]) così che l'utente veda solo i messaggi
     * successivi all'ultima eliminazione.
     *
     * @param userId1 ID del primo utente della conversazione.
     * @param userId2 ID del secondo utente della conversazione.
     * @return [Flow] che emette la lista aggiornata dei messaggi ad ogni modifica.
     */
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

                // Legge la data di eliminazione logica per userId1
                chatRef.get().addOnSuccessListener { doc ->
                    val deletedAt = doc.getTimestamp("deletedAt_$userId1")

                    val messages = snapshot?.documents?.mapNotNull { msgDoc ->
                        val message = msgDoc.toObject(ChatMessage::class.java)
                            ?.copy(id = msgDoc.id)
                            ?: return@mapNotNull null

                        // Nasconde i messaggi precedenti all'ultima eliminazione logica
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

    /**
     * Invia un messaggio e aggiorna i metadati della chat (ultimo messaggio, contatore non letti).
     *
     * Usa [SetOptions.merge] per non sovrascrivere campi esistenti del documento chat.
     * Prima di aggiungere il messaggio, rimuove il mittente da `deletedFor` così che
     * la chat ricompaia nella lista del destinatario se l'aveva eliminata.
     *
     * @param message Messaggio da inviare (l'[id][ChatMessage.id] sarà assegnato da Firestore).
     */
    suspend fun sendMessage(message: ChatMessage) {
        val chatId = generateChatId(message.senderId, message.receiverId)
        val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)

        // Aggiorna i metadati della chat (merge per non perdere altri campi)
        chatRef.set(
            mapOf(
                "participants" to listOf(message.senderId, message.receiverId),
                "lastMessage" to message.text,
                "timestamp" to message.timestamp,
                "unreadCount_${message.receiverId}" to FieldValue.increment(1)
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()

        // Rimuove il destinatario da deletedFor così che la chat ricompaia nella sua lista
        chatRef.update(
            mapOf("deletedFor" to FieldValue.arrayRemove(message.receiverId))
        ).await()

        chatRef.collection(MESSAGES_COLLECTION).add(message).await()
    }

    /**
     * Azzera il contatore di messaggi non letti per [userId] nella chat con [otherUserId].
     *
     * @param userId ID dell'utente che ha letto i messaggi.
     * @param otherUserId ID dell'altro utente della conversazione.
     */
    suspend fun resetUnreadCount(userId: String, otherUserId: String) {
        val chatId = generateChatId(userId, otherUserId)
        try {
            firestore.collection(CHATS_COLLECTION).document(chatId)
                .update("unreadCount_$userId", 0)
                .await()
        } catch (_: Exception) {
            // Il documento potrebbe non esistere ancora se la chat è nuova
        }
    }

    /**
     * Marca come letti tutti i messaggi ricevuti da [userId] nella chat con [otherUserId].
     *
     * Usa un batch write per aggiornare tutti i documenti in un'unica operazione atomica.
     *
     * @param userId ID dell'utente che sta leggendo i messaggi.
     * @param otherUserId ID del mittente dei messaggi da marcare come letti.
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
     * Elimina la chat tra [userId1] e [userId2].
     *
     * Implementa una **doppia eliminazione logica**:
     * - Se l'altro utente ([userId2]) ha già eliminato la sua copia → elimina fisicamente
     *   tutti i messaggi e il documento chat.
     * - Altrimenti → marca la chat come eliminata per [userId1] con timestamp ([deletedAt_userId1])
     *   e aggiunge [userId1] a `deletedFor`, nascondendo la chat solo per lui.
     *
     * @param userId1 ID dell'utente che sta eliminando la chat.
     * @param userId2 ID dell'altro utente della conversazione.
     */
    suspend fun deleteChat(userId1: String, userId2: String) {
        val chatId = generateChatId(userId1, userId2)
        val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)

        val doc = chatRef.get().await()
        val deletedFor = doc.get("deletedFor") as? List<*> ?: emptyList<String>()

        if (deletedFor.contains(userId2)) {
            // L'altro utente ha già eliminato → elimina fisicamente
            val messagesSnapshot = chatRef.collection(MESSAGES_COLLECTION).get().await()
            if (messagesSnapshot.documents.isNotEmpty()) {
                val batch = firestore.batch()
                messagesSnapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            chatRef.delete().await()
        } else {
            // Eliminazione logica per userId1
            chatRef.update(
                mapOf(
                    "deletedFor" to FieldValue.arrayUnion(userId1),
                    "deletedAt_$userId1" to Timestamp.now()
                )
            ).await()
        }
    }

    /**
     * Osserva in tempo reale la lista delle chat recenti dell'utente.
     *
     * Filtra le chat che l'utente ha eliminato logicamente (campo `deletedFor`).
     * I risultati sono ordinati per timestamp decrescente (più recenti prima).
     *
     * @param userId ID dell'utente di cui caricare le chat recenti.
     * @return [Flow] che emette la lista aggiornata delle [ChatPreview] ad ogni modifica.
     */
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
                    // Salta le chat eliminate logicamente dall'utente
                    val deletedFor = doc.get("deletedFor") as? List<*> ?: emptyList<Any>()
                    if (deletedFor.contains(userId)) return@mapNotNull null

                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: return@mapNotNull null
                    val participants = doc.get("participants") as? List<*> ?: return@mapNotNull null
                    val otherUserId = participants.firstOrNull { it != userId }?.toString()
                        ?: return@mapNotNull null
                    val unreadCount = (doc.getLong("unreadCount_$userId") ?: 0).toInt()

                    ChatPreview(otherUserId, lastMessage, timestamp, unreadCount)
                } ?: emptyList()

                trySend(chats).isSuccess
            }

        awaitClose { listener.remove() }
    }
}