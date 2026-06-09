package com.example.allcollections.feature.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.CollectionItem
import com.example.allcollections.data.model.Comment
import com.example.allcollections.data.model.UserData
import com.example.allcollections.feature.collection.components.CollectionHeader
import com.example.allcollections.feature.collection.components.CommentsSection
import com.example.allcollections.feature.collection.components.ItemsCarousel
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.example.allcollections.feature.profile.ProfileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Schermata di dettaglio di una collezione.
 *
 * Contenuto principale:
 * - **[CollectionHeader]**: copertina con nome, categoria, statistiche (oggetti, commenti, like)
 *   e menu owner (modifica/elimina). Il tap sulla copertina apre la visualizzazione fullscreen.
 * - **[ItemsCarousel]**: carosello orizzontale degli oggetti con swipe/frecce. Per ogni oggetto
 *   mostra immagine, descrizione, commenti dell'oggetto e pulsanti modifica/elimina (solo owner).
 * - **[CommentsSection]**: commenti della collezione con possibilità di aggiungere, modificare
 *   ed eliminare commenti.
 *
 * La navigazione da notifica (`itemId` non null) fa scorrere automaticamente il carosello
 * all'oggetto corrispondente al primo caricamento degli items.
 *
 * @param navController NavController per la navigazione.
 * @param collectionId ID della collezione da visualizzare.
 * @param itemId ID opzionale dell'oggetto a cui scorrere automaticamente il carosello.
 *   Usato quando si naviga da una notifica di commento su un oggetto.
 * @param viewModel ViewModel che gestisce items, commenti, like e operazioni sulla collezione.
 * @param profileViewModel ViewModel per recuperare dati utente (username, foto profilo).
 * @param notificationViewModel ViewModel per inviare notifiche ai destinatari.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    navController: NavController,
    collectionId: String,
    itemId: String? = null,
    viewModel: CollectionViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel(),
    notificationViewModel: NotificationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteItemDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<CollectionItem?>(null) }
    var showComments by remember { mutableStateOf(true) }
    var currentItemIndex by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var likesCount by remember { mutableStateOf(0) }
    var showLikersDialog by remember { mutableStateOf(false) }
    var likers by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var initialItemIndexApplied by remember { mutableStateOf(false) }

    val itemsList = uiState.items
    val currentUserId = profileViewModel.getCurrentUserId()
    var collection by remember { mutableStateOf<com.example.allcollections.data.model.UserCollection?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }

    // Mappe username/foto condivise tra commenti collezione e commenti oggetti
    val usernames = remember { mutableStateMapOf<String, String>() }
    val userPhotos = remember { mutableStateMapOf<String, String>() }
    var itemComments by remember { mutableStateOf<List<Comment>>(emptyList()) }

    // Ascolta eventi one-shot del ViewModel (eliminazione collezione, errori)
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

    // Carica collezione, oggetti e conteggio like al primo accesso
    LaunchedEffect(collectionId) {
        isLoading = true
        viewModel.getCollectionById(
            collectionId = collectionId,
            onSuccess = { collection = it; isLoading = false },
            onFailure = { error ->
                isLoading = false
                scope.launch { snackbarHostState.showSnackbar("Errore caricamento collezione: $error") }
            }
        )
        viewModel.loadItems(collectionId)
        viewModel.getLikesCount(collectionId) { likesCount = it }
    }

    // Osserva i commenti della collezione in tempo reale
    LaunchedEffect(collectionId) {
        viewModel.getComments(collectionId).collect { commentList ->
            comments = commentList
            commentList.forEach { comment ->
                loadUserData(comment.userId, viewModel, profileViewModel, usernames, userPhotos)
            }
        }
    }

    // Osserva i commenti dell'oggetto corrente nel carosello in tempo reale
    LaunchedEffect(itemsList.size, currentItemIndex) {
        val currentItemId = itemsList.getOrNull(currentItemIndex)?.id ?: return@LaunchedEffect
        itemComments = emptyList()
        viewModel.getItemComments(collectionId, currentItemId).collect { commentList ->
            itemComments = commentList
            commentList.forEach { comment ->
                loadUserData(comment.userId, viewModel, profileViewModel, usernames, userPhotos)
            }
        }
    }

    // Mantiene l'indice del carosello nei bounds validi se gli items cambiano
    LaunchedEffect(itemsList.size) {
        currentItemIndex = currentItemIndex.coerceIn(0, (itemsList.size - 1).coerceAtLeast(0))
    }

    // Se arriva un itemId dalla notifica, salta all'indice corrispondente una sola volta
    LaunchedEffect(itemsList) {
        if (itemId != null && !initialItemIndexApplied && itemsList.isNotEmpty()) {
            val index = itemsList.indexOfFirst { it.id == itemId }
            if (index >= 0) {
                currentItemIndex = index
                initialItemIndexApplied = true
            }
        }
    }

    // ─────────── Dialog eliminazione collezione ───────────
    collection?.let { col ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Elimina collezione") },
                text = { Text("Sei sicuro di voler eliminare '${col.name}'?\nQuesta azione non può essere annullata.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCollection(col.id)
                        scope.launch { snackbarHostState.showSnackbar("Eliminazione in corso...") }
                    }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") }
                }
            )
        }
    }

    // ─────────── Dialog eliminazione oggetto ───────────
    if (showDeleteItemDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteItemDialog = false; itemToDelete = null },
            title = { Text("Elimina oggetto") },
            text = { Text("Sei sicuro di voler eliminare '${itemToDelete?.description ?: "questo oggetto"}'?\nQuesta azione non può essere annullata.") },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.id?.let { id ->
                        viewModel.deleteItemFromCollection(collectionId, id)
                        scope.launch { snackbarHostState.showSnackbar("Oggetto eliminato") }
                    }
                    showDeleteItemDialog = false
                    itemToDelete = null
                }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteItemDialog = false; itemToDelete = null }) { Text("Annulla") }
            }
        )
    }

    // ─────────── Dialog lista likers (solo owner) ───────────
    if (showLikersDialog) {
        AlertDialog(
            onDismissRequest = { showLikersDialog = false },
            title = { Text("Mi piace (${likers.size})") },
            text = {
                if (likers.isEmpty()) {
                    Text("Nessun like ancora.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(likers) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showLikersDialog = false
                                        navController.navigate(Screens.PublicProfileScreen.createRoute(user.userId))
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (user.profileImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = user.profileImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text("@${user.username}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLikersDialog = false }) { Text("Chiudi") }
            }
        )
    }

    Scaffold(
        topBar = {
            // Nasconde la TopBar quando è attiva la visualizzazione fullscreen
            if (fullscreenImageUrl == null) {
                MyTopBar(navController = navController, title = "")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->

        // Stato di caricamento
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Caricamento collezione...")
                }
            }
            return@Scaffold
        }

        // Collezione non trovata
        if (collection == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Collezione non trovata", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { navController.popBackStack() }) { Text("Torna indietro") }
                }
            }
            return@Scaffold
        }

        val safeCollection = collection!!
        val isOwner = safeCollection.iduser == currentUserId

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con copertina, statistiche e menu owner
            item {
                CollectionHeader(
                    collection = safeCollection,
                    itemsCount = itemsList.size,
                    commentsCount = comments.size,
                    likesCount = likesCount,
                    isOwner = isOwner,
                    onAddObjectClick = {
                        navController.navigate(Screens.AddCollectionObjectScreen.addCollectionObjectRoute(collectionId))
                    },
                    onImageClick = { fullscreenImageUrl = it },
                    onMenuClick = { showMenu = true },
                    onLikesCountClick = {
                        if (isOwner) {
                            viewModel.getLikers(collectionId) { likers = it }
                            showLikersDialog = true
                        }
                    }
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Modifica collezione") },
                        onClick = {
                            showMenu = false
                            navController.navigate(Screens.EditCollectionScreen.editCollectionRoute(safeCollection.id))
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Elimina collezione", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; showDeleteDialog = true },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }

            // Stato vuoto oggetti
            if (itemsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Nessun oggetto in questa collezione",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Carosello oggetti
                item {
                    ItemsCarousel(
                        items = itemsList,
                        currentIndex = currentItemIndex,
                        onIndexChange = { currentItemIndex = it },
                        isOwner = isOwner,
                        onEdit = { item ->
                            navController.navigate(
                                Screens.EditCollectionItemScreen.editCollectionItemRoute(collectionId, item.id ?: "")
                            )
                        },
                        onDelete = { item ->
                            itemToDelete = item
                            showDeleteItemDialog = true
                        },
                        onImageClick = { fullscreenImageUrl = it },
                        itemComments = itemComments,
                        usernames = usernames,
                        userPhotos = userPhotos,
                        currentUserId = currentUserId,
                        onAddItemComment = { text ->
                            val currentItemId = itemsList.getOrNull(currentItemIndex)?.id ?: return@ItemsCarousel
                            if (currentUserId != null) {
                                val comment = Comment.createForItem(
                                    collectionId = collectionId,
                                    itemId = currentItemId,
                                    userId = currentUserId,
                                    text = text,
                                    username = usernames[currentUserId] ?: ""
                                )
                                viewModel.addItemComment(comment, notificationViewModel)
                            }
                        },
                        onDeleteItemComment = { comment -> viewModel.deleteComment(comment.id) },
                        onEditItemComment = { comment, newText -> viewModel.updateComment(comment.id, newText) },
                        navController = navController
                    )
                }
            }

            // Sezione commenti della collezione
            item {
                CommentsSection(
                    comments = comments,
                    usernames = usernames,
                    userPhotos = userPhotos,
                    currentUserId = currentUserId,
                    showComments = showComments,
                    onToggleComments = { showComments = !showComments },
                    onAddComment = { text ->
                        if (currentUserId != null) {
                            val comment = Comment.create(
                                collectionId = collectionId,
                                userId = currentUserId,
                                text = text,
                                username = usernames[currentUserId] ?: ""
                            )
                            viewModel.addComment(comment, notificationViewModel)
                        }
                    },
                    onDeleteComment = { comment -> viewModel.deleteComment(comment.id) },
                    onEditComment = { comment, newText -> viewModel.updateComment(comment.id, newText) },
                    navController = navController
                )
            }
        }

        // Visualizzazione fullscreen immagine (overlay sopra la LazyColumn)
        fullscreenImageUrl?.let { url ->
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = url,
                    contentDescription = "Immagine ingrandita",
                    modifier = Modifier.fillMaxSize().clickable { fullscreenImageUrl = null },
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { fullscreenImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Chiudi", tint = Color.White)
                }
            }
        }
    }
}

/**
 * Carica username e foto profilo di un utente e li inserisce nelle mappe condivise.
 *
 * Esegue le chiamate solo se i dati non sono già presenti nella mappa,
 * evitando query duplicate a Firestore per lo stesso userId nella stessa schermata.
 *
 * @param userId ID dell'utente di cui caricare i dati.
 * @param viewModel Usato per recuperare lo username.
 * @param profileViewModel Usato per recuperare l'URL della foto profilo.
 * @param usernames Mappa mutabile `userId → username` da popolare.
 * @param userPhotos Mappa mutabile `userId → URL foto profilo` da popolare.
 */
private fun loadUserData(
    userId: String,
    viewModel: CollectionViewModel,
    profileViewModel: ProfileViewModel,
    usernames: MutableMap<String, String>,
    userPhotos: MutableMap<String, String>
) {
    if (!usernames.containsKey(userId)) {
        viewModel.getUsernameById(userId) { username -> usernames[userId] = username }
    }
    if (!userPhotos.containsKey(userId)) {
        profileViewModel.getUserProfilePhoto(userId) { photoUrl -> userPhotos[userId] = photoUrl ?: "" }
    }
}