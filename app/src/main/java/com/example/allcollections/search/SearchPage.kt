package com.example.allcollections.search

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
import com.example.allcollections.collection.UserCollection
import com.example.allcollections.viewModel.CollectionViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun SearchPage(viewModel: CollectionViewModel, navController: NavController) {
    val allCollections = remember { mutableStateOf(emptyList<UserCollection>()) }
    val searchQuery = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    val currentUserId = Firebase.auth.currentUser?.uid

    // Recupera tutte le collezioni degli altri utenti
    LaunchedEffect(Unit) {
        viewModel.getAllCollectionsWithUsernames(
            onSuccess = { collections ->
                allCollections.value = collections.filter { it.iduser != currentUserId }
            },
            onFailure = { error -> errorMessage.value = error }
        )
    }

    val filteredCollections = allCollections.value.filter {
        it.name.contains(searchQuery.value, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = searchQuery.value,
            onQueryChange = { searchQuery.value = it },
            onClear = { searchQuery.value = "" }
        )

        if (filteredCollections.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Nessuna collezione trovata", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredCollections.forEach { collection ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable{
                                    navController.navigate("collectionDetail/${collection.id}")
                                },
                            elevation = CardDefaults.cardElevation()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                AsyncImage(
                                    model = collection.collectionImageUrl + "?t=${System.currentTimeMillis()}",
                                    contentDescription = "Immagine collezione",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = collection.name,
                                    style = MaterialTheme.typography.titleSmall
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