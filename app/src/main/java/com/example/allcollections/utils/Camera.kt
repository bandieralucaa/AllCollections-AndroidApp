package com.example.allcollections.utils

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File

interface CameraLauncher {
    val capturedImageUri: Uri
    fun captureImage()
    val snackbarHostState: SnackbarHostState
}

@Composable
fun rememberCameraLauncher(onImageCaptured: (Uri) -> Unit): CameraLauncher {
    val ctx = LocalContext.current
    val storagePermission = rememberPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    val imageUri = remember {
        val imageFile = File.createTempFile("tmp_image", ".jpg", ctx.externalCacheDir)
        FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", imageFile)
    }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cameraActivityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { pictureTaken ->
            if (pictureTaken) {
                if (storagePermission.status == PermissionStatus.Granted) {
                    saveImageToStorage(imageUri, ctx.applicationContext.contentResolver)
                    onImageCaptured(imageUri)
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Permesso non concesso per salvare l'immagine")
                    }
                }
            }
        }

    return object : CameraLauncher {
        override val capturedImageUri: Uri = imageUri
        override fun captureImage() {
            when (storagePermission.status) {
                PermissionStatus.Granted -> cameraActivityLauncher.launch(imageUri)
                else -> storagePermission.launchPermissionRequest()
            }
        }

        override val snackbarHostState = snackbarHostState
    }

}



