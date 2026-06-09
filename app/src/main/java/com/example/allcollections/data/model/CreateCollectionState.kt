package com.example.allcollections.data.model

/**
 * Stato UI per il flusso di creazione di una nuova collezione.
 *
 * @property isLoading `true` mentre è in corso la chiamata di creazione su Firestore.
 * @property error Messaggio di errore da mostrare all'utente; `null` se non ci sono errori.
 * @property createdCollectionId ID della collezione appena creata; `null` finché la creazione
 *   non è completata con successo. Usato per navigare automaticamente alla nuova collezione.
 */
data class CreateCollectionState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdCollectionId: String? = null
)
