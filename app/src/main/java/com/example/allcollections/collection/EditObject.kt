package com.example.allcollections.collection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.allcollections.viewModel.CollectionViewModel
import kotlinx.coroutines.launch

@Composable
fun EditObject(
    navController: NavController,
    itemId: String,
    collectionId: String,
    viewModel: CollectionViewModel
) {
    val itemState = remember { mutableStateOf<CollectionItem?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        newImageUri = uri
    }

    LaunchedEffect(itemId) {
        viewModel.getItemById(
            collectionId = collectionId,
            itemId = itemId,
            onSuccess = { itemState.value = it },
            onFailure = { error ->
                scope.launch {
                    snackbarHostState.showSnackbar("Errore caricamento: $error")
                }
            }
        )
    }

    val item = itemState.value
    if (item != null) {
        var description by remember { mutableStateOf(item.description) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Modifica descrizione:")
            TextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("Scegli nuova immagine")
            }

            newImageUri?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            } ?: item.imageUrl?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                // Aggiorna descrizione
                viewModel.updateItemDescription(
                    collectionId = collectionId,
                    itemId = item.id,
                    newDescription = description,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Descrizione aggiornata")
                        }
                    },
                    onFailure = { error ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Errore salvataggio: $error")
                        }
                    }
                )

                newImageUri?.let { uri ->
                    viewModel.updateItemImage(
                        collectionId = collectionId,
                        itemId = item.id,
                        newImageUri = uri,
                        onSuccess = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Immagine aggiornata")
                                navController.popBackStack()
                            }
                        },
                        onFailure = { error ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Errore immagine: $error")
                            }
                        }
                    )
                } ?: scope.launch {
                    navController.popBackStack()
                }

            }) {
                Text("Salva modifiche")
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}
