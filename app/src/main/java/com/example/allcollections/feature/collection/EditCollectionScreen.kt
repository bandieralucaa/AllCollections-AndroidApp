package com.example.allcollections.feature.collection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.components.PRESET_CATEGORIES
import kotlinx.coroutines.launch

/**
 * Schermata per modificare i dettagli di una collezione.
 *
 * Permette di aggiornare:
 *  - Nome
 *  - Categoria
 *  - Descrizione
 *  - Immagine di copertina
 *
 * Utilizza i metodi del CollectionViewModel per aggiornamenti e upload immagini.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCollectionScreen(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var collection by remember { mutableStateOf<UserCollection?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedChip by remember { mutableStateOf<String?>(null) }
    var isCustomCategory by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    LaunchedEffect(collectionId) {
        viewModel.getCollectionById(
            collectionId = collectionId,
            onSuccess = {
                collection = it
                it.category?.let { cat ->
                    if (cat in PRESET_CATEGORIES) {
                        selectedChip = cat
                        isCustomCategory = false
                    } else {
                        selectedChip = "Altro ✏️"
                        isCustomCategory = true
                    }
                }
            },
            onFailure = { error ->
                scope.launch { snackbarHostState.showSnackbar("Errore caricamento: $error") }
            }
        )
    }

    collection?.let { currentCollection ->
        var name by remember { mutableStateOf(currentCollection.name) }
        var category by remember { mutableStateOf(currentCollection.category ?: "") }
        var description by remember { mutableStateOf(currentCollection.description ?: "") }

        Scaffold(
            topBar = { MyTopBar(navController = navController, title = "Modifica collezione") },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { showCategoryDialog = true }
                ) {
                    OutlinedTextField(
                        value = if (category.isNotEmpty()) category else "Seleziona categoria *",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize(),
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Apri categorie",
                                modifier = Modifier.clickable { showCategoryDialog = true }
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            disabledTextColor = if (category.isNotEmpty())
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        enabled = false
                    )
                }

                if (showCategoryDialog) {
                    Dialog(onDismissRequest = { showCategoryDialog = false }) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.background,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Seleziona una categoria",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PRESET_CATEGORIES.forEach { chip ->
                                        FilterChip(
                                            selected = selectedChip == chip,
                                            onClick = {
                                                selectedChip = chip
                                                if (chip == "Altro ✏️") {
                                                    isCustomCategory = true
                                                    category = ""
                                                } else {
                                                    isCustomCategory = false
                                                    category = chip
                                                }
                                                showCategoryDialog = false
                                            },
                                            label = { Text(chip) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isCustomCategory) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Scrivi la tua categoria...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scegli nuova immagine")
                }

                val imageModel = selectedImageUri ?: currentCollection.collectionImageUrl.takeIf { !it.isNullOrEmpty() }
                imageModel?.let { model ->
                    AsyncImage(
                        model = model,
                        contentDescription = "Anteprima immagine collezione",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp, max = 400.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentScale = ContentScale.Fit
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            val hasFieldChanges = name != currentCollection.name ||
                                    category != (currentCollection.category ?: "") ||
                                    description != (currentCollection.description ?: "")
                            val hasImageChange = selectedImageUri != null

                            try {
                                if (hasFieldChanges) {
                                    viewModel.updateCollection(
                                        updatedCollection = currentCollection.copy(
                                            name = name,
                                            category = category,
                                            description = description
                                        ),
                                        onSuccess = {},
                                        onFailure = { error ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Errore salvataggio: $error")
                                            }
                                        }
                                    )
                                }

                                if (hasImageChange) {
                                    viewModel.updateCollectionImage(
                                        collectionId = currentCollection.id,
                                        newImageUri = selectedImageUri!!,
                                        onSuccess = {},
                                        onFailure = { error ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Errore immagine: $error")
                                            }
                                        }
                                    )
                                }

                                navController.navigate(Screens.CollectionDetailScreen.collectionDetailRoute(currentCollection.id)) {
                                    popUpTo("edit_collection/${currentCollection.id}") { inclusive = true }
                                }

                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Errore imprevisto: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Salva modifiche")
                }
            }
        }
    }
}