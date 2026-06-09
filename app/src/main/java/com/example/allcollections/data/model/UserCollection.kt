package com.example.allcollections.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Modello dati per una collezione creata da un utente.
 *
 * Rappresenta una raccolta tematica di oggetti/items. I campi annotati con
 * [@PropertyName][PropertyName] garantiscono la corretta mappatura con i nomi dei
 * campi su Firestore, indipendentemente dall'eventuale offuscazione del codice (Proguard/R8).
 *
 * **Nota:** i campi sono dichiarati come `var` perché Firestore richiede mutabilità
 * per la deserializzazione tramite reflection con costruttore no-arg.
 *
 * @property id ID univoco del documento Firestore (non salvato come campo, assegnato al fetch).
 * @property iduser ID dell'utente proprietario della collezione.
 * @property name Nome della collezione.
 * @property username Nome utente del proprietario, denormalizzato per la visualizzazione.
 * @property collectionImageUrl URL dell'immagine di copertina della collezione su Cloudinary.
 * @property category Categoria tematica della collezione (es. "Libri", "Vinili").
 * @property description Descrizione testuale della collezione.
 */
data class UserCollection(
    val id: String = "",

    @get:PropertyName("iduser")
    @set:PropertyName("iduser")
    var iduser: String = "",

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    var username: String = "",

    @get:PropertyName("collectionImageUrl")
    @set:PropertyName("collectionImageUrl")
    var collectionImageUrl: String = "",

    @get:PropertyName("category")
    @set:PropertyName("category")
    var category: String = "",

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = ""
)