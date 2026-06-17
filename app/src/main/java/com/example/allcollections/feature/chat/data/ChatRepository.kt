package com.example.allcollections.feature.chat.data

import com.example.allcollections.data.model.ChatMessage
import com.example.allcollections.data.model.ChatPreview
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository per la gestione delle chat in tempo reale su Firestore.
 *
 * ### Struttura del database
 * - Collezione `chats`: ogni documento rappresenta una conversazione tra due utenti.
 *   L'ID del documento è generato deterministicamente (vedi [generateChatId]).
 *   - Campi standard: `participants` (lista di due ID), `lastMessage` (testo), `timestamp` (ultimo messaggio)
 *   - Campi dinamici per i non letti: `unreadCount_<userId>` (numero messaggi non letti per quell'utente)
 *   - Campi per eliminazione logica: `deletedFor` (lista degli utenti che hanno eliminato la chat),
 *     `deletedAt_<userId>` (timestamp di eliminazione per quell'utente)
 * - Sottocollezione `messages` all'interno di ogni chat: contiene i singoli messaggi.
 *
 * ### Eliminazione logica
 * Quando un utente elimina una chat, essa non viene rimossa fisicamente subito.
 * Viene aggiunto il suo ID a `deletedFor` e viene salvato `deletedAt_<userId>`.
 * Nelle query successive, i messaggi antecedenti a quella data vengono nascosti.
 * Solo quando entrambi gli utenti hanno eliminato la chat, i messaggi e il documento chat
 * vengono cancellati fisicamente da Firestore.
 *
 * @param firestore Istanza di FirebaseFirestore (tipicamente iniettata tramite Koin).
 * @see ChatMessage
 * @see ChatPreview
 */
class ChatRepository(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val CHATS_COLLECTION = "chats"
        private const val MESSAGES_COLLECTION = "messages"
    }

    /**
     * Genera un ID chat deterministico da due ID utente.
     *
     * L'ID è ottenuto ordinando alfabeticamente i due ID e unendoli con `_`.
     * Esempio: `generateChatId("abc", "def")` → `"abc_def"`
     *          `generateChatId("def", "abc")` → `"abc_def"`
     *
     * Questa proprietà garantisce che entrambi gli utenti utilizzino lo stesso
     * documento Firestore indipendentemente dall'ordine dei parametri.
     *
     * @param userId1 Primo ID utente.
     * @param userId2 Secondo ID utente.
     * @return ID chat univoco per la coppia di utenti.
     */
    private fun generateChatId(userId1: String, userId2: String): String =
        if (userId1 < userId2) "${userId1}_$userId2" else "${userId2}_$userId1"

    /**
     * Osserva in tempo reale i messaggi di una conversazione.
     *
     * Restituisce un [Flow] che emette la lista dei messaggi ogni volta che
     * vengono aggiunti, modificati o eliminati. L'ordine è cronologico ascendente
     * (dal più vecchio al più recente).
     *
     * **Filtro di eliminazione logica:** vengono nascosti i messaggi la cui data
     * è antecedente all'ultima eliminazione della chat da parte dell'utente corrente
     * (campo `deletedAt_<userId>`).
     *
     * @param userId1 ID del primo utente (tipicamente l'utente corrente).
     * @param userId2 ID del secondo utente (l'interlocutore).
     * @return [Flow] che emette la lista aggiornata dei messaggi.
     */
    fun getMessages(userId1: String, userId2: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatId = generateChatId(userId1, userId2)
        val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)

        val listener = chatRef
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Se l'errore è di permessi (dopo logout), chiudi silenziosamente
                    if (error is FirebaseFirestoreException &&
                        error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        close()
                        return@addSnapshotListener
                    }
                    close(error)
                    return@addSnapshotListener
                }

                // Legge la data di eliminazione logica per userId1 per filtrare i messaggi
                chatRef.get().addOnSuccessListener { doc ->
                    val deletedAt = doc.getTimestamp("deletedAt_$userId1")

                    val messages = snapshot?.documents?.mapNotNull { msgDoc ->
                        val message = msgDoc.toObject(ChatMessage::class.java)
                            ?.copy(id = msgDoc.id)
                            ?: return@mapNotNull null

                        // Filtra i messaggi più vecchi dell'eliminazione
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
     * Invia un messaggio e aggiorna i metadati della chat.
     *
     * Operazioni atomiche:
     * 1. Aggiorna (o crea) il documento chat con merge:
     *    - `participants` = lista dei due utenti
     *    - `lastMessage` = testo del messaggio
     *    - `timestamp` = data del messaggio
     *    - `unreadCount_<receiverId>` incrementato di 1
     * 2. Rimuove il destinatario dal campo `deletedFor`, in modo che la chat
     *    ricompaia nella sua lista se l'aveva eliminata.
     * 3. Aggiunge il messaggio alla sottocollezione `messages`.
     *
     * @param message Messaggio da inviare. L'ID sarà generato automaticamente da Firestore.
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

        // Rimuove il destinatario da deletedFor per fargli riapparire la chat
        chatRef.update(
            mapOf("deletedFor" to FieldValue.arrayRemove(message.receiverId))
        ).await()

        // Aggiunge il messaggio alla sottocollezione
        chatRef.collection(MESSAGES_COLLECTION).add(message).await()
    }

    /**
     * Azzera il contatore dei messaggi non letti per un utente in una chat.
     *
     * @param userId ID dell'utente che ha letto i messaggi (il suo contatore viene azzerato).
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
     * Marca come letti tutti i messaggi ricevuti da un utente in una chat.
     *
     * Utilizza un batch write per aggiornare tutti i messaggi non letti in una
     * singola operazione atomica.
     *
     * @param userId ID dell'utente che sta leggendo i messaggi (ricevente).
     * @param otherUserId ID del mittente (per filtrare i messaggi da lui inviati).
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
     * Elimina la chat tra due utenti per uno di loro (eliminazione logica).
     *
     * ### Comportamento dettagliato
     * - Se l'altro utente ha già eliminato la chat (il suo ID è presente in `deletedFor`),
     *   allora questa chiamata **elimina fisicamente** l'intera chat (documento principale
     *   e tutti i messaggi).
     * - Altrimenti, esegue un'**eliminazione logica** per l'utente corrente:
     *   - Aggiunge il suo ID al campo `deletedFor`
     *   - Imposta il timestamp `deletedAt_<userId>` al momento corrente
     *   - La chat non apparirà più nelle sue [getRecentChats] e i vecchi messaggi
     *     verranno filtrati da [getMessages].
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
            // L'altro utente ha già eliminato → eliminazione fisica definitiva
            val messagesSnapshot = chatRef.collection(MESSAGES_COLLECTION).get().await()
            if (messagesSnapshot.documents.isNotEmpty()) {
                val batch = firestore.batch()
                messagesSnapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            chatRef.delete().await()
        } else {
            // Eliminazione logica solo per userId1
            chatRef.update(
                mapOf(
                    "deletedFor" to FieldValue.arrayUnion(userId1),
                    "deletedAt_$userId1" to Timestamp.now()
                )
            ).await()
        }
    }

    /**
     * Osserva in tempo reale la lista delle chat recenti di un utente.
     *
     * Restituisce un [Flow] che emette una lista di [ChatPreview] ogni volta che
     * viene aggiunto un nuovo messaggio, letto un messaggio, o eliminata una chat.
     * Le chat sono ordinate per [timestamp] decrescente (più recente prima).
     *
     * **Filtro:** vengono escluse le chat in cui l'utente corrente è presente in `deletedFor`.
     *
     * @param userId ID dell'utente di cui ottenere le chat recenti.
     * @return [Flow] di [List]<[ChatPreview]>, aggiornato in tempo reale.
     */
    fun getRecentChats(userId: String): Flow<List<ChatPreview>> = callbackFlow {
        val listener = firestore.collection(CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Se l'errore è di permessi (dopo logout), chiudi silenziosamente
                    if (error is FirebaseFirestoreException &&
                        error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        close()
                        return@addSnapshotListener
                    }
                    close(error)
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