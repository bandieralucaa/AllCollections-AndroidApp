package com.example.allcollections.feature.collection.components


import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.data.model.CollectionItem
import com.example.allcollections.data.model.Comment

/**
 * Carosello degli oggetti di una collezione.
 *
 * Permette di scorrere gli oggetti tramite frecce laterali o swipe orizzontale.
 * Mostra l'indice corrente, i dot di navigazione in basso e delega il rendering
 * del singolo oggetto a [CarouselItemCard].
 */
@Composable
fun ItemsCarousel(
    items: List<CollectionItem>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    isOwner: Boolean,
    onEdit: (CollectionItem) -> Unit,
    onDelete: (CollectionItem) -> Unit,
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
    if (items.isEmpty()) return

    val safeIndex = currentIndex.coerceIn(0, items.lastIndex)
    val currentItem = items[safeIndex]

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Oggetto ${safeIndex + 1} di ${items.size}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 0) {
                            if (safeIndex > 0) onIndexChange(safeIndex - 1)
                        } else {
                            if (safeIndex < items.lastIndex) onIndexChange(safeIndex + 1)
                        }
                    }
                }
        ) {
            CarouselItemCard(
                item = currentItem,
                isOwner = isOwner,
                onEdit = { onEdit(currentItem) },
                onDelete = { onDelete(currentItem) },
                onImageClick = onImageClick,
                itemComments = itemComments,
                usernames = usernames,
                userPhotos = userPhotos,
                currentUserId = currentUserId,
                onAddItemComment = onAddItemComment,
                onDeleteItemComment = onDeleteItemComment,
                onEditItemComment = onEditItemComment,
                navController = navController
            )

            IconButton(
                onClick = { if (safeIndex > 0) onIndexChange(safeIndex - 1) },
                enabled = safeIndex > 0,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Oggetto precedente",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { if (safeIndex < items.lastIndex) onIndexChange(safeIndex + 1) },
                enabled = safeIndex < items.lastIndex,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Oggetto successivo",
                    tint = Color.White
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(items.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == safeIndex) 12.dp else 8.dp)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == safeIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                )
            }
        }
    }
}