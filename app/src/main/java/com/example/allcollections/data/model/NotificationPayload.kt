package com.example.allcollections.data.model

import com.google.firebase.firestore.PropertyName

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
    @get:PropertyName("collectionId")
    @set:PropertyName("collectionId")
    var collectionId: String? = null,

    @get:PropertyName("collectionName")
    @set:PropertyName("collectionName")
    var collectionName: String? = null,

    @get:PropertyName("itemId")
    @set:PropertyName("itemId")
    var itemId: String? = null,

    @get:PropertyName("itemDescription")
    @set:PropertyName("itemDescription")
    var itemDescription: String? = null,

    @get:PropertyName("commentText")
    @set:PropertyName("commentText")
    var commentText: String? = null,

    @get:PropertyName("pushTitle")
    @set:PropertyName("pushTitle")
    var pushTitle: String? = null,

    @get:PropertyName("pushMessage")
    @set:PropertyName("pushMessage")
    var pushMessage: String? = null
)