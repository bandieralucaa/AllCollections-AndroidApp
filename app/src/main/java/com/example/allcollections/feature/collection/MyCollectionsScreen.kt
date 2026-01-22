package com.example.allcollections.feature.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.components.CollectionCard
import com.example.allcollections.feature.collection.components.CollectionCardLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

/**
 * Schermata principale delle collezioni dell'utente.
 *
 * Mostra tutte le collezioni dell'utente corrente in una lista verticale.
 * Permette di navigare al dettaglio di una collezione e di creare nuove collezioni
 * tramite il FloatingActionButton.
 */
@Composable
fun MyCollectionsScreen(
    navController: NavController,
    viewModel: CollectionViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Aggiungi questo: rileva quando lo screen diventa attivo
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var isScreenActive by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            isScreenActive = event == androidx.lifecycle.Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Ricarica le collezioni quando lo screen diventa attivo
    LaunchedEffect(isScreenActive) {
        if (isScreenActive) {
            Firebase.auth.currentUser?.uid?.let { viewModel.loadUserCollections(it) }
        }
    }

    // Ascolta gli eventi di eliminazione
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CollectionViewModel.CollectionEvent.CollectionDeleted -> {
                    // Ricarica le collezioni
                    Firebase.auth.currentUser?.uid?.let { userId ->
                        viewModel.loadUserCollections(userId)
                    }
                }
                is CollectionViewModel.CollectionEvent.Error -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
            }
        }
    }

    // Mostra eventuali errori con Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    // Carica le collezioni dell'utente al primo avvio
    LaunchedEffect(Unit) {
        Firebase.auth.currentUser?.uid?.let { viewModel.loadUserCollections(it) }
    }

    Scaffold(
        topBar = {
            MyTopBar(
                navController = navController,
                title = "Le Mie Collezioni"
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screens.AddCollectionScreen.route)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crea nuova collezione"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.collections.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Non hai ancora nessuna collezione",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Crea la tua prima collezione con il pulsante +!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.collections, key = { it.id }) { collection ->
                            CollectionCard(
                                collection = collection,
                                layoutType = CollectionCardLayout.Horizontal, // Layout orizzontale per lista
                                showMenu = true, // Mostra menu modifica/elimina
                                onEdit = {
                                    navController.navigate("editCollection/${collection.id}")
                                },
                                onDelete = {
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Eliminare '${collection.name}'?",
                                            actionLabel = "Elimina",
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.deleteCollection(collection.id)
                                        }
                                    }
                                },
                                onCardClick = { collectionId ->
                                    navController.navigate("collection_detail/$collectionId")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}