package com.example.allcollections.feature.home.components

import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.profile.ProfileViewModel

/**
 * Funzioni di supporto per il caricamento delle collezioni nella HomeScreen.
 *
 * Raggruppa le chiamate ai ViewModel per caricare:
 * - Tutte le collezioni pubbliche ([loadAllCollections])
 * - Solo le collezioni degli utenti seguiti ([loadFollowedCollections])
 */

/**
 * Carica tutte le collezioni pubbliche con username denormalizzato.
 *
 * Utilizza [CollectionViewModel.getAllCollectionsWithUsernames] che restituisce
 * già le collezioni arricchite con lo username del proprietario.
 * Eventuali errori di caricamento vengono ignorati (lista vuota).
 *
 * @param viewModel ViewModel delle collezioni.
 * @param onSuccess Callback con la lista delle collezioni trovate.
 */
fun loadAllCollections(
    viewModel: CollectionViewModel,
    onSuccess: (List<UserCollection>) -> Unit
) {
    viewModel.getAllCollectionsWithUsernames(
        onSuccess = onSuccess,
        onFailure = {} // Silenzioso: in caso di errore, la lista rimane vuota
    )
}

/**
 * Carica le collezioni degli utenti che l'utente corrente segue.
 *
 * Procedura:
 * 1. Recupera gli ID degli utenti seguiti tramite [ProfileViewModel.getFollowedUserIds].
 * 2. Se la lista non è vuota, carica le loro collezioni tramite
 *    [CollectionViewModel.getCollectionsByUserIds].
 * 3. Altrimenti, restituisce una lista vuota.
 *
 * @param profileViewModel ViewModel del profilo (per la lista di utenti seguiti).
 * @param collectionViewModel ViewModel delle collezioni (per caricare le collezioni).
 * @param onResult Callback con la lista delle collezioni trovate (o vuota).
 */
fun loadFollowedCollections(
    profileViewModel: ProfileViewModel,
    collectionViewModel: CollectionViewModel,
    onResult: (List<UserCollection>) -> Unit
) {
    profileViewModel.getFollowedUserIds { followedIds ->
        if (followedIds.isNotEmpty()) {
            collectionViewModel.getCollectionsByUserIds(followedIds) { onResult(it) }
        } else {
            onResult(emptyList())
        }
    }
}