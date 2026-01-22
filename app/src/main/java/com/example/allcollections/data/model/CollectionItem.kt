package com.example.allcollections.data.model

import com.google.firebase.Timestamp
import java.util.Date

/**
 * Modello dati per un elemento all'interno di una collezione.
 * Ogni elemento rappresenta un oggetto/item nella collezione di un utente.
 */
data class CollectionItem(
    val id: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val timestamp: Timestamp? = null,
    val publicId: String? = null // Per eliminare immagini da Cloudinary
) {

    /** Verifica se l'elemento ha un'immagine impostata */
    val hasImage: Boolean
        get() = imageUrl.isNotBlank()

    /** Verifica se l'elemento ha una descrizione */
    val hasDescription: Boolean
        get() = description.isNotBlank()

    /** Data di creazione come oggetto Date (utile per UI/formattazione) */
    val createdAt: Date?
        get() = timestamp?.toDate()

    /**
     * Restituisce una descrizione abbreviata per anteprime/card
     * @param maxLength Lunghezza massima (default: 80 caratteri)
     */
    fun getShortDescription(maxLength: Int = 80): String {
        val safeMax = maxOf(3, maxLength) // evita problemi se maxLength < 3
        return if (description.length <= safeMax) {
            description
        } else {
            description.take(safeMax - 3) + "..."
        }
    }

    companion object {
        /** Crea un elemento vuoto/placeholder */
        fun empty(): CollectionItem = CollectionItem()
    }
}

/** Extension per verificare se un elemento è valido (ha almeno descrizione o immagine) */
val CollectionItem.isValid: Boolean
    get() = description.isNotBlank() || imageUrl.isNotBlank()
