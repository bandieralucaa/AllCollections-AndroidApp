package com.example.allcollections.feature.publicProfile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.example.allcollections.feature.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.compose.koinViewModel

/**
 * Schermata per visualizzare il profilo pubblico di un altro utente.
 *
 * Mostra:
 * - Foto profilo e username
 * - Conteggio follower
 * - Pulsante Segui/Seguito
 * - Pulsante Messaggia
 * - Lista collezioni dell’utente
 *
 * Funziona con ProfileViewModel, CollectionViewModel e NotificationViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    userId: String,
    navController: NavController
) {
    // ViewModel
    val profileVM: ProfileViewModel = koinViewModel()
    val collectionVM: CollectionViewModel = koinViewModel()
    val notificationVM: NotificationViewModel = koinViewModel()

    // Stati locali per UI
    var username by remember { mutableStateOf("Utente") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var userCollections by remember { mutableStateOf<List<UserCollection>>(emptyList()) }
    var followerCount by remember { mutableStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    var isFollowLoading by remember { mutableStateOf(false) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Funzione per ricaricare dati dal DB
    fun refreshProfile() {
        profileVM.getUserProfilePhoto(userId) { profileImageUrl = it }
        collectionVM.getUsernameById(userId) { username = it }
        collectionVM.getCollectionsByUserId(userId) { userCollections = it }
        profileVM.isFollowing(currentUserId, userId) { isFollowing = it }
        profileVM.getFollowerCount(userId) { followerCount = it }
    }

    // Caricamento iniziale
    LaunchedEffect(userId) { refreshProfile() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilo utente") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBackIosNew,
                            contentDescription = "Torna indietro"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Foto profilo con fade-in ---
            item {
                AnimatedVisibility(
                    visible = profileImageUrl != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto profilo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }
            }

            // --- Username ---
            item { Text(username, style = MaterialTheme.typography.titleMedium) }

            // --- Conteggio follower ---
            item { Text("Follower: $followerCount") }

            // --- Pulsanti Segui e Messaggia affiancati ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsante Segui/Seguito
                    Button(
                        onClick = {
                            if (isFollowing) {
                                isFollowLoading = true
                                profileVM.unfollowUser(currentUserId, userId) { success ->
                                    if (success) refreshProfile()
                                    isFollowLoading = false
                                }
                            } else {
                                isFollowLoading = true
                                profileVM.followUser(currentUserId, userId) { success ->
                                    if (success) {
                                        refreshProfile()
                                        notificationVM.sendFollowNotification(userId)
                                    }
                                    isFollowLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isFollowLoading
                    ) {
                        if (isFollowLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (isFollowing) "Seguito" else "Segui")
                        }
                    }

                    // Pulsante Messaggia
                    Button(
                        onClick = {
                            navController.navigate(Screens.ChatScreen.createRoute(userId))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Messaggia")
                    }
                }
            }

            // --- Lista collezioni ---
            items(userCollections.size) { index ->
                val collection = userCollections[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            navController.navigate(Screens.CollectionDetailScreen.collectionDetailRoute(collection.id))
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = collection.collectionImageUrl != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            AsyncImage(
                                model = collection.collectionImageUrl,
                                contentDescription = "Immagine collezione",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(collection.name, style = MaterialTheme.typography.titleSmall)
                            Text(collection.category ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}