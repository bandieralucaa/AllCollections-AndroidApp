package com.example.allcollections.feature.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.ErrorText
import com.example.allcollections.core.utils.image.rememberCameraLauncher
import com.example.allcollections.core.utils.permissions.PermissionStatus
import com.example.allcollections.core.utils.permissions.rememberPermission
import kotlinx.coroutines.launch

/**
 * Schermata per la selezione e il caricamento della foto profilo.
 *
 * Permette all'utente di scegliere un'immagine dalla galleria o scattarla con la
 * fotocamera, gestendo i relativi permessi (Android 13+ inclusi).
 *
 * ### Flussi
 * - **Registrazione** (`isRegistration = true`): dopo il caricamento, logout automatico
 *   e navigazione al login (per evitare dati inconsistenti).
 * - **Modifica profilo** (`isRegistration = false`): dopo il salvataggio, torna indietro.
 *
 * @param navController Controller per la navigazione.
 * @param userId ID dell'utente a cui associare la foto.
 * @param profileViewModel ViewModel del profilo (upload e salvataggio URL).
 * @param isRegistration Se `true`, proviene dal flusso di registrazione.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoProfileScreen(
    navController: NavController,
    userId: String,
    profileViewModel: ProfileViewModel,
    isRegistration: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Launcher per la galleria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
        errorMessage = null
    }

    // Launcher per la fotocamera (tramite utility)
    val cameraLauncher = rememberCameraLauncher { uri ->
        selectedImageUri = uri
        errorMessage = null
    }

    // Permessi fotocamera
    val cameraPermission = rememberPermission(Manifest.permission.CAMERA)

    // Permessi galleria (differenziati per Android 13+)
    val galleryPermission = rememberPermission(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    // Mostra snackbar esplicativa per permesso negato (non permanente)
    fun showRationaleSnackbar(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "OK",
                duration = SnackbarDuration.Short
            )
        }
    }

    // Mostra snackbar che invita ad andare nelle impostazioni (permesso negato permanentemente)
    fun showGoToSettingsSnackbar() {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Vai alle impostazioni per abilitare il permesso",
                actionLabel = "IMPOSTAZIONI",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    // Gestisce il tap su "Galleria" in base allo stato del permesso
    fun handleGalleryClick() {
        when (galleryPermission.status) {
            PermissionStatus.Granted -> galleryLauncher.launch("image/*")
            PermissionStatus.Denied -> {
                showRationaleSnackbar("Permesso galleria necessario per selezionare un'immagine")
                galleryPermission.launchPermissionRequest()
            }
            PermissionStatus.Unknown -> galleryPermission.launchPermissionRequest()
            PermissionStatus.PermanentlyDenied -> showGoToSettingsSnackbar()
        }
    }

    // Gestisce il tap su "Fotocamera" in base allo stato del permesso
    fun handleCameraClick() {
        when (cameraPermission.status) {
            PermissionStatus.Granted -> cameraLauncher.captureImage()
            PermissionStatus.Denied -> {
                showRationaleSnackbar("Permesso fotocamera necessario per scattare una foto")
                cameraPermission.launchPermissionRequest()
            }
            PermissionStatus.Unknown -> cameraPermission.launchPermissionRequest()
            PermissionStatus.PermanentlyDenied -> showGoToSettingsSnackbar()
        }
    }

    // Carica l'immagine su Cloudinary e salva l'URL su Firestore
    fun uploadProfileImage() {
        val uri = selectedImageUri ?: return
        isLoading = true
        errorMessage = null

        profileViewModel.uploadProfileImage(
            imageUri = uri,
            onSuccess = { imageUrl ->
                profileViewModel.saveProfileImageUrl(
                    userId = userId,
                    imageUrl = imageUrl,
                    onSuccess = {
                        isLoading = false
                        // Feedback utente (Toast breve, può essere sostituito con Snackbar)
                        android.widget.Toast.makeText(
                            context,
                            if (isRegistration) "Registrazione completata!" else "Foto aggiornata!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()

                        if (isRegistration) {
                            // Durante la registrazione, effettua il logout per evitare dati inconsistenti
                            profileViewModel.logout()
                            navController.navigate(Screens.LoginScreen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onFailure = { error ->
                        isLoading = false
                        errorMessage = null
                    }
                )
            },
            onFailure = { error ->
                isLoading = false
                errorMessage = null
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Titolo dinamico
            Text(
                text = if (isRegistration) "Scegli foto profilo" else "Cambia foto profilo",
                fontSize = 22.sp,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(32.dp))

            // Anteprima immagine profilo (circolare)
            Card(
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier.size(180.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(selectedImageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Anteprima foto profilo",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Icona profilo predefinita",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Stato: nessuna immagine selezionata
            if (selectedImageUri == null) {
                // Pulsante galleria
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { handleGalleryClick() }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Galleria")
                }

                Spacer(Modifier.height(12.dp))

                // Pulsante fotocamera
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { handleCameraClick() }
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Fotocamera")
                }

                Spacer(Modifier.height(24.dp))

                // Pulsante Salta / Annulla
                TextButton(
                    onClick = {
                        if (isRegistration) {
                            profileViewModel.logout()
                            navController.navigate(Screens.LoginScreen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text(if (isRegistration) "Salta" else "Annulla")
                }
            } else {
                // Stato: immagine selezionata, attesa upload
                if (isLoading) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    onClick = { uploadProfileImage() }
                ) {
                    Text(if (isRegistration) "Completa registrazione" else "Salva")
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    onClick = { selectedImageUri = null }
                ) {
                    Text("Scegli un'altra")
                }

                errorMessage?.let {
                    ErrorText(text = it, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}