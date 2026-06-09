package com.example.allcollections.feature.home.components

import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.profile.ProfileViewModel

/**
 * Funzioni di supporto per il caricamento delle collezioni nella HomeScreen.
 *
 * Raggruppa le chiamate ai ViewModel per caricare tutte le collezioni
 * pubbliche o solo quelle degli utenti seguiti.
 */

/**
 * Carica tutte le collezioni pubbliche con username denormalizzato.
 *
 * @param viewModel ViewModel usato per la query su Firestore.
 * @param onSuccess Callback con la lista delle collezioni trovate.
 */
fun loadAllCollections(
    viewModel: CollectionViewModel,
    onSuccess: (List<UserCollection>) -> Unit
) {
    viewModel.getAllCollectionsWithUsernames(onSuccess = onSuccess, onFailure = {})
}

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