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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.data.model.Comment
import com.example.allcollections.feature.comment.CommentItem

/**
 * Sezione commenti della collezione, collassabile.
 *
 * Mostra:
 * - Header con icona, titolo "Commenti (n)" e freccia per espandere/collassare.
 * - Lista dei commenti (in [LazyColumn] con altezza massima di 400dp).
 * - Messaggio "Nessun commento" se la lista è vuota.
 * - Campo di input per aggiungere un nuovo commento (solo se [currentUserId] non è null).
 * - Dialog di conferma per l'eliminazione di un commento.
 * - Dialog per la modifica di un commento (con campo di testo precompilato).
 *
 * @param comments Lista dei commenti della collezione (già arricchita con username? No, le mappe separatamente).
 * @param usernames Mappa `userId -> username` per i commenti (per visualizzare l'autore).
 * @param userPhotos Mappa `userId -> URL foto profilo` per i commenti.
 * @param currentUserId ID dell'utente corrente (se null, non mostra il campo di input).
 * @param showComments Se `true`, mostra il contenuto espanso della sezione.
 * @param onToggleComments Callback per espandere/collassare la sezione.
 * @param onAddComment Callback per aggiungere un commento (riceve il testo).
 * @param onDeleteComment Callback per eliminare un commento (riceve il commento).
 * @param onEditComment Callback per modificare un commento (riceve commento e nuovo testo).
 * @param navController Controller per la navigazione ai profili pubblici (passato a [CommentItem]).
 */
@Composable
fun CommentsSection(
    comments: List<Comment>,
    usernames: Map<String, String>,
    userPhotos: Map<String, String>,
    currentUserId: String?,
    showComments: Boolean,
    onToggleComments: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (Comment) -> Unit,
    onEditComment: (Comment, String) -> Unit,
    navController: NavController
) {
    // Stato per il nuovo commento
    var newComment by remember { mutableStateOf("") }

    // Stato per il dialog di eliminazione
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }

    // Stato per il dialog di modifica
    var commentToEdit by remember { mutableStateOf<Comment?>(null) }
    var editText by remember { mutableStateOf("") }

    // ─────────── Dialog di conferma eliminazione ───────────
    commentToDelete?.let { comment ->
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = { Text("Elimina commento") },
            text = { Text("Sei sicuro di voler eliminare questo commento?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteComment(comment)
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

    // ─────────── Dialog di modifica commento ───────────
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
                        onEditComment(comment, editText)
                        commentToEdit = null
                    }
                }) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = { commentToEdit = null }) { Text("Annulla") }
            }
        )
    }

    // ─────────── Card principale della sezione commenti ───────────
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header cliccabile per espandere/collassare
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleComments() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Commenti (${comments.size})", style = MaterialTheme.typography.titleMedium)
                }
                Icon(
                    if (showComments) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showComments) "Comprimi commenti" else "Espandi commenti"
                )
            }

            // Contenuto espanso
            if (showComments) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Lista vuota
                if (comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Nessun commento. Sii il primo a commentare!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Lista dei commenti con altezza massima di 400dp
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(comments) { comment ->
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

                // Campo per aggiungere un nuovo commento (solo se l'utente è autenticato)
                if (currentUserId != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newComment,
                            onValueChange = { newComment = it },
                            placeholder = { Text("Scrivi un commento...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                if (newComment.isNotBlank()) {
                                    onAddComment(newComment)
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