package com.example.allcollections.data.model

import com.example.allcollections.core.utils.time.formatRelativeTime
import com.example.allcollections.feature.notification.domain.NotificationType
import java.util.Date

/**
 * Modello di una notifica ricevuta dall'utente.
 *
 * Contiene i dati del mittente, il tipo di evento che ha generato la notifica
 * e il payload con le informazioni contestuali (collezione, oggetto, commento).
 * Le notifiche di sistema (es. benvenuto) hanno [senderId] = `"system"`.
 *
 * @property id ID univoco del documento Firestore.
 * @property recipientId ID dell'utente che ha ricevuto la notifica.
 * @property senderId ID dell'utente che ha generato la notifica; `"system"` per notifiche automatiche.
 * @property type Tipo di evento che ha generato la notifica (vedi [NotificationType]).
 * @property timestamp Data e ora di creazione della notifica.
 * @property read `true` se l'utente ha già visualizzato la notifica.
 * @property data Payload con i dettagli contestuali della notifica (vedi [NotificationPayload]).
 * @property sender Dati dell'utente mittente; `null` per notifiche di sistema o se non ancora caricato.
 */
data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val timestamp: Date = Date(),
    val read: Boolean = false,
    val data: NotificationPayload = NotificationPayload(),
    val sender: UserData? = null
) {
    /** `true` se la notifica è di sistema (es. notifica di benvenuto), `false` se generata da un utente. */
    val isSystem: Boolean get() = senderId == "system"

    /** Tempo trascorso dalla notifica in formato leggibile (es. "2 ore fa", "ieri"). */
    val formattedTime: String
        get() = formatRelativeTime(timestamp)
}
