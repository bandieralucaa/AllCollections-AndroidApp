package com.example.allcollections.feature.notification.data

import com.example.allcollections.data.model.UserData
import com.example.allcollections.feature.notification.domain.Notification
import com.example.allcollections.feature.notification.domain.NotificationData
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

class NotificationRepository(
    private val firestore: FirebaseFirestore
) {

    fun observeNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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

    suspend fun markAsRead(notificationId: String) {
        firestore.collection("notifications")
            .document(notificationId)
            .update("read", true)
            .await()
    }

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

    suspend fun enrichWithSenders(notifications: List<Notification>): List<Notification> {
        val senderIds = notifications.map { it.senderId }.filter { it.isNotBlank() && it != "system" }
        if (senderIds.isEmpty()) return notifications

        val usersSnapshot = firestore.collection("users")
            .whereIn(FieldPath.documentId(), senderIds)
            .get()
            .await()

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
                data = NotificationData(
                    collectionId = doc.getString("collectionId"),
                    collectionName = doc.getString("collectionName"),
                    itemId = doc.getString("itemId"),
                    commentText = doc.getString("commentText"),
                    pushTitle = doc.getString("pushTitle"),
                    pushMessage = doc.getString("pushMessage")
                )
            )
        } catch (e: Exception) {
            null
        }
    }
}

// System user (spostato in un file separato)
object SystemUser {
    val data = UserData(
        userId = "system",
        name = "All Collections",
        surname = "App",
        username = "AllCollections",
        email = "noreply@allcollections.app",
        dateOfBirth = "2024-01-01",
        gender = "",
        profileImageUrl = "https://example.com/logo.png"
    )
}