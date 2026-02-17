package com.example.allcollections.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.data.model.UserData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SearchState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val collections: List<UserCollection> = emptyList(),
    val users: List<UserData> = emptyList()
)

class SearchViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    fun search(query: String, tab: Int, currentUserId: String?) {
        if (query.length < 2) {
            clearResults()
            return
        }

        viewModelScope.launch {
            _searchState.value = _searchState.value.copy(isLoading = true, error = null)

            try {
                val lowercaseQuery = query.lowercase()

                val collections = if (tab != 1) {
                    searchCollections(lowercaseQuery, currentUserId)
                } else emptyList()

                val users = if (tab != 0) {
                    searchUsers(lowercaseQuery, currentUserId)
                } else emptyList()

                _searchState.value = SearchState(
                    isLoading = false,
                    collections = collections,
                    users = users
                )
            } catch (e: Exception) {
                _searchState.value = SearchState(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun clearResults() {
        _searchState.value = SearchState()
    }

    private suspend fun searchCollections(query: String, currentUserId: String?): List<UserCollection> {
        return try {
            val collections = mutableListOf<UserCollection>()

            // Cerca per nome
            val nameSnapshot = db.collection("collections")
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .await()

            collections.addAll(nameSnapshot.documents.mapNotNull { doc ->
                doc.toObject(UserCollection::class.java)?.copy(id = doc.id)
            })

            // Se non ci sono risultati, cerca anche per categoria
            if (collections.isEmpty()) {
                val categorySnapshot = db.collection("collections")
                    .orderBy("category")
                    .startAt(query)
                    .endAt(query + "\uf8ff")
                    .limit(20)
                    .get()
                    .await()

                collections.addAll(categorySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserCollection::class.java)?.copy(id = doc.id)
                })
            }

            // Filtra le collezioni dell'utente corrente e rimuovi duplicati
            collections
                .filter { it.iduser != currentUserId }
                .distinctBy { it.id }
                .take(20)

        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun searchUsers(query: String, currentUserId: String?): List<UserData> {
        return try {
            val users = mutableListOf<UserData>()

            // Cerca per username
            val usernameSnapshot = db.collection("users")
                .orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(15)
                .get()
                .await()

            users.addAll(usernameSnapshot.documents.mapNotNull { doc ->
                doc.toObject(UserData::class.java)?.copy(userId = doc.id)
            })

            // Cerca per nome
            val nameSnapshot = db.collection("users")
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(15)
                .get()
                .await()

            users.addAll(nameSnapshot.documents.mapNotNull { doc ->
                doc.toObject(UserData::class.java)?.copy(userId = doc.id)
            })

            // Cerca per cognome
            val surnameSnapshot = db.collection("users")
                .orderBy("surname")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(15)
                .get()
                .await()

            users.addAll(surnameSnapshot.documents.mapNotNull { doc ->
                doc.toObject(UserData::class.java)?.copy(userId = doc.id)
            })

            // Rimuovi l'utente corrente e duplicati
            users
                .filter { it.userId != currentUserId }
                .distinctBy { it.userId }
                .take(20)

        } catch (e: Exception) {
            emptyList()
        }
    }
}