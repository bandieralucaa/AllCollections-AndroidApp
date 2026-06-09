package com.example.allcollections.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.home.components.CollectionsGrid
import com.example.allcollections.feature.home.components.EmptyView
import com.example.allcollections.feature.home.components.FilterButton
import com.example.allcollections.feature.home.components.FilterDialog
import com.example.allcollections.feature.home.components.LoadingView
import com.example.allcollections.feature.home.components.loadAllCollections
import com.example.allcollections.feature.home.components.loadFollowedCollections
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.example.allcollections.feature.profile.ProfileViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.koin.androidx.compose.koinViewModel

/**
 * Schermata principale dell'app.
 *
 * Mostra le collezioni pubbliche in una griglia a due colonne, con supporto
 * per tre filtri: tutte, solo utenti seguiti, solo con like. Gestisce
 * il caricamento, lo stato vuoto e le interazioni like in tempo reale.
 */

/**
 * Schermata principale dell'app (feed collezioni pubbliche).
 *
 * Mostra le collezioni in una griglia a due colonne con tre filtri selezionabili:
 * - [HomeFilter.All] — tutte le collezioni (escluse le proprie).
 * - [HomeFilter.Followed] — solo collezioni di utenti seguiti.
 * - [HomeFilter.Liked] — solo collezioni a cui si è messo like.
 *
 * I like vengono aggiornati ottimisticamente (UI immediata, Firestore in background).
 * Se il filtro è [HomeFilter.Liked] e si rimuove un like, la collezione scompare dalla lista.
 *
 * @param navController NavController per la navigazione.
 * @param collectionViewModel ViewModel per like e caricamento collezioni.
 * @param profileViewModel ViewModel per recuperare gli ID degli utenti seguiti.
 */

/** Filtri disponibili per la lista delle collezioni nella HomeScreen. */
enum class HomeFilter { All, Followed, Liked }

@Composable
fun HomeScreen(
    navController: NavController,
    collectionViewModel: CollectionViewModel,
    profileViewModel: ProfileViewModel
) {
    val notificationViewModel: NotificationViewModel = koinViewModel()

    var activeFilter by remember { mutableStateOf(HomeFilter.All) }
    var showFilterDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId = Firebase.auth.currentUser?.uid

    var filteredCollections by remember { mutableStateOf(emptyList<UserCollection>()) }
    var isLoading by remember { mutableStateOf(true) }

    val likedMap = remember { mutableStateMapOf<String, Boolean>() }
    val likesCountMap = remember { mutableStateMapOf<String, Int>() }

    fun loadLikesForCollections(collections: List<UserCollection>) {
        collections.forEach { collection ->
            if (!likedMap.containsKey(collection.id)) {
                collectionViewModel.hasLiked(collection.id) { likedMap[collection.id] = it }
                collectionViewModel.getLikesCount(collection.id) { likesCountMap[collection.id] = it }
            }
        }
    }

    LaunchedEffect(activeFilter, currentUserId) {
        isLoading = true
        when (activeFilter) {
            HomeFilter.Followed -> {
                if (currentUserId != null) {
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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