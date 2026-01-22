package com.example.allcollections.feature.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allcollections.data.model.UserData
import com.example.allcollections.feature.notification.components.NotificationItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.CancellationException

// Aggiungi dopo gli import, prima della classe NotificationViewModel
object SystemUser {
    val data: UserData = UserData(
        userId = "system",
        name = "All Collections",
        surname = "App",
        dateOfBirth = "2024-01-01",
        email = "noreply@allcollections.app",
        gender = "",
        username = "AllCollections",
        profileImageUrl = "https://example.com/app_logo.png"
    )
}

/**
 * ViewModel per gestire tutte le notifiche dell'app:
 * - segnalazione notifiche non lette
 * - invio notifiche (follow, commenti)
 * - osservazione notifiche
 * - segna come letto / cancella notifiche
 */
class NotificationViewModel : ViewModel() {

    // Dependencies
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth

    // Stato delle notifiche non lette
    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications.asStateFlow()

    // Handler globale per errori coroutines
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            throwable.printStackTrace()
        }
    }

    // Costanti Firestore
    private companion object {
        const val COLLECTION_NOTIFICATIONS = "notifications"
        const val COLLECTION_USERS = "users"

        const val FIELD_RECIPIENT_ID = "recipientId"
        const val FIELD_SENDER_ID = "senderId"
        const val FIELD_TYPE = "type"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_READ = "read"
        const val FIELD_COLLECTION_ID = "collectionId"
        const val FIELD_COLLECTION_NAME = "collectionName"
        const val FIELD_COMMENT_TEXT = "commentText"

        const val FIELD_NAME = "name"
        const val FIELD_SURNAME = "surname"
        const val FIELD_DATE_OF_BIRTH = "dateOfBirth"
        const val FIELD_EMAIL = "email"
        const val FIELD_GENDER = "gender"
        const val FIELD_USERNAME = "username"
        const val FIELD_PROFILE_IMAGE_URL = "profileImageUrl"

        const val TYPE_FOLLOW = "follow"
        const val TYPE_COMMENT = "comment"
        const val TYPE_PUSH_GENERAL = "push_general"
        const val TYPE_PUSH_NEW_ITEM = "push_new_item"
        const val TYPE_PUSH_NEW_COMMENT = "push_new_comment"
    }

    // --------------------------
    // PUBBLICO
    // --------------------------

    /** Restituisce l'ID utente corrente */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** Controlla se ci sono notifiche non lette */
    fun checkUnreadNotifications() {
        val userId = getCurrentUserId() ?: return
        db.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo(FIELD_RECIPIENT_ID, userId)
            .whereEqualTo(FIELD_READ, false)
            .addSnapshotListener { snapshot, error ->
                _hasUnreadNotifications.value = snapshot?.isEmpty == false
            }
    }

    /** Segna una notifica come letta (funzione pubblica accessibile dal Composable) */
    fun markNotificationAsReadPublic(notificationId: String, onComplete: (() -> Unit)? = null) {
        markNotificationAsRead(notificationId, onComplete)
    }

    /** Cancella tutte le notifiche dell'utente corrente */
    fun deleteAllNotifications(onComplete: () -> Unit) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch(exceptionHandler) {
            try {
                val snapshot = db.collection(COLLECTION_NOTIFICATIONS)
                    .whereEqualTo(FIELD_RECIPIENT_ID, userId)
                    .get()
                    .await()

                if (snapshot.documents.isNotEmpty()) {
                    val batch = db.batch()
                    snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit().await()
                }
                checkUnreadNotifications()
                onComplete()
            } catch (e: Exception) {
                onComplete()
            }
        }
    }

    // --------------------------
    // INVIO NOTIFICHE
    // --------------------------

    /** Invia notifica di follow */
    fun sendFollowNotification(recipientId: String, senderId: String) {
        if (recipientId == senderId) return
        val data = mapOf(
            FIELD_RECIPIENT_ID to recipientId,
            FIELD_SENDER_ID to senderId,
            FIELD_TYPE to TYPE_FOLLOW,
            FIELD_TIMESTAMP to Timestamp.now(),
            FIELD_READ to false
        )
        viewModelScope.launch(exceptionHandler) {
            db.collection(COLLECTION_NOTIFICATIONS).add(data).await()
        }
    }

    /** Invia notifica di commento */
    fun sendCommentNotification(
        recipientId: String,
        senderId: String,
        collectionId: String,
        collectionName: String,
        commentText: String? = null
    ) {
        if (recipientId == senderId) return
        val data = mutableMapOf(
            FIELD_RECIPIENT_ID to recipientId,
            FIELD_SENDER_ID to senderId,
            FIELD_TYPE to TYPE_COMMENT,
            FIELD_COLLECTION_ID to collectionId,
            FIELD_COLLECTION_NAME to collectionName,
            FIELD_TIMESTAMP to Timestamp.now(),
            FIELD_READ to false
        )
        commentText?.let { data[FIELD_COMMENT_TEXT] = it }

        viewModelScope.launch(exceptionHandler) {
            db.collection(COLLECTION_NOTIFICATIONS).add(data).await()
        }
    }

    /** Invia notifica push a un utente */
    fun sendPushNotificationToUser(
        recipientUserId: String,
        title: String,
        body: String,
        type: String = TYPE_PUSH_GENERAL,
        collectionId: String? = null,
        collectionName: String? = null,
        itemId: String? = null
    ) {
        viewModelScope.launch(exceptionHandler) {
            try {
                // Salva la notifica in Firestore (per l'UI dell'app)
                val notificationData = HashMap<String, Any>().apply {
                    put(FIELD_RECIPIENT_ID, recipientUserId)
                    put(FIELD_SENDER_ID, auth.currentUser?.uid ?: "system")
                    put(FIELD_TYPE, type)
                    put("pushTitle", title)
                    put("pushMessage", body)
                    put(FIELD_TIMESTAMP, Timestamp.now())
                    put(FIELD_READ, false)
                    put("isPushNotification", true)

                    collectionId?.let { put(FIELD_COLLECTION_ID, it) }
                    collectionName?.let { put(FIELD_COLLECTION_NAME, it) }
                    itemId?.let { put("itemId", it) }
                }

                collectionId?.let { notificationData[FIELD_COLLECTION_ID] = it }
                collectionName?.let { notificationData[FIELD_COLLECTION_NAME] = it }
                itemId?.let { notificationData["itemId"] = it }

                db.collection(COLLECTION_NOTIFICATIONS).add(notificationData).await()

                Log.d("NotificationVM", "Notifica push salvata per $recipientUserId")
            } catch (e: Exception) {
                Log.e("NotificationVM", "Errore invio notifica push", e)
            }
        }
    }

    // --------------------------
    // OSSERVA NOTIFICHE
    // --------------------------

    /**
     * Osserva notifiche dell'utente corrente ordinate per data decrescente
     * Arricchisce con dati utente del mittente
     */
    fun observeNotifications(userId: String, onResult: (List<NotificationItem>) -> Unit) {
        db.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo(FIELD_RECIPIENT_ID, userId)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null || snapshot == null -> onResult(emptyList())
                    else -> processRawNotifications(snapshot.documents, onResult)
                }
            }
    }

    // --------------------------
    // PRIVATO
    // --------------------------

    /** Segna una notifica come letta */
    private fun markNotificationAsRead(notificationId: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(exceptionHandler) {
            try {
                db.collection(COLLECTION_NOTIFICATIONS)
                    .document(notificationId)
                    .update(FIELD_READ, true)
                    .await()
                checkUnreadNotifications()
                onComplete?.invoke()
            } catch (e: Exception) {
                onComplete?.invoke()
            }
        }
    }

    /** Converte i DocumentSnapshot in NotificationItem arricchite */
    private fun processRawNotifications(
        documents: List<DocumentSnapshot>,
        onResult: (List<NotificationItem>) -> Unit
    ) {
        val rawNotifications = documents.mapNotNull { createRawNotification(it) }
        if (rawNotifications.isEmpty()) {
            onResult(emptyList())
            return
        }

        val senderIds = rawNotifications.map { it.senderId }.distinct().filter { it.isNotBlank() }
        if (senderIds.isEmpty()) {
            onResult(emptyList())
            return
        }

        db.collection(COLLECTION_USERS)
            .whereIn(FieldPath.documentId(), senderIds)
            .get()
            .addOnSuccessListener { usersSnap ->
                val usersMap = buildUsersMap(usersSnap.documents)
                val notifications = enrichNotifications(rawNotifications, usersMap)
                onResult(notifications)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    /** Crea oggetto RawNotification da snapshot */
    private fun createRawNotification(doc: DocumentSnapshot): RawNotification? = try {
        RawNotification(
            notificationId = doc.id,
            senderId = doc.getString(FIELD_SENDER_ID) ?: "",
            timestamp = doc.getTimestamp(FIELD_TIMESTAMP)?.toDate() ?: Date(),
            read = doc.getBoolean(FIELD_READ) ?: false,
            type = doc.getString(FIELD_TYPE) ?: TYPE_FOLLOW,
            collectionId = doc.getString(FIELD_COLLECTION_ID),
            collectionName = doc.getString(FIELD_COLLECTION_NAME),
            commentText = doc.getString(FIELD_COMMENT_TEXT),
            pushTitle = doc.getString("pushTitle"),
            pushMessage = doc.getString("pushMessage"),
            isPushNotification = doc.getBoolean("isPushNotification") ?: false
        )
    } catch (e: Exception) {
        null
    }

    /** Costruisce mappa userId -> UserData */
    private fun buildUsersMap(userDocuments: List<DocumentSnapshot>): Map<String, UserData> {
        val map = mutableMapOf<String, UserData>()

        userDocuments.forEach { doc ->
            try {
                val userData = UserData(
                    userId = doc.id,
                    name = doc.getString(FIELD_NAME) ?: "",
                    surname = doc.getString(FIELD_SURNAME) ?: "",
                    dateOfBirth = doc.getString(FIELD_DATE_OF_BIRTH) ?: "2000-01-01",
                    email = doc.getString(FIELD_EMAIL) ?: "",
                    gender = doc.getString(FIELD_GENDER) ?: "",
                    username = doc.getString(FIELD_USERNAME) ?: "",
                    profileImageUrl = doc.getString(FIELD_PROFILE_IMAGE_URL) ?: ""
                )
                map[doc.id] = userData
            } catch (e: Exception) {
                // Ignora documenti errati
            }
        }

        return map
    }

    /** Arricchisce le notifiche raw con i dati utente */
    private fun enrichNotifications(
        rawNotifications: List<RawNotification>,
        usersMap: Map<String, UserData>
    ): List<NotificationItem> {
        val notifications = mutableListOf<NotificationItem>()

        rawNotifications.forEach { raw ->
            val user = if (raw.senderId == "system" || raw.isPushNotification) {
                SystemUser.data
            } else {
                usersMap[raw.senderId]
            }

            user?.let {
                val notification = NotificationItem(
                    notificationId = raw.notificationId,
                    user = it,
                    timestamp = raw.timestamp,
                    read = raw.read,
                    type = raw.type,
                    collectionId = raw.collectionId,
                    collectionName = raw.collectionName,
                    commentText = raw.commentText,
                    pushTitle = raw.pushTitle,
                    pushMessage = raw.pushMessage,
                    isPushNotification = raw.isPushNotification
                )
                notifications.add(notification)
            }
        }

        return notifications
    }
}

/** Oggetto raw della notifica prima dell'arricchimento */
data class RawNotification(
    val notificationId: String,
    val senderId: String,
    val timestamp: Date,
    val read: Boolean,
    val type: String,
    val collectionId: String?,
    val collectionName: String?,
    val commentText: String?,
    val pushTitle: String? = null,
    val pushMessage: String? = null,
    val isPushNotification: Boolean = false
)