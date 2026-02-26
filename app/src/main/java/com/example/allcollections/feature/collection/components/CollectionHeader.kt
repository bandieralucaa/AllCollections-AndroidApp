package com.example.allcollections.feature.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun CollectionHeader(
    collection: com.example.allcollections.data.model.UserCollection,
    itemsCount: Int,
    commentsCount: Int,
    likesCount: Int,
    isOwner: Boolean,
    onAddObjectClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    onLikesCountClick: () -> Unit = {}
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
        ) {
            if (!collection.collectionImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = collection.collectionImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clickable { collection.collectionImageUrl?.let { onImageClick(it) } },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.White.copy(alpha = 0.5f))
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(text = collection.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(text = collection.category ?: "Senza categoria", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Text(text = itemsCount.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Text(text = commentsCount.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = if (isOwner) Modifier.clickable { onLikesCountClick() } else Modifier
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Text(text = likesCount.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }

            if (isOwner) {
                Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opzioni", tint = Color.White)
                    }
                }
            }
        }

        if (!collection.description.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Text(text = collection.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            }
        }

        if (isOwner) {
            Button(
                onClick = onAddObjectClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi oggetto")
            }
        }
    }
}
