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

/**
 * Componente per la visualizzazione di un singolo commento.
 *
 * Mostra:
 * - Foto profilo dell'autore (cliccabile per navigare al profilo)
 * - Username (cliccabile per navigare al profilo)
 * - Testo del commento
 * - Menu contestuale (modifica/elimina) se il commento appartiene all'utente corrente
 *
 * @param comment Il commento da visualizzare.
 * @param username Nome utente dell'autore (se null, mostra l'userId come fallback).
 * @param photoUrl URL della foto profilo dell'autore (opzionale).
 * @param navController Controller per la navigazione.
 * @param onDelete Callback opzionale per eliminare il commento.
 * @param onEdit Callback opzionale per modificare il commento.
 */
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
        // Foto profilo (cliccabile)
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

        // Contenuto testuale: username + commento
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
                        // Naviga al proprio profilo con reset della back stack per evitare loop
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

        // Menu contestuale (solo per i commenti dell'utente corrente e se ci sono callback)
        if (isMyComment && (onDelete != null || onEdit != null)) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opzioni commento",
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
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Modifica commento") }
                        )
                    }
                    onDelete?.let {
                        DropdownMenuItem(
                            text = { Text("Elimina", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; it(comment) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Elimina commento",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}