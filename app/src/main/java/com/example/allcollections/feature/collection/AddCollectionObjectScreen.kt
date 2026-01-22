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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import kotlinx.coroutines.launch

/**
 * Schermata per aggiungere un oggetto a una collezione.
 *
 * Permette di:
 * - Inserire una descrizione
 * - Selezionare un'immagine dalla galleria
 * - Caricare l'immagine su Cloudinary tramite CollectionViewModel
 *
 * Mostra anteprima dell'immagine selezionata, progress indicator durante l'upload
 * e snackbar in caso di errori.
 */
@Composable
fun AddCollectionObjectScreen(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel = viewModel()
) {
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Launcher per selezione immagine dalla galleria
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { MyTopBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo descrizione
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrizione") },
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            // Pulsante selezione immagine
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                enabled = !isUploading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(text = if (isUploading) "Caricamento in corso..." else "Scegli immagine")
            }

            // Anteprima immagine
            selectedImageUri?.let { uri ->
                Spacer(modifier = Modifier.height(12.dp))
                if (isUploading) {
                    CircularProgressIndicator()
                    Text("Caricamento immagine in corso...", modifier = Modifier.padding(top = 4.dp))
                } else {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Anteprima immagine",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(180.dp)
                            .padding(4.dp)
                    )
                }
            }

            // Pulsante aggiungi oggetto
            Button(
                enabled = selectedImageUri != null && description.isNotBlank() && !isUploading,
                onClick = {
                    selectedImageUri?.let { uri ->
                        isUploading = true
                        viewModel.addItem(
                            collectionId = collectionId,
                            imageUri = uri,
                            description = description
                        ) { success, error ->
                            coroutineScope.launch {
                                isUploading = false
                                if (success) {
                                    description = ""
                                    selectedImageUri = null
                                    navController.navigate(Screens.CollectionDetailScreen.createRoute(collectionId)) {
                                        popUpTo(Screens.MyCollectionsScreen.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                } else {
                                    snackbarHostState.showSnackbar(error ?: "Errore durante l'upload")
                                }
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text("Aggiungi oggetto")
            }
        }
    }
}
