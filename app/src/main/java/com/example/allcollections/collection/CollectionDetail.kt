package com.example.allcollections.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.comment.Comment
import com.example.allcollections.comment.CommentItem
import com.example.allcollections.navigation.MyTopBar
import com.example.allcollections.navigation.Screens
import com.example.allcollections.viewModel.CollectionViewModel
import com.example.allcollections.viewModel.NotificationViewModel
import com.example.allcollections.viewModel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetail(
    navController: NavController,
    collectionId: String,
    viewModel: CollectionViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val comments = remember { mutableStateListOf<Comment>() }
    val usernames = remember { mutableStateMapOf<String, String>() }
    var newComment by remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf("") }
    val objects = remember { mutableStateOf(emptyList<CollectionItem>()) }
    val collection = remember { mutableStateOf<UserCollection?>(null) }
    val currentUserId = profileViewModel.getCurrentUserId()
    val userPhotos = remember { mutableStateMapOf<String, String>() }
    val notificationViewModel: NotificationViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(collectionId) {
        viewModel.getCollectionById(
            collectionId,
            onSuccess = { collection.value = it },
            onFailure = { errorMessage.value = it }
        )

        viewModel.getItemsFromCollection(
            collectionId,
            onSuccess = { objects.value = it },
            onFailure = { errorMessage.value = it }
        )

        viewModel.getCommentsForCollection(
            collectionId,
            onSuccess = {
                comments.clear()
                comments.addAll(it)
                loadUsernamesForComments(it, usernames, viewModel)
                it.forEach { comment ->
                    if (!userPhotos.containsKey(comment.userId)) {
                        profileViewModel.getUserProfilePhoto(comment.userId) { photoUrl ->
                            userPhotos[comment.userId] = photoUrl
                        }
                    }
                }
            },
            onFailure = { errorMessage.value = it }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MyTopBar(navController = navController)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            collection.value?.let { col ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = col.name,
                            style = MaterialTheme.typography.headlineMedium
                        )

                        if (col.iduser == currentUserId) {
                            var expanded by remember { mutableStateOf(false) }

                            Box {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = "Opzioni")
                                }

                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Modifica") },
                                        onClick = {
                                            expanded = false
                                            navController.navigate("editCollection/${col.id}")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Elimina") },
                                        onClick = {
                                            expanded = false
                                            viewModel.deleteCollection(
                                                col.id,
                                                onSuccess = {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Collezione eliminata")
                                                    }
                                                    navController.popBackStack()
                                                },
                                                onFailure = { errorMessage.value = it }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Text(text = col.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    AsyncImage(
                        model = col.collectionImageUrl,
                        contentDescription = "Immagine collezione",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )

                    if (col.iduser == currentUserId) {
                        Button(
                            onClick = {
                                navController.navigate("${Screens.AddObjectCollection.name}/$collectionId")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text("Aggiungi oggetto")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Oggetti nella collezione", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (objects.value.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun oggetto presente all'interno di questa collezione!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(objects.value, key = { it.id }) { item ->
                    CollectionItemCard(
                        item = item,
                        showMenu = collection.value?.iduser == currentUserId,
                        onEdit = {
                            navController.navigate("editItem/${collectionId}/${item.id}")
                        },
                        onDelete = {
                            viewModel.deleteItemFromCollection(
                                collectionId = collectionId,
                                itemId = item.id,
                                onSuccess = {
                                    objects.value = objects.value.filter { it.id != item.id }
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Oggetto eliminato")
                                        viewModel.getItemsFromCollection(
                                            collectionId,
                                            onSuccess = { objects.value = it },
                                            onFailure = { errorMessage.value = it }
                                        )
                                    }
                                },
                                onFailure = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Errore: $error")
                                    }
                                }
                            )
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Commenti", style = MaterialTheme.typography.titleMedium)
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 150.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(comments, key = { it.timestamp }) { comment ->
                            val username = usernames[comment.userId]
                            val photoUrl = userPhotos[comment.userId]
                            CommentItem(comment = comment, username = username, photoUrl = photoUrl, navController)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newComment,
                    onValueChange = { newComment = it },
                    label = { Text("Scrivi un commento") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val userId = profileViewModel.getCurrentUserId()
                        val comment = Comment(
                            collectionId = collectionId,
                            userId = userId,
                            text = newComment
                        )
                        viewModel.addCommentToCollection(comment, notificationViewModel = notificationViewModel) { success ->
                            if (success) {
                                newComment = ""
                                loadCommentsAndUsernames(collectionId, viewModel, comments, usernames, errorMessage)
                            } else {
                                errorMessage.value = "Errore nell'invio del commento"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.End)
                ) {
                    Text("Invia")
                }

                if (errorMessage.value.isNotEmpty()) {
                    Text(
                        text = errorMessage.value,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}


fun loadUsernamesForComments(
    comments: List<Comment>,
    usernames: MutableMap<String, String>,
    viewModel: CollectionViewModel
) {
    comments.forEach { comment ->
        if (!usernames.containsKey(comment.userId)) {
            viewModel.getUsernameById(comment.userId) { username ->
                usernames[comment.userId] = username
            }
        }
    }
}

fun loadCommentsAndUsernames(
    collectionId: String,
    viewModel: CollectionViewModel,
    comments: SnapshotStateList<Comment>,
    usernames: MutableMap<String, String>,
    errorMessage: MutableState<String>
) {
    viewModel.getCommentsForCollection(
        collectionId,
        onSuccess = { commentList ->
            comments.clear()
            comments.addAll(commentList)

            commentList.forEach { comment ->
                if (!usernames.containsKey(comment.userId)) {
                    viewModel.getUsernameById(comment.userId) { username ->
                        usernames[comment.userId] = username
                    }
                }
            }
        },
        onFailure = { errorMessage.value = it }
    )
}