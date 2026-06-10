package com.example.allcollections.feature.collection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.ui.ErrorText
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.CollectionItem

/**
 * Schermata per la modifica di un oggetto di una collezione.
 *
 * Permette di aggiornare:
 * - Descrizione (campo testuale)
 * - Immagine (sostituzione con una nuova dalla galleria)
 *
 * Le modifiche possono essere applicate singolarmente o insieme:
 * - Se si seleziona una nuova immagine + si modifica la descrizione → viene chiamato
 *   [uploadItemImageAndUpdate] (upload Cloudinary + aggiornamento completo).
 * - Se si modifica solo la descrizione → viene chiamato [updateItemDescription].
 *
 * @param navController Controller per la navigazione.
 * @param collectionId ID della collezione contenente l'oggetto.
 * @param itemId ID dell'oggetto da modificare.
 * @param viewModel ViewModel delle collezioni (gestisce le operazioni).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCollectionItemScreen(
    navController: NavController,
    collectionId: String,
    itemId: String,
    viewModel: CollectionViewModel
) {
    val scrollState = rememberScrollState()

    var item by remember { mutableStateOf<CollectionItem?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    // Launcher per selezionare una nuova immagine dalla galleria
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            errorMessage = null
        }
    }

    // Carica i dati correnti dell'oggetto all'avvio
    LaunchedEffect(itemId) {
        viewModel.getItemById(
            collectionId,
            itemId,
            onSuccess = {
                item = it
                description = it.description
            },
            onFailure = { error ->
                errorMessage = error
            }
        )
    }

    item?.let { currentItem ->
        Scaffold(
            topBar = { MyTopBar(navController = navController, title = "Modifica oggetto") },
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
                // Campo descrizione
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )

                Text("Immagine attuale:", style = MaterialTheme.typography.labelMedium)

                // Anteprima immagine (nuova se selezionata, altrimenti quella corrente)
                AsyncImage(
                    model = selectedImageUri ?: currentItem.imageUrl,
                    contentDescription = "Anteprima immagine oggetto",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp, max = 400.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentScale = ContentScale.Fit
                )

                Button(
                    onClick = { imageLauncher.launch("image/*") },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Scegli nuova immagine")
                }

                Spacer(Modifier.height(24.dp))

                // Pulsante di salvataggio
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        isSaving = true
                        errorMessage = null

                        if (selectedImageUri != null) {
                            // Upload nuova immagine + aggiornamento descrizione (entrambi)
                            viewModel.uploadItemImageAndUpdate(
                                collectionId,
                                itemId,
                                selectedImageUri!!,
                                description,
                                onSuccess = {
                                    isSaving = false
                                    navController.popBackStack()
                                },
                                onFailure = { error ->
                                    isSaving = false
                                    errorMessage = error
                                }
                            )
                        } else {
                            // Solo aggiornamento della descrizione
                            viewModel.updateItemDescription(
                                collectionId,
                                itemId,
                                description,
                                onSuccess = {
                                    isSaving = false
                                    navController.popBackStack()
                                },
                                onFailure = { error ->
                                    isSaving = false
                                    errorMessage = error
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

                errorMessage?.let {
                    ErrorText(
                        text = it,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}