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
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Schermata per aggiungere un nuovo oggetto a una collezione.
 *
 * Permette di inserire una descrizione testuale e selezionare un'immagine
 * dalla galleria. Il bottone "Aggiungi oggetto" è abilitato solo quando
 * entrambi i campi sono valorizzati. Al salvataggio:
 * 1. Carica l'immagine su Cloudinary.
 * 2. Salva i metadati dell'oggetto su Firestore.
 * 3. Invia notifiche push agli utenti che hanno messo like alla collezione.
 * 4. Naviga al dettaglio della collezione.
 *
 * @param navController NavController per la navigazione.
 * @param collectionId ID della collezione a cui aggiungere l'oggetto.
 * @param viewModel ViewModel che gestisce l'upload e il salvataggio.
 * @param notificationViewModel ViewModel usato per inviare notifiche ai follower.
 */
@Composable
fun AddCollectionObjectScreen(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel = koinViewModel(),
    notificationViewModel: NotificationViewModel = koinViewModel()
) {
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrizione") },
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                enabled = !isUploading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(text = if (isUploading) "Caricamento in corso..." else "Scegli immagine")
            }

            // Anteprima dell'immagine selezionata
            selectedImageUri?.let { uri ->
                Spacer(modifier = Modifier.height(12.dp))
                if (isUploading) {
                    CircularProgressIndicator()
                    Text("Caricamento immagine in corso...", modifier = Modifier.padding(top = 4.dp))
                } else {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Anteprima immagine oggetto",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .heightIn(min = 150.dp, max = 400.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .padding(4.dp)
                    )
                }
            }

            // Abilitato solo se l'immagine è selezionata, la descrizione non è vuota e non si sta caricando
            Button(
                enabled = selectedImageUri != null && description.isNotBlank() && !isUploading,
                onClick = {
                    selectedImageUri?.let { uri ->
                        isUploading = true
                        viewModel.addItem(
                            collectionId = collectionId,
                            imageUri = uri,
                            description = description,
                            notificationViewModel = notificationViewModel
                        ) { success, error ->
                            coroutineScope.launch {
                                isUploading = false
                                if (success) {
                                    description = ""
                                    selectedImageUri = null
                                    navController.navigate(
                                        Screens.CollectionDetailScreen.createRoute(collectionId)
                                    ) {
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