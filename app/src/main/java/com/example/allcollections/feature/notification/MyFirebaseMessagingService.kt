package com.example.allcollections.feature.notification

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
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "allcollections_channel"
        const val CHANNEL_NAME = "All Collections"

        const val TYPE_PUSH_GENERAL = "push_general"
        const val TYPE_PUSH_NEW_ITEM = "push_new_item"
        const val TYPE_PUSH_NEW_COMMENT = "push_new_comment"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Messaggio ricevuto da: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body ?: ""
        val data = remoteMessage.data

        Log.d(TAG, "Titolo: $title, Body: $body, Dati: $data")

        savePushNotificationToFirestore(title, body, data)
        showSystemNotification(title, body, data)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Nuovo token FCM: $token")
        saveFCMTokenToUserProfile(token)
    }

    private fun savePushNotificationToFirestore(title: String, body: String, data: Map<String, String>) {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationData = HashMap<String, Any>().apply {
                    put("recipientId", currentUserId)
                    put("senderId", "system")
                    put("type", data["type"] ?: TYPE_PUSH_GENERAL)
                    put("pushTitle", title)
                    put("pushMessage", body)
                    put("timestamp", Timestamp.now())
                    put("read", false)
                    put("isPushNotification", true)

                    data["collectionId"]?.let { put("collectionId", it) }
                    data["collectionName"]?.let { put("collectionName", it) }
                    data["itemId"]?.let { put("itemId", it) }
                    data["commentText"]?.let { put("commentText", it) }
                    data["userId"]?.let { put("userId", it) }
                }

                Firebase.firestore.collection("notifications")
                    .add(notificationData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Notifica push salvata su Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Errore salvataggio notifica", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Errore salvataggio notifica", e)
            }
        }
    }

    private fun showSystemNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            data.forEach { (key, value) ->
                putExtra(key, value)
            }

            putExtra("notification_type", data["type"] ?: TYPE_PUSH_GENERAL)
            putExtra("notification_title", title)
            putExtra("notification_body", body)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            Random().nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Random().nextInt(), notificationBuilder.build())
    }

    private fun saveFCMTokenToUserProfile(token: String) {
        val userId = Firebase.auth.currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userData = HashMap<String, Any>().apply {
                    put("fcmToken", token)
                    put("fcmTokenUpdated", Timestamp.now())
                }

                Firebase.firestore.collection("users")
                    .document(userId)
                    .update(userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Token FCM salvato nel profilo")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Errore salvataggio token", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Errore salvataggio token", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifiche della app All Collections"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}