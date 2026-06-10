package com.example.allcollections.feature.notification.presentation.components

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
import com.example.allcollections.data.model.Notification
import com.example.allcollections.feature.notification.domain.NotificationType

/**
 * Componente che visualizza una singola notifica nella schermata [NotificationsScreen].
 *
 * La card mostra:
 * - Avatar del mittente (foto profilo se disponibile, altrimenti icona in base al tipo).
 * - Testo descrittivo generato dinamicamente in base al tipo di notifica.
 * - Timestamp relativo (es. "5 minuti fa").
 * - Indicatore visivo (pallino blu) per le notifiche non lette.
 *
 * ### Stili
 * - Notifiche **non lette**: sfondo [ColorScheme.primaryContainer] con alpha 0.3,
 *   bordo leggermente rialzato (elevation 2.dp), testo in grassetto ([bodyLarge]).
 * - Notifiche **lette**: sfondo [ColorScheme.surface], nessuna elevazione,
 *   testo normale ([bodyMedium]).
 *
 * Al tap:
 * - Se non letta, viene marcata come letta tramite [onMarkAsRead].
 * - Viene eseguita la navigazione contestuale tramite [onClick].
 *
 * @param notification Dati della notifica da visualizzare.
 * @param onMarkAsRead Callback per marcare la notifica come letta.
 * @param onClick Callback per la navigazione (dopo la marcatura).
 * @param modifier Modificatore opzionale per personalizzare dimensioni e posizionamento.
 *
 * @see Notification
 * @see NotificationType
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItem(
    notification: Notification,
    onMarkAsRead: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Sfondo più scuro e leggera elevazione per le notifiche non lette
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar (foto profilo o icona di sistema)
            NotificationAvatar(notification)

            // Contenuto testuale + timestamp
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

            // Pallino indicatore per notifiche non lette
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Avatar del mittente della notifica.
 *
 * Se la notifica è di sistema o il mittente non ha foto profilo,
 * mostra un cerchio colorato con un'icona relativa al tipo di notifica.
 * Altrimenti, mostra la foto profilo (circolare).
 *
 * @param notification Notifica contenente mittente e tipo.
 */
@Composable
private fun NotificationAvatar(notification: Notification) {
    // Sceglie l'icona in base al tipo di notifica
    val icon = when (notification.type) {
        NotificationType.FOLLOW -> Icons.Default.PersonAdd
        NotificationType.COMMENT, NotificationType.ITEM_COMMENT -> Icons.Default.Chat
        NotificationType.LIKE -> Icons.Default.Favorite
        NotificationType.NEW_ITEM -> Icons.Default.Collections
        else -> Icons.Default.Info
    }

    // Colore dell'icona: rosso per i like, altrimenti colore primario del tema
    val iconTint = when (notification.type) {
        NotificationType.LIKE -> Color.Red
        else -> MaterialTheme.colorScheme.primary
    }

    // Salva il sender in una variabile locale per evitare problemi di smart cast
    val sender = notification.sender

    // Se la notifica non è di sistema e il mittente ha una foto profilo, la mostra
    if (!notification.isSystem && sender?.profileImageUrl?.isNotBlank() == true) {
        AsyncImage(
            model = sender.profileImageUrl,
            contentDescription = "Foto profilo di ${sender.username}",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Altrimenti, mostra un cerchio colorato con icona
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Genera il testo descrittivo della notifica in base al tipo.
 *
 * Il testo include il nome del mittente (o "All Collections" per notifiche di sistema)
 * e i dettagli contestuali come nome della collezione, descrizione dell'oggetto,
 * testo del commento, ecc.
 *
 * @param notification Notifica da cui generare il testo.
 */
@Composable
private fun NotificationContent(notification: Notification) {
    // Salva il sender in una variabile locale
    val sender = notification.sender

    // Nome del mittente (formattato)
    val senderName = when {
        notification.isSystem -> "All Collections"
        sender?.username?.isNotBlank() == true -> "@${sender.username}"
        sender?.name?.isNotBlank() == true -> "${sender.name} ${sender.surname}"
        else -> "Utente"
    }

    // Costruisce il messaggio in base al tipo
    val text = when (notification.type) {
        NotificationType.FOLLOW -> "$senderName ha iniziato a seguirti"

        NotificationType.COMMENT -> buildString {
            append("$senderName ha commentato")
            if (!notification.data.collectionName.isNullOrBlank()) {
                append(" in \"${notification.data.collectionName}\"")
            }
            if (!notification.data.commentText.isNullOrBlank()) {
                append(": \"${notification.data.commentText}\"")
            }
        }

        NotificationType.ITEM_COMMENT -> buildString {
            append("$senderName ha commentato l'oggetto")
            if (!notification.data.itemDescription.isNullOrBlank()) {
                append(" \"${notification.data.itemDescription}\"")
            }
            if (!notification.data.collectionName.isNullOrBlank()) {
                append(" nella collezione \"${notification.data.collectionName}\"")
            }
            if (!notification.data.commentText.isNullOrBlank()) {
                append(": \"${notification.data.commentText}\"")
            }
        }

        NotificationType.LIKE -> buildString {
            append("$senderName ha messo like alla tua collezione")
            if (!notification.data.collectionName.isNullOrBlank()) {
                append(" \"${notification.data.collectionName}\"")
            }
        }

        NotificationType.NEW_ITEM -> buildString {
            append("$senderName ha aggiunto un nuovo oggetto")
            if (!notification.data.collectionName.isNullOrBlank()) {
                append(" in \"${notification.data.collectionName}\"")
            }
        }

        else -> notification.data.pushMessage ?: "Nuova notifica"
    }

    // Stile: grassetto per non lette, normale per lette
    Text(
        text = text,
        style = if (!notification.read) {
            MaterialTheme.typography.bodyLarge
        } else {
            MaterialTheme.typography.bodyMedium
        },
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}