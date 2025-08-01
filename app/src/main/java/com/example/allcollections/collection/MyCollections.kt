package com.example.allcollections.collection

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.example.allcollections.navigation.Screens
import com.example.allcollections.viewModel.CollectionViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCollections(navController: NavController, viewModel: CollectionViewModel) {
    val collections = remember { mutableStateOf(emptyList<UserCollection>()) }
    val iduser = Firebase.auth.currentUser?.uid
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()


    LaunchedEffect(errorMessage.value) {
        errorMessage.value?.let { error ->
            snackbarHostState.showSnackbar("Errore: $error")
            errorMessage.value = null
        }
    }

    LaunchedEffect(navController.currentBackStackEntry) {
        viewModel.getCollections(iduser,
            onSuccess = { collections.value = it },
            onFailure = { error -> errorMessage.value = error }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Le mie Collezioni") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            collections.value.forEach { collection ->
                item {
                    val showMenu = remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            elevation = CardDefaults.cardElevation()
                        ) {
                            AsyncImage(
                                model = collection?.collectionImageUrl + "?t=${System.currentTimeMillis()}",
                                contentDescription = "Immagine collezione",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                        }

                        var expanded by remember { mutableStateOf(false) }

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
                                        navController.navigate("editCollection/${collection.id}")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Elimina") },
                                    onClick = {
                                        expanded = false
                                        viewModel.deleteCollection(
                                            collection.id,
                                            onSuccess = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Collezione eliminata")
                                                }
                                                viewModel.getCollections(iduser,
                                                    onSuccess = { collections.value = it },
                                                    onFailure = { error -> errorMessage.value = error }
                                                )
                                            },
                                            onFailure = { error -> errorMessage.value = error }
                                        )
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
