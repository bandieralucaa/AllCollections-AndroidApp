package com.example.allcollections.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Modello dati per un commento su una collezione o su un singolo oggetto.
 *
 * Un commento è sempre associato a una collezione tramite [collectionId].
 * Se [itemId] è vuoto, il commento si riferisce alla collezione stessa;
 * se [itemId] è valorizzato, il commento si riferisce a un oggetto specifico
 * all'interno della collezione.
 *
 * Il campo [username] è **denormalizzato** (copiato al momento della creazione)
 * per evitare query aggiuntive su Firestore durante la visualizzazione dei commenti.
 *
 * Il timestamp utilizza [Timestamp] di Firebase, che garantisce precisione
 * e compatibilità con gli indici di Firestore.
 *
 * @property id ID del documento Firestore (vuoto prima del salvataggio).
 * @property collectionId ID della collezione a cui appartiene il commento.
 * @property itemId ID dell'oggetto commentato; stringa vuota se commento sulla collezione.
 * @property userId ID dell'utente autore del commento.
 * @property text Testo del commento (già trimmato nei metodi factory).
 * @property timestamp Data e ora di creazione (impostata automaticamente).
 * @property username Nome utente dell'autore (denormalizzato).
 *
 * @see create per commenti alla collezione
 * @see createForItem per commenti a un oggetto
 */
data class Comment(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("collectionId")
    @set:PropertyName("collectionId")
    var collectionId: String = "",

    @get:PropertyName("itemId")
    @set:PropertyName("itemId")
    var itemId: String = "",

    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("text")
    @set:PropertyName("text")
    var text: String = "",

    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Timestamp = Timestamp.now(),

    @get:PropertyName("username")
    @set:PropertyName("username")
    var username: String = ""
) {
    companion object {
        /**
         * Crea un nuovo commento sulla collezione, pronto per il salvataggio su Firestore.
         *
         * Il commento avrà [itemId] vuoto e timestamp corrente.
         * Il testo viene automaticamente trimmato per rimuovere spazi iniziali/finali.
         *
         * @param collectionId ID della collezione commentata.
         * @param userId ID dell'utente autore.
         * @param text Testo del commento (verrà trimmato).
         * @param username Nome utente dell'autore per la visualizzazione (opzionale, può essere vuoto).
         * @return Nuova istanza di [Comment] pronta per l'inserimento in Firestore.
         *
         * @sample
         * val comment = Comment.create(
         *     collectionId = "abc123",
         *     userId = "user456",
         *     text = "  Bella collezione!  ",
         *     username = "mario_rossi"
         * )
         */
        fun create(
            collectionId: String,
            userId: String,
            text: String,
            username: String = ""
        ): Comment = Comment(
            id = "",
            collectionId = collectionId,
            itemId = "",  // Vuoto = commento sulla collezione
            userId = userId,
            text = text.trim(),
            timestamp = Timestamp.now(),
            username = username
        )

        /**
         * Crea un nuovo commento su un oggetto specifico della collezione.
         *
         * Il commento avrà [itemId] valorizzato e timestamp corrente.
         * Il testo viene automaticamente trimmato.
         *
         * @param collectionId ID della collezione contenente l'oggetto.
         * @param itemId ID dell'oggetto commentato.
         * @param userId ID dell'utente autore.
         * @param text Testo del commento (verrà trimmato).
         * @param username Nome utente dell'autore per la visualizzazione (opzionale).
         * @return Nuova istanza di [Comment] pronta per l'inserimento in Firestore.
         *
         * @sample
         * val comment = Comment.createForItem(
         *     collectionId = "abc123",
         *     itemId = "item789",
         *     userId = "user456",
         *     text = "Questo oggetto è fantastico!",
         *     username = "mario_rossi"
         * )
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