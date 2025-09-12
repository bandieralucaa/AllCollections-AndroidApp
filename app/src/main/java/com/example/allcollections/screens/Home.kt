package com.example.allcollections.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(navController: NavController, viewModel: CollectionViewModel) {
    val allCollections = remember { mutableStateOf(emptyList<UserCollection>()) }
    val currentUserId = Firebase.auth.currentUser?.uid
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.getAllCollectionsWithUsernames(
            onSuccess = { collections ->
                allCollections.value = collections
            },
            onFailure = { error -> errorMessage.value = error }
        )
    }

    LaunchedEffect(errorMessage.value) {
        errorMessage.value?.let { error ->
            snackbarHostState.showSnackbar("Errore: $error")
            errorMessage.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collezioni degli altri utenti") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
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
                allCollections.value.forEach { collection ->
                    item {
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
}