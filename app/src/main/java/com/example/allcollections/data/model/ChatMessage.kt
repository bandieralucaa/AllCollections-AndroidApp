package com.example.allcollections.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Modello dati per un messaggio in una conversazione privata 1-1.
 *
 * Utilizzato nella funzionalità di chat in tempo reale. I messaggi sono archiviati
 * come sottocollezione di ogni documento chat in Firestore.
 *
 * @property id ID univoco del documento Firestore (vuoto prima del salvataggio).
 * @property senderId ID dell'utente che ha inviato il messaggio.
 * @property receiverId ID dell'utente destinatario.
 * @property text Contenuto testuale del messaggio.
 * @property timestamp Data e ora di invio (Firebase Timestamp).
 * @property read `true` se il destinatario ha già letto il messaggio (usato per badge non letti).
 */
data class ChatMessage(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("senderId")
    @set:PropertyName("senderId")
    var senderId: String = "",

    @get:PropertyName("receiverId")
    @set:PropertyName("receiverId")
    var receiverId: String = "",

    @get:PropertyName("text")
    @set:PropertyName("text")
    var text: String = "",

    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Timestamp = Timestamp.now(),

    @get:PropertyName("read")
    @set:PropertyName("read")
    var read: Boolean = false
) {
    companion object {
        /**
         * Crea un nuovo messaggio pronto per il salvataggio su Firestore.
         *
         * Il campo [id] viene lasciato vuoto perché sarà assegnato automaticamente
         * da Firestore al momento dell'inserimento. Il messaggio viene creato
         * come non letto ([read] = `false`).
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