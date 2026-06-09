package com.example.allcollections.feature.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.data.model.Notification
import com.example.allcollections.feature.notification.presentation.components.NotificationItem
import com.example.allcollections.feature.notification.domain.NotificationType
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import kotlinx.coroutines.launch

/**
 * Schermata delle notifiche ricevute dall'utente corrente.
 *
 * Mostra la lista delle notifiche in ordine cronologico inverso (più recente in alto)
 * ottenute in tempo reale tramite [NotificationViewModel.notifications].
 *
 * ### Funzionalità
 * - Tap su una notifica: la marca come letta (se non lo era) e naviga alla schermata
 *   corrispondente in base al tipo (collezione, commento, follow, like, nuovo oggetto).
 * - Pulsante "Elimina tutte" (icona cestino in alto a destra) che cancella tutte le notifiche
 *   dell'utente dopo conferma tramite [AlertDialog].
 * - Stato vuoto: mostra un'icona e messaggi descrittivi quando non ci sono notifiche.
 *
 * @param navController Controller per la navigazione.
 * @param viewModel ViewModel delle notifiche (osserva la lista, gestisce operazioni).
 *
 * @see NotificationViewModel
 * @see NotificationItem
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationViewModel
) {
    val notifications by viewModel.notifications.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Dialog di conferma per eliminare tutte le notifiche
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Elimina tutte le notifiche") },
            text = { Text("Sei sicuro di voler eliminare tutte le notifiche? Questa azione non può essere annullata.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    showDeleteAllDialog = false
                    scope.launch { snackbarHostState.showSnackbar("Notifiche eliminate") }
                }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Annulla") }
            }
        )
    }

    Scaffold(
        topBar = {
            MyTopBar(
                navController = navController,
                title = "Notifiche",
                actions = {
                    // Mostra l'icona "Elimina tutte" solo se ci sono notifiche
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina tutte")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                notifications.isEmpty() -> EmptyNotificationsView()
                else -> NotificationsList(
                    notifications = notifications,
                    viewModel = viewModel,
                    navController = navController,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

/**
 * Lista delle notifiche in un [LazyColumn].
 *
 * Ogni notifica è renderizzata da [NotificationItem] e supporta l'animazione
 * di inserimento/rimozione tramite [Modifier.animateItem].
 *
 * @param notifications Lista delle notifiche da visualizzare.
 * @param viewModel ViewModel per marcare come letta ed eliminare.
 * @param navController Controller per la navigazione al tap.
 * @param snackbarHostState Per mostrare snackbar in caso di errori (opzionale).
 */
@Composable
private fun NotificationsList(
    notifications: List<Notification>,
    viewModel: NotificationViewModel,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications, key = { it.id }) { notification ->
            NotificationItem(
                notification = notification,
                onMarkAsRead = { viewModel.markAsRead(notification.id) },
                onClick = { handleNotificationClick(notification, navController, viewModel) },
                modifier = Modifier.animateItem()
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

/**
 * Vista mostrata quando la lista delle notifiche è vuota.
 *
 * Mostra un'icona stilizzata e due messaggi di testo.
 */
@Composable
private fun EmptyNotificationsView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                "Nessuna notifica",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Quando riceverai notifiche, appariranno qui",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Gestisce la navigazione al tap su una notifica in base al suo tipo.
 *
 * La notifica viene marcata come letta (se non lo era) prima di navigare.
 *
 * ### Regole di navigazione
 * - [NotificationType.COMMENT] → dettaglio della collezione (con `collectionId`).
 * - [NotificationType.ITEM_COMMENT] → dettaglio della collezione con scroll all'oggetto (se `itemId` presente).
 * - [NotificationType.LIKE] o [NotificationType.NEW_ITEM] → dettaglio della collezione.
 * - [NotificationType.FOLLOW] → profilo pubblico dell'utente che ha seguito.
 * - Altri tipi → schermata home.
 *
 * @param notification Notifica ricevuta.
 * @param navController Controller per la navigazione.
 * @param viewModel ViewModel per marcare come letta.
 */
private fun handleNotificationClick(
    notification: Notification,
    navController: NavController,
    viewModel: NotificationViewModel
) {
    // Marca come letta se non lo è già
    if (!notification.read) viewModel.markAsRead(notification.id)

    when (notification.type) {
        NotificationType.COMMENT -> {
            notification.data.collectionId?.let {
                navController.navigate(Screens.CollectionDetailScreen.createRoute(it))
            } ?: navController.navigate(Screens.NotificationsScreen.route)
        }
        NotificationType.ITEM_COMMENT -> {
            val collId = notification.data.collectionId
            val itmId = notification.data.itemId
            when {
                collId != null && itmId != null ->
                    navController.navigate(Screens.CollectionDetailScreen.collectionDetailWithItemRoute(collId, itmId))
                collId != null ->
                    navController.navigate(Screens.CollectionDetailScreen.createRoute(collId))
                else -> navController.navigate(Screens.NotificationsScreen.route)
            }
        }
        NotificationType.LIKE, NotificationType.NEW_ITEM -> {
            notification.data.collectionId?.let {
                navController.navigate(Screens.CollectionDetailScreen.createRoute(it))
            } ?: navController.navigate(Screens.HomeScreen.route)
        }
        NotificationType.FOLLOW -> {
            notification.sender?.userId?.let {
                navController.navigate(Screens.PublicProfileScreen.createRoute(it))
            } ?: navController.navigate(Screens.HomeScreen.route)
        }
        else -> navController.navigate(Screens.HomeScreen.route)
    }
}