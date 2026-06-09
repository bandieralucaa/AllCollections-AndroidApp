package com.example.allcollections.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.data.model.SearchState
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.search.components.UserSearchCard
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay

/**
 * Schermata di ricerca di collezioni e utenti.
 *
 * Supporta tre tab:
 * - 0: solo collezioni
 * - 1: solo utenti
 * - 2: tutto (collezioni + utenti)
 *
 * La ricerca parte automaticamente con un debounce di 500ms dopo aver digitato
 * almeno 2 caratteri. I risultati delle collezioni sono mostrati in una griglia a
 * 2 colonne (grazie al chunking in righe di massimo 2 card), mentre gli utenti
 * sono mostrati in lista verticale.
 *
 * @param viewModel ViewModel per le operazioni sulle collezioni (non usato direttamente nella ricerca).
 * @param searchViewModel ViewModel che gestisce la logica di ricerca su Firestore.
 * @param navController Controller per la navigazione ai dettagli delle collezioni e profili utente.
 *
 * @see SearchViewModel
 * @see UserSearchCard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: CollectionViewModel,
    searchViewModel: SearchViewModel,
    navController: NavController
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0=Collezioni, 1=Utenti, 2=Tutto
    var isSearching by remember { mutableStateOf(false) }

    val currentUserId = Firebase.auth.currentUser?.uid
    val searchState by searchViewModel.searchState.collectAsState()

    // Debounce della ricerca: aspetta 500ms dopo l'ultima digitazione (se lunghezza >= 2)
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isSearching = true
            delay(500)
            searchViewModel.search(searchQuery, selectedTab, currentUserId)
            isSearching = false
        } else {
            searchViewModel.clearResults()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra di ricerca personalizzata
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onClear = {
                searchQuery = ""
                searchViewModel.clearResults()
            },
            placeholder = when (selectedTab) {
                0 -> "Cerca collezioni..."
                1 -> "Cerca utenti..."
                else -> "Cerca collezioni o utenti..."
            }
        )

        // Tab per selezionare la categoria di ricerca
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    if (searchQuery.length >= 2) {
                        searchViewModel.search(searchQuery, 0, currentUserId)
                    }
                },
                text = { Text("Collezioni") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    if (searchQuery.length >= 2) {
                        searchViewModel.search(searchQuery, 1, currentUserId)
                    }
                },
                text = { Text("Utenti") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = {
                    selectedTab = 2
                    if (searchQuery.length >= 2) {
                        searchViewModel.search(searchQuery, 2, currentUserId)
                    }
                },
                text = { Text("Tutto") }
            )
        }

        // Stato della vista: ricerca non iniziata, in caricamento, errore, risultati
        when {
            searchQuery.length < 2 -> EmptySearchView()
            isSearching -> LoadingSearchView()
            searchState.error != null -> ErrorSearchView(searchState.error!!)
            else -> SearchResults(
                searchState = searchState,
                selectedTab = selectedTab,
                navController = navController
            )
        }
    }
}

/**
 * Visualizzazione dei risultati di ricerca suddivisi per sezioni.
 */
@Composable
fun SearchResults(
    searchState: SearchState,
    selectedTab: Int,
    navController: NavController
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sezione collezioni (visibile se non siamo solo nella scheda utenti)
        if (selectedTab != 1 && searchState.collections.isNotEmpty()) {
            item { SectionHeader("Collezioni") }

            // Raggruppa le collezioni in righe da massimo 2 elementi per creare una griglia
            items(searchState.collections.chunked(2)) { rowCollections ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (collection in rowCollections) {
                        CollectionSearchCard(
                            collection = collection,
                            onCardClick = {
                                navController.navigate(Screens.CollectionDetailScreen.createRoute(collection.id))
                            },
                            onUsernameClick = {
                                navController.navigate(Screens.PublicProfileScreen.createRoute(collection.iduser))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Bilanciamento dell'ultima riga con un elemento vuoto se dispari
                    if (rowCollections.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Sezione utenti (visibile se non siamo solo nella scheda collezioni)
        if (selectedTab != 0 && searchState.users.isNotEmpty()) {
            item { SectionHeader("Utenti") }

            items(searchState.users) { user ->
                UserSearchCard(
                    user = user,
                    onClick = {
                        navController.navigate(Screens.PublicProfileScreen.createRoute(user.userId))
                    }
                )
            }
        }

        // Messaggio di nessun risultato
        if (searchState.collections.isEmpty() && searchState.users.isEmpty()) {
            item { EmptyResultsView() }
        }
    }
}

/**
 * Card per una collezione nei risultati di ricerca (formato verticale compatto).
 *
 * Mostra: immagine, nome, categoria, username del proprietario (cliccabile).
 */
@Composable
fun CollectionSearchCard(
    collection: UserCollection,
    onCardClick: () -> Unit,
    onUsernameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Immagine di copertina (placeholder se assente)
            AsyncImage(
                model = collection.collectionImageUrl.takeIf { !it.isNullOrBlank() },
                contentDescription = "Immagine della collezione ${collection.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Nome collezione
            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Categoria
            Text(
                text = collection.category ?: "Senza categoria",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Username del proprietario (cliccabile)
            Text(
                text = "di @${collection.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onUsernameClick() }
            )
        }
    }
}

/**
 * Intestazione di sezione (es. "Collezioni", "Utenti").
 */
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * Vista mostrata quando la query di ricerca è troppo corta (<2 caratteri).
 */
@Composable
fun EmptySearchView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🔍", fontSize = MaterialTheme.typography.displayLarge.fontSize)
            Text("Inizia a cercare", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Digita almeno 2 caratteri per cercare collezioni o utenti",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

/**
 * Vista di caricamento durante la ricerca.
 */
@Composable
fun LoadingSearchView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator()
            Text("Ricerca in corso...")
        }
    }
}

/**
 * Vista di errore (mostra il messaggio di errore).
 */
@Composable
fun ErrorSearchView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("❌", fontSize = MaterialTheme.typography.displayLarge.fontSize)
            Text("Errore", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Vista quando non ci sono risultati per la query corrente.
 */
@Composable
fun EmptyResultsView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("😕", fontSize = MaterialTheme.typography.displayLarge.fontSize)
            Text("Nessun risultato", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Prova con altri termini di ricerca",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}