package com.example.allcollections.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.allcollections.navigation.Screens
import com.example.allcollections.viewModel.CollectionViewModel
import com.example.allcollections.collection.UserCollection
import kotlinx.coroutines.launch

@Composable
fun CollectionDetail(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel
) {
    val collectionState = remember { mutableStateOf<UserCollection?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(collectionId) {
        viewModel.getCollectionById(collectionId,
            onSuccess = { collection ->
                collectionState.value = collection
            },
            onFailure = { error ->
                scope.launch {
                    snackbarHostState.showSnackbar("Errore: $error")
                }
            }
        )
    }

    val collection = collectionState.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Indietro",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (collection?.collectionImageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(collection.collectionImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Immagine collezione",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessuna immagine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }


        if (collection == null) {
            Text("Caricamento...")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("Nome:", style = MaterialTheme.typography.titleMedium)
                Text(text = collection.name)

                Text("Categoria:", style = MaterialTheme.typography.titleMedium)
                Text(text = collection.category)

                Text("Descrizione:", style = MaterialTheme.typography.titleMedium)
                Text(text = collection.description)
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Spinge il bottone in basso

        // Pulsante "+" in basso a destra
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    navController.navigate(Screens.ObjectCollection.name)
                },
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aggiungi oggetto",
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}

