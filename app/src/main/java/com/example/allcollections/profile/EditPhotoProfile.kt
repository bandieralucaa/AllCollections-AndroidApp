package com.example.allcollections.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cloudinary.android.MediaManager
import com.example.allcollections.utils.PermissionStatus
import com.example.allcollections.utils.rememberCameraLauncher
import com.example.allcollections.utils.rememberPermission
import com.example.allcollections.viewModel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.provider.Settings


@Composable
fun EditPhotoProfile(navController: NavController, profileViewModel: ProfileViewModel) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var uploadSuccess by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    val cameraLauncher = rememberCameraLauncher {
        selectedImageUri = it
    }

    val cameraPermission = rememberPermission(Manifest.permission.CAMERA) {
        if (it.isGranted) {
            cameraLauncher.captureImage()
        } else {
            Toast.makeText(context, "Permesso fotocamera negato", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(hostState = cameraLauncher.snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Modifica la foto profilo", fontSize = 22.sp)

            Spacer(modifier = Modifier.height(16.dp))

            AsyncImage(
                model = ImageRequest.Builder(context).data(selectedImageUri).crossfade(true).build(),
                contentDescription = "Immagine selezionata",
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = {
                        when (cameraPermission.status) {
                            PermissionStatus.Granted -> cameraLauncher.captureImage()
                            PermissionStatus.Denied -> cameraPermission.launchPermissionRequest()
                            PermissionStatus.PermanentlyDenied -> Toast.makeText(
                                context,
                                "Vai nelle impostazioni per abilitare la fotocamera",
                                Toast.LENGTH_LONG
                            ).show()
                            else -> cameraPermission.launchPermissionRequest()
                        }
                    }) {
                        Text("Scatta una foto")
                    }

                    if (cameraPermission.status == PermissionStatus.PermanentlyDenied) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }) {
                            Text("Apri Impostazioni")
                        }
                    }
                }

                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Galleria")
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uploadSuccess) {
                Text("Foto aggiornata con successo!", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(onClick = {
                if (selectedImageUri == null || currentUserId == null) {
                    Toast.makeText(context, "Seleziona una foto prima di salvare", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                uploadSuccess = false

                profileViewModel.saveProfilePicture(selectedImageUri!!, context) { rawUrl ->
                    Log.d("EditPhotoProfile", "Cloudinary upload callback: rawUrl=$rawUrl")
                    if (rawUrl != null) {
                        val publicId = rawUrl.substringAfter("upload/").substringBeforeLast(".")
                        val finalUrl = MediaManager.get().url().generate(publicId)

                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(currentUserId)
                            .update("profileImageUrl", finalUrl)
                            .addOnSuccessListener {
                                isLoading = false
                                uploadSuccess = true
                                Toast.makeText(context, "Immagine aggiornata!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                            .addOnFailureListener {
                                isLoading = false
                                Toast.makeText(context, "Errore Firestore", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        isLoading = false
                        Toast.makeText(context, "Upload fallito", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Salva immagine")
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(onClick = { navController.popBackStack() }) {
                Text("Annulla")
            }
        }
    }
}
