package com.example.allcollections.feature.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.CollectionCardLayout
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.components.CollectionCard
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

/**
 * Schermata principale delle collezioni dell'utente corrente.
 *
 * Mostra tutte le proprie collezioni in una lista verticale con layout orizzontale.
 * Le collezioni vengono ricaricate automaticamente ad ogni ripresa della schermata
 * (evento `ON_RESUME`) grazie a un [LifecycleEventObserver], in modo da riflettere
 * eventuali modifiche effettuate in altre schermate (es. modifica, eliminazione).
 *
 * Il [FloatingActionButton] naviga alla creazione di una nuova collezione.
 * Il menu contestuale su ogni card permette modifica o eliminazione con dialog di conferma.
 *
 * @param navController NavController per la navigazione.
 * @param viewModel ViewModel che gestisce il caricamento e l'eliminazione delle collezioni.
 */
@Composable
fun MyCollectionsScreen(
    navController: NavController,
    viewModel: CollectionViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var collectionToDelete by remember { mutableStateOf<UserCollection?>(null) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var isScreenActive by remember { mutableStateOf(false) }

    // Ricarica le collezioni ogni volta che la schermata torna in primo piano
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            isScreenActive = event == androidx.lifecycle.Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isScreenActive) {
        if (isScreenActive) {
            Firebase.auth.currentUser?.uid?.let { viewModel.loadUserCollections(it) }
        }
    }

    // Ascolta eventi one-shot (eliminazione, errori)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CollectionViewModel.CollectionEvent.CollectionDeleted -> {
                    Firebase.auth.currentUser?.uid?.let { userId ->
                        viewModel.loadUserCollections(userId)
                    }
                }
                is CollectionViewModel.CollectionEvent.Error -> {
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                }
            }
        }
    }

    // Mostra eventuali errori di stato tramite Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
        }
    }

    // Caricamento iniziale
    LaunchedEffect(Unit) {
        Firebase.auth.currentUser?.uid?.let { viewModel.loadUserCollections(it) }
    }

    // Dialog di conferma eliminazione
    collectionToDelete?.let { collection ->
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = { Text("Elimina collezione") },
            text = { Text("Sei sicuro di voler eliminare '${collection.name}'? L'operazione non è reversibile.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCollection(collection.id)
                        collectionToDelete = null
                    }
                ) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToDelete = null }) { Text("Annulla") }
            }
        )
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
                onClick = { navController.navigate(Screens.AddCollectionScreen.route) }
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
                            textAlign = TextAlign.Center
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
                                layoutType = CollectionCardLayout.Horizontal,
                                showMenu = true,
                                onEdit = {
                                    navController.navigate(
                                        Screens.EditCollectionScreen.editCollectionRoute(collection.id)
                                    )
                                },
                                onDelete = { collectionToDelete = collection },
                                onCardClick = { collectionId ->
                                    navController.navigate(
                                        Screens.CollectionDetailScreen.collectionDetailRoute(collectionId)
                                    )
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