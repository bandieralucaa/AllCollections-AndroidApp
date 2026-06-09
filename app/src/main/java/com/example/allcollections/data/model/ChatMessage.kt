package com.example.allcollections.data.model

import com.google.firebase.Timestamp

/**
 * Modello dati per un messaggio in una conversazione privata 1-1.
 *
 * @property id ID univoco del documento Firestore.
 * @property senderId ID dell'utente che ha inviato il messaggio.
 * @property receiverId ID dell'utente destinatario.
 * @property text Contenuto testuale del messaggio.
 * @property timestamp Data e ora di invio (Firebase Timestamp).
 * @property read `true` se il destinatario ha già letto il messaggio.
 */
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false
) {
    companion object {

        /**
         * Crea un nuovo messaggio pronto per il salvataggio su Firestore.
         *
         * Il campo [id] viene lasciato vuoto perché sarà assegnato da Firestore al momento
         * della scrittura. Il messaggio viene creato come non letto ([read] = `false`).
         *
         * @param senderId ID dell'utente mittente.
         * @param receiverId ID dell'utente destinatario.
         * @param text Testo del messaggio.
         * @return Una nuova istanza di [ChatMessage] con timestamp corrente.
         */
        fun create(
            senderId: String,
            receiverId: String,
            text: String
        ) = ChatMessage(
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            timestamp = Timestamp.now(),
            read = false
        )
    }
}
