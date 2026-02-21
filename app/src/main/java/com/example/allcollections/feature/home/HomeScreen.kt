package com.example.allcollections.feature.home

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
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.collection.components.CollectionCard
import com.example.allcollections.feature.collection.components.CollectionCardLayout
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.example.allcollections.feature.profile.ProfileViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.koin.androidx.compose.koinViewModel

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
                actions = { FilterButton(activeFilter = activeFilter) { showFilterDialog = true } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

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
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun FilterButton(activeFilter: HomeFilter, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.FilterList, contentDescription = "Filtra collezioni")
        }
        if (activeFilter != HomeFilter.All) {
            Badge(modifier = Modifier.offset(x = (-8).dp, y = 8.dp))
        }
    }
}

@Composable
fun FilterDialog(
    activeFilter: HomeFilter,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectFollowed: () -> Unit,
    onSelectLiked: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtra collezioni") },
        text = {
            Column {
                FilterOption("Tutte le collezioni pubbliche", activeFilter == HomeFilter.All, onSelectAll)
                FilterOption("Solo utenti seguiti", activeFilter == HomeFilter.Followed, onSelectFollowed)
                FilterOption("Collezioni preferite", activeFilter == HomeFilter.Liked, onSelectLiked)
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
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun CollectionsGrid(
    collections: List<UserCollection>,
    navController: NavController,
    likedMap: Map<String, Boolean>,
    likesCountMap: Map<String, Int>,
    onLikeClick: (UserCollection) -> Unit
) {
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
                hasLiked = likedMap[collection.id] ?: false,
                likesCount = likesCountMap[collection.id] ?: 0,
                onLikeClick = { onLikeClick(collection) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Caricamento collezioni...")
        }
    }
}

@Composable
fun EmptyView(activeFilter: HomeFilter, currentUserId: String?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            val message = when {
                activeFilter == HomeFilter.Liked -> "Non hai ancora messo like a nessuna collezione"
                activeFilter == HomeFilter.Followed && currentUserId != null -> "Non segui ancora nessun utente con collezioni pubbliche"
                activeFilter == HomeFilter.Followed && currentUserId == null -> "Accedi per vedere le collezioni degli utenti che segui"
                currentUserId != null -> "Nessun'altra collezione pubblica disponibile."
                else -> "Nessuna collezione pubblica disponibile"
            }
            Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
    }
}

private fun loadAllCollections(viewModel: CollectionViewModel, onSuccess: (List<UserCollection>) -> Unit) {
    viewModel.getAllCollectionsWithUsernames(onSuccess = onSuccess, onFailure = {})
}

private fun loadFollowedCollections(
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