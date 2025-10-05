package com.example.allcollections.collection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.viewModel.CollectionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItem(
    navController: NavController,
    collectionId: String,
    itemId: String,
    viewModel: CollectionViewModel
) {
    val itemState = remember { mutableStateOf<CollectionItem?>(null) }
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSaving by remember { mutableStateOf(false) }
    val item = itemState.value

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri.value = uri
    }

    LaunchedEffect(itemId) {
        viewModel.getItemById(
            collectionId = collectionId,
            itemId = itemId,
            onSuccess = { item ->
                itemState.value = item
            },
            onFailure = { error ->
                scope.launch {
                    snackbarHostState.showSnackbar("Errore: $error")
                }
            }
        )
    }

    if (item != null) {
        var description by remember { mutableStateOf(item.description ?: "") }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Modifica oggetto") }) },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Immagine attuale:", style = MaterialTheme.typography.labelMedium)

                AsyncImage(
                    model = imageUri.value ?: item.imageUrl,
                    contentDescription = "Immagine oggetto",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(
                        onClick = { launcher.launch("image/*") },
                        enabled = !isSaving
                    ) {
                        Text("Scegli nuova immagine")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        isSaving = true
                        val selectedUri = imageUri.value
                        if (selectedUri != null) {
                            viewModel.uploadItemImageAndUpdate(
                                collectionId = collectionId,
                                itemId = itemId,
                                imageUri = selectedUri,
                                updatedDescription = description,
                                onSuccess = {
                                    isSaving = false
                                    navController.popBackStack()
                                },
                                onFailure = { error ->
                                    isSaving = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Errore: $error")
                                    }
                                }
                            )
                        } else {
                            viewModel.updateItemDescription(
                                collectionId = collectionId,
                                itemId = itemId,
                                newDescription = description,
                                onSuccess = {
                                    isSaving = false
                                    navController.popBackStack()
                                },
                                onFailure = { error ->
                                    isSaving = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Errore: $error")
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(56.dp)
                        .fillMaxWidth(0.8f),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Salva modifiche", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}