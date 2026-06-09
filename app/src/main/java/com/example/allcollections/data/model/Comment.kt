package com.example.allcollections.data.model

import com.google.firebase.Timestamp

/**
 * Modello dati per un commento su una collezione o su un singolo oggetto.
 *
 * Un commento è associato sempre a una collezione ([collectionId]); se [itemId] è
 * non vuoto, il commento si riferisce a un oggetto specifico all'interno di essa,
 * altrimenti è un commento sulla collezione in generale.
 *
 * Usa [Timestamp] di Firebase per un ordinamento efficiente su Firestore.
 *
 * @property id ID del documento Firestore (vuoto prima del salvataggio).
 * @property collectionId ID della collezione a cui appartiene il commento.
 * @property itemId ID dell'oggetto commentato; stringa vuota se il commento è sulla collezione.
 * @property userId ID dell'utente autore del commento.
 * @property text Testo del commento.
 * @property timestamp Data e ora di creazione.
 * @property username Nome utente dell'autore, denormalizzato per evitare query aggiuntive.
 */
data class Comment(
    val id: String = "",
    val collectionId: String = "",
    val itemId: String = "",
    val userId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val username: String = ""
) {
    companion object {

        /**
         * Crea un nuovo commento sulla collezione, pronto per il salvataggio su Firestore.
         *
         * @param collectionId ID della collezione commentata.
         * @param userId ID dell'utente autore.
         * @param text Testo del commento (verrà trimmato).
         * @param username Nome utente dell'autore per la visualizzazione.
         * @return Nuova istanza di [Comment] con [itemId] vuoto e timestamp corrente.
         */
        fun create(
            collectionId: String,
            userId: String,
            text: String,
            username: String = ""
        ): Comment = Comment(
            id = "",
            collectionId = collectionId,
            itemId = "",
            userId = userId,
            text = text.trim(),
            timestamp = Timestamp.now(),
            username = username
        )

        /**
         * Crea un nuovo commento su un oggetto specifico, pronto per il salvataggio su Firestore.
         *
         * @param collectionId ID della collezione contenente l'oggetto.
         * @param itemId ID dell'oggetto commentato.
         * @param userId ID dell'utente autore.
         * @param text Testo del commento (verrà trimmato).
         * @param username Nome utente dell'autore per la visualizzazione.
         * @return Nuova istanza di [Comment] con [itemId] valorizzato e timestamp corrente.
         */
        fun createForItem(
            collectionId: String,
            itemId: String,
            userId: String,
            text: String,
            username: String = ""
        ): Comment = Comment(
            id = "",
            collectionId = collectionId,
            itemId = itemId,
            userId = userId,
            text = text.trim(),
            timestamp = Timestamp.now(),
            username = username
        )
    }
}
