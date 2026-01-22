package com.example.allcollections.feature.collection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.UserCollection
import kotlinx.coroutines.launch

/**
 * Schermata per modificare i dettagli di una collezione.
 *
 * Permette di aggiornare:
 *  - Nome
 *  - Categoria
 *  - Descrizione
 *  - Immagine di copertina
 *
 * Utilizza i metodi del CollectionViewModel per aggiornamenti e upload immagini.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCollectionScreen(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Stato della collezione
    var collection by remember { mutableStateOf<UserCollection?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher per selezionare una nuova immagine
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    // Carica la collezione all'avvio
    LaunchedEffect(collectionId) {
        viewModel.getCollectionById(
            collectionId = collectionId,
            onSuccess = { collection = it },
            onFailure = { error ->
                scope.launch { snackbarHostState.showSnackbar("Errore caricamento: $error") }
            }
        )
    }

    collection?.let { currentCollection ->
        var name by remember { mutableStateOf(currentCollection.name) }
        var category by remember { mutableStateOf(currentCollection.category ?: "") }
        var description by remember { mutableStateOf(currentCollection.description ?: "") }

        Scaffold(
            topBar = { MyTopBar(navController = navController) },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Modifica collezione", style = MaterialTheme.typography.titleLarge)

                // Input nome
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Input categoria
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Input descrizione
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Pulsante selezione immagine
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Scegli nuova immagine")
                }

                // Anteprima immagine
                val painter = when {
                    selectedImageUri != null -> rememberAsyncImagePainter(selectedImageUri)
                    !currentCollection.collectionImageUrl.isNullOrEmpty() -> rememberAsyncImagePainter(currentCollection.collectionImageUrl)
                    else -> null
                }

                painter?.let {
                    Image(
                        painter = it,
                        contentDescription = "Anteprima immagine collezione",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Pulsante salva modifiche
                Button(
                    onClick = {
                        scope.launch {
                            val hasFieldChanges = name != currentCollection.name ||
                                    category != (currentCollection.category ?: "") ||
                                    description != (currentCollection.description ?: "")
                            val hasImageChange = selectedImageUri != null

                            try {
                                // Aggiorna campi testuali se cambiati
                                if (hasFieldChanges) {
                                    viewModel.updateCollection(
                                        updatedCollection = currentCollection.copy(
                                            name = name,
                                            category = category,
                                            description = description
                                        ),
                                        onSuccess = {},
                                        onFailure = { error ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Errore salvataggio: $error")
                                            }
                                        }
                                    )
                                }

                                // Aggiorna immagine se cambiata
                                if (hasImageChange) {
                                    viewModel.updateCollectionImage(
                                        collectionId = currentCollection.id,
                                        newImageUri = selectedImageUri!!,
                                        onSuccess = {},
                                        onFailure = { error ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Errore immagine: $error")
                                            }
                                        }
                                    )
                                }

                                // Naviga alla detail screen dopo salvataggio
                                navController.navigate("CollectionDetailScreen/${currentCollection.id}") {
                                    popUpTo("editCollection/${currentCollection.id}") { inclusive = true }
                                }

                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Errore imprevisto: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp)
                ) {
                    Text("Salva modifiche")
                }
            }
        }
    }

    // Snackbar host
    SnackbarHost(hostState = snackbarHostState)
}
