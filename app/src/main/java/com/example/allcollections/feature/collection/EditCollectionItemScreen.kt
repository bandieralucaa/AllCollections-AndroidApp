package com.example.allcollections.feature.collection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.data.model.CollectionItem
import kotlinx.coroutines.launch

/**
 * Schermata per modificare un oggetto di una collezione.
 *
 * Permette di aggiornare la descrizione e/o sostituire l'immagine.
 * Gestisce l'upload dell'immagine su Cloudinary tramite CollectionViewModel.
 *
 * @param navController Controller per la navigazione tra schermate
 * @param collectionId ID della collezione a cui appartiene l'oggetto
 * @param itemId ID dell'oggetto da modificare
 * @param viewModel ViewModel della collezione, gestisce operazioni su Firestore e Cloudinary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCollectionItemScreen(
    navController: NavController,
    collectionId: String,
    itemId: String,
    viewModel: CollectionViewModel
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var item by remember { mutableStateOf<CollectionItem?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Launcher per selezionare una nuova immagine dall'archivio
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    // Carica l'item quando la schermata viene aperta
    LaunchedEffect(itemId) {
        viewModel.getItemById(
            collectionId,
            itemId,
            onSuccess = {
                item = it
                description = it.description ?: ""
            },
            onFailure = { error ->
                scope.launch { snackbarHostState.showSnackbar("Errore caricamento: $error") }
            }
        )
    }

    item?.let { currentItem ->
        Scaffold(
            topBar = { TopAppBar(title = { Text("Modifica oggetto") }) },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Campo per modificare la descrizione
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Anteprima immagine corrente o selezionata
                Text("Immagine attuale:", style = MaterialTheme.typography.labelMedium)
                AsyncImage(
                    model = selectedImageUri ?: currentItem.imageUrl,
                    contentDescription = "Anteprima immagine",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                // Pulsante per scegliere una nuova immagine
                Button(
                    onClick = { imageLauncher.launch("image/*") },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Scegli nuova immagine")
                }

                Spacer(Modifier.height(24.dp))

                // Pulsante per salvare le modifiche
                Button(
                    onClick = {
                        isSaving = true
                        if (selectedImageUri != null) {
                            // Aggiorna immagine e descrizione
                            viewModel.uploadItemImageAndUpdate(
                                collectionId,
                                itemId,
                                selectedImageUri!!,
                                description,
                                onSuccess = {
                                    isSaving = false
                                    scope.launch { snackbarHostState.showSnackbar("Modifica completata") }
                                    navController.popBackStack()
                                },
                                onFailure = { error ->
                                    isSaving = false
                                    scope.launch { snackbarHostState.showSnackbar("Errore: $error") }
                                }
                            )
                        } else {
                            // Aggiorna solo descrizione
                            scope.launch {
                                try {
                                    viewModel.updateItemDescription(collectionId, itemId, description)
                                    isSaving = false
                                    scope.launch { snackbarHostState.showSnackbar("Descrizione aggiornata") }
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    isSaving = false
                                    scope.launch { snackbarHostState.showSnackbar("Errore aggiornamento") }
                                }
                            }
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
}
