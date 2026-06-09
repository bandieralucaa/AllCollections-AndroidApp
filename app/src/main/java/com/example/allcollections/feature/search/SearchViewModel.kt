package com.example.allcollections.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allcollections.data.model.SearchState
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.data.model.UserData
import com.example.allcollections.feature.collection.CollectionViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel per la ricerca di collezioni e utenti.
 *
 * Supporta tre modalità tramite [selectedTab]:
 * - 0 = solo collezioni
 * - 1 = solo utenti
 * - 2 = tutto
 *
 * I risultati sono esposti tramite [searchState]. Per cancellare i risultati
 * precedenti usa [clearResults].
 */
class SearchViewModel(
    private val collectionViewModel: CollectionViewModel
) : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    /**
     * Cancella i risultati di ricerca precedenti.
     */
    fun clearResults() {
        _searchState.value = SearchState()
    }

    /**
     * Cerca collezioni, utenti o entrambi a seconda di [selectedTab].
     *
     * @param query Testo da cercare (insensibile alle maiuscole).
     * @param selectedTab 0 = Collezioni, 1 = Utenti, 2 = Tutto.
     * @param currentUserId ID dell'utente corrente (escluso dai risultati utenti).
     */
    fun search(query: String, selectedTab: Int, currentUserId: String?) {
        viewModelScope.launch {
            try {
                val lowerQuery = query.lowercase()

                var collectionResults: List<UserCollection> = emptyList()
                var userResults: List<UserData> = emptyList()

                // ────────── Collezioni ──────────
                if (selectedTab != 1) {
                    val snapshot = db.collection("collections").get().await()
                    val collections = snapshot.documents.mapNotNull { it.toObject<UserCollection>()?.copy(id = it.id) }

                    // Filtra per nome o categoria contenente la query
                    val filteredCollections = collections.filter {
                        it.name.lowercase().contains(lowerQuery) ||
                                it.category.lowercase().contains(lowerQuery)
                    }

                    // Aggiungi username al volo
                    collectionResults = filteredCollections.map { coll ->
                        async {
                            val username = getUsernameByIdSync(coll.iduser ?: "")
                            coll.copy(username = username)
                        }
                    }.awaitAll()
                }

                // ────────── Utenti ──────────
                if (selectedTab != 0) {
                    val snapshot = db.collection("users").get().await()
                    val users = snapshot.documents.mapNotNull { doc ->
                        val user = doc.toObject<UserData>()?.copy(userId = doc.id)
                        user?.takeIf { it.userId != currentUserId }
                    }

                    // Filtra per username o nome/cognome
                    userResults = users.filter {
                        it.username.lowercase().contains(lowerQuery) ||
                                it.name.lowercase().contains(lowerQuery) ||
                                it.surname.lowercase().contains(lowerQuery)
                    }
                }

                _searchState.value = SearchState(
                    collections = collectionResults,
                    users = userResults,
                    error = null
                )

            } catch (e: Exception) {
                _searchState.value = SearchState(error = e.message ?: "Errore ricerca")
            }
        }
    }

    /**
     * Funzione sospesa per ottenere username da userId
     */
    private suspend fun getUsernameByIdSync(userId: String): String {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.getString("username") ?: "Utente"
        } catch (_: Exception) {
            "Utente"
        }
    }
}