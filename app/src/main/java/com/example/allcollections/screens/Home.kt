package com.example.allcollections.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.viewModel.CollectionViewModel
import com.example.allcollections.viewModel.ProfileViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    navController: NavController,
    viewModel: CollectionViewModel,
    profileViewModel: ProfileViewModel
) {
    val allCollections = remember { mutableStateOf(emptyList<UserCollection>()) }
    val currentUserId = Firebase.auth.currentUser?.uid
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val currentError by rememberUpdatedState(errorMessage.value)
    var showOnlyFollowed by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showOnlyFollowed) {
        if (showOnlyFollowed && currentUserId != null) {
            profileViewModel.getFollowedUserIds { followedIds ->
                viewModel.getCollectionsByUserIds(followedIds) { filtered ->
                    allCollections.value = filtered
                }
            }
        } else {
            viewModel.getAllCollectionsWithUsernames(
                onSuccess = { allCollections.value = it },
                onFailure = { errorMessage.value = it }
            )
        }
    }

    LaunchedEffect(currentError) {
        currentError?.let {
            snackbarHostState.showSnackbar("Errore: $it")
            errorMessage.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collezioni degli altri utenti") },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Filtri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filtra collezioni") },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !showOnlyFollowed,
                                onClick = {
                                    showOnlyFollowed = false
                                    showFilterDialog = false
                                }
                            )
                            Text("Tutte le collezioni")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = showOnlyFollowed,
                                onClick = {
                                    showOnlyFollowed = true
                                    showFilterDialog = false
                                }
                            )
                            Text("Solo seguiti")
                        }
                    }
                },
                confirmButton = {
                    Text("Chiudi", modifier = Modifier.clickable { showFilterDialog = false })
                }
            )
        }

        if (allCollections.value.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nessuna collezione disponibile", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allCollections.value.size) { index ->
                    val collection = allCollections.value[index]
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                navController.navigate("collectionDetail/${collection.id}")
                            },
                        elevation = CardDefaults.cardElevation()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AsyncImage(
                                model = collection.collectionImageUrl + "?t=${System.currentTimeMillis()}",
                                contentDescription = "Immagine collezione",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )

                            Text(
                                text = collection.name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Creato da: ${collection.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    navController.navigate("publicProfile/${collection.iduser}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}