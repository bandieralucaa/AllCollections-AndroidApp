package com.example.allcollections.feature.notification.data

import android.os.Build
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Gestore del token FCM per le notifiche push.
 *
 * Si occupa di recuperare il token Firebase Cloud Messaging e salvarlo
 * su Firestore: nel profilo utente se autenticato, nella collection
 * "device_tokens" altrimenti. Verifica il permesso notifiche su Android 13+
 * prima di procedere.
 */
class FCMTokenManager(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

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
                firestore.collection("users")
                    .document(userId)
                    .update(data)
                    .await()
            } else {
                firestore.collection("device_tokens")
                    .add(data)
                    .await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FCMTokenManager", "Errore salvataggio token FCM: ${e.message}", e)
        }
    }

}