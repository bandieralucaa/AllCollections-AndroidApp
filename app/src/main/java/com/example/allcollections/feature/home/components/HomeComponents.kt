package com.example.allcollections.feature.home.components

import com.example.allcollections.feature.home.HomeFilter
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
import com.example.allcollections.data.model.CollectionCardLayout
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.components.CollectionCard

/**
 * Componenti UI della HomeScreen.
 *
 * Raggruppa i composable riutilizzabili della home: bottone filtro,
 * dialog di selezione filtro, griglia delle collezioni, vista di
 * caricamento e vista per lista vuota.
 */
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
                FilterOption("Tutte le collezioni", activeFilter == HomeFilter.All, onSelectAll)
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                activeFilter == HomeFilter.Liked ->
                    "Non hai ancora messo like a nessuna collezione"
                activeFilter == HomeFilter.Followed && currentUserId != null ->
                    "Non segui ancora nessun utente con collezioni pubbliche"
                activeFilter == HomeFilter.Followed && currentUserId == null ->
                    "Accedi per vedere le collezioni degli utenti che segui"
                currentUserId != null ->
                    "Nessun'altra collezione pubblica disponibile."
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