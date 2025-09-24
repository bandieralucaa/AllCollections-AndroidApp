package com.example.allcollections.profile

import android.Manifest
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
import com.example.allcollections.navigation.Screens
import com.example.allcollections.utils.rememberCameraLauncher
import com.example.allcollections.utils.rememberPermission
import com.example.allcollections.viewModel.ProfileViewModel
import com.cloudinary.android.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.provider.Settings
import com.example.allcollections.utils.PermissionStatus


@Composable
fun PhotoProfile(navController: NavController, userId: String, profileViewModel: ProfileViewModel) {
    val context = LocalContext.current
    val userData = profileViewModel.pendingUserData
    var shouldNavigateToLogin by remember { mutableStateOf(false) }



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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = cameraLauncher.snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Scegli la tua foto profilo", fontSize = 22.sp)

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedImageUri == null) {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Scegli dalla galleria")
                }

                Spacer(modifier = Modifier.height(8.dp))

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


                Spacer(modifier = Modifier.height(16.dp))
            }


            Spacer(modifier = Modifier.height(16.dp))

            selectedImageUri?.let { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto selezionata",
                    modifier = Modifier.size(180.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (uploadSuccess) {
                    Text("✅ Foto salvata con successo!", color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(onClick = {
                    profileViewModel.saveProfilePicture(uri, context) { rawUrl ->
                        val publicId = rawUrl?.substringAfter("upload/")?.substringBeforeLast(".")
                        val finalImageUrl = publicId?.let { MediaManager.get().url().generate(it) }

                        profileViewModel.finalizeUserRegistration(
                            imageUrl = finalImageUrl,
                            onSuccess = {
                                uploadSuccess = true
                                shouldNavigateToLogin = true
                                Toast.makeText(context, "Registrazione completata! Accedi ora 🎉", Toast.LENGTH_LONG).show()
                            },
                            onFailure = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }) {
                    Text("Conferma foto profilo")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedImageUri == null) {

                Button(onClick = {
                    profileViewModel.finalizeUserRegistration(
                        imageUrl = null,
                        onSuccess = {
                            uploadSuccess = true
                            shouldNavigateToLogin = true
                            Toast.makeText(context, "Registrazione completata! Accedi ora 🎉", Toast.LENGTH_LONG).show()
                        },
                        onFailure = {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    )
                }) {
                    Text("Salta")
                }
            }

        }
    }

    LaunchedEffect(shouldNavigateToLogin) {
        if (shouldNavigateToLogin) {
            navController.navigate(Screens.Login.name) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
            shouldNavigateToLogin = false
        }
    }


}
