package com.example.allcollections.feature.collection.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.allcollections.data.model.CollectionItem

/**
 * Card riutilizzabile per mostrare un oggetto della collezione.
 *
 * @param item Dati dell'oggetto da visualizzare
 * @param showMenu Se true mostra il menu per Modifica/Elimina
 * @param onEdit Callback quando si clicca su "Modifica"
 * @param onDelete Callback quando si clicca su "Elimina"
 * @param onImageClick Callback quando si clicca sull'immagine
 * @param modifier Modifier opzionale per personalizzazioni
 */
@Composable
fun CollectionItemCard(
    item: CollectionItem,
    showMenu: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onImageClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Immagine oggetto cliccabile
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "Immagine oggetto",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onImageClick(item.imageUrl) },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Descrizione oggetto
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            // Menu opzionale Modifica/Elimina
            if (showMenu) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Opzioni oggetto")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Modifica") },
                            onClick = {
                                expanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Elimina") },
                            onClick = {
                                expanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
