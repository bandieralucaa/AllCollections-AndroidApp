package com.example.allcollections.feature.comment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.AsyncImage
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.data.model.Comment
import com.google.firebase.auth.FirebaseAuth

@Composable
fun CommentItem(
    comment: Comment,
    username: String?,
    photoUrl: String?,
    navController: NavController,
    onDelete: ((Comment) -> Unit)? = null,
    onEdit: ((Comment) -> Unit)? = null
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isMyComment = currentUserId == comment.userId
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.Top
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Foto profilo",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable {
                    if (currentUserId == comment.userId) {
                        navController.navigate(Screens.ProfileScreen.route)
                    } else {
                        navController.navigate(Screens.PublicProfileScreen.createRoute(comment.userId))
                    }
                },
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (currentUserId == comment.userId) "Tu" else (username ?: comment.userId),
                style = MaterialTheme.typography.labelSmall,
                color = if (currentUserId == comment.userId) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.clickable {
                    if (isMyComment) {
                        navController.navigate(Screens.ProfileScreen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navController.navigate(Screens.PublicProfileScreen.createRoute(comment.userId))
                    }
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Menu solo per i propri commenti
        if (isMyComment && (onDelete != null || onEdit != null)) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opzioni",
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    onEdit?.let {
                        DropdownMenuItem(
                            text = { Text("Modifica") },
                            onClick = { showMenu = false; it(comment) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                    }
                    onDelete?.let {
                        DropdownMenuItem(
                            text = { Text("Elimina", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; it(comment) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
    }
}