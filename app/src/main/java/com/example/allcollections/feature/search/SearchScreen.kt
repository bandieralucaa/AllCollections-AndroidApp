package com.example.allcollections.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.data.model.UserData
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.search.components.UserSearchCard
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: CollectionViewModel,
    searchViewModel: SearchViewModel,
    navController: NavController
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }

    val currentUserId = Firebase.auth.currentUser?.uid
    val searchState by searchViewModel.searchState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // Debounce per la ricerca (aspetta che l'utente finisca di scrivere)
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isSearching = true
            delay(500) // Aspetta 500ms dopo l'ultima digitazione
            searchViewModel.search(searchQuery, selectedTab, currentUserId)
            isSearching = false
        } else {
            searchViewModel.clearResults()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Barra di ricerca
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

        // Tabs
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

        // Contenuto
        when {
            searchQuery.length < 2 -> {
                EmptySearchView()
            }
            isSearching -> {
                LoadingSearchView()
            }
            searchState.error != null -> {
                ErrorSearchView(searchState.error!!)
            }
            else -> {
                SearchResults(
                    searchState = searchState,
                    selectedTab = selectedTab,
                    navController = navController
                )
            }
        }
    }
}

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
        // Collezioni
        if (selectedTab != 1 && searchState.collections.isNotEmpty()) {
            item {
                SectionHeader("Collezioni")
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchState.collections) { collection ->
                        CollectionSearchCard(
                            collection = collection,
                            onCardClick = {
                                navController.navigate(Screens.CollectionDetailScreen.createRoute(collection.id))
                            },
                            onUsernameClick = {
                                navController.navigate(Screens.PublicProfileScreen.createRoute(collection.iduser))
                            }
                        )
                    }
                }
            }
        }

        // Utenti
        if (selectedTab != 0 && searchState.users.isNotEmpty()) {
            item {
                SectionHeader("Utenti")
            }

            items(searchState.users) { user ->
                UserSearchCard(
                    user = user,
                    onClick = {
                        navController.navigate(Screens.PublicProfileScreen.createRoute(user.userId))
                    }
                )
            }
        }

        // Empty state
        if (searchState.collections.isEmpty() && searchState.users.isEmpty()) {
            item {
                EmptyResultsView()
            }
        }
    }
}

@Composable
fun CollectionSearchCard(
    collection: UserCollection,
    onCardClick: () -> Unit,
    onUsernameClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Immagine collezione
            AsyncImage(
                model = collection.collectionImageUrl,
                contentDescription = null,
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
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Categoria
            Text(
                text = collection.category ?: "Senza categoria",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Username autore
            Text(
                text = "di @${collection.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onUsernameClick() }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EmptySearchView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🔍",
                fontSize = MaterialTheme.typography.displayLarge.fontSize
            )
            Text(
                text = "Inizia a cercare",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Digita almeno 2 caratteri per cercare collezioni o utenti",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun LoadingSearchView() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
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

@Composable
fun ErrorSearchView(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "❌",
                fontSize = MaterialTheme.typography.displayLarge.fontSize
            )
            Text(
                text = "Errore",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

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
            Text(
                text = "😕",
                fontSize = MaterialTheme.typography.displayLarge.fontSize
            )
            Text(
                text = "Nessun risultato",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Prova con altri termini di ricerca",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}