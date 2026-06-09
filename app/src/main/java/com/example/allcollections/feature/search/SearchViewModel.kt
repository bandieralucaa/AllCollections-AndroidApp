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
 * ViewModel per la ricerca di collezioni e utenti all'interno dell'app.
 *
 * Supporta tre modalità di ricerca selezionabili tramite un TabLayout:
 * - `selectedTab = 0` → solo collezioni
 * - `selectedTab = 1` → solo utenti
 * - `selectedTab = 2` → collezioni + utenti
 *
 * I risultati vengono esposti tramite [searchState] (una [StateFlow] di [SearchState]).
 * Per cancellare i risultati precedenti (es. quando l'utente cancella la query)
 * è disponibile [clearResults].
 *
 * ### Note sulle performance
 * L'implementazione attuale carica **tutti** i documenti delle collezioni e degli utenti
 * da Firestore ad ogni chiamata di [search], filtrandoli poi lato client.
 * Per dataset di grandi dimensioni (migliaia di documenti) questo potrebbe diventare inefficiente.
 * In produzione, si consiglia di utilizzare query Firestore con `orderBy` e `startAt/endAt`
 * per eseguire il filtro direttamente sul database.
 *
 * @param collectionViewModel ViewModel delle collezioni (attualmente non utilizzato direttamente,
 *                            ma iniettato per future estensioni o per condividere stato).
 * @see SearchState
 */
class SearchViewModel(
    private val collectionViewModel: CollectionViewModel  // Iniettato, utile per future espansioni
) : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore

    private val _searchState = MutableStateFlow(SearchState())
    /**
     * Stato della ricerca corrente: collezioni trovate, utenti trovati, eventuale errore.
     * Emette un nuovo valore ogni volta che [search] viene chiamato con successo o con errore.
     */
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    /**
     * Cancella i risultati di ricerca precedenti, ripristinando [SearchState] vuoto.
     * Utile quando l'utente cancella il testo dalla barra di ricerca.
     */
    fun clearResults() {
        _searchState.value = SearchState()
    }

    /**
     * Cerca collezioni, utenti o entrambi in base al tab selezionato.
     *
     * La ricerca è **case‑insensitive** (viene tutto convertito in minuscolo).
     * Per le collezioni, il filtro viene applicato su [UserCollection.name] e [UserCollection.category].
     * Per gli utenti, il filtro viene applicato su [UserData.username], [UserData.name] e [UserData.surname].
     * L'utente corrente (se `currentUserId` fornito) viene escluso automaticamente dai risultati utenti.
     *
     * ### Operazioni asincrone
     * - Carica tutte le collezioni (se `selectedTab != 1`) e filtra localmente.
     * - Per ogni collezione filtrata, recupera lo username del proprietario in parallelo.
     * - Carica tutti gli utenti (se `selectedTab != 0`) e filtra localmente.
     * - I risultati vengono aggregati e pubblicati in [searchState].
     *
     * @param query Testo da cercare (case‑insensitive, anche parziale).
     * @param selectedTab 0 = solo collezioni, 1 = solo utenti, 2 = tutto.
     * @param currentUserId ID dell'utente corrente (per escluderlo dai risultati utenti), può essere `null`.
     */
    fun search(query: String, selectedTab: Int, currentUserId: String?) {
        viewModelScope.launch {
            try {
                val lowerQuery = query.lowercase()

                var collectionResults: List<UserCollection> = emptyList()
                var userResults: List<UserData> = emptyList()

                // ────────── Ricerca collezioni (se non siamo nella scheda "solo utenti") ──────────
                if (selectedTab != 1) {
                    // Carica TUTTE le collezioni da Firestore
                    val snapshot = db.collection("collections").get().await()
                    val collections = snapshot.documents.mapNotNull { it.toObject<UserCollection>()?.copy(id = it.id) }

                    // Filtra per nome o categoria contenente la query (client‑side)
                    val filteredCollections = collections.filter {
                        it.name.lowercase().contains(lowerQuery) ||
                                it.category.lowercase().contains(lowerQuery)
                    }

                    // Arricchisce ogni collezione con lo username del proprietario (chiamata Firestore aggiuntiva)
                    collectionResults = filteredCollections.map { coll ->
                        async {
                            val username = getUsernameByIdSync(coll.iduser ?: "")
                            coll.copy(username = username)
                        }
                    }.awaitAll()
                }

                // ────────── Ricerca utenti (se non siamo nella scheda "solo collezioni") ──────────
                if (selectedTab != 0) {
                    // Carica TUTTI gli utenti da Firestore
                    val snapshot = db.collection("users").get().await()
                    val users = snapshot.documents.mapNotNull { doc ->
                        val user = doc.toObject<UserData>()?.copy(userId = doc.id)
                        // Esclude l'utente corrente
                        user?.takeIf { it.userId != currentUserId }
                    }

                    // Filtra per username, nome o cognome contenente la query (client‑side)
                    userResults = users.filter {
                        it.username.lowercase().contains(lowerQuery) ||
                                it.name.lowercase().contains(lowerQuery) ||
                                it.surname.lowercase().contains(lowerQuery)
                    }
                }

                // Pubblica i risultati
                _searchState.value = SearchState(
                    collections = collectionResults,
                    users = userResults,
                    error = null
                )

            } catch (e: Exception) {
                // In caso di errore, notifica lo stato di errore
                _searchState.value = SearchState(error = e.message ?: "Errore ricerca")
            }
        }
    }

    /**
     * Recupera lo username di un utente dato il suo ID (operazione sospesa).
     * Usata internamente per arricchire le collezioni con il nome del proprietario.
     *
     * @param userId ID dell'utente.
     * @return Username trovato, o `"Utente"` come fallback.
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