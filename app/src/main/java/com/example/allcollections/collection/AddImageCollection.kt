package com.example.allcollections.collection

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.allcollections.navigation.Screens
import com.example.allcollections.utils.rememberCameraLauncher
import com.example.allcollections.utils.rememberPermission
import com.example.allcollections.viewModel.CollectionViewModel

@Composable
fun AddImageCollection(navController: NavController, collectionId: String) {
    val collectionViewModel: CollectionViewModel = viewModel()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }


    val ctx = LocalContext.current

    val cameraLauncher = rememberCameraLauncher { capturedUri ->
        selectedImageUri = capturedUri
    }


    val cameraPermission = rememberPermission(Manifest.permission.CAMERA) { status ->
        if (status.isGranted) {
            cameraLauncher.captureImage()
        } else {
            Toast.makeText(ctx, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun takePicture() {
        if (cameraPermission.status.isGranted) {
            cameraLauncher.captureImage()
        } else {
            cameraPermission.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Scegli foto", fontSize = 28.sp)

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = ::takePicture) {
            Text(text = "Scatta foto")
        }

        if (cameraLauncher.capturedImageUri.path?.isNotEmpty() == true) {
            AsyncImage(
                ImageRequest.Builder(ctx)
                    .data(cameraLauncher.capturedImageUri)
                    .crossfade(true)
                    .build(),
                "Captured image"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val selectedImageUri: Uri? = data?.data
                if (selectedImageUri != null) {
                    collectionViewModel.saveImageCollection(collectionId, selectedImageUri) { success, error ->
                        if (success) {
                            navController.navigate(Screens.Profile.name)
                        } else {
                            Toast.makeText(ctx, "Errore durante il salvataggio dell'immagine: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        Button(onClick = { galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }) {
            Text(text = "Scegli dalla galleria")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { navController.navigate(Screens.MyCollections.name) }) {
            Text(text = "Non ora")
        }
    }

    if (cameraLauncher.capturedImageUri.path?.isNotEmpty() == true) {
        collectionViewModel.saveImageCollection(collectionId, cameraLauncher.capturedImageUri) { success, error ->
            if (success) {
                navController.navigate(Screens.Profile.name)
            } else {
                Toast.makeText(ctx, "Errore durante il salvataggio dell'immagine: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
