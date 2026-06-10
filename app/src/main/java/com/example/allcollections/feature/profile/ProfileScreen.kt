package com.example.allcollections.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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

/**
 * Schermata del profilo personale dell'utente corrente.
 *
 * Questa schermata mostra tutte le informazioni relative all'utente autenticato:
 * - Foto profilo (cliccabile per modificarla? In questa versione non gestito, ma si può estendere)
 * - Username e biografia (bio cliccabile per modificarla)
 * - Contatori di follower e following (cliccabili per visualizzare le liste)
 * - Pulsanti per "Le mie collezioni" e "Crea collezione"
 *
 * ### Flusso di navigazione
 * - Se l'utente non è autenticato, viene reindirizzato automaticamente a [Screens.LoginScreen]
 *   e la back stack viene resettata.
 * - I clic sui follower/following aprono un dialog che mostra la lista (ottenuta in tempo reale
 *   tramite listener Firestore).
 * - Cliccando su un utente nella lista, si naviga al suo profilo pubblico.
 *
 * @param navController Controller per la navigazione.
 *
 * @see ProfileViewModel
 * @see FollowersDialog
 */
@Composable
fun ProfileScreen(navController: NavController) {
    val viewModel: ProfileViewModel = viewModel()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    // Stato UI locale
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var followerCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }

    // Stato dai ViewModel
    val profileImageUrl by viewModel.profileImageUrl
    val followersList by viewModel.followersList
    val followingList by viewModel.followingList
    val isLoadingFollowers by viewModel.isLoadingFollowers
    val isLoadingFollowing by viewModel.isLoadingFollowing

    // Controllo visibilità dialog
    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }

    // Se non autenticato, reindirizza al login e resetta la back stack
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate(Screens.LoginScreen.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Carica i dati dell'utente quando l'userId è disponibile
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Foto profilo
            ProfileImage(imageUrl = profileImageUrl)

            Spacer(Modifier.height(12.dp))

            // Username
            Text(
                text = username.ifBlank { "Utente" },
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif
            )

            // Biografia (cliccabile per modificarla)
            Text(
                text = if (bio.isNotBlank()) bio else "Aggiungi una bio...",
                style = MaterialTheme.typography.bodyMedium,
                color = if (bio.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            Spacer(Modifier.height(24.dp))

            // Statistiche follower/following
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

            // Pulsante: Le mie collezioni
            Button(
                onClick = { navController.navigate(Screens.MyCollectionsScreen.route) },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Le mie collezioni")
            }

            Spacer(Modifier.height(12.dp))

            // Pulsante: Crea collezione
            Button(
                onClick = { navController.navigate(Screens.AddCollectionScreen.route) },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Crea collezione")
            }
        }
    }

    // Dialog per la lista dei follower
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

    // Dialog per la lista degli utenti seguiti
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

/**
 * Elemento cliccabile per visualizzare una statistica (follower o following).
 *
 * Mostra il conteggio in grassetto e una label sotto. Il componente è interamente
 * cliccabile ed esegue [onClick] quando toccato.
 *
 * @param label Etichetta descrittiva (es. "Follower").
 * @param count Valore numerico da mostrare.
 * @param onClick Callback invocato al click.
 */
@Composable
private fun ClickableStatItem(label: String, count: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Dialog che mostra una lista di utenti (follower o seguiti) in un [LazyColumn].
 *
 * Utilizzato sia per i follower che per i following, con titolo personalizzabile.
 * Durante il caricamento mostra un [CircularProgressIndicator]; se la lista è vuota
 * mostra un messaggio.
 *
 * @param title Titolo del dialog (es. "Follower", "Seguiti").
 * @param users Lista di [FollowUser] da visualizzare.
 * @param isLoading Se `true`, mostra l'indicatore di caricamento.
 * @param onDismiss Callback per chiudere il dialog (eseguito al tap su "Chiudi" o back button).
 * @param onUserClick Callback invocato quando si tocca un utente; riceve l'ID dell'utente.
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
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 500.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Divider(modifier = Modifier.padding(bottom = 8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (users.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nessun utente",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(users) { user ->
                            UserListItem(user = user, onClick = { onUserClick(user.userId) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Chiudi")
                }
            }
        }
    }
}

/**
 * Singola riga della lista all'interno di [FollowersDialog].
 *
 * Mostra la foto profilo (o un placeholder), lo username, e una freccia a destra.
 * L'intera card è cliccabile ed esegue [onClick].
 *
 * @param user Dati dell'utente da visualizzare.
 * @param onClick Callback invocato al tap sull'elemento.
 */
@Composable
fun UserListItem(user: FollowUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (user.profileImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = "Foto profilo di ${user.username}",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Avatar predefinito",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.username, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Vedi profilo",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Componente che mostra l'immagine profilo in un cerchio.
 *
 * Se [imageUrl] è nullo o vuoto, viene mostrato un placeholder circolare con la scritta "Foto".
 *
 * @param imageUrl URL dell'immagine profilo (opzionale).
 */
@Composable
private fun ProfileImage(imageUrl: String?) {
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        if (imageUrl.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Foto")
                }
            }
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Foto profilo utente",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}