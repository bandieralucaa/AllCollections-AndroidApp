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
import com.example.allcollections.feature.notification.components.NotificationItem
import com.example.allcollections.feature.notification.domain.NotificationType
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import kotlinx.coroutines.launch
import kotlin.collections.isNotEmpty

/**
 * Schermata delle notifiche ricevute dall'utente.
 *
 * Mostra la lista delle notifiche in ordine cronologico inverso, con
 * supporto per marcatura come letta al tap e eliminazione di tutte.
 * Il tap su una notifica naviga alla schermata contestuale in base
 * al tipo (collezione, oggetto, profilo utente).
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

@Composable
private fun EmptyNotificationsView() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text("Nessuna notifica", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Quando riceverai notifiche, appariranno qui", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}

private fun handleNotificationClick(
    notification: Notification,
    navController: NavController,
    viewModel: NotificationViewModel
) {
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
        NotificationType.LIKE -> {
            notification.data.collectionId?.let {
                navController.navigate("collection_detail/$it")
            } ?: navController.navigate(Screens.HomeScreen.route)
        }
        NotificationType.NEW_ITEM -> {
            notification.data.collectionId?.let {
                navController.navigate("collection_detail/$it")
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