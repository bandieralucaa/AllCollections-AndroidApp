package com.example.allcollections.data.model

/**
 * Payload dei dati contestuali di una notifica salvata su Firestore.
 *
 * Contiene le informazioni aggiuntive necessarie per costruire il testo
 * della notifica e per navigare alla schermata corretta al tap.
 * Tutti i campi sono nullable perché il payload varia in base al [type][com.example.allcollections.feature.notification.domain.NotificationType].
 *
 * @property collectionId ID della collezione coinvolta, se presente.
 * @property collectionName Nome della collezione coinvolta, se presente.
 * @property itemId ID dell'oggetto coinvolto, se presente.
 * @property itemDescription Descrizione dell'oggetto coinvolto, se presente.
 * @property commentText Testo del commento, presente per notifiche di tipo commento.
 * @property pushTitle Titolo della notifica push inviata via FCM.
 * @property pushMessage Corpo della notifica push inviata via FCM.
 */
data class NotificationPayload(
    val collectionId: String? = null,
    val collectionName: String? = null,
    val itemId: String? = null,
    val itemDescription: String? = null,
    val commentText: String? = null,
    val pushTitle: String? = null,
    val pushMessage: String? = null
)
