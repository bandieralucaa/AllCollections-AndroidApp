package com.example.allcollections.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.collection.components.CollectionCard
import com.example.allcollections.feature.collection.components.CollectionCardLayout
import com.example.allcollections.feature.profile.ProfileViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * HomeScreen: mostra tutte le collezioni o solo quelle degli utenti seguiti.
 * - Filtro collezioni con dialog
 * - Gestione caricamento e stato vuoto
 * - Navigazione a dettaglio collezione e profilo utente
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    collectionViewModel: CollectionViewModel,
    profileViewModel: ProfileViewModel
) {
    // Stati UI locali
    var showOnlyFollowed by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId = Firebase.auth.currentUser?.uid

    // StateFlow dalle collezioni filtrate
    var allCollections by remember { mutableStateOf(emptyList<UserCollection>()) }
    var filteredCollections by remember { mutableStateOf(emptyList<UserCollection>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Caricamento dati quando cambia filtro o utente
    LaunchedEffect(showOnlyFollowed, currentUserId) {
        isLoading = true

        if (showOnlyFollowed && currentUserId != null) {
            loadFollowedCollections(profileViewModel, collectionViewModel) { collections ->
                allCollections = collections
                // FILTRA: rimuovi le tue collezioni anche dai seguiti
                filteredCollections = collections.filter { it.iduser != currentUserId }
                isLoading = false
            }
        } else {
            loadAllCollections(collectionViewModel) { collections ->
                allCollections = collections
                // FILTRA: rimuovi le tue collezioni dalla lista generale
                filteredCollections = if (currentUserId != null) {
                    collections.filter { it.iduser != currentUserId }
                } else {
                    collections // Se non loggato, mostra tutto
                }
                isLoading = false
            }
        }
    }

    // Scaffold principale
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showOnlyFollowed) "Collezioni seguite" else "Collezioni pubbliche",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    FilterButton(showOnlyFollowed = showOnlyFollowed) {
                        showFilterDialog = true
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Dialog filtro collezioni
            if (showFilterDialog) {
                FilterDialog(
                    showOnlyFollowed = showOnlyFollowed,
                    onDismiss = { showFilterDialog = false },
                    onSelectAll = { showOnlyFollowed = false; showFilterDialog = false },
                    onSelectFollowed = { showOnlyFollowed = true; showFilterDialog = false }
                )
            }

            // Contenuto principale
            when {
                isLoading -> LoadingView()
                filteredCollections.isEmpty() -> EmptyView(showOnlyFollowed, currentUserId)
                else -> CollectionsGrid(filteredCollections, navController)
            }

            // Contatore collezioni (debug/informativo)
            if (!isLoading && filteredCollections.isNotEmpty()) {
                Text(
                    text = "Mostrando ${filteredCollections.size} collezioni",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}

/** Pulsante filtro con badge se attivo */
@Composable
fun FilterButton(showOnlyFollowed: Boolean, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.FilterList, contentDescription = "Filtra collezioni")
        }
        if (showOnlyFollowed) {
            Badge(modifier = Modifier.offset(x = (-8).dp, y = 8.dp))
        }
    }
}

/** Dialog per scelta filtro */
@Composable
fun FilterDialog(
    showOnlyFollowed: Boolean,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectFollowed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtra collezioni") },
        text = {
            Column {
                FilterOption("Tutte le collezioni pubbliche", !showOnlyFollowed, onSelectAll)
                FilterOption("Solo utenti seguiti", showOnlyFollowed, onSelectFollowed)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}

@Composable
fun FilterOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Griglia delle collezioni */
@Composable
fun CollectionsGrid(collections: List<UserCollection>, navController: NavController) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(collections) { collection ->
            CollectionCard(
                collection = collection,
                layoutType = CollectionCardLayout.Vertical,
                showMenu = false,
                onCardClick = { collectionId ->
                    navController.navigate(Screens.CollectionDetailScreen.createRoute(collectionId))
                },
                onUsernameClick = { userId ->
                    navController.navigate(Screens.PublicProfileScreen.createRoute(userId))
                },
                onMyProfileClick = {
                    navController.navigate(Screens.ProfileScreen.route)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** View loading */
@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Caricamento collezioni...")
        }
    }
}

/** View per stato vuoto */
@Composable
fun EmptyView(showOnlyFollowed: Boolean, currentUserId: String?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            val message = when {
                showOnlyFollowed && currentUserId != null ->
                    "Non segui ancora nessun utente con collezioni pubbliche"
                showOnlyFollowed && currentUserId == null ->
                    "Accedi per vedere le collezioni degli utenti che segui"
                currentUserId != null ->
                    "Nessun'altra collezione pubblica disponibile. Le tue collezioni non sono visibili qui."
                else ->
                    "Nessuna collezione pubblica disponibile"
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** --- Funzioni helper per caricamento collezioni --- */
private fun loadAllCollections(
    viewModel: CollectionViewModel,
    onSuccess: (List<UserCollection>) -> Unit
) {
    viewModel.getAllCollectionsWithUsernames(onSuccess = onSuccess, onFailure = { /* Ignora errori */ })
}

private fun loadFollowedCollections(
    profileViewModel: ProfileViewModel,
    collectionViewModel: CollectionViewModel,
    onResult: (List<UserCollection>) -> Unit
) {
    profileViewModel.getFollowedUserIds { followedIds ->
        if (followedIds.isNotEmpty()) {
            collectionViewModel.getCollectionsByUserIds(followedIds) { collections ->
                onResult(collections)
            }
        } else {
            onResult(emptyList())
        }
    }
}