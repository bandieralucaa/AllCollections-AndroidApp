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
 *
 * @property capturedImageUri URI temporaneo del file immagine che verrà scritto dalla fotocamera.
 * @property snackbarHostState Host state per mostrare snackbar in caso di errore o annullamento.
 */
interface CameraLauncher {
    val capturedImageUri: Uri
    val snackbarHostState: SnackbarHostState
    fun captureImage()
}

/**
 * Composable che crea e ricorda un [CameraLauncher] per la cattura di immagini.
 *
 * Questa funzione gestisce l'intero flusso di acquisizione di un'immagine dalla fotocamera:
 * - Crea un file temporaneo `.jpg` nella cache esterna dell'app.
 * - Configura un [FileProvider] per condividere l'URI con l'app fotocamera.
 * - Lancia l'intent della fotocamera tramite [rememberLauncherForActivityResult].
 * - Al successo, restituisce l'URI dell'immagine catturata.
 * - In caso di annullamento o errore, mostra una snackbar tramite lo stato fornito.
 *
 * ### Requisiti di permessi
 * Non richiede il permesso `WRITE_EXTERNAL_STORAGE` perché l'app ha `minSdk >= 29` (Android 10+),
 * dove lo scoped storage è gestito automaticamente. Il file viene salvato nella cache esterna
 * dell'app, accessibile solo all'app stessa.
 *
 * ### Utilizzo tipico
 * ```
 * val camera = rememberCameraLauncher { uri ->
 *     viewModel.updateProfileImage(uri)
 * }
 *
 * Button(onClick = { camera.captureImage() }) {
 *     Text("Scatta foto")
 * }
 *
 * SnackbarHost(hostState = camera.snackbarHostState)
 * ```
 *
 * **Attenzione:** Assicurati di aver dichiarato il [FileProvider] nel `AndroidManifest.xml`
 * e una risorsa `file_paths.xml` adeguata, altrimenti l'app crasha.
 *
 * @param onImageCaptured Callback invocato con l'[Uri] dell'immagine catturata con successo.
 * @return Un [CameraLauncher] pronto all'uso.
 *
 * @see androidx.activity.compose.rememberLauncherForActivityResult
 * @see FileProvider
 */
@Composable
fun rememberCameraLauncher(
    onImageCaptured: (Uri) -> Unit
): CameraLauncher {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Crea un file temporaneo univoco nella cache esterna dell'app
    // Il nome "tmp_image" verrà automaticamente reso unico da createTempFile
    val imageUri: Uri = remember {
        val file = File.createTempFile("tmp_image", ".jpg", context.externalCacheDir)
        // Ottiene l'URI tramite FileProvider per rendere il file accessibile alla fotocamera
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    // Launcher per l'intent della fotocamera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Foto scattata con successo → invoca il callback
            onImageCaptured(imageUri)
        } else {
            // Annullato o errore → mostra snackbar
            scope.launch {
                snackbarHostState.showSnackbar("Acquisizione immagine annullata o fallita")
            }
        }
    }

    // Restituisce l'implementazione di CameraLauncher
    return object : CameraLauncher {
        override val capturedImageUri: Uri = imageUri
        override val snackbarHostState: SnackbarHostState = snackbarHostState

        override fun captureImage() {
            cameraLauncher.launch(imageUri)
        }
    }
}