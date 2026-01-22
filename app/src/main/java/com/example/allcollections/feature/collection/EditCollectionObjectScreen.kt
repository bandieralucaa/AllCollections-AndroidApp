package com.example.allcollections.feature.collection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.allcollections.data.model.CollectionItem
import kotlinx.coroutines.launch

/**
 * Schermata per modificare un oggetto di una collezione.
 *
 * Permette di aggiornare la descrizione e/o sostituire l'immagine.
 * Gestisce upload su Cloudinary tramite CollectionViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCollectionObjectScreen(
    navController: NavController,
    collectionId: String,
    itemId: String,
    viewModel: CollectionViewModel
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var item by remember { mutableStateOf<CollectionItem?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Launcher per selezionare nuova immagine
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    // Carica l'item all'avvio
    LaunchedEffect(itemId) {
        viewModel.getItemById(
            collectionId = collectionId,
            itemId = itemId,
            onSuccess = { item = it },
            onFailure = { error ->
                scope.launch { snackbarHostState.showSnackbar("Errore caricamento: $error") }
            }
        )
    }

    item?.let { currentItem ->
        var description by remember { mutableStateOf(currentItem.description ?: "") }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Modifica oggetto") }) },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Campo descrizione
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Anteprima immagine
                Text("Immagine attuale:", style = MaterialTheme.typography.labelMedium)
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri ?: currentItem.imageUrl),
                    contentDescription = "Anteprima immagine",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                // Pulsante per scegliere nuova immagine
                Button(
                    onClick = { imageLauncher.launch("image/*") },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Scegli nuova immagine")
                }

                Spacer(Modifier.height(24.dp))

                // Pulsante salva modifiche
                Button(
                    onClick = {
                        isSaving = true
                        val uriToUpload = selectedImageUri
                        if (uriToUpload != null) {
                            // Aggiorna immagine + descrizione
                            viewModel.uploadItemImageAndUpdate(
                                collectionId,
                                itemId,
                                uriToUpload,
                                description,
                                onSuccess = {
                                    isSaving = false
                                    scope.launch { snackbarHostState.showSnackbar("Oggetto aggiornato") }
                                    navController.popBackStack()
                                },
                                onFailure = { error ->
                                    isSaving = false
                                    scope.launch { snackbarHostState.showSnackbar("Errore: $error") }
                                }
                            )
                        } else {
                            // Aggiorna solo descrizione
                            viewModel.updateItemDescription(
                                collectionId,
                                itemId,
                                description,
                                onSuccess = {
                                    isSaving = false
                                    scope.launch { snackbarHostState.showSnackbar("Descrizione aggiornata") }
                                    navController.popBackStack()
                                },
                                onFailure = { error ->
                                    isSaving = false
                                    scope.launch { snackbarHostState.showSnackbar("Errore: $error") }
                                }
                            )
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Salva modifiche")
                    }
                }
            }
        }
    }

    // Snackbar globale
    SnackbarHost(hostState = snackbarHostState)
}
