package com.example.allcollections.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.home.components.*
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.example.allcollections.feature.profile.ProfileViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.koin.androidx.compose.koinViewModel

/**
 * Schermata principale dell'app (feed delle collezioni pubbliche).
 *
 * Mostra una griglia a due colonne di collezioni, con supporto a tre filtri:
 * - [HomeFilter.All] – tutte le collezioni pubbliche (escluse quelle dell'utente corrente).
 * - [HomeFilter.Followed] – solo collezioni create da utenti che l'utente corrente segue.
 * - [HomeFilter.Liked] – solo collezioni a cui l'utente corrente ha messo like.
 *
 * I like sono gestiti ottimisticamente:
 * - L'UI aggiorna immediatamente lo stato (like/dislike e contatore).
 * - La richiesta a Firestore avviene in background.
 * - Se il filtro attivo è [HomeFilter.Liked] e l'utente rimuove un like, la collezione
 *   scompare automaticamente dalla lista.
 *
 * @param navController Controller per la navigazione verso i dettagli delle collezioni.
 * @param collectionViewModel ViewModel per operazioni su collezioni (like, caricamento).
 * @param profileViewModel ViewModel per recuperare gli ID degli utenti seguiti.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    collectionViewModel: CollectionViewModel,
    profileViewModel: ProfileViewModel
) {
    val notificationViewModel: NotificationViewModel = koinViewModel()
    val currentUserId = Firebase.auth.currentUser?.uid

    var activeFilter by remember { mutableStateOf(HomeFilter.All) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filteredCollections by remember { mutableStateOf(emptyList<UserCollection>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Mappe per mantenere stato like e conteggi (aggiornamento ottimistico)
    val likedMap = remember { mutableStateMapOf<String, Boolean>() }
    val likesCountMap = remember { mutableStateMapOf<String, Int>() }

    /**
     * Carica lo stato like e il conteggio like per una lista di collezioni.
     * Se i dati sono già presenti nelle mappe, evita di ricaricarli.
     */
    fun loadLikesForCollections(collections: List<UserCollection>) {
        collections.forEach { collection ->
            if (!likedMap.containsKey(collection.id)) {
                collectionViewModel.hasLiked(collection.id) { likedMap[collection.id] = it }
                collectionViewModel.getLikesCount(collection.id) { likesCountMap[collection.id] = it }
            }
        }
    }

    // Ricarica le collezioni ogni volta che cambia il filtro o l'utente corrente
    LaunchedEffect(activeFilter, currentUserId) {
        isLoading = true
        when (activeFilter) {
            HomeFilter.Followed -> {
                if (currentUserId != null) {
                    // Carica le collezioni degli utenti seguiti (esclude quelle dell'utente stesso)
                    loadFollowedCollections(profileViewModel, collectionViewModel) { collections ->
                        filteredCollections = collections.filter { it.iduser != currentUserId }
                        loadLikesForCollections(filteredCollections)
                        isLoading = false
                    }
                } else {
                    filteredCollections = emptyList()
                    isLoading = false
                }
            }
            HomeFilter.Liked -> {
                if (currentUserId != null) {
                    // Carica le collezioni a cui l'utente ha messo like
                    collectionViewModel.getLikedCollections { collections ->
                        filteredCollections = collections
                        loadLikesForCollections(filteredCollections)
                        isLoading = false
                    }
                } else {
                    filteredCollections = emptyList()
                    isLoading = false
                }
            }
            HomeFilter.All -> {
                // Carica tutte le collezioni pubbliche, escludendo quelle dell'utente
                loadAllCollections(collectionViewModel) { collections ->
                    filteredCollections = if (currentUserId != null) {
                        collections.filter { it.iduser != currentUserId }
                    } else collections
                    loadLikesForCollections(filteredCollections)
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            MyTopBar(
                navController = navController,
                showBackButton = false,
                title = when (activeFilter) {
                    HomeFilter.All -> "Collezioni pubbliche"
                    HomeFilter.Followed -> "Collezioni seguite"
                    HomeFilter.Liked -> "Collezioni con like"
                },
                actions = {
                    FilterButton(activeFilter = activeFilter) { showFilterDialog = true }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screens.AddCollectionScreen.route) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crea nuova collezione")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                // Dialog per la selezione del filtro
                if (showFilterDialog) {
                    FilterDialog(
                        activeFilter = activeFilter,
                        onDismiss = { showFilterDialog = false },
                        onSelectAll = { activeFilter = HomeFilter.All; showFilterDialog = false },
                        onSelectFollowed = { activeFilter = HomeFilter.Followed; showFilterDialog = false },
                        onSelectLiked = { activeFilter = HomeFilter.Liked; showFilterDialog = false }
                    )
                }

                when {
                    isLoading -> LoadingView()
                    filteredCollections.isEmpty() -> EmptyView(activeFilter, currentUserId)
                    else -> CollectionsGrid(
                        collections = filteredCollections,
                        navController = navController,
                        likedMap = likedMap,
                        likesCountMap = likesCountMap,
                        onLikeClick = { collection ->
                            val wasLiked = likedMap[collection.id] ?: false
                            if (wasLiked) {
                                likedMap[collection.id] = false
                                likesCountMap[collection.id] = (likesCountMap[collection.id] ?: 1) - 1
                                collectionViewModel.unlikeCollection(collection.id)
                            } else {
                                likedMap[collection.id] = true
                                likesCountMap[collection.id] = (likesCountMap[collection.id] ?: 0) + 1
                                collectionViewModel.likeCollection(collection.id, notificationViewModel)
                            }
                            if (activeFilter == HomeFilter.Liked && wasLiked) {
                                filteredCollections = filteredCollections.filter { it.id != collection.id }
                            }
                        }
                    )
                }

            }

            // Contatore in basso (solo se ci sono risultati)
            if (!isLoading && filteredCollections.isNotEmpty()) {
                Text(
                    text = "Mostrando ${filteredCollections.size} collezioni",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}

/**
 * Filtri disponibili per la lista delle collezioni nella [HomeScreen].
 */
enum class HomeFilter {
    /** Tutte le collezioni pubbliche (escluse le proprie). */
    All,
    /** Solo collezioni di utenti seguiti. */
    Followed,
    /** Solo collezioni a cui l'utente ha messo like. */
    Liked
}