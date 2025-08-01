package com.example.allcollections.collection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.viewModel.CollectionViewModel
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import coil.compose.rememberAsyncImagePainter

@Composable
fun EditCollection(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel
) {
    val collectionState = remember { mutableStateOf<UserCollection?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> newImageUri = uri }

    val scrollState = rememberScrollState()

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

    val collection = collectionState.value
    if (collection != null) {
        var name by remember { mutableStateOf(collection.name) }
        var category by remember { mutableStateOf(collection.category ?: "") }
        var description by remember { mutableStateOf(collection.description ?: "") }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
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
                    var updateSuccess = false
                    var imageSuccess = true

                    try {
                        // Aggiorna i dati della collezione
                        viewModel.updateCollection(
                            updatedCollection = collection.copy(
                                name = name,
                                category = category,
                                description = description
                            ),
                            onSuccess = {
                                updateSuccess = true
                            },
                            onFailure = { error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Errore salvataggio: $error")
                                }
                            }
                        )

                        // Se è stata scelta una nuova immagine, aggiorna anche quella
                        if (newImageUri != null) {
                            viewModel.updateCollectionImage(
                                collectionId = collection.id,
                                newImageUri = newImageUri!!,
                                onSuccess = {
                                    imageSuccess = true
                                },
                                onFailure = { error ->
                                    imageSuccess = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Errore immagine: $error")
                                    }
                                }
                            )
                        }

                        // Se tutto ok, torna indietro
                        if (updateSuccess && imageSuccess) {
                            navController.popBackStack()
                            // opzionale: mostra snackbar nella pagina precedente
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

    SnackbarHost(hostState = snackbarHostState)
}
