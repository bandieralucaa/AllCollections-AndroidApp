package com.example.allcollections.feature.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.allcollections.data.model.CollectionCardLayout
import com.example.allcollections.data.model.UserCollection
import com.google.firebase.auth.FirebaseAuth


/**
 * Card per la visualizzazione di una collezione.
 *
 * Funge da dispatcher tra i due layout disponibili:
 * - [CollectionCardLayout.Horizontal]: usato nella lista delle proprie collezioni,
 *   mostra immagine + nome + categoria affiancati e un menu modifica/elimina.
 * - [CollectionCardLayout.Vertical]: usato nella home e nella ricerca,
 *   mostra nome, username, immagine e pulsante like.
 *
 * @param collection Collezione da visualizzare.
 * @param layoutType Layout della card. Default = Horizontal.
 * @param showMenu Mostra il menu contestuale (modifica/elimina). Solo per Horizontal.
 * @param onEdit Callback per la modifica della collezione.
 * @param onDelete Callback per l'eliminazione della collezione.
 * @param onCardClick Callback invocato con l'ID collezione quando si tocca la card.
 * @param onUsernameClick Callback invocato con lo userId quando si tocca l'username di un altro utente.
 * @param onMyProfileClick Callback invocato quando si tocca il proprio username ("Tu").
 * @param hasLiked Indica se l'utente corrente ha già messo like a questa collezione.
 * @param likesCount Numero totale di like sulla collezione.
 * @param onLikeClick Callback per il pulsante like. Se null il pulsante non viene mostrato.
 * @param modifier Modifier opzionale per personalizzare il layout esterno.
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
    hasLiked: Boolean = false,
    likesCount: Int = 0,
    onLikeClick: (() -> Unit)? = null,
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
            hasLiked = hasLiked,
            likesCount = likesCount,
            onLikeClick = onLikeClick,
            modifier = modifier
        )
    }
}

/**
 * Layout orizzontale della card collezione.
 *
 * Mostra immagine a sinistra (se presente), nome e categoria a destra,
 * e un menu a tre puntini per modificare o eliminare se [showMenu] è true.
 * Usato principalmente nella schermata "Le mie collezioni".
 */
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
            collection.collectionImageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Immagine collezione ${collection.name}",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = collection.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = collection.category,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showMenu) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Opzioni collezione")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Modifica") },
                            onClick = { expanded = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Elimina") },
                            onClick = { expanded = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Layout verticale della card collezione.
 *
 * Mostra nell'ordine: nome (cliccabile), username dell'autore (con distinzione
 * tra utente corrente e altri), immagine di copertina e pulsante like con contatore.
 * Il like è visibile solo se l'utente non è il proprietario e [onLikeClick] non è null.
 * Usato nella home feed e nella schermata di ricerca.
 */
@Composable
private fun VerticalCollectionCard(
    collection: UserCollection,
    onCardClick: (String) -> Unit,
    onUsernameClick: (String) -> Unit,
    onMyProfileClick: () -> Unit,
    hasLiked: Boolean,
    likesCount: Int,
    onLikeClick: (() -> Unit)?,
    modifier: Modifier
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isOwner = currentUserId == collection.iduser

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCardClick(collection.id) }
            )

            if (collection.username.isNotBlank()) {
                val isCurrentUser = currentUserId == collection.iduser
                Text(
                    text = if (isCurrentUser) "Tu" else "@${collection.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentUser) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isCurrentUser) onMyProfileClick()
                            else onUsernameClick(collection.iduser)
                        }
                        .padding(vertical = 4.dp)
                )
            }

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
                        contentDescription = "Immagine collezione ${collection.name}",
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

            if (!isOwner && onLikeClick != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onLikeClick() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (likesCount > 0) {
                            Text(
                                text = likesCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (hasLiked) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = if (hasLiked) "Rimuovi like" else "Metti like",
                            tint = if (hasLiked) Color.Red
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}