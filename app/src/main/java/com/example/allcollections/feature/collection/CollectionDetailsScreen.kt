package com.example.allcollections.feature.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.data.model.Comment
import com.example.allcollections.data.model.CollectionItem
import com.example.allcollections.feature.comment.CommentItem
import com.example.allcollections.feature.profile.ProfileViewModel
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
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
    val itemsList = uiState.items

    val currentUserId = profileViewModel.getCurrentUserId()

    var collection by remember { mutableStateOf<com.example.allcollections.data.model.UserCollection?>(null) }
    var isLoading by remember { mutableStateOf(true) }

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
        viewModel.loadItems(collectionId)
    }

    // ===================== CARICAMENTO COMMENTI =====================
    LaunchedEffect(collectionId) {
        viewModel.getComments(collectionId).collect { commentList ->
            comments = commentList
            commentList.forEach { comment ->
                loadUserData(comment.userId, viewModel, profileViewModel, usernames, userPhotos)
            }
        }
    }

    LaunchedEffect(itemsList.size) {
        currentItemIndex = 0
    }

    // ===================== DIALOG ELIMINA COLLEZIONE =====================
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

    // ===================== DIALOG ELIMINA OGGETTO =====================
    if (showDeleteItemDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteItemDialog = false; itemToDelete = null },
            title = { Text("Elimina oggetto") },
            text = { Text("Sei sicuro di voler eliminare '${itemToDelete?.description ?: "questo oggetto"}'?\nQuesta azione non può essere annullata.") },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.id?.let { itemId ->
                        viewModel.deleteItemFromCollection(collectionId, itemId)
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

    // ===================== SCAFFOLD =====================
    Scaffold(
        topBar = {
            if (fullscreenImageUrl == null) {
                MyTopBar(navController = navController, title = "")
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

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===================== HEADER =====================
            item {
                CollectionHeader(
                    collection = safeCollection,
                    itemsCount = uiState.items.size,
                    commentsCount = comments.size,
                    isOwner = safeCollection.iduser == currentUserId,
                    onAddObjectClick = {
                        navController.navigate(Screens.AddCollectionObjectScreen.addCollectionObjectRoute(collectionId))
                    },
                    onImageClick = { fullscreenImageUrl = it },
                    onMenuClick = { showMenu = true }
                )

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }

            // ===================== CAROSELLO OGGETTI =====================
            if (uiState.items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nessun oggetto in questa collezione", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item {
                    ItemsCarousel(
                        items = itemsList,
                        currentIndex = currentItemIndex,
                        onIndexChange = { currentItemIndex = it },
                        isOwner = safeCollection.iduser == currentUserId,
                        onEdit = { item ->
                            navController.navigate(
                                Screens.EditCollectionItemScreen.editCollectionItemRoute(collectionId, item.id ?: "")
                            )
                        },
                        onDelete = { item ->
                            itemToDelete = item
                            showDeleteItemDialog = true
                        },
                        onImageClick = { fullscreenImageUrl = it }
                    )
                }
            }

            // ===================== SEZIONE COMMENTI =====================
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
                    onDeleteComment = { comment ->
                        viewModel.deleteComment(comment.id)
                    },
                    onEditComment = { comment, newText ->
                        viewModel.updateComment(comment.id, newText)
                    },
                    navController = navController
                )
            }
        }

        // ===================== FULLSCREEN IMAGE =====================
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

// ===================== COMPONENTI =====================

@Composable
fun CollectionHeader(
    collection: com.example.allcollections.data.model.UserCollection,
    itemsCount: Int,
    commentsCount: Int,
    isOwner: Boolean,
    onAddObjectClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
        ) {
            if (!collection.collectionImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = collection.collectionImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clickable { collection.collectionImageUrl?.let { onImageClick(it) } },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.White.copy(alpha = 0.5f))
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(text = collection.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(text = collection.category ?: "Senza categoria", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Text(text = itemsCount.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Text(text = commentsCount.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }

            if (isOwner) {
                Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opzioni", tint = Color.White)
                    }
                }
            }
        }

        if (!collection.description.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Text(text = collection.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            }
        }

        if (isOwner) {
            Button(
                onClick = onAddObjectClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi oggetto")
            }
        }
    }
}

@Composable
fun ItemsCarousel(
    items: List<CollectionItem>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    isOwner: Boolean,
    onEdit: (CollectionItem) -> Unit,
    onDelete: (CollectionItem) -> Unit,
    onImageClick: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(text = "Oggetto ${currentIndex + 1} di ${items.size}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))

        Box(
            modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount > 0) { if (currentIndex > 0) onIndexChange(currentIndex - 1) }
                    else if (dragAmount < 0) { if (currentIndex < items.size - 1) onIndexChange(currentIndex + 1) }
                }
            }
        ) {
            CarouselItemCard(
                item = items[currentIndex],
                isOwner = isOwner,
                onEdit = { onEdit(items[currentIndex]) },
                onDelete = { onDelete(items[currentIndex]) },
                onImageClick = onImageClick
            )

            IconButton(
                onClick = { if (currentIndex > 0) onIndexChange(currentIndex - 1) },
                enabled = currentIndex > 0,
                modifier = Modifier.align(Alignment.CenterStart).size(48.dp).padding(start = 8.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Oggetto precedente", tint = Color.White)
            }

            IconButton(
                onClick = { if (currentIndex < items.size - 1) onIndexChange(currentIndex + 1) },
                enabled = currentIndex < items.size - 1,
                modifier = Modifier.align(Alignment.CenterEnd).size(48.dp).padding(end = 8.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Oggetto successivo", tint = Color.White)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(items.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentIndex) 12.dp else 8.dp)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(if (index == currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                )
            }
        }
    }
}

@Composable
fun CarouselItemCard(
    item: CollectionItem,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit
) {
    var showItemMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(350.dp).clickable { item.imageUrl?.let { onImageClick(it) } },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(250.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)), startY = 0f, endY = 300f))
                        .padding(16.dp)
                ) {
                    Text(text = item.description, color = Color.White, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }

                if (isOwner) {
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        IconButton(onClick = { showItemMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opzioni", tint = Color.White)
                        }
                        DropdownMenu(expanded = showItemMenu, onDismissRequest = { showItemMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Modifica") },
                                onClick = { showItemMenu = false; onEdit() },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Elimina", color = MaterialTheme.colorScheme.error) },
                                onClick = { showItemMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            if (!item.description.isNullOrBlank()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Divider(modifier = Modifier.padding(bottom = 12.dp))
                    Text(text = "Descrizione", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
                    Text(text = item.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun CommentsSection(
    comments: List<Comment>,
    usernames: Map<String, String>,
    userPhotos: Map<String, String>,
    currentUserId: String?,
    showComments: Boolean,
    onToggleComments: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (Comment) -> Unit,
    onEditComment: (Comment, String) -> Unit,
    navController: NavController
) {
    var newComment by remember { mutableStateOf("") }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }
    var commentToEdit by remember { mutableStateOf<Comment?>(null) }
    var editText by remember { mutableStateOf("") }

    // Dialog elimina commento
    commentToDelete?.let { comment ->
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = { Text("Elimina commento") },
            text = { Text("Sei sicuro di voler eliminare questo commento?") },
            confirmButton = {
                TextButton(onClick = { onDeleteComment(comment); commentToDelete = null }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { commentToDelete = null }) { Text("Annulla") }
            }
        )
    }

    // Dialog modifica commento
    commentToEdit?.let { comment ->
        AlertDialog(
            onDismissRequest = { commentToEdit = null },
            title = { Text("Modifica commento") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    placeholder = { Text("Scrivi il tuo commento...") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editText.isNotBlank()) { onEditComment(comment, editText); commentToEdit = null }
                }) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = { commentToEdit = null }) { Text("Annulla") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleComments() }.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Commenti (${comments.size})", style = MaterialTheme.typography.titleMedium)
                }
                Icon(if (showComments) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }

            if (showComments) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (comments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Nessun commento. Sii il primo a commentare!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(comments) { comment ->
                            CommentItem(
                                comment = comment,
                                username = usernames[comment.userId] ?: "Utente",
                                photoUrl = userPhotos[comment.userId],
                                navController = navController,
                                onDelete = { commentToDelete = it },
                                onEdit = { editText = it.text; commentToEdit = it }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                if (currentUserId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newComment,
                            onValueChange = { newComment = it },
                            placeholder = { Text("Scrivi un commento...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                if (newComment.isNotBlank()) { onAddComment(newComment); newComment = "" }
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Invia", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}