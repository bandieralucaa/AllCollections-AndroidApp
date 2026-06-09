package com.example.allcollections.data.model

/**
 * Stato UI per la schermata di ricerca.
 *
 * Contiene i risultati di ricerca separati per tipo (collezioni e utenti),
 * in modo da poterli visualizzare in sezioni distinte.
 *
 * @property collections Lista delle collezioni che corrispondono alla query di ricerca.
 * @property users Lista degli utenti che corrispondono alla query di ricerca.
 * @property error Messaggio di errore da mostrare all'utente; `null` se non ci sono errori.
 */
data class SearchState(
    val collections: List<UserCollection> = emptyList(),
    val users: List<UserData> = emptyList(),
    val error: String? = null
)