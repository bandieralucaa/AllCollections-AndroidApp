package com.example.allcollections.feature.chat.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.core.utils.time.formatRelativeTime
import com.example.allcollections.data.model.ChatPreview
import com.example.allcollections.feature.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Schermata con la lista delle conversazioni recenti.
 *
 * Mostra una card per ogni conversazione con foto profilo dell'interlocutore,
 * username, ultimo messaggio troncato, timestamp relativo e badge con il
 * contatore dei messaggi non letti. Se non ci sono conversazioni, mostra
 * uno stato vuoto illustrativo.
 *
 * @param navController NavController per navigare alla singola schermata di chat.
 * @param viewModel ViewModel che espone la lista [ChatPreview] delle chat recenti.
 * @param profileViewModel ViewModel per caricare username e foto profilo di ogni interlocutore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    navController: NavController,
    viewModel: ChatViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel()
) {
    val recentChats by viewModel.recentChats.collectAsState()

    // Avvia il listener real-time delle chat al primo ingresso nella schermata
    LaunchedEffect(Unit) {
        viewModel.observeRecentChats()
    }

    Scaffold(
        topBar = {
            MyTopBar(
                navController = navController,
                title = "Messaggi"
            )
        }
    ) { padding ->
        if (recentChats.isEmpty()) {
            // Stato vuoto
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Nessuna conversazione",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Inizia a chattare con altri utenti",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentChats) { chat ->
                    ChatPreviewItem(
                        chat = chat,
                        onClick = {
                            navController.navigate(Screens.ChatScreen.createRoute(chat.otherUserId))
                        },
                        profileViewModel = profileViewModel
                    )
                }
            }
        }
    }
}

/**
 * Card di anteprima per una singola conversazione.
 *
 * Mostra foto profilo e username dell'interlocutore (caricati in modo asincrono),
 * l'ultimo messaggio troncato a una riga, il timestamp relativo e un badge con
 * il numero di messaggi non letti (visibile solo se > 0). Il testo dell'ultimo
 * messaggio è più marcato se ci sono messaggi non letti.
 *
 * @param chat Dati dell'anteprima della conversazione.
 * @param onClick Callback invocato al tap sulla card per aprire la chat.
 * @param profileViewModel ViewModel usato per caricare username e foto profilo.
 */
@Composable
fun ChatPreviewItem(
    chat: ChatPreview,
    onClick: () -> Unit,
    profileViewModel: ProfileViewModel
) {
    var username by remember { mutableStateOf("Utente") }
    var profileImage by remember { mutableStateOf<String?>(null) }

    // Carica i dati dell'interlocutore ogni volta che cambia l'userId della chat
    LaunchedEffect(chat.otherUserId) {
        profileViewModel.getUsernameById(chat.otherUserId) { username = it }
        profileViewModel.getUserProfilePhoto(chat.otherUserId) { profileImage = it }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = profileImage,
                contentDescription = "Foto profilo di $username",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                // Riga superiore: username + timestamp
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatRelativeTime(chat.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Riga inferiore: ultimo messaggio + badge non letti
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // Testo più marcato se ci sono messaggi non letti
                        color = if (chat.unreadCount > 0)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.unreadCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = chat.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}