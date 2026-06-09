package com.example.allcollections.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.data.model.FollowUser
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/**
 * Schermata del profilo personale dell'utente corrente.
 *
 * Mostra foto profilo, username, bio (cliccabile per modificarla), contatori
 * follower/seguiti (cliccabili per vedere la lista) e i pulsanti di accesso
 * alle proprie collezioni. Se l'utente non è autenticato naviga al login.
 *
 * @param navController NavController per la navigazione.
 */
@Composable
fun ProfileScreen(navController: NavController) {
    val viewModel: ProfileViewModel = viewModel()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var followerCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }

    val profileImageUrl by viewModel.profileImageUrl
    val followersList by viewModel.followersList
    val followingList by viewModel.followingList
    val isLoadingFollowers by viewModel.isLoadingFollowers
    val isLoadingFollowing by viewModel.isLoadingFollowing

    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate(Screens.LoginScreen.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(userId) {
        if (userId != null) {
            val user = viewModel.getUserData()
            username = user?.username ?: "Utente"
            bio = user?.bio ?: ""
            viewModel.loadProfileImage()
            viewModel.getFollowerCount(userId) { followerCount = it }
            viewModel.getFollowingCount(userId) { followingCount = it }
        }
    }

    Scaffold(
        topBar = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { navController.navigate(Screens.SettingsScreen.route) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            ProfileImage(imageUrl = profileImageUrl)

            Spacer(Modifier.height(12.dp))

            Text(
                text = username.ifBlank { "Utente" },
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif
            )

            Text(
                text = if (bio.isNotBlank()) bio else "Aggiungi una bio...",
                style = MaterialTheme.typography.bodyMedium,
                color = if (bio.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                ClickableStatItem(
                    label = "Follower",
                    count = followerCount,
                    onClick = {
                        showFollowersDialog = true
                        if (userId != null) viewModel.loadFollowers(userId)
                    }
                )
                ClickableStatItem(
                    label = "Seguiti",
                    count = followingCount,
                    onClick = {
                        showFollowingDialog = true
                        if (userId != null) viewModel.loadFollowing(userId)
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate(Screens.MyCollectionsScreen.route) },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Le mie collezioni")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { navController.navigate(Screens.AddCollectionScreen.route) },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Crea collezione")
            }
        }
    }

    if (showFollowersDialog) {
        FollowersDialog(
            title = "Follower",
            users = followersList,
            isLoading = isLoadingFollowers,
            onDismiss = { showFollowersDialog = false },
            onUserClick = { clickedUserId ->
                navController.navigate(Screens.PublicProfileScreen.createRoute(clickedUserId))
                showFollowersDialog = false
            }
        )
    }

    if (showFollowingDialog) {
        FollowersDialog(
            title = "Seguiti",
            users = followingList,
            isLoading = isLoadingFollowing,
            onDismiss = { showFollowingDialog = false },
            onUserClick = { clickedUserId ->
                navController.navigate(Screens.PublicProfileScreen.createRoute(clickedUserId))
                showFollowingDialog = false
            }
        )
    }
}

@Composable
private fun ClickableStatItem(label: String, count: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Dialog che mostra la lista di follower o seguiti.
 *
 * @param title Titolo del dialog ("Follower" o "Seguiti").
 * @param users Lista di [FollowUser] da visualizzare.
 * @param isLoading `true` se i dati sono ancora in caricamento.
 * @param onDismiss Callback per chiudere il dialog.
 * @param onUserClick Callback con lo userId quando si tocca un utente.
 */
@Composable
fun FollowersDialog(
    title: String,
    users: List<FollowUser>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 500.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                Divider(modifier = Modifier.padding(bottom = 8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (users.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Nessun utente", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(users) { user ->
                            UserListItem(user = user, onClick = { onUserClick(user.userId) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Chiudi") }
            }
        }
    }
}

@Composable
fun UserListItem(user: FollowUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (user.profileImageUrl.isNotEmpty()) {
                AsyncImage(model = user.profileImageUrl, contentDescription = "Foto profilo", modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Surface(modifier = Modifier.size(40.dp).clip(CircleShape), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = "Avatar", modifier = Modifier.size(24.dp))
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.username, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Vedi profilo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileImage(imageUrl: String?) {
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        if (imageUrl.isNullOrBlank()) {
            Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) { Text("Foto") }
            }
        } else {
            AsyncImage(model = imageUrl, contentDescription = "Foto profilo", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        }
    }
}