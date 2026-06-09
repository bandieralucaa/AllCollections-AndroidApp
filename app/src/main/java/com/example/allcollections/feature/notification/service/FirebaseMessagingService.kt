package com.example.allcollections.feature.notification.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.allcollections.R
import com.example.allcollections.app.MainActivity
import com.example.allcollections.feature.notification.data.FCMTokenManager
import com.example.allcollections.feature.notification.domain.NotificationType
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Servizio Firebase Cloud Messaging per la ricezione delle notifiche push.
 *
 * Gestisce i messaggi in arrivo salvandoli su Firestore e mostrandoli
 * come notifiche di sistema. Si occupa anche del refresh del token FCM
 * quando viene rinnovato da Firebase.
 */
class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body ?: ""
        val data = remoteMessage.data

        saveNotificationToFirestore(title, body, data)
        showSystemNotification(title, body, data)
    }

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val tokenManager = FCMTokenManager(Firebase.auth, Firebase.firestore)
            tokenManager.refreshToken(token)
        }
    }

    private fun saveNotificationToFirestore(title: String, body: String, data: Map<String, String>) {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationData = hashMapOf(
                    "recipientId" to currentUserId,
                    "senderId" to "system",
                    "type" to (data["type"] ?: NotificationType.GENERAL.value),
                    "pushTitle" to title,
                    "pushMessage" to body,
                    "timestamp" to Timestamp.now(),
                    "read" to false
                ).apply {
                    data["collectionId"]?.let { put("collectionId", it) }
                    data["collectionName"]?.let { put("collectionName", it) }
                    data["itemId"]?.let { put("itemId", it) }
                    data["commentText"]?.let { put("commentText", it) }
                }

                Firebase.firestore.collection("notifications")
                    .add(notificationData)
                    .await()
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    private fun showSystemNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifiche di All Collections"
                enableVibration(true)
                setShowBadge(true)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "allcollections_channel"
        private const val CHANNEL_NAME = "All Collections"
    }
}