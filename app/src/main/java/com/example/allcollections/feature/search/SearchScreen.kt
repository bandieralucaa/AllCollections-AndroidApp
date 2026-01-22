package com.example.allcollections.feature.search

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
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.CollectionViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * SearchPage
 *
 * Schermata di ricerca delle collezioni pubbliche.
 *
 * Funzionalità:
 * - Carica tutte le collezioni (escludendo quelle dell’utente corrente)
 * - Permette la ricerca per nome collezione
 * - Visualizza i risultati in una griglia
 * - Navigazione verso:
 *   - dettaglio collezione
 *   - profilo pubblico dell’autore
 */
@Composable
fun SearchScreen(
    viewModel: CollectionViewModel,
    navController: NavController
) {
    // Stato UI
    val allCollections = remember { mutableStateOf<List<UserCollection>>(emptyList()) }
    val searchQuery = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    val currentUserId = Firebase.auth.currentUser?.uid

    /**
     * Caricamento iniziale delle collezioni pubbliche
     */
    LaunchedEffect(Unit) {
        viewModel.getAllCollectionsWithUsernames(
            onSuccess = { collections ->
                // Escludo le collezioni dell'utente loggato
                allCollections.value = collections.filter {
                    it.iduser != currentUserId
                }
            },
            onFailure = { error ->
                errorMessage.value = error
            }
        )
    }

    /**
     * Filtro in base al testo di ricerca
     */
    val filteredCollections = allCollections.value.filter {
        it.name.contains(searchQuery.value, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // Barra di ricerca
        SearchBar(
            query = searchQuery.value,
            onQueryChange = { searchQuery.value = it },
            onClear = { searchQuery.value = "" }
        )

        // Messaggio errore (se presente)
        errorMessage.value?.let {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Column
        }

        // Nessun risultato
        if (filteredCollections.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessuna collezione trovata",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {

            // Griglia risultati
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(filteredCollections.size) { index ->
                    val collection = filteredCollections[index]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate(Screens.CollectionDetailScreen.createRoute(collection.id))
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {

                            // Immagine collezione
                            AsyncImage(
                                model = collection.collectionImageUrl,
                                contentDescription = "Immagine collezione",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Nome collezione
                            Text(
                                text = collection.name,
                                style = MaterialTheme.typography.titleSmall
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Username autore
                            Text(
                                text = "Creato da: ${collection.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    navController.navigate(Screens.PublicProfileScreen.createRoute(collection.iduser))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
