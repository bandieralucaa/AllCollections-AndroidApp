package com.example.allcollections.feature.comment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    navController: NavController
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.Top
    ) {
        // Foto profilo
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

        Column(modifier = Modifier.fillMaxWidth()) {
            // Username
            Text(
                text = if (currentUserId == comment.userId) "Tu" else (username ?: comment.userId),
                style = MaterialTheme.typography.labelSmall,
                color = if (currentUserId == comment.userId) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.clickable {
                    val isCurrentUser = currentUserId == comment.userId

                    if (isCurrentUser) {
                        // Se sono io, naviga al profilo già presente nella bottom bar
                        navController.navigate(Screens.ProfileScreen.route) {
                            // Torna alla tab del profilo (bottom bar)
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        // Se è un altro, naviga al profilo pubblico (stack normale)
                        navController.navigate(Screens.PublicProfileScreen.createRoute(comment.userId))
                    }
                }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Testo commento
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}