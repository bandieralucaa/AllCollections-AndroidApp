package com.example.allcollections.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.allcollections.navigation.Screens
import com.example.allcollections.viewModel.CollectionViewModel
import kotlinx.coroutines.launch

@Composable
fun CollectionDetail(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel
) {
    val collectionState = remember { mutableStateOf<UserCollection?>(null) }
    val itemsState = remember { mutableStateOf<List<CollectionItem>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(collectionId) {
        viewModel.getCollectionById(
            collectionId,
            onSuccess = { collectionState.value = it },
            onFailure = { error ->
                scope.launch { snackbarHostState.showSnackbar("Errore: $error") }
            }
        )
        viewModel.getItemsFromCollection(
            collectionId,
            onSuccess = { itemsState.value = it },
            onFailure = { error ->
                scope.launch { snackbarHostState.showSnackbar("Errore oggetti: $error") }
            }
        )
    }

    val collection = collectionState.value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", modifier = Modifier.size(32.dp))
            }
        }

        item {
            val imageUrl = collection?.collectionImageUrl
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                    contentDescription = "Immagine collezione",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nessuna immagine", color = Color.White)
                }
            }
        }

        item {
            if (collection != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Nome:", style = MaterialTheme.typography.titleMedium)
                    Text(collection.name)
                    Text("Categoria:", style = MaterialTheme.typography.titleMedium)
                    Text(collection.category)
                    Text("Descrizione:", style = MaterialTheme.typography.titleMedium)
                    Text(collection.description)
                }
            } else {
                Text("Caricamento dettagli...", modifier = Modifier.padding(16.dp))
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.Black)
                    .padding(horizontal = 16.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        if (itemsState.value.isEmpty()) {
            item {
                Text("Nessun oggetto presente.", modifier = Modifier.padding(horizontal = 16.dp))
            }
        } else {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 800.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    items(itemsState.value, key = { it.id }) { item ->
                        var expanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .aspectRatio(1f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Opzioni")
                                    }

                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Modifica") },
                                            onClick = {
                                                expanded = false
                                                navController.navigate("editObject/$collectionId/${item.id}")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Elimina") },
                                            onClick = {
                                                expanded = false
                                                viewModel.deleteItemFromCollection(
                                                    collectionId = collectionId,
                                                    itemId = item.id,
                                                    onSuccess = {
                                                        itemsState.value = itemsState.value.filter { it.id != item.id }
                                                        scope.launch { snackbarHostState.showSnackbar("Elemento eliminato") }
                                                    },
                                                    onFailure = { error ->
                                                        scope.launch { snackbarHostState.showSnackbar("Errore eliminazione: $error") }
                                                    }
                                                )
                                            }
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.Bottom,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = "Immagine oggetto",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(MaterialTheme.shapes.medium),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {
                    navController.navigate("${Screens.AddObjectCollection.name}/$collectionId")
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi oggetto", modifier = Modifier.size(48.dp))
                }
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}
