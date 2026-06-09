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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Schermata per aggiungere o sostituire la copertina di una collezione.
 *
 * Permette di selezionare un'immagine dalla galleria di sistema; non appena
 * l'immagine viene selezionata, parte automaticamente il caricamento su
 * Cloudinary. In caso di successo naviga al dettaglio della collezione,
 * rimuovendo questa schermata dallo stack. È possibile saltare il passaggio
 * tramite il bottone "Non ora".
 *
 * @param collectionId ID della collezione a cui aggiungere la copertina.
 * @param navController NavController per la navigazione.
 * @param viewModel ViewModel che gestisce il caricamento su Cloudinary.
 */
@Composable
fun AddCollectionImageScreen(
    collectionId: String,
    navController: NavController,
    viewModel: CollectionViewModel = koinViewModel()
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Avvia l'upload automaticamente appena viene selezionata un'immagine
    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            isUploading = true
            viewModel.uploadImageToCloudinary(
                collectionId = collectionId,
                imageUri = uri,
                onSuccess = {
                    isUploading = false
                    navController.navigate(
                        Screens.CollectionDetailScreen.createRoute(collectionId)
                    ) {
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
        topBar = {
            MyTopBar(navController = navController, title = "Aggiungi copertina collezione")
        },
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
            // Anteprima dell'immagine selezionata (visibile prima dell'upload)
            selectedImageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Anteprima immagine copertina",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp, max = 400.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = { galleryLauncher.launch("image/*") },
                enabled = !isUploading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(
                    text = if (isUploading) "Caricamento in corso..." else "Seleziona immagine dalla galleria",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    navController.navigate(
                        Screens.CollectionDetailScreen.createRoute(collectionId)
                    ) {
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