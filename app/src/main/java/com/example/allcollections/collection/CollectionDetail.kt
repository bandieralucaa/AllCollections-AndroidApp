package com.example.allcollections.collection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.navigation.Screens
import com.example.allcollections.viewModel.CollectionViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetail(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel
) {
    val collection = remember { mutableStateOf<UserCollection?>(null) }
    val objects = remember { mutableStateOf(emptyList<CollectionItem>()) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId = Firebase.auth.currentUser?.uid
    val isOwner = collection.value?.iduser == currentUserId

    LaunchedEffect(collectionId) {
        viewModel.getCollectionById(
            collectionId,
            onSuccess = { collection.value = it },
            onFailure = { errorMessage.value = it }
        )

        viewModel.getItemsFromCollection(
            collectionId,
            onSuccess = { objects.value = it },
            onFailure = { errorMessage.value = it }
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
                title = { Text(collection.value?.name ?: "Dettagli collezione") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isOwner) {
                FloatingActionButton(onClick = {
                    navController.navigate("${Screens.AddObjectCollection.name}/$collectionId")
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi oggetto")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                AsyncImage(
                    model = collection.value?.collectionImageUrl,
                    contentDescription = "Immagine collezione",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }

            item {
                Text(
                    text = "Categoria: ${collection.value?.category ?: "Nessuna"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                Text(
                    text = "Descrizione: ${collection.value?.description ?: "Nessuna"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (objects.value.isEmpty()) {
                item {
                    Text(
                        text = "Nessun oggetto presente in questa collezione.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(objects.value.size) { index ->
                    val obj = objects.value[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = obj.imageUrl + "?t=${System.currentTimeMillis()}",
                                contentDescription = "Immagine oggetto",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = obj.description ?: "Nessuna descrizione",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}