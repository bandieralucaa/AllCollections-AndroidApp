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
 * Responsabilità:
 * - Salvare il token FCM corrente su Firestore (nel profilo utente se autenticato,
 *   altrimenti nella collection `"device_tokens"`).
 * - Aggiornare il token quando viene rinnovato da Firebase (chiamato da [FirebaseMessagingService.onNewToken]).
 *
 * ### Gestione permessi (Android 13+)
 * Su Android 13 (API 33) e superiori, l'app deve richiedere il permesso
 * `POST_NOTIFICATIONS` prima di poter ottenere il token. La logica di controllo
 * permessi **non è implementata** in questa classe; dovrebbe essere gestita
 * prima della chiamata a [refreshToken] (ad esempio da una schermata di onboarding o
 * da [MainActivity] al primo avvio).
 *
 * ### Struttura Firestore
 * - Utente autenticato: il token viene salvato nel campo `fcmToken` del documento utente
 *   (collection `"users"`), insieme a timestamp e metadati dispositivo.
 * - Utente non autenticato: il token viene salvato in un documento separato nella
 *   collection `"device_tokens"` (utile per inviare notifiche prima del login).
 *
 * @param auth Istanza di [FirebaseAuth] per ottenere l'utente corrente.
 * @param firestore Istanza di [FirebaseFirestore] per il salvataggio.
 *
 * @see FirebaseMessagingService
 * @see android.Manifest.permission.POST_NOTIFICATIONS
 */
class FCMTokenManager(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /**
     * Aggiorna il token FCM nel database.
     *
     * Chiamato quando Firebase genera un nuovo token (es. prima installazione,
     * token scaduto, reset app). Delega il salvataggio a [saveToken].
     *
     * @param newToken Nuovo token FCM da salvare.
     */
    suspend fun refreshToken(newToken: String) {
        saveToken(newToken)
    }

    /**
     * Salva il token FCM su Firestore con metadati del dispositivo.
     *
     * Se l'utente è autenticato, il token viene salvato nel documento `users/{userId}`
     * aggiornando i campi:
     * - `fcmToken` (stringa)
     * - `fcmTokenUpdated` (timestamp)
     * - `platform` (sempre `"android"`)
     * - `deviceModel` (es. `"Pixel 6"`)
     * - `osVersion` (API level, es. `"33"`)
     *
     * Se l'utente non è autenticato, il token viene salvato in un nuovo documento
     * nella collection `"device_tokens"` con gli stessi campi (utile per notifiche
     * pre‑login). Eventuali errori vengono loggati ma non propagati.
     *
     * @param token Token FCM da salvare.
     */
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
                // Utente loggato → aggiorna il documento utente
                firestore.collection("users")
                    .document(userId)
                    .update(data)
                    .await()
            } else {
                // Utente non loggato → salva in device_tokens
                firestore.collection("device_tokens")
                    .add(data)
                    .await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FCMTokenManager", "Errore salvataggio token FCM: ${e.message}", e)
        }
    }
}