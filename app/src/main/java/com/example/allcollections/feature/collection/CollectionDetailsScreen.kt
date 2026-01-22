package com.example.allcollections.feature.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.data.model.Comment
import com.example.allcollections.feature.collection.components.CollectionItemCard
import com.example.allcollections.feature.comment.CommentItem
import com.example.allcollections.feature.notification.NotificationViewModel
import com.example.allcollections.feature.profile.ProfileViewModel
import com.example.allcollections.core.ui.MyTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var newComment by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Ottieni ID utente corrente dal ProfileViewModel
    val currentUserId = profileViewModel.getCurrentUserId()

    // Collezione caricata
    var collection by remember { mutableStateOf<com.example.allcollections.data.model.UserCollection?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Commenti
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    val usernames = remember { mutableStateMapOf<String, String>() }
    val userPhotos = remember { mutableStateMapOf<String, String>() }

    var showMenu by remember { mutableStateOf(false) }

    // ===================== ASCOLTA EVENTI =====================
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CollectionViewModel.CollectionEvent.CollectionDeleted -> {
                    if (event.collectionId == collectionId) {
                        scope.launch { snackbarHostState.showSnackbar("Collezione eliminata") }
                        delay(300)
                        navController.popBackStack()
                    }
                }
                is CollectionViewModel.CollectionEvent.Error -> {
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                }
            }
        }
    }

    // ===================== CARICAMENTO COLLEZIONE =====================
    LaunchedEffect(collectionId) {
        isLoading = true
        viewModel.getCollectionById(
            collectionId = collectionId,
            onSuccess = { loadedCollection ->
                collection = loadedCollection
                isLoading = false
            },
            onFailure = { error ->
                isLoading = false
                scope.launch { snackbarHostState.showSnackbar("Errore caricamento collezione: $error") }
            }
        )

        // Carica oggetti e commenti
        viewModel.loadItems(collectionId)
        viewModel.getComments(collectionId).collect { commentList ->
            comments = commentList
            commentList.forEach { comment ->
                if (!usernames.containsKey(comment.userId)) {
                    viewModel.getUsernameById(comment.userId) { username ->
                        usernames[comment.userId] = username
                    }
                }
                if (!userPhotos.containsKey(comment.userId)) {
                    profileViewModel.getUserProfilePhoto(comment.userId) { photoUrl ->
                        // Assicurati che photoUrl non sia null
                        userPhotos[comment.userId] = photoUrl ?: ""
                    }
                }
            }
        }
    }

    // ===================== DIALOG ELIMINA =====================
    collection?.let { col ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Elimina collezione") },
                text = { Text("Sei sicuro di voler eliminare '${col.name}'?\nQuesta azione non può essere annullata.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteCollection(col.id)
                            scope.launch { snackbarHostState.showSnackbar("Eliminazione in corso...") }
                        }
                    ) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") }
                }
            )
        }
    }

    // ===================== SCAFFOLD =====================
    Scaffold(
        topBar = {
            if (fullscreenImageUrl == null) {
                val currentCollection = collection
                MyTopBar(
                    navController = navController,
                    title = if (isLoading) "Caricamento..." else (currentCollection?.name ?: "Dettagli Collezione"),
                    actions = {
                        if (currentCollection?.iduser == currentUserId) {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu opzioni")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Modifica collezione") },
                                        onClick = {
                                            showMenu = false
                                            currentCollection?.id?.let { id ->
                                                navController.navigate("editCollection/$id")
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Elimina collezione", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMenu = false
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Caricamento collezione...")
                }
            }
            return@Scaffold
        }

        if (collection == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Collezione non trovata", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { navController.popBackStack() }) { Text("Torna indietro") }
                }
            }
            return@Scaffold
        }

        val safeCollection = collection!!

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {

            // HEADER COLLEZIONE
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Categoria: ${safeCollection.category}", style = MaterialTheme.typography.bodyMedium)
                    if (safeCollection.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(safeCollection.description, style = MaterialTheme.typography.bodyLarge)
                    }

                    safeCollection.collectionImageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        Spacer(modifier = Modifier.height(16.dp))
                        AsyncImage(
                            model = url,
                            contentDescription = "Immagine collezione",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { fullscreenImageUrl = url },
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (safeCollection.iduser == currentUserId) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.navigate("add_object_collection/$collectionId") }, modifier = Modifier.fillMaxWidth()) {
                            Text("➕ Aggiungi oggetto")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Divider()
                }
            }

            // OGGETTI
            item { Text("Oggetti nella collezione", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp)) }

            if (uiState.items.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Nessun oggetto presente", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.items, key = { it.id ?: "" }) { item ->
                    CollectionItemCard(
                        item = item,
                        showMenu = safeCollection.iduser == currentUserId,
                        onEdit = { navController.navigate("editItem/$collectionId/${item.id}") },
                        onDelete = {
                            scope.launch {
                                val result = snackbarHostState.showSnackbar("Eliminare questo oggetto?", "Elimina")
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.deleteItemFromCollection(collectionId, item.id ?: "")
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        onImageClick = { fullscreenImageUrl = it }
                    )
                }
            }

            // COMMENTI
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Commenti (${comments.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            if (comments.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("Nessun commento. Sii il primo a commentare!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(comments, key = { it.id ?: "" }) { comment ->
                    CommentItem(
                        comment = comment,
                        username = usernames[comment.userId] ?: "Utente",
                        photoUrl = userPhotos[comment.userId] ?: "",
                        navController = navController
                    )
                }
            }

            // NUOVO COMMENTO
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        label = { Text("Scrivi un commento") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newComment.isNotBlank() && currentUserId != null) {
                                val comment = Comment(collectionId = collectionId, userId = currentUserId, text = newComment)
                                viewModel.addComment(comment, notificationViewModel)
                                newComment = ""
                            } else if (currentUserId == null) {
                                scope.launch { snackbarHostState.showSnackbar("Devi accedere per commentare") }
                            }
                        },
                        enabled = newComment.isNotBlank() && currentUserId != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Invia commento") }
                }
            }
        }

        // FULLSCREEN IMAGE
        fullscreenImageUrl?.let { url ->
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(model = url, contentDescription = "Immagine ingrandita", modifier = Modifier.fillMaxSize().clickable { fullscreenImageUrl = null }, contentScale = ContentScale.Fit)
                IconButton(
                    onClick = { fullscreenImageUrl = null },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(48.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                ) { Icon(Icons.Default.ArrowBack, contentDescription = "Chiudi", tint = Color.White) }
            }
        }
    }
}
