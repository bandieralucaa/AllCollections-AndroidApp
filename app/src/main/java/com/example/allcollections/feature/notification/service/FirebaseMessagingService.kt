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
 * Servizio Firebase Cloud Messaging per la ricezione di notifiche push.
 *
 * Estende [FirebaseMessagingService] e gestisce:
 * - **Messaggi in arrivo** ([onMessageReceived]): salva la notifica su Firestore
 *   e mostra una notifica di sistema all'utente.
 * - **Refresh del token FCM** ([onNewToken]): aggiorna il token nel database
 *   tramite [FCMTokenManager].
 *
 * Le notifiche mostrate utilizzano un canale dedicato (Android 8+)
 * e al tap aprono [MainActivity] con i dati della notifica come extra,
 * consentendo la navigazione context-aware.
 *
 * @see FirebaseMessagingService
 * @see FCMTokenManager
 */
class FirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Chiamato quando arriva un nuovo messaggio FCM, sia che l'app sia in foreground
     * che in background (con alcune limitazioni per quest'ultimo caso).
     *
     * @param remoteMessage Il messaggio ricevuto, contenente titolo, corpo e dati custom.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Estrae titolo e corpo (con fallback al nome app)
        val title = remoteMessage.notification?.title ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body ?: ""
        val data = remoteMessage.data

        // Salva la notifica su Firestore per la cronologia
        saveNotificationToFirestore(title, body, data)

        // Mostra la notifica di sistema
        showSystemNotification(title, body, data)
    }

    /**
     * Chiamato quando Firebase genera un nuovo token FCM per il dispositivo
     * (prima installazione, reset app, token scaduto).
     *
     * @param token Il nuovo token FCM da inviare al server (Firestore).
     */
    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val tokenManager = FCMTokenManager(Firebase.auth, Firebase.firestore)
            tokenManager.refreshToken(token)
        }
    }

    /**
     * Salva la notifica ricevuta nel database Firestore dell'utente corrente.
     *
     * La notifica viene salvata con:
     * - `recipientId` = ID dell'utente autenticato
     * - `senderId` = "system" (notifica di sistema)
     * - `type` = dal payload custom o [NotificationType.GENERAL] come fallback
     * - `pushTitle`, `pushMessage` = titolo e corpo
     * - `timestamp` = data/ora corrente
     * - `read` = false
     * - Campi aggiuntivi da `data` (collectionId, itemId, commentText, ecc.)
     *
     * @param title Titolo della notifica.
     * @param body Corpo della notifica.
     * @param data Mappa di dati custom dal messaggio FCM.
     */
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
                    // Aggiunge campi opzionali se presenti nel payload
                    data["collectionId"]?.let { put("collectionId", it) }
                    data["collectionName"]?.let { put("collectionName", it) }
                    data["itemId"]?.let { put("itemId", it) }
                    data["commentText"]?.let { put("commentText", it) }
                }

                Firebase.firestore.collection("notifications")
                    .add(notificationData)
                    .await()
            } catch (e: Exception) {
                // Logga l'errore per debug, ma non blocca l'esecuzione
                Log.e("FCMService", "Errore salvataggio notifica su Firestore: ${e.message}", e)
            }
        }
    }

    /**
     * Mostra una notifica di sistema nel drawer delle notifiche Android.
     *
     * La notifica:
     * - Al tap avvia [MainActivity] con gli extra del payload della notifica.
     * - Utilizza un [PendingIntent] con flag `IMMUTABLE` (richiesto da Android 12+).
     * - Crea automaticamente il canale di notifica (Android 8+).
     *
     * @param title Titolo della notifica.
     * @param body Corpo della notifica.
     * @param data Dati custom da aggiungere come extra all'Intent.
     */
    private fun showSystemNotification(title: String, body: String, data: Map<String, String>) {
        // Crea l'Intent per MainActivity con i dati della notifica
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        // Crea PendingIntent per aprire l'app al tap sulla notifica
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Assicura che il canale esista (Android 8+)
        createNotificationChannel()

        // Costruisce la notifica
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)                     // Rimuove la notifica al tap
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        // Mostra la notifica al sistema
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Crea il canale di notifica necessario su Android Oreo (API 26) e superiori.
     * Il canale ha priorità DEFAULT, vibrazione abilitata e badge attivo.
     */
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
            // Evita di ricreare il canale se già esiste
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        /** ID del canale di notifica (usato in Android 8+). */
        private const val CHANNEL_ID = "allcollections_channel"

        /** Nome leggibile del canale mostrato nelle impostazioni di sistema. */
        private const val CHANNEL_NAME = "All Collections"
    }
}