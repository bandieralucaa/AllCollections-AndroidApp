package com.example.allcollections.feature.notification.data

import android.util.Log
import com.example.allcollections.data.model.Notification
import com.example.allcollections.data.model.NotificationPayload
import com.example.allcollections.data.model.UserData
import com.example.allcollections.feature.notification.domain.NotificationType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repository per la gestione delle notifiche su Firestore.
 *
 * Gestisce:
 * - Invio di notifiche push (follow, commento, like, nuovo oggetto)
 * - Osservazione in tempo reale delle notifiche ricevute
 * - Marcatura come lette e cancellazione
 * - Arricchimento con i dati del mittente tramite query batch
 *
 * @param firestore Istanza di [FirebaseFirestore] (iniettata tipicamente via Koin).
 * @see Notification
 * @see NotificationType
 */
class NotificationRepository(
    private val firestore: FirebaseFirestore
) {

    /**
     * Osserva in tempo reale le notifiche destinate a un utente specifico.
     *
     * Restituisce un [Flow] che emette la lista aggiornata delle notifiche
     * ogni volta che ci sono cambiamenti nel database. Le notifiche sono ordinate
     * per timestamp decrescente (più recenti prima).
     *
     * @param userId ID dell'utente destinatario delle notifiche.
     * @return [Flow] che emette [List]<[Notification]>, aggiornata in tempo reale.
     */
    fun observeNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NotificationRepo", "Errore observeNotifications: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val notifications = snapshot.documents.mapNotNull { doc ->
                    mapToNotification(doc)
                }

                trySend(notifications).isSuccess
            }

        awaitClose { listener.remove() }
    }

    /**
     * Invia una notifica di follow.
     * Se mittente e destinatario coincidono, non fa nulla.
     *
     * @param recipientId ID dell'utente che riceve il follow.
     * @param senderId ID dell'utente che ha seguito.
     */
    suspend fun sendFollowNotification(recipientId: String, senderId: String) {
        if (recipientId == senderId) return

        val notification = mapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "type" to NotificationType.FOLLOW.value,
            "timestamp" to Timestamp.now(),
            "read" to false
        )

        firestore.collection("notifications").add(notification).await()
    }

    /**
     * Invia una notifica di commento su una collezione.
     *
     * @param recipientId ID del proprietario della collezione.
     * @param senderId ID dell'utente che ha commentato.
     * @param collectionId ID della collezione commentata.
     * @param collectionName Nome della collezione.
     * @param commentText Testo del commento (opzionale, mostrato nella notifica).
     */
    suspend fun sendCommentNotification(
        recipientId: String,
        senderId: String,
        collectionId: String,
        collectionName: String,
        commentText: String? = null
    ) {
        if (recipientId == senderId) return

        val notification = mapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "type" to NotificationType.COMMENT.value,
            "collectionId" to collectionId,
            "collectionName" to collectionName,
            "commentText" to commentText,
            "timestamp" to Timestamp.now(),
            "read" to false
        )

        firestore.collection("notifications").add(notification).await()
    }

    /**
     * Invia una notifica di commento su un singolo oggetto della collezione.
     *
     * @param recipientId ID del proprietario della collezione.
     * @param senderId ID dell'utente che ha commentato.
     * @param collectionId ID della collezione.
     * @param collectionName Nome della collezione.
     * @param itemId ID dell'oggetto commentato.
     * @param itemDescription Descrizione dell'oggetto (opzionale).
     * @param commentText Testo del commento (opzionale).
     */
    suspend fun sendItemCommentNotification(
        recipientId: String,
        senderId: String,
        collectionId: String,
        collectionName: String,
        itemId: String,
        itemDescription: String?,
        commentText: String? = null
    ) {
        if (recipientId == senderId) return

        val notification = mapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "type" to NotificationType.ITEM_COMMENT.value,
            "collectionId" to collectionId,
            "collectionName" to collectionName,
            "itemId" to itemId,
            "itemDescription" to itemDescription,
            "commentText" to commentText,
            "timestamp" to Timestamp.now(),
            "read" to false
        )

        firestore.collection("notifications").add(notification).await()
    }

    /**
     * Invia una notifica di like su una collezione.
     *
     * @param recipientId ID del proprietario della collezione.
     * @param senderId ID dell'utente che ha messo like.
     * @param collectionId ID della collezione.
     * @param collectionName Nome della collezione.
     */
    suspend fun sendLikeNotification(
        recipientId: String,
        senderId: String,
        collectionId: String,
        collectionName: String
    ) {
        if (recipientId == senderId) return

        val notification = mapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "type" to NotificationType.LIKE.value,
            "collectionId" to collectionId,
            "collectionName" to collectionName,
            "timestamp" to Timestamp.now(),
            "read" to false
        )

        firestore.collection("notifications").add(notification).await()
    }

    /**
     * Invia una notifica di nuovo oggetto aggiunto a una collezione.
     * Tipicamente inviata a tutti gli utenti che hanno messo like alla collezione.
     *
     * @param recipientId ID dell'utente da notificare.
     * @param senderId ID dell'utente che ha aggiunto l'oggetto.
     * @param collectionId ID della collezione.
     * @param collectionName Nome della collezione.
     */
    suspend fun sendNewItemNotification(
        recipientId: String,
        senderId: String,
        collectionId: String,
        collectionName: String
    ) {
        if (recipientId == senderId) return
        val notification = mapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "type" to NotificationType.NEW_ITEM.value,
            "collectionId" to collectionId,
            "collectionName" to collectionName,
            "timestamp" to Timestamp.now(),
            "read" to false
        )
        firestore.collection("notifications").add(notification).await()
    }

    /**
     * Segna una singola notifica come letta.
     *
     * @param notificationId ID del documento notifica.
     */
    suspend fun markAsRead(notificationId: String) {
        firestore.collection("notifications")
            .document(notificationId)
            .update("read", true)
            .await()
    }

    /**
     * Elimina tutte le notifiche di un utente.
     * Utilizza un batch di Firestore per operazioni atomiche.
     *
     * @param userId ID dell'utente di cui eliminare le notifiche.
     */
    suspend fun deleteAll(userId: String) {
        val snapshot = firestore.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .get()
            .await()

        if (snapshot.documents.isNotEmpty()) {
            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    /**
     * Arricchisce una lista di notifiche con i dati completi del mittente ([UserData]).
     *
     * Esegue una singola query Firestore per recuperare tutti gli utenti necessari
     * (tranne il mittente "system", che viene gestito separatamente).
     *
     * @param notifications Lista di notifiche da arricchire.
     * @return Lista di notifiche con il campo [Notification.sender] popolato.
     */
    suspend fun enrichWithSenders(notifications: List<Notification>): List<Notification> {
        val senderIds = notifications.map { it.senderId }.filter { it.isNotBlank() && it != "system" }
        if (senderIds.isEmpty()) return notifications

        val usersSnapshot = firestore.collection("users")
            .whereIn(FieldPath.documentId(), senderIds)
            .get()
            .await()

        // Mappa userId -> UserData
        val usersMap = usersSnapshot.documents.associate { doc ->
            doc.id to UserData(
                userId = doc.id,
                name = doc.getString("name") ?: "",
                surname = doc.getString("surname") ?: "",
                username = doc.getString("username") ?: "",
                profileImageUrl = doc.getString("profileImageUrl") ?: "",
                email = doc.getString("email") ?: "",
                dateOfBirth = doc.getString("dateOfBirth") ?: "",
                gender = doc.getString("gender") ?: ""
            )
        }

        return notifications.map { notification ->
            if (notification.senderId == "system") {
                notification.copy(sender = SystemUser.data)
            } else {
                notification.copy(sender = usersMap[notification.senderId])
            }
        }
    }

    /**
     * Converte un documento Firestore in un oggetto [Notification].
     * Se la conversione fallisce (es. campo mancante), ritorna `null` e l'elemento viene scartato.
     *
     * @param doc Documento Firestore della notifica.
     * @return [Notification] valida, o `null` in caso di errore.
     */
    private fun mapToNotification(doc: com.google.firebase.firestore.DocumentSnapshot): Notification? {
        return try {
            val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: Date()

            Notification(
                id = doc.id,
                recipientId = doc.getString("recipientId") ?: "",
                senderId = doc.getString("senderId") ?: "",
                type = NotificationType.fromString(doc.getString("type")),
                timestamp = timestamp,
                read = doc.getBoolean("read") ?: false,
                data = NotificationPayload(
                    collectionId = doc.getString("collectionId"),
                    collectionName = doc.getString("collectionName"),
                    itemId = doc.getString("itemId"),
                    itemDescription = doc.getString("itemDescription"),
                    commentText = doc.getString("commentText"),
                    pushTitle = doc.getString("pushTitle"),
                    pushMessage = doc.getString("pushMessage")
                )
            )
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Errore mapToNotification per doc ${doc.id}: ${e.message}", e)
            null
        }
    }
}

/**
 * Dati fittizi per il mittente "system", usato per notifiche di sistema.
 * Ad esempio: benvenuto, annunci, ecc.
 */
object SystemUser {
    val data = UserData(
        userId = "system",
        name = "All Collections",
        surname = "App",
        username = "AllCollections",
        email = "noreply@allcollections.app",
        dateOfBirth = "2024-01-01",
        gender = "",
        profileImageUrl = "https://example.com/logo.png"  // Aggiorna con URL reale se necessario
    )
}