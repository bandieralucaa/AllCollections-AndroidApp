package com.example.allcollections.feature.collection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import kotlinx.coroutines.launch

/**
 * Schermata per aggiungere un'immagine principale a una collezione.
 *
 * Mostra un pulsante per selezionare l'immagine dalla galleria,
 * visualizza un'anteprima della selezione e gestisce il caricamento
 * su Cloudinary tramite CollectionViewModel.
 *
 * Se l'utente sceglie "Non ora", viene reindirizzato alla CollectionDetail senza upload.
 */
@Composable
fun AddCollectionImageScreen(
    collectionId: String,
    navController: NavController,
    viewModel: CollectionViewModel = viewModel()
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Launcher per selezione immagine dalla galleria
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Effetto: appena l'immagine è selezionata, esegue l'upload
    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            isUploading = true
            viewModel.uploadImageToCloudinary(
                collectionId = collectionId,
                imageUri = uri,
                onSuccess = {
                    isUploading = false
                    navController.navigate(Screens.CollectionDetailScreen.createRoute(collectionId)) {
                        popUpTo(Screens.AddCollectionImageScreen.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onFailure = { error ->
                    isUploading = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Errore upload: $error")
                    }
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Anteprima immagine selezionata con bordi arrotondati
            selectedImageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Anteprima immagine",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp, max = 400.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentScale = ContentScale.Fit  // FIX: immagine intera senza crop
                )
                Spacer(Modifier.height(16.dp))
            }

            // Pulsante principale: seleziona immagine
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                enabled = !isUploading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(
                    text = if (isUploading) "Caricamento in corso..." else "Seleziona immagine dalla galleria",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(Modifier.height(12.dp))

            // Pulsante secondario: non ora
            OutlinedButton(
                onClick = {
                    navController.navigate(Screens.CollectionDetailScreen.createRoute(collectionId)) {
                        popUpTo(Screens.AddCollectionImageScreen.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Non ora", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}