package com.example.allcollections.feature.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.feature.notification.components.NotificationItem
import com.example.allcollections.feature.notification.components.relativeTime
import kotlinx.coroutines.launch

/**
 * NotificationsScreen: visualizza tutte le notifiche dell’utente.
 * Funzionalità:
 * - Clic su notifica → segna come letta
 * - Pulsante "Elimina tutte"
 * - Snackbar per feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationViewModel
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val userId = viewModel.getCurrentUserId()
    var notifications by remember { mutableStateOf(listOf<NotificationItem>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Osserva notifiche utente
    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.observeNotifications(userId) {
                notifications = it
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifiche") },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.deleteAllNotifications {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Tutte le notifiche eliminate")
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina tutte")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                notifications.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nessuna notifica disponibile", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(notifications, key = { it.notificationId }) { notification ->
                            NotificationRow(
                                notification = notification,
                                onClick = {
                                    scope.launch {
                                        viewModel.markNotificationAsReadPublic(notification.notificationId) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Notifica letta")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Riga singola notifica */
@Composable
fun NotificationRow(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicator unread
            if (notification.isUnread) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.Red, shape = MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
