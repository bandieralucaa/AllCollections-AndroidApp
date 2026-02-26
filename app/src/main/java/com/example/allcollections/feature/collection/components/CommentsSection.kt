package com.example.allcollections.feature.collection.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.data.model.Comment
import com.example.allcollections.feature.comment.CommentItem

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
    var newComment by remember { mutableStateOf("") }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }
    var commentToEdit by remember { mutableStateOf<Comment?>(null) }
    var editText by remember { mutableStateOf("") }

    commentToDelete?.let { comment ->
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = { Text("Elimina commento") },
            text = { Text("Sei sicuro di voler eliminare questo commento?") },
            confirmButton = {
                TextButton(onClick = { onDeleteComment(comment); commentToDelete = null }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { commentToDelete = null }) { Text("Annulla") }
            }
        )
    }

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
                    if (editText.isNotBlank()) { onEditComment(comment, editText); commentToEdit = null }
                }) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = { commentToEdit = null }) { Text("Annulla") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleComments() }.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Commenti (${comments.size})", style = MaterialTheme.typography.titleMedium)
                }
                Icon(if (showComments) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }

            if (showComments) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (comments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Nessun commento. Sii il primo a commentare!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
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

                if (currentUserId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
                                if (newComment.isNotBlank()) { onAddComment(newComment); newComment = "" }
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Invia", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}