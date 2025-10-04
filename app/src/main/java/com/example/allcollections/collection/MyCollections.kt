package com.example.allcollections.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.navigation.MyTopBar
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
    val currentError by rememberUpdatedState(errorMessage.value)

    LaunchedEffect(currentError) {
        currentError?.let {
            snackbarHostState.showSnackbar("Errore: $it")
            errorMessage.value = null
        }
    }

    LaunchedEffect(navController.currentBackStackEntry) {
        viewModel.getCollections(
            iduser,
            onSuccess = { collections.value = it },
            onFailure = { errorMessage.value = it }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState)},
        topBar = {
            MyTopBar(navController = navController)
        }
    ) { padding ->
        if (collections.value.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Non hai ancora nessuna collezione,\ncreane una!", style = MaterialTheme.typography.bodyLarge)
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
                items(collections.value.size) { index ->
                    val collection = collections.value[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                navController.navigate("collectionDetail/${collection.id}")
                            },
                        elevation = CardDefaults.cardElevation()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = collection.collectionImageUrl + "?t=${System.currentTimeMillis()}",
                                contentDescription = "Immagine collezione",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
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