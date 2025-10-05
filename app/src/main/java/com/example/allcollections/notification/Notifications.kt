package com.example.allcollections.notification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.allcollections.utils.formatRelativeTime
import com.example.allcollections.viewModel.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun Notifications(
    navController: NavController,
    notificationViewModel: NotificationViewModel
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    val showDialog = remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        userId?.let {
            notificationViewModel.observeNotifications(it) { result ->
                notifications = result
            }
            notificationViewModel.checkUnreadNotifications()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Notifiche",
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )

            if (notifications.isNotEmpty()) {
                Text(
                    text = "Elimina tutto",
                    fontSize = 14.sp,
                    color = Color.Red,
                    modifier = Modifier
                        .clickable { showDialog.value = true }
                        .padding(8.dp)
                )
            }
        }

        if (notifications.isEmpty()) {
            Text("Nessuna notifica ricevuta.")
        } else {
            notifications.forEach { item ->
                val user = item.user
                val timestamp = item.timestamp
                val read = item.read
                val (message, targetCollectionId) = buildNotificationMessage(item)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            notificationViewModel.markNotificationAsRead(item.notificationId) {
                                notifications = notifications.map {
                                    if (it.notificationId == item.notificationId) it.copy(read = true) else it
                                }
                                targetCollectionId?.let {
                                    navController.navigate("collectionDetail/$it")
                                } ?: navController.navigate("publicProfile/${user.userId}")
                            }
                        }
                        .padding(vertical = 8.dp)
                        .background(
                            if (!read) MaterialTheme.colorScheme.surfaceVariant
                            else Color.Transparent
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val painter = rememberAsyncImagePainter(user.profileImageUrl)
                    Image(
                        painter = painter,
                        contentDescription = "Foto profilo",
                        modifier = Modifier.size(40.dp)
                    )

                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "@${user.username}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val relativeTime = formatRelativeTime(item.timestamp)
                        Text(
                            buildAnnotatedString {
                                append(message)
                                append(" — ")
                                withStyle(SpanStyle(color = Color.Gray)) {
                                    append(relativeTime)
                                }
                            },
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                confirmButton = {
                    Text("Conferma", modifier = Modifier.clickable {
                        notificationViewModel.deleteAllNotifications {
                            notifications = emptyList()
                            showDialog.value = false
                        }
                    })
                },
                dismissButton = {
                    Text("Annulla", modifier = Modifier.clickable {
                        showDialog.value = false
                    })
                },
                title = { Text("Eliminare tutte le notifiche?") },
                text = { Text("Questa azione non può essere annullata.") }
            )
        }
    }
}

fun buildNotificationMessage(item: NotificationItem): Pair<String, String?> {
    val username = item.user.username
    return when (item.type) {
        "comment" -> {
            val collName = item.collectionName ?: "la tua collezione"
            "@$username ha commentato \"$collName\"" to item.collectionId
        }
        "follow" -> "@$username ti ha seguito" to null

        else -> "@$username ha effettuato un'azione" to item.collectionId
    }
}