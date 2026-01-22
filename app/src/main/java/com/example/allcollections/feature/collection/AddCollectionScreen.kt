package com.example.allcollections.feature.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.core.navigation.Screens
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Schermata per creare una nuova collezione.
 * Permette di inserire nome, categoria e descrizione.
 * Dopo il salvataggio, naviga allo screen di aggiunta immagine.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCollectionScreen(
    navController: NavController,
    viewModel: CollectionViewModel = koinViewModel()
) {
    // Stati locali per i campi del form
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Stato dalla ViewModel
    val createState by viewModel.createCollectionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val scrollState = rememberScrollState()

    // Gestione navigazione quando la creazione ha successo
    LaunchedEffect(createState.createdCollectionId) {
        createState.createdCollectionId?.let { collectionId ->
            // Naviga alla schermata di aggiunta immagine
            navController.navigate(
                Screens.AddCollectionImageScreen.createRoute(collectionId)
            ) {
                // Rimuovi questa schermata dallo stack di navigazione
                popUpTo(Screens.AddCollectionScreen.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }

            // Resetta lo stato dopo un breve delay
            launch {
                kotlinx.coroutines.delay(300)
                viewModel.resetCreateCollectionState()
            }
        }
    }

    // Gestione errori con Snackbar
    LaunchedEffect(createState.error) {
        createState.error?.let { error ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { MyTopBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Campo Nome
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome *") },
                isError = name.isEmpty() && createState.error != null,
                supportingText = {
                    if (name.isEmpty()) {
                        Text("Campo obbligatorio")
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo Categoria
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Categoria *") },
                isError = category.isEmpty() && createState.error != null,
                supportingText = {
                    if (category.isEmpty()) {
                        Text("Campo obbligatorio")
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo Descrizione (opzionale)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrizione") },
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bottone di creazione
            Button(
                onClick = {
                    // Valida i campi obbligatori
                    if (name.isBlank() || category.isBlank()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Compila tutti i campi obbligatori",
                                duration = SnackbarDuration.Short
                            )
                        }
                        return@Button
                    }

                    // Chiama la ViewModel per salvare la collezione
                    viewModel.saveCollection(
                        name = name.trim(),
                        category = category.trim(),
                        description = description.trim()
                    )
                },
                enabled = !createState.isLoading && name.isNotBlank() && category.isNotBlank(),
                modifier = Modifier.fillMaxWidth(0.8f)
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

            // Messaggi informativi
            if (createState.isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Sto creando la tua collezione...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}