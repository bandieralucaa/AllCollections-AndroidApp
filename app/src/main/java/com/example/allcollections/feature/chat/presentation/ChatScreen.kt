package com.example.allcollections.feature.chat.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.ChatMessage
import com.example.allcollections.feature.profile.ProfileViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Schermata della chat privata tra due utenti.
 *
 * Questa schermata gestisce l'invio e la visualizzazione in tempo reale dei messaggi
 * tra l'utente corrente e un altro utente identificato da [otherUserId].
 *
 * ### Caratteristiche principali
 * - I messaggi vengono osservati in tempo reale tramite [ChatViewModel.observeMessages].
 * - La lista dei messaggi è in ordine cronologico (dal più vecchio al più nuovo)
 *   ma visualizzata con `reverseLayout = true` e `messages.reversed()` in modo che
 *   i messaggi più recenti appaiano in fondo e lo scroll naturale vada verso l'alto.
 * - Scroll automatico all'ultimo messaggio quando arriva un nuovo messaggio.
 * - All'uscita dalla schermata, i messaggi vengono cancellati dalla memoria locale
 *   tramite [ChatViewModel.clearMessages] per evitare "flash" alla riapertura.
 * - È possibile eliminare l'intera conversazione con un dialog di conferma.
 * - Il profilo dell'interlocutore (foto, username) è cliccabile e porta al suo profilo pubblico.
 *
 * @param otherUserId ID dell'altro utente con cui si sta chattando.
 * @param otherUsername Username dell'altro utente (opzionale; se vuoto, viene caricato).
 * @param navController Controller per la navigazione (profilo pubblico, back stack).
 * @param viewModel ViewModel che gestisce i messaggi e le operazioni sulla chat.
 * @param profileViewModel ViewModel per caricare username e foto profilo dell'interlocutore.
 *
 * @see ChatViewModel
 * @see ChatBubble
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    otherUserId: String,
    otherUsername: String = "",
    navController: NavController,
    viewModel: ChatViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Stato UI locale
    var messageText by remember { mutableStateOf("") }
    var username by remember { mutableStateOf(otherUsername) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Carica i dati dell'interlocutore e avvia l'osservazione dei messaggi
    LaunchedEffect(otherUserId) {
        if (username.isEmpty()) {
            profileViewModel.getUsernameById(otherUserId) { username = it }
        }
        profileViewModel.getUserProfilePhoto(otherUserId) { profilePhotoUrl = it }
        viewModel.observeMessages(otherUserId)
    }

    // Scroll automatico all'ultimo messaggio quando la lista si aggiorna
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                // messages.size - 1 è l'indice dell'ultimo messaggio (più recente)
                // Poiché la LazyColumn ha reverseLayout = true, dobbiamo scorrere all'indice 0?
                // No: con reverseLayout = true, l'indice 0 è l'ultimo elemento in fondo.
                // La formula corretta è: scrollare all'indice 0 quando reverseLayout = true.
                // Ma messages.reversed() viene usato nella LazyColumn, quindi l'ultimo messaggio
                // originale (più recente) diventa il primo elemento della lista invertita.
                // Quindi listState.animateScrollToItem(0) funziona.
                // Tuttavia, per semplicità e dato che la lista è piccola, scrolliamo all'indice 0.
                listState.animateScrollToItem(0)
            }
        }
    }

    // Pulisce i messaggi quando si esce dalla schermata per evitare "flash" alla prossima apertura
    DisposableEffect(Unit) {
        onDispose { viewModel.clearMessages() }
    }

    // Dialog di conferma per l'eliminazione della conversazione
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Elimina conversazione") },
            text = { Text("Sei sicuro di voler eliminare questa conversazione? Questa azione non può essere annullata.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteChat(otherUserId) {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") }
            }
        )
    }

    Scaffold(
        topBar = {
            MyTopBar(
                navController = navController,
                title = "",
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opzioni")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Elimina conversazione") },
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
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header con foto profilo e username dell'interlocutore (cliccabile)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        navController.navigate(Screens.PublicProfileScreen.createRoute(otherUserId))
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = profilePhotoUrl,
                    contentDescription = "Foto profilo di $username",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (username.isNotEmpty()) "@$username" else "Chat",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            HorizontalDivider()

            // Lista dei messaggi in ordine inverso (più recente in basso)
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                reverseLayout = true,          // Inverte l'ordine di layout: l'elemento 0 è in fondo
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // messages.reversed() viene usato perché la LazyColumn ha reverseLayout = true
                // In questo modo l'ultimo messaggio (più recente) viene posizionato in fondo.
                items(messages.reversed()) { message ->
                    ChatBubble(message = message)
                }
            }

            // Barra di input per inviare nuovi messaggi
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Scrivi un messaggio...") },
                        maxLines = 3,
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(otherUserId, messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Invia",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bolla di un singolo messaggio nella chat.
 *
 * Questo componente visualizza un messaggio con stili diversi a seconda del mittente:
 * - **Messaggi propri** (inviati dall'utente corrente) → allineati a destra, sfondo [ColorScheme.primaryContainer].
 * - **Messaggi ricevuti** → allineati a sinistra, sfondo [ColorScheme.surfaceVariant].
 *
 * Il testo è avvolto in una [Card] con padding e stile tipografico [MaterialTheme.typography.bodyMedium].
 *
 * @param message Il messaggio da visualizzare (contiene testo, mittente, timestamp, ecc.).
 */
@Composable
fun ChatBubble(message: ChatMessage) {
    val currentUserId = Firebase.auth.currentUser?.uid
    val isMine = message.senderId == currentUserId

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isMine)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}