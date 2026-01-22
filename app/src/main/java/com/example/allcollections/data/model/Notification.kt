package com.example.allcollections.feature.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.navigation.NavController
import com.example.allcollections.core.utils.time.formatRelativeTime
import com.example.allcollections.feature.notification.components.NotificationItem

/**
 * Composable principale per visualizzare le notifiche.
 */
@Composable
fun Notifications(
    navController: NavController,
    notificationViewModel: NotificationViewModel
) {
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val currentUserId = notificationViewModel.getCurrentUserId()

    // Osserva notifiche
    LaunchedEffect(currentUserId) {
        currentUserId?.let {
            notificationViewModel.observeNotifications(it) { list ->
                notifications = list
            }
            notificationViewModel.checkUnreadNotifications()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifiche",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )

            if (notifications.isNotEmpty()) {
                Text(
                    text = "Elimina tutto",
                    color = Color.Red,
                    modifier = Modifier
                        .clickable { showDeleteDialog = true }
                        .padding(8.dp)
                )
            }
        }

        // Lista notifiche
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessuna notifica ricevuta.", color = Color.Gray)
            }
        } else {
            Column {
                notifications.forEach { item ->
                    NotificationRow(item, navController, notificationViewModel)
                }
            }
        }
    }

    // Dialog conferma eliminazione
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminare tutte le notifiche?") },
            text = { Text("Questa azione non può essere annullata.") },
            confirmButton = {
                Text(
                    "Conferma",
                    modifier = Modifier.clickable {
                        notificationViewModel.deleteAllNotifications {
                            notifications = emptyList()
                            showDeleteDialog = false
                        }
                    },
                    color = MaterialTheme.colorScheme.primary
                )
            },
            dismissButton = {
                Text(
                    "Annulla",
                    modifier = Modifier.clickable { showDeleteDialog = false }
                )
            }
        )
    }
}

/**
 * Singola riga notifica.
 * Usa colore di sfondo diverso per notifiche non lette.
 */
@Composable
private fun NotificationRow(
    item: NotificationItem,
    navController: NavController,
    notificationViewModel: NotificationViewModel
) {
    val message = buildNotificationMessage(item).first

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                notificationViewModel.markNotificationAsReadPublic(item.notificationId) {
                    buildNotificationMessage(item).second?.let { collectionId ->
                        navController.navigate("collection_detail/$collectionId")
                    } ?: navController.navigate("publicProfile/${item.user.userId}")
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (!item.read) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(item.user.profileImageUrl),
                contentDescription = "Foto profilo",
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            )

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "@${item.user.username}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                val relativeTime = formatRelativeTime(item.timestamp)
                Text(
                    buildAnnotatedString {
                        append(message)
                        append(" • ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(relativeTime)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Restituisce messaggio leggibile e eventuale id collezione
 */
fun buildNotificationMessage(item: NotificationItem): Pair<String, String?> {
    return when (item.type) {
        "comment" -> {
            val collName = item.collectionName ?: "la tua collezione"
            "@${item.user.username} ha commentato \"$collName\"" to item.collectionId
        }
        "follow" -> "@${item.user.username} ti ha seguito" to null
        else -> "@${item.user.username} ha effettuato un'azione" to item.collectionId
    }
}
