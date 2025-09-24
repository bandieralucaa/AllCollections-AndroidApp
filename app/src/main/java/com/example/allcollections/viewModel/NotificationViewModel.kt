package com.example.allcollections.viewModel

import android.icu.text.SimpleDateFormat
import androidx.lifecycle.ViewModel
import com.example.allcollections.profile.UserData
import com.example.allcollections.notification.NotificationItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class NotificationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications

    fun checkUnreadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, _ ->
                _hasUnreadNotifications.value = snapshot?.isEmpty == false
            }
    }

    fun markNotificationAsRead(notificationId: String, onComplete: (() -> Unit)? = null) {
        db.collection("notifications").document(notificationId)
            .update("read", true)
            .addOnSuccessListener {
                checkUnreadNotifications()
                onComplete?.invoke()
            }
            .addOnFailureListener {
                onComplete?.invoke()
            }
    }

    fun deleteAllNotifications(onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().addOnSuccessListener {
                    checkUnreadNotifications()
                    onComplete()
                }.addOnFailureListener {
                    onComplete()
                }
            }
            .addOnFailureListener {
                onComplete()
            }
    }

    fun sendFollowNotification(recipientId: String, senderId: String) {
        val data = mapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "type" to "follow",
            "timestamp" to Timestamp.now(),
            "read" to false
        )
        db.collection("notifications").add(data)
    }

    fun sendCommentNotification(
        recipientId: String,
        senderId: String,
        collectionId: String,
        collectionName: String,
        commentText: String? = null
    ) {
        val data = mutableMapOf(
            "recipientId" to recipientId,
            "senderId" to senderId,
            "type" to "comment",
            "collectionId" to collectionId,
            "collectionName" to collectionName,
            "timestamp" to Timestamp.now(),
            "read" to false
        )
        commentText?.let { data["commentText"] = it }
        db.collection("notifications").add(data)
    }

    fun observeNotifications(userId: String, onResult: (List<NotificationItem>) -> Unit) {
        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }

                val rawList = snapshot.documents.map { doc ->
                    val notificationId = doc.id
                    val senderId = doc.getString("senderId") ?: ""
                    val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: Date()
                    val read = doc.getBoolean("read") ?: false
                    val type = doc.getString("type") ?: "follow"
                    val collectionId = doc.getString("collectionId")
                    val collectionName = doc.getString("collectionName")
                    val commentText = doc.getString("commentText")

                    RawNotification(
                        notificationId = notificationId,
                        senderId = senderId,
                        timestamp = timestamp,
                        read = read,
                        type = type,
                        collectionId = collectionId,
                        collectionName = collectionName,
                        commentText = commentText
                    )
                }

                val senderIds = rawList.map { it.senderId }.distinct().filter { it.isNotBlank() }
                if (senderIds.isEmpty()) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }

                db.collection("users")
                    .whereIn(FieldPath.documentId(), senderIds)
                    .get()
                    .addOnSuccessListener { usersSnap ->
                        val usersMap = usersSnap.documents.mapNotNull { userDoc ->
                            try {
                                val uid = userDoc.id
                                uid to UserData(
                                    userId = uid,
                                    name = userDoc.getString("name") ?: "",
                                    surname = userDoc.getString("surname") ?: "",
                                    dateOfBirth = LocalDate.parse(userDoc.getString("dateOfBirth") ?: "2000-01-01"),
                                    email = userDoc.getString("email") ?: "",
                                    gender = userDoc.getString("gender") ?: "",
                                    username = userDoc.getString("username") ?: "",
                                    profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.toMap()

                        val notifications = rawList.mapNotNull { raw ->
                            val user = usersMap[raw.senderId] ?: return@mapNotNull null
                            NotificationItem(
                                user = user,
                                timestamp = raw.timestamp,
                                read = raw.read,
                                notificationId = raw.notificationId,
                                type = raw.type,
                                collectionId = raw.collectionId,
                                collectionName = raw.collectionName,
                                commentText = raw.commentText
                            )
                        }

                        onResult(notifications)
                    }
                    .addOnFailureListener {
                        onResult(emptyList())
                    }
            }
    }

    fun formatRelativeTime(date: Date): String {
        val now = Date()
        val diff = now.time - date.time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "$seconds secondi fa"
            minutes < 60 -> "$minutes minuti fa"
            hours < 24 -> "$hours ore fa"
            days == 1L -> "ieri"
            days < 7 -> "$days giorni fa"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        }
    }

    private data class RawNotification(
        val notificationId: String,
        val senderId: String,
        val timestamp: Date,
        val read: Boolean,
        val type: String,
        val collectionId: String?,
        val collectionName: String?,
        val commentText: String?
    )
}