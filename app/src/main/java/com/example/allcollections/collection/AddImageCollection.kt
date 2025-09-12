package com.example.allcollections.collection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.allcollections.navigation.Screens
import com.example.allcollections.viewModel.CollectionViewModel
import kotlinx.coroutines.launch

@Composable
fun AddImageCollection(
    collectionId: String,
    navController: NavController,
    viewModel: CollectionViewModel = viewModel()
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            isUploading = true
            viewModel.uploadImageToCloudinary(collectionId, uri,
                onSuccess = {
                    isUploading = false
                    navController.navigate("${Screens.CollectionDetail.name}/$collectionId") {
                        popUpTo(Screens.AddCollection.name) { inclusive = true }
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
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            selectedImageUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Immagine selezionata",
                    modifier = Modifier.size(150.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = { galleryLauncher.launch("image/*") },
                enabled = !isUploading
            ) {
                Text(if (isUploading) "Caricamento in corso..." else "Seleziona immagine dalla galleria")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    navController.navigate("${Screens.CollectionDetail.name}/$collectionId") {
                        popUpTo(Screens.AddCollection.name) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                Text("Non ora")
            }

        }
    }
}

