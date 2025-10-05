package com.example.allcollections.collection

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.navigation.MyTopBar
import com.example.allcollections.navigation.Screens
import com.example.allcollections.viewModel.CollectionViewModel

@Composable
fun AddObjectCollection(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel
) {
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold (
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MyTopBar(navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(text = "Descrizione") },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(onClick = { launcher.launch("image/*") }) {
                Text("Scegli immagine")
            }

            selectedImageUri?.let { uri ->
                Spacer(modifier = Modifier.height(16.dp))

                if (isUploading) {
                    CircularProgressIndicator()
                    Text("Caricamento immagine...", modifier = Modifier.padding(top = 8.dp))
                } else {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Anteprima immagine",
                        modifier = Modifier
                            .padding(8.dp)
                            .height(180.dp)
                            .fillMaxWidth()
                    )
                    Text("Immagine selezionata", modifier = Modifier.padding(top = 4.dp))
                }
            }



            Spacer(modifier = Modifier.height(16.dp))

            Button(
                enabled = selectedImageUri != null && description.isNotBlank() && !isUploading,
                onClick = {
                    isUploading = true
                    viewModel.addItemToCollection(
                        collectionId,
                        selectedImageUri!!,
                        description,
                        onSuccess = {
                            description = ""
                            selectedImageUri = null
                            isUploading = false
                            Toast.makeText(context, "Oggetto aggiunto!", Toast.LENGTH_SHORT).show()
                            navController.navigate("CollectionDetail/$collectionId") {
                                popUpTo(Screens.MyCollections.name) { inclusive = false }
                            }
                        },
                        onFailure = {
                            isUploading = false
                            Toast.makeText(context, "Errore: $it", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

            ) {
                Text("Aggiungi")
            }
        }
    }
}
