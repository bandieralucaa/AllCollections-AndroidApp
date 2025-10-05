package com.example.allcollections.collection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.viewModel.CollectionViewModel
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import coil.compose.rememberAsyncImagePainter
import com.example.allcollections.navigation.MyTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCollection(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel
) {
    val collectionState = remember { mutableStateOf<UserCollection?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    val collection = collectionState.value

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> newImageUri = uri }

    LaunchedEffect(collectionId) {
        viewModel.getCollectionById(
            collectionId = collectionId,
            onSuccess = { collectionState.value = it },
            onFailure = { error ->
                scope.launch {
                    snackbarHostState.showSnackbar("Errore: $error")
                }
            }
        )
    }

    if (collection != null) {
        var name by remember { mutableStateOf(collection.name) }
        var category by remember { mutableStateOf(collection.category ?: "") }
        var description by remember { mutableStateOf(collection.description ?: "") }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                MyTopBar(navController = navController)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Modifica collezione", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("Scegli nuova immagine")
                }

                val imagePainter = when {
                    newImageUri != null -> rememberAsyncImagePainter(newImageUri)
                    collection.collectionImageUrl != null -> rememberAsyncImagePainter(collection.collectionImageUrl)
                    else -> null
                }

                imagePainter?.let {
                    Image(
                        painter = it,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }

                Button(onClick = {
                    scope.launch {
                        val hasFieldChanges = name != collection.name ||
                                category != (collection.category ?: "") ||
                                description != (collection.description ?: "")
                        val hasImageChange = newImageUri != null


                        try {
                            if (hasFieldChanges) {
                                viewModel.updateCollection(
                                    updatedCollection = collection.copy(
                                        name = name,
                                        category = category,
                                        description = description
                                    ),
                                    onSuccess = { /* ok */ },
                                    onFailure = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Errore salvataggio: $error")
                                        }
                                    }
                                )
                            }

                            if (hasImageChange) {
                                viewModel.updateCollectionImage(
                                    collectionId = collection.id,
                                    newImageUri = newImageUri!!,
                                    onSuccess = { /* ok */ },
                                    onFailure = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Errore immagine: $error")
                                        }
                                    }
                                )
                            }

                            navController.navigate("collectionDetail/${collection.id}") {
                                popUpTo("editCollection/${collection.id}") { inclusive = true }
                            }

                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Errore imprevisto: ${e.message}")
                            }
                        }
                    }
                }) {
                    Text("Salva modifiche")
                }

            }
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}
