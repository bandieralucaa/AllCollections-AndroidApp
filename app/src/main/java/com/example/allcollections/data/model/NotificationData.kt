package com.example.allcollections.data.model

/**
 * Dati estratti da una notifica push FCM in arrivo.
 *
 * Utilizzata per determinare la schermata di destinazione quando l'app
 * viene aperta tramite tap su una notifica. I campi sono tutti nullable
 * perché non tutte le notifiche contengono gli stessi dati.
 *
 * @property type Tipo di notifica (es. `"follow"`, `"comment"`, `"item_comment"`).
 * @property collectionId ID della collezione coinvolta, se presente.
 * @property itemId ID dell'oggetto coinvolto, se presente.
 * @property collectionName Nome della collezione coinvolta, se presente.
 * @property userId ID dell'utente coinvolto (es. chi ha seguito), se presente.
 */
data class NotificationData(
    val type: String?,
    val collectionId: String?,
    val itemId: String?,
    val collectionName: String?,
    val userId: String?
)