package com.example.allcollections.feature.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.allcollections.data.model.CollectionItem
import com.example.allcollections.data.model.Comment
import com.example.allcollections.feature.collection.ItemCommentsSection

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
    var showItemMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 500.dp)
                            .background(Color.Black)
                            .clickable { onImageClick(item.imageUrl) },
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(250.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (isOwner) {
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        IconButton(onClick = { showItemMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opzioni", tint = Color.White)
                        }
                        DropdownMenu(expanded = showItemMenu, onDismissRequest = { showItemMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Modifica") },
                                onClick = { showItemMenu = false; onEdit() },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Elimina", color = MaterialTheme.colorScheme.error) },
                                onClick = { showItemMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            if (!item.description.isNullOrBlank()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Divider(modifier = Modifier.padding(bottom = 12.dp))
                    Text(text = "Descrizione", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
                    Text(text = item.description, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            ItemCommentsSection(
                comments = itemComments,
                usernames = usernames,
                userPhotos = userPhotos,
                currentUserId = currentUserId,
                onAddComment = onAddItemComment,
                onDeleteComment = onDeleteItemComment,
                onEditComment = onEditItemComment,
                navController = navController
            )
        }
    }
}