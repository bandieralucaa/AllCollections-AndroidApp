package com.example.allcollections.feature.notification.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FCMTokenManager(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun initializeToken(context: Context) {
        if (!hasNotificationPermission(context)) return

        try {
            val token = FirebaseMessaging.getInstance().token.await()
            saveToken(token)
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun refreshToken(newToken: String) {
        saveToken(newToken)
    }

    private suspend fun saveToken(token: String) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid

        val data = mapOf(
            "fcmToken" to token,
            "fcmTokenUpdated" to Timestamp.now(),
            "platform" to "android",
            "deviceModel" to Build.MODEL,
            "osVersion" to Build.VERSION.SDK_INT.toString()
        )

        try {
            if (userId != null) {
                // Utente loggato - salva nel profilo
                firestore.collection("users")
                    .document(userId)
                    .update(data)
                    .await()
            } else {
                // Utente anonimo - salva come dispositivo
                firestore.collection("device_tokens")
                    .add(data)
                    .await()
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}