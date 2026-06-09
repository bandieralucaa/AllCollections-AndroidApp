package com.example.allcollections.feature.collection

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.feature.collection.components.PRESET_CATEGORIES
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Schermata per la creazione di una nuova collezione.
 *
 * Raccoglie nome (obbligatorio), categoria (obbligatoria, da lista predefinita o
 * personalizzata tramite "Altro") e descrizione (opzionale). Al salvataggio su
 * Firestore naviga automaticamente ad [AddCollectionImageScreen] per aggiungere
 * la copertina; lo stato `createdCollectionId` è resettato con un breve delay
 * per evitare ri-navigazioni spurie al rientro nella schermata.
 *
 * @param navController NavController per la navigazione.
 * @param viewModel ViewModel che gestisce la creazione della collezione su Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCollectionScreen(
    navController: NavController,
    viewModel: CollectionViewModel = koinViewModel()
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf<String?>(null) }
    var isCustomCategory by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val createState by viewModel.createCollectionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Naviga alla schermata immagine appena la collezione è stata creata con successo
    LaunchedEffect(createState.createdCollectionId) {
        createState.createdCollectionId?.let { collectionId ->
            navController.navigate(
                Screens.AddCollectionImageScreen.createRoute(collectionId)
            ) {
                popUpTo(Screens.AddCollectionScreen.route) { inclusive = true }
                launchSingleTop = true
            }
            // Reset con delay per evitare ri-navigazioni spurie alla prossima composizione
            launch {
                kotlinx.coroutines.delay(300)
                viewModel.resetCreateCollectionState()
            }
        }
    }

    // Mostra gli errori di creazione tramite Snackbar
    LaunchedEffect(createState.error) {
        createState.error?.let { error ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { MyTopBar(navController = navController, title = "Nuova collezione") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─────────── Nome ───────────
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome *") },
                isError = name.isEmpty() && createState.error != null,
                supportingText = { if (name.isEmpty()) Text("Campo obbligatorio") },
                modifier = Modifier.fillMaxWidth()
            )

            // ─────────── Selettore categoria (read-only + click apre dialog) ───────────
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
                    isError = (category.isEmpty() && createState.error != null),
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
                        disabledIndicatorColor = if (category.isEmpty() && createState.error != null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    enabled = false
                )
            }

            if (category.isEmpty() && createState.error != null) {
                Text(
                    text = "Seleziona una categoria",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            // ─────────── Dialog selezione categoria ───────────
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

            // ─────────── Campo categoria personalizzata ───────────
            if (isCustomCategory) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Scrivi la tua categoria...") },
                    isError = category.isEmpty() && createState.error != null,
                    supportingText = { if (category.isEmpty()) Text("Campo obbligatorio") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // ─────────── Descrizione (opzionale) ───────────
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrizione") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // ─────────── Bottone Prosegui ───────────
            Button(
                onClick = {
                    if (name.isBlank() || category.isBlank()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Compila tutti i campi obbligatori",
                                duration = SnackbarDuration.Short
                            )
                        }
                        return@Button
                    }
                    viewModel.saveCollection(
                        name = name.trim(),
                        category = category.trim(),
                        description = description.trim()
                    )
                },
                enabled = !createState.isLoading && name.isNotBlank() && category.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (createState.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Creazione in corso...")
                    }
                } else {
                    Text("Prosegui")
                }
            }

            if (createState.isLoading) {
                Text(
                    "Sto creando la tua collezione...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}