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

                Button(onClick = {
                    if (cameraPermission.status.isGranted) {
                        cameraLauncher.captureImage()
                    } else {
                        cameraPermission.launchPermissionRequest()
                    }
                }) {
                    Text("Scatta una foto")
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
                    if (selectedImageUri == null) {
                        Toast.makeText(context, "Seleziona una foto prima di confermare", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    uploadSuccess = false

                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                    if (currentUserId == null || userData == null) {
                        isLoading = false
                        Toast.makeText(context, "Errore: dati mancanti", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    profileViewModel.saveProfilePicture(uri, context) { rawUrl ->

                        Log.d("PhotoProfile", "Callback upload triggered, rawUrl=$rawUrl")

                        if (rawUrl != null) {
                            val publicId = rawUrl.substringAfter("upload/").substringBeforeLast(".")
                            val finalImageUrl = MediaManager.get().url().generate(publicId)

                            val user = hashMapOf(
                                "name" to userData.name,
                                "surname" to userData.surname,
                                "dateOfBirth" to userData.dateOfBirth.toString(),
                                "email" to userData.email,
                                "gender" to userData.gender,
                                "username" to userData.username,
                                "profileImageUrl" to finalImageUrl
                            )

                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(currentUserId)
                                .set(user)
                                .addOnSuccessListener {
                                    Log.d("PhotoProfile", "Firestore upload success")

                                    isLoading = false
                                    uploadSuccess = true
                                    profileViewModel.pendingUserData = null
                                    FirebaseAuth.getInstance().signOut()
                                    Toast.makeText(context, "Registrazione completata! Accedi ora 🎉", Toast.LENGTH_LONG).show()

                                    shouldNavigateToLogin = true  // <-- attiva la navigazione
                                }


                                .addOnFailureListener {
                                    Log.d("PhotoProfile", "Firestore upload failure")

                                    isLoading = false
                                    Toast.makeText(context, "Errore durante salvataggio", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Log.d("PhotoProfile", "Upload immagine fallito")

                            isLoading = false
                            Toast.makeText(context, "Errore nell'upload immagine", Toast.LENGTH_SHORT).show()
                        }
                    }


                }) {
                    Text("Conferma foto profilo")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedImageUri == null) {

                Button(onClick = {
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                    if (userData == null || currentUserId == null) {
                        Toast.makeText(context, "Errore: dati mancanti", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val defaultImageUrl =
                        "https://res.cloudinary.com/demo/image/upload/v123456789/default_profile.png"

                    val user = hashMapOf(
                        "name" to userData.name,
                        "surname" to userData.surname,
                        "dateOfBirth" to userData.dateOfBirth.toString(),
                        "email" to userData.email,
                        "gender" to userData.gender,
                        "username" to userData.username,
                        "profileImageUrl" to defaultImageUrl
                    )

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUserId)
                        .set(user)
                        .addOnSuccessListener {
                            isLoading = false
                            uploadSuccess = true
                            profileViewModel.pendingUserData = null
                            FirebaseAuth.getInstance().signOut()
                            Toast.makeText(
                                context,
                                "Registrazione completata! Accedi ora 🎉",
                                Toast.LENGTH_LONG
                            ).show()

                            shouldNavigateToLogin = true  // <-- attiva la navigazione
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                context,
                                "Errore durante la registrazione",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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
