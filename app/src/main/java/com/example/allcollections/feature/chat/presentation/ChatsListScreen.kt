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
 * Schermata che mostra la lista delle conversazioni recenti dell'utente.
 *
 * Le conversazioni vengono osservate in tempo reale tramite [ChatViewModel.recentChats]
 * e mostrate in ordine decrescente di timestamp (la più recente in alto).
 *
 * ### Comportamento
 * - Se non ci sono conversazioni, viene mostrato uno stato vuoto con icona e messaggio.
 * - Ogni elemento della lista (card) mostra:
 *   - Foto profilo dell'interlocutore (placeholder circolare se assente).
 *   - Username dell'interlocutore (caricato asincronamente tramite [ProfileViewModel]).
 *   - Ultimo messaggio (troncato a una riga).
 *   - Timestamp relativo (es. "5m", "2h").
 *   - Badge con il numero di messaggi non letti (visibile solo se > 0).
 * - Cliccando su una card si naviga alla schermata di chat con quell'utente.
 *
 * @param navController Controller per la navigazione verso [ChatScreen].
 * @param viewModel ViewModel delle chat (osserva le conversazioni recenti).
 * @param profileViewModel ViewModel del profilo (per caricare username e foto).
 *
 * @see ChatViewModel
 * @see ChatPreview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    navController: NavController,
    viewModel: ChatViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel()
) {
    val recentChats by viewModel.recentChats.collectAsState()

    // Avvia l'osservazione delle chat recenti quando la schermata viene composta
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
            // Stato vuoto: nessuna conversazione
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
            // Lista delle conversazioni
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
 * Visualizza i dati principali della conversazione e gestisce il caricamento asincrono
 * di username e foto profilo dell'interlocutore tramite [ProfileViewModel].
 *
 * ### Componenti visualizzati
 * - Foto profilo (circolare, con placeholder se non disponibile)
 * - Username
 * - Timestamp relativo (es. "5 minuti fa")
 * - Ultimo messaggio (troncato)
 * - Badge con numero di messaggi non letti (evidenziato con colore primario)
 *
 * Il testo dell'ultimo messaggio appare più scuro (primary) se ci sono messaggi non letti.
 *
 * @param chat Dati dell'anteprima della conversazione.
 * @param onClick Callback invocato al tap sulla card (solitamente navigazione alla chat).
 * @param profileViewModel ViewModel per caricare username e foto profilo dell'interlocutore.
 */
@Composable
fun ChatPreviewItem(
    chat: ChatPreview,
    onClick: () -> Unit,
    profileViewModel: ProfileViewModel
) {
    var username by remember { mutableStateOf("Utente") }
    var profileImage by remember { mutableStateOf<String?>(null) }

    // Carica i dati dell'interlocutore quando cambia l'userId della chat
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
            // Foto profilo (placeholder se non disponibile)
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
                // Riga superiore: username e timestamp
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
                        // Se ci sono messaggi non letti, il testo è più scuro (primary)
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