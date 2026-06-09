package com.example.allcollections.data.model

/**
 * Stato UI per le schermate legate alle collezioni.
 *
 * Modella lo stato completo della UI con un approccio a stato singolo (single source of truth),
 * evitando stati inconsistenti tra loading, errore e dati.
 *
 * @property isLoading `true` mentre è in corso un'operazione asincrona (caricamento/salvataggio).
 * @property error Messaggio di errore da mostrare all'utente; `null` se non ci sono errori.
 * @property collections Lista delle collezioni dell'utente corrente.
 * @property items Lista degli oggetti della collezione attualmente visualizzata.
 */
data class CollectionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val collections: List<UserCollection> = emptyList(),
    val items: List<CollectionItem> = emptyList()
)
