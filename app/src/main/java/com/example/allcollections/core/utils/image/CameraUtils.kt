package com.example.allcollections.core.utils.image

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File

/**
 * Contratto per un launcher della fotocamera che gestisce la cattura di un'immagine.
 *
 * Ottenuto tramite [rememberCameraLauncher]; non istanziare direttamente.
 */
interface CameraLauncher {
    /** URI temporaneo del file immagine che verrà scritto dalla fotocamera. */
    val capturedImageUri: Uri

    /** Host state per mostrare snackbar in caso di errore o annullamento. */
    val snackbarHostState: SnackbarHostState

    /** Avvia l'intent della fotocamera per acquisire un'immagine. */
    fun captureImage()
}

/**
 * Composable che crea e ricorda un [CameraLauncher] per la cattura di immagini.
 *
 * Crea un file temporaneo `.jpg` nella cache esterna e ne espone l'URI tramite
 * [FileProvider]. Non richiede il permesso `WRITE_EXTERNAL_STORAGE` perché l'app
 * ha `minSdk >= 29` (Android 10+), dove lo scoped storage è gestito automaticamente.
 *
 * In caso di annullamento o errore della fotocamera, viene mostrata una [Snackbar]
 * tramite [SnackbarHostState].
 *
 * **Utilizzo:**
 * ```kotlin
 * val camera = rememberCameraLauncher { uri -> viewModel.onImageCaptured(uri) }
 * Button(onClick = { camera.captureImage() }) { Text("Scatta foto") }
 * SnackbarHost(camera.snackbarHostState)
 * ```
 *
 * @param onImageCaptured Callback invocato con l'[Uri] dell'immagine catturata con successo.
 * @return Un [CameraLauncher] pronto all'uso.
 */
@Composable
fun rememberCameraLauncher(
    onImageCaptured: (Uri) -> Unit
): CameraLauncher {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val imageUri: Uri = remember {
        val file = File.createTempFile("tmp_image", ".jpg", context.externalCacheDir)
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageCaptured(imageUri)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Acquisizione immagine annullata o fallita")
            }
        }
    }

    return object : CameraLauncher {
        override val capturedImageUri: Uri = imageUri
        override val snackbarHostState: SnackbarHostState = snackbarHostState

        override fun captureImage() {
            cameraLauncher.launch(imageUri)
        }
    }
}
