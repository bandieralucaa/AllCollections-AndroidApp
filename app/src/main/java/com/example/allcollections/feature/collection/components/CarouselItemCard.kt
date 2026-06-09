package com.example.allcollections.feature.collection.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.data.model.CollectionItem
import com.example.allcollections.data.model.Comment
import com.example.allcollections.feature.comment.CommentItem

/**
 * Card di un singolo oggetto all'interno del carosello della collezione (ItemsCarousel).
 *
 * Mostra:
 * - Immagine dell'oggetto (cliccabile per fullscreen)
 * - Descrizione testuale
 * - Pulsanti "Modifica" ed "Elimina" (visibili solo per il proprietario della collezione)
 * - Sezione commenti dedicata all'oggetto (espandibile/collassabile)
 * - Campo per aggiungere un nuovo commento (solo se l'utente è autenticato)
 *
 * I commenti dell'oggetto sono gestiti separatamente dai commenti della collezione.
 *
 * @param item Oggetto della collezione da visualizzare.
 * @param isOwner Indica se l'utente corrente è il proprietario della collezione.
 * @param onEdit Callback per la modifica dell'oggetto.
 * @param onDelete Callback per l'eliminazione dell'oggetto.
 * @param onImageClick Callback per visualizzare l'immagine in fullscreen (riceve URL).
 * @param itemComments Lista dei commenti relativi a questo oggetto.
 * @param usernames Mappa `userId -> username` per visualizzare l'autore dei commenti.
 * @param userPhotos Mappa `userId -> URL foto profilo` per i commenti.
 * @param currentUserId ID dell'utente corrente (se null, il campo commento non è visibile).
 * @param onAddItemComment Callback per aggiungere un commento all'oggetto (riceve testo).
 * @param onDeleteItemComment Callback per eliminare un commento (riceve il commento).
 * @param onEditItemComment Callback per modificare un commento (riceve commento e nuovo testo).
 * @param navController Controller per navigazione ai profili pubblici dai commenti.
 */
@Composable
fun CarouselItemCard(
    item: CollectionItem,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit,
    itemComments: List<Comment>,
    usernames: Map<String, String>,
    userPhotos: Map<String, String>,
    currentUserId: String?,
    onAddItemComment: (String) -> Unit,
    onDeleteItemComment: (Comment) -> Unit,
    onEditItemComment: (Comment, String) -> Unit,
    navController: NavController
) {
    var newComment by remember { mutableStateOf("") }
    var showItemComments by remember(item.id) { mutableStateOf(true) }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }
    var commentToEdit by remember { mutableStateOf<Comment?>(null) }
    var editText by remember { mutableStateOf("") }

    // Dialog di conferma eliminazione commento
    commentToDelete?.let { comment ->
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = { Text("Elimina commento") },
            text = { Text("Sei sicuro di voler eliminare questo commento?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteItemComment(comment)
                    commentToDelete = null
                }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { commentToDelete = null }) { Text("Annulla") }
            }
        )
    }

    // Dialog di modifica commento
    commentToEdit?.let { comment ->
        AlertDialog(
            onDismissRequest = { commentToEdit = null },
            title = { Text("Modifica commento") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    placeholder = { Text("Scrivi il tuo commento...") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editText.isNotBlank()) {
                        onEditItemComment(comment, editText)
                        commentToEdit = null
                    }
                }) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = { commentToEdit = null }) { Text("Annulla") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Immagine dell'oggetto (o placeholder)
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.description.ifBlank { "Immagine oggetto" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .clickable { onImageClick(item.imageUrl) },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Nessuna immagine",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Descrizione dell'oggetto
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "Nessuna descrizione",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                // Pulsanti Modifica/Elimina (solo per il proprietario)
                if (isOwner) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Modifica oggetto",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Modifica")
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Elimina oggetto",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Elimina")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Header commenti oggetto (espandibile/collassabile)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showItemComments = !showItemComments }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Commenti oggetto (${itemComments.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (showItemComments) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showItemComments) "Comprimi" else "Espandi",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Lista commenti oggetto
                if (showItemComments) {
                    Spacer(modifier = Modifier.height(8.dp))

                    if (itemComments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nessun commento su questo oggetto.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(
                                items = itemComments,
                                key = { it.id }
                            ) { comment ->
                                CommentItem(
                                    comment = comment,
                                    username = usernames[comment.userId] ?: "Utente",
                                    photoUrl = userPhotos[comment.userId],
                                    navController = navController,
                                    onDelete = { commentToDelete = it },
                                    onEdit = { editText = it.text; commentToEdit = it }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    // Campo per aggiungere un nuovo commento (solo se autenticato)
                    if (currentUserId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newComment,
                                onValueChange = { newComment = it },
                                placeholder = { Text("Commenta questo oggetto...") },
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                shape = RoundedCornerShape(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FloatingActionButton(
                                onClick = {
                                    if (newComment.isNotBlank()) {
                                        onAddItemComment(newComment)
                                        newComment = ""
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Invia commento",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}