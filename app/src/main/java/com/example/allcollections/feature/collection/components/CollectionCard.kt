package com.example.allcollections.feature.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.allcollections.data.model.UserCollection
import com.google.firebase.auth.FirebaseAuth // ✅ Import

/**
 * Card riutilizzabile per mostrare una collezione.
 * Supporta due layout: ORIZZONTALE (per liste) e VERTICALE (per griglie).
 *
 * @param collection Dati della collezione da visualizzare
 * @param layoutType Layout da usare: Horizontal (default) o Vertical
 * @param showMenu Se true mostra il menu per Modifica/Elimina (solo per Horizontal)
 * @param onEdit Callback quando si clicca su "Modifica"
 * @param onDelete Callback quando si clicca su "Elimina"
 * @param onCardClick Callback quando si clicca sulla card
 * @param onUsernameClick Callback quando si clicca sullo username (solo Vertical)
 * @param onMyProfileClick Callback quando si clicca sul proprio username "Tu" (solo Vertical)
 * @param modifier Modifier opzionale per personalizzazioni
 */
@Composable
fun CollectionCard(
    collection: UserCollection,
    layoutType: CollectionCardLayout = CollectionCardLayout.Horizontal,
    showMenu: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCardClick: (String) -> Unit = {},
    onUsernameClick: (String) -> Unit = {},
    onMyProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (layoutType) {
        CollectionCardLayout.Horizontal -> HorizontalCollectionCard(
            collection = collection,
            showMenu = showMenu,
            onEdit = onEdit,
            onDelete = onDelete,
            onCardClick = onCardClick,
            modifier = modifier
        )
        CollectionCardLayout.Vertical -> VerticalCollectionCard(
            collection = collection,
            onCardClick = onCardClick,
            onUsernameClick = onUsernameClick,
            onMyProfileClick = onMyProfileClick,
            modifier = modifier
        )
    }
}

/** Layout per card orizzontale (lista in MyCollectionsScreen) */
@Composable
private fun HorizontalCollectionCard(
    collection: UserCollection,
    showMenu: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCardClick: (String) -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onCardClick(collection.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Immagine della collezione (opzionale)
            collection.collectionImageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Immagine collezione",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Info collezione: nome e descrizione
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = collection.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Menu opzionale Modifica/Elimina
            if (showMenu) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Opzioni collezione")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Modifica") },
                            onClick = {
                                expanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Elimina") },
                            onClick = {
                                expanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Layout per card verticale (griglia in HomeScreen) */
@Composable
private fun VerticalCollectionCard(
    collection: UserCollection,
    onCardClick: (String) -> Unit,
    onUsernameClick: (String) -> Unit,
    onMyProfileClick: () -> Unit,
    modifier: Modifier
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Nome collezione (cliccabile per aprire collezione)
            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCardClick(collection.id) }
            )

            // Username CLICCABILE (solo se disponibile)
            if (collection.username.isNotBlank()) {
                val isCurrentUser = currentUserId == collection.iduser

                Text(
                    text = if (isCurrentUser) "Tu" else "@${collection.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentUser) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isCurrentUser) {
                                onMyProfileClick()
                            } else {
                                onUsernameClick(collection.iduser)
                            }
                        }
                        .padding(vertical = 4.dp)
                )
            }

            // Immagine collezione o placeholder (cliccabile per aprire collezione)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCardClick(collection.id) }
            ) {
                if (collection.collectionImageUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = collection.collectionImageUrl,
                        contentDescription = "Immagine collezione",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessuna immagine",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

/** Enum per specificare il tipo di layout */
enum class CollectionCardLayout {
    Horizontal, // Per liste (MyCollectionsScreen)
    Vertical    // Per griglie (HomeScreen)
}