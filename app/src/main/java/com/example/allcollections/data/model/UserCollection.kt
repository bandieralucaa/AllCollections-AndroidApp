package com.example.allcollections.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Modello dati per una collezione creata da un utente.
 * Rappresenta una raccolta di oggetti/items organizzata dall'utente.
 */
data class UserCollection(
    val id: String = "",                 // ID univoco della collezione

    @get:PropertyName("iduser")
    @set:PropertyName("iduser")
    var iduser: String = "",             // ID dell'utente proprietario

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",               // Nome della collezione

    var username: String = "",           // Username del proprietario (calcolato, non in DB)

    @get:PropertyName("collectionImageUrl")
    @set:PropertyName("collectionImageUrl")
    var collectionImageUrl: String = "", // Immagine rappresentativa della collezione

    @get:PropertyName("category")
    @set:PropertyName("category")
    var category: String = "",           // Categoria della collezione

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = ""         // Descrizione testuale
) {

    /** Verifica se la collezione ha un'immagine impostata */
    val hasImage: Boolean
        get() = collectionImageUrl.isNotBlank()

    /** Verifica se la collezione ha un nome valido */
    val hasValidName: Boolean
        get() = name.isNotBlank()

    /** Nome formattato per UI (prima lettera maiuscola) */
    val displayName: String
        get() = name.trim().replaceFirstChar { it.uppercase() }

    /**
     * Restituisce una descrizione abbreviata per anteprime/card
     * @param maxLength Lunghezza massima (default: 100 caratteri)
     */
    fun getShortDescription(maxLength: Int = 100): String =
        if (description.length <= maxLength) description
        else description.take(maxLength - 3) + "..."

    /** Verifica se questa collezione appartiene a un utente specifico */
    fun belongsTo(userId: String): Boolean = this.iduser == userId

    companion object {
        /** Crea una collezione vuota/placeholder */
        fun empty(): UserCollection = UserCollection()
    }
}

/** Extension per verificare se una collezione è valida per il salvataggio */
val UserCollection.isValidForSave: Boolean
    get() = name.isNotBlank() && iduser.isNotBlank() && category.isNotBlank()

/** Extension per ottenere un'etichetta categoria leggibile (con fallback) */
val UserCollection.categoryDisplay: String
    get() = category.ifBlank { "Senza categoria" }

/** Extension per ottenere il nome del proprietario formattato per UI */
val UserCollection.ownerDisplay: String
    get() = if (username.isNotBlank()) "@$username" else "Utente"