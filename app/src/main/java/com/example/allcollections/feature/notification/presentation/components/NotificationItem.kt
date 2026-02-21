package com.example.allcollections.feature.notification.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.allcollections.feature.notification.domain.Notification
import com.example.allcollections.feature.notification.domain.NotificationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItem(
    notification: Notification,
    onMarkAsRead: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (!notification.read) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (!notification.read) onMarkAsRead()
                onClick()
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!notification.read) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NotificationAvatar(notification)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NotificationContent(notification)
                Text(
                    text = notification.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!notification.read) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun NotificationAvatar(notification: Notification) {
    val icon = when (notification.type) {
        NotificationType.FOLLOW -> Icons.Default.PersonAdd
        NotificationType.COMMENT -> Icons.Default.Chat
        NotificationType.LIKE -> Icons.Default.Favorite
        NotificationType.NEW_ITEM -> Icons.Default.Collections
        else -> Icons.Default.Info
    }

    val iconTint = when (notification.type) {
        NotificationType.LIKE -> Color.Red
        else -> MaterialTheme.colorScheme.primary
    }

    if (!notification.isSystem && notification.sender?.profileImageUrl?.isNotBlank() == true) {
        AsyncImage(
            model = notification.sender.profileImageUrl,
            contentDescription = "Foto profilo",
            modifier = Modifier.size(48.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun NotificationContent(notification: Notification) {
    val senderName = when {
        notification.isSystem -> "All Collections"
        notification.sender?.username?.isNotBlank() == true -> "@${notification.sender.username}"
        notification.sender?.name?.isNotBlank() == true -> "${notification.sender.name} ${notification.sender.surname}"
        else -> "Utente"
    }

    val text = when (notification.type) {
        NotificationType.FOLLOW -> "$senderName ha iniziato a seguirti"
        NotificationType.COMMENT -> buildString {
            append("$senderName ha commentato")
            if (!notification.data.collectionName.isNullOrBlank()) append(" in ${notification.data.collectionName}")
            if (!notification.data.commentText.isNullOrBlank()) append(": \"${notification.data.commentText}\"")
        }
        NotificationType.LIKE -> buildString {
            append("$senderName ha messo like alla tua collezione")
            if (!notification.data.collectionName.isNullOrBlank()) append(" \"${notification.data.collectionName}\"")
        }
        NotificationType.NEW_ITEM -> buildString {
            append("$senderName ha aggiunto un nuovo oggetto")
            if (!notification.data.collectionName.isNullOrBlank()) append(" in \"${notification.data.collectionName}\"")
        }
        else -> notification.data.pushMessage ?: "Nuova notifica"
    }

    Text(
        text = text,
        style = if (!notification.read) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}