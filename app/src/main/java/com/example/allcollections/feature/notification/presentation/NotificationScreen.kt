package com.example.allcollections.feature.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
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
import com.example.allcollections.feature.notification.components.NotificationItem
import com.example.allcollections.feature.notification.domain.Notification
import com.example.allcollections.feature.notification.domain.NotificationType
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import kotlinx.coroutines.launch

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
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MyTopBar(
                navController = navController,
                title = "Notifiche",
                actions = {
                    if (notifications.isNotEmpty()) {
                        // Icona "Elimina tutti"
                        IconButton(
                            onClick = { showDeleteAllDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Elimina tutte"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        // Dialog conferma eliminazione tutte
        if (showDeleteAllDialog) {
            DeleteAllConfirmationDialog(
                onConfirm = {
                    showDeleteAllDialog = false
                    showDeleteConfirmation = true
                },
                onDismiss = { showDeleteAllDialog = false }
            )
        }

        // Dialog conferma finale
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Elimina tutte le notifiche") },
                text = { Text("Sei sicuro di voler eliminare tutte le notifiche? Questa azione non può essere annullata.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAll()
                            showDeleteConfirmation = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Notifiche eliminate")
                            }
                        }
                    ) {
                        Text("Elimina", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Annulla")
                    }
                }
            )
        }

        // Contenuto principale
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
    val scope = rememberCoroutineScope()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications, key = { it.id }) { notification ->
            NotificationItem(
                notification = notification,
                onMarkAsRead = {
                    viewModel.markAsRead(notification.id)
                },
                onClick = {
                    handleNotificationClick(notification, navController, viewModel)
                },
                modifier = Modifier.animateItem()
            )
        }

        // Spazio in fondo
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

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
                text = "Nessuna notifica",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Quando riceverai notifiche, appariranno qui",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DeleteAllConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elimina tutte le notifiche") },
        text = { Text("Sei sicuro di voler eliminare tutte le notifiche?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Elimina")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

private fun handleNotificationClick(
    notification: Notification,
    navController: NavController,
    viewModel: NotificationViewModel
) {
    // Segna come letta se non lo è
    if (!notification.read) {
        viewModel.markAsRead(notification.id)
    }

    // Naviga in base al tipo
    when (notification.type) {
        NotificationType.COMMENT -> {
            when {
                notification.data.itemId != null -> {
                    navController.navigate("item_detail/${notification.data.itemId}")
                }
                notification.data.collectionId != null -> {
                    navController.navigate("collection_detail/${notification.data.collectionId}")
                }
                else -> navController.navigate(Screens.NotificationsScreen.route)
            }
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
        else -> {
            // Per notifiche generali, vai alla home
            navController.navigate(Screens.HomeScreen.route)
        }
    }
}