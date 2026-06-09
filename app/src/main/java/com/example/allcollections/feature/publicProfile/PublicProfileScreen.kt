package com.example.allcollections.feature.publicProfile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.example.allcollections.feature.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.compose.koinViewModel

/**
 * Schermata del profilo pubblico di un altro utente.
 *
 * Mostra:
 * - Foto profilo, username, biografia, conteggio follower
 * - Pulsanti "Segui/Seguito" e "Messaggia"
 * - Lista delle collezioni pubbliche dell'utente con supporto like (solo se l'utente loggato non è il proprietario)
 *
 * La navigazione follow/unfollow aggiorna il profilo in tempo reale e invia una notifica push
 * al destinatario tramite [NotificationViewModel.sendFollowNotification].
 *
 * @param userId ID dell'utente di cui visualizzare il profilo.
 * @param navController Controller per la navigazione.
 *
 * @see ProfileViewModel
 * @see CollectionViewModel
 * @see NotificationViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    userId: String,
    navController: NavController
) {
    val profileVM: ProfileViewModel = koinViewModel()
    val collectionVM: CollectionViewModel = koinViewModel()
    val notificationVM: NotificationViewModel = koinViewModel()

    // Stato UI locale
    var username by remember { mutableStateOf("Utente") }
    var bio by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var userCollections by remember { mutableStateOf<List<UserCollection>>(emptyList()) }
    var followerCount by remember { mutableStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    var isFollowLoading by remember { mutableStateOf(false) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Mappe per lo stato like delle collezioni (aggiornamento ottimistico)
    val likedMap = remember { mutableStateMapOf<String, Boolean>() }
    val likesCountMap = remember { mutableStateMapOf<String, Int>() }

    /**
     * Carica lo stato like e il conteggio like per una lista di collezioni.
     */
    fun loadLikesForCollections(collections: List<UserCollection>) {
        collections.forEach { collection ->
            if (!likedMap.containsKey(collection.id)) {
                collectionVM.hasLiked(collection.id) { likedMap[collection.id] = it }
                collectionVM.getLikesCount(collection.id) { likesCountMap[collection.id] = it }
            }
        }
    }

    /**
     * Aggiorna tutti i dati del profilo pubblico.
     */
    fun refreshProfile() {
        profileVM.getUserProfilePhoto(userId) { profileImageUrl = it }
        collectionVM.getUsernameById(userId) { username = it }
        collectionVM.getCollectionsByUserId(userId) { collections ->
            userCollections = collections
            loadLikesForCollections(collections)
        }
        profileVM.isFollowing(currentUserId, userId) { isFollowing = it }
        profileVM.getFollowerCount(userId) { followerCount = it }
    }

    // Caricamento iniziale dei dati
    LaunchedEffect(userId) {
        refreshProfile()
        profileVM.getUserBio(userId) { bio = it }
    }

    Scaffold(
        topBar = {
            MyTopBar(navController = navController, title = "Profilo utente")
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
            // Foto profilo
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
                        contentDescription = "Foto profilo di $username",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }
            }

            // Username
            item {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Biografia
            if (bio.isNotBlank()) {
                item {
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
            }

            // Conteggio follower
            item {
                Text(
                    text = "Follower: $followerCount",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Pulsanti Segui e Messaggia
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
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
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
                        Text("Messaggia")
                    }
                }
            }

            // Lista delle collezioni dell'utente
            items(userCollections.size) { index ->
                val collection = userCollections[index]
                val hasLiked = likedMap[collection.id] ?: false
                val likesCount = likesCountMap[collection.id] ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            navController.navigate(
                                Screens.CollectionDetailScreen.collectionDetailRoute(collection.id)
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Immagine di copertina della collezione (opzionale)
                        AnimatedVisibility(
                            visible = collection.collectionImageUrl?.isNotBlank() == true,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            AsyncImage(
                                model = collection.collectionImageUrl,
                                contentDescription = "Immagine collezione ${collection.name}",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                alignment = Alignment.Center
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        // Nome e categoria
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = collection.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = collection.category ?: "Senza categoria",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Like button (solo se l'utente loggato non è il proprietario della collezione)
                        if (currentUserId != userId) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    if (hasLiked) {
                                        // Rimuovi like (aggiornamento ottimistico)
                                        likedMap[collection.id] = false
                                        likesCountMap[collection.id] = (likesCount - 1).coerceAtLeast(0)
                                        collectionVM.unlikeCollection(collection.id)
                                    } else {
                                        // Aggiungi like
                                        likedMap[collection.id] = true
                                        likesCountMap[collection.id] = likesCount + 1
                                        collectionVM.likeCollection(collection.id, notificationVM)
                                    }
                                }
                            ) {
                                if (likesCount > 0) {
                                    Text(
                                        text = likesCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (hasLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (hasLiked) "Rimuovi like" else "Metti like",
                                    tint = if (hasLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}