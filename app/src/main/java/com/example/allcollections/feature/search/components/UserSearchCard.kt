package com.example.allcollections.feature.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.allcollections.data.model.UserData

/**
 * Card per la visualizzazione di un utente nei risultati di ricerca.
 *
 * Mostra:
 * - Foto profilo (circolare, con placeholder se assente)
 * - Nome e cognome
 * - Username (con @)
 * - Badge circolare a destra (placeholder per conteggio collezioni, attualmente fisso a "0")
 *
 * @param user Dati dell'utente da visualizzare (nome, cognome, username, foto profilo).
 * @param onClick Callback invocato al tap sulla card (es. navigazione al profilo pubblico).
 */
@Composable
fun UserSearchCard(
    user: UserData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto profilo (placeholder se vuota)
            AsyncImage(
                model = user.profileImageUrl.ifEmpty { null },
                contentDescription = "Foto profilo di ${user.username}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Informazioni utente (nome, cognome, username)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${user.name} ${user.surname}".trim(),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Badge per conteggio collezioni (attualmente placeholder)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}