package com.example.allcollections.core.utils.image

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.allcollections.core.utils.permissions.PermissionHandler
import com.example.allcollections.core.utils.permissions.PermissionStatus
import com.example.allcollections.core.utils.permissions.rememberPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * Interfaccia che definisce un launcher per la fotocamera.
 */
interface CameraLauncher {
    val capturedImageUri: Uri
    val snackbarHostState: SnackbarHostState
    fun captureImage()
}

/**
 * Composable che restituisce un CameraLauncher per acquisire immagini con la fotocamera.
 *
 * Gestisce:
 * - Creazione URI temporaneo
 * - Permessi di scrittura (storage, se necessario)
 * - Snackbar in caso di errori
 *
 * @param onImageCaptured Callback chiamato con l'URI dell'immagine catturata.
 */
@Composable
fun rememberCameraLauncher(
    onImageCaptured: (Uri) -> Unit
): CameraLauncher {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Permesso di scrittura (solo se necessario)
    val storagePermission: PermissionHandler = rememberPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    // URI temporaneo per la foto
    val imageUri: Uri = remember {
        val file = File.createTempFile("tmp_image", ".jpg", context.externalCacheDir)
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    // Launcher fotocamera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            handleImageCapture(
                context = context,
                imageUri = imageUri,
                storagePermission = storagePermission,
                snackbarHostState = snackbarHostState,
                scope = scope,
                onImageCaptured = onImageCaptured
            )
        }
    }

    return object : CameraLauncher {
        override val capturedImageUri: Uri = imageUri
        override val snackbarHostState: SnackbarHostState = snackbarHostState

        override fun captureImage() {
            if (storagePermission.status == PermissionStatus.Granted) {
                cameraLauncher.launch(imageUri)
            } else {
                storagePermission.launchPermissionRequest()
            }
        }
    }
}

/**
 * Gestisce la cattura dell'immagine e la logica dei permessi.
 */
private fun handleImageCapture(
    context: Context,
    imageUri: Uri,
    storagePermission: PermissionHandler,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onImageCaptured: (Uri) -> Unit
) {
    if (storagePermission.status == PermissionStatus.Granted) {
        // Salva l'immagine nella cache (funzione definita altrove o implementabile)
        saveImageToStorage(imageUri, context.contentResolver)
        onImageCaptured(imageUri)
    } else {
        scope.launch {
            snackbarHostState.showSnackbar("Permesso non concesso per salvare l'immagine")
        }
    }
}
