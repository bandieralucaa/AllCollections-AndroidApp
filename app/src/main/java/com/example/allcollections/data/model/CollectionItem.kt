package com.example.allcollections.data.model

import com.google.firebase.Timestamp

/**
 * Modello dati per un elemento (oggetto) all'interno di una collezione.
 *
 * Ogni istanza rappresenta un singolo oggetto aggiunto dall'utente
 * a una delle sue collezioni. L'immagine è ospitata su Cloudinary,
 * mentre i metadati sono salvati su Firestore come sottocollezione
 * del documento della collezione.
 *
 * @property id ID univoco del documento Firestore (vuoto prima del salvataggio).
 * @property description Descrizione testuale dell'oggetto.
 * @property imageUrl URL pubblico dell'immagine su Cloudinary.
 * @property timestamp Data e ora di aggiunta dell'oggetto alla collezione (ordinamento).
 * @property publicId Public ID Cloudinary, usato per aggiornare o eliminare l'immagine.
 */
data class CollectionItem(
    val id: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val timestamp: Timestamp? = null,
    val publicId: String? = null
)