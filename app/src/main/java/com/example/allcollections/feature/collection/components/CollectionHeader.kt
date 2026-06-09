package com.example.allcollections.feature.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Header della schermata di dettaglio collezione.
 *
 * Questo componente visualizza la parte superiore della schermata [CollectionDetailScreen],
 * includendo:
 * - Immagine di copertina (o placeholder con gradiente e icona)
 * - Overlay scuro per migliorare la leggibilità del testo
 * - Nome e categoria della collezione in sovrimpressione
 * - Statistiche (numero di oggetti, commenti, like) in alto a destra
 * - Pulsante menu (solo owner) in alto a sinistra
 * - Descrizione della collezione (se presente) in una card sottostante
 * - Pulsante "Aggiungi oggetto" (solo owner)
 *
 * Il conteggio dei like è cliccabile solo per il proprietario, e apre il dialog
 * con la lista degli utenti che hanno messo like.
 *
 * @param collection La collezione da visualizzare (nome, categoria, descrizione, immagine).
 * @param itemsCount Numero di oggetti nella collezione.
 * @param commentsCount Numero di commenti sulla collezione.
 * @param likesCount Numero di like ricevuti dalla collezione.
 * @param isOwner Se l'utente corrente è il proprietario (abilita menu e pulsante aggiungi oggetto).
 * @param onAddObjectClick Callback per aggiungere un nuovo oggetto alla collezione.
 * @param onImageClick Callback per visualizzare l'immagine di copertina in fullscreen.
 * @param onMenuClick Callback per aprire il menu contestuale (modifica/elimina collezione).
 * @param onLikesCountClick Callback per visualizzare la lista dei likers (solo owner). Default vuoto.
 */
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
        // ─────────── Banner superiore con immagine di copertina (o placeholder) ───────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
        ) {
            // Immagine di copertina (se disponibile)
            if (!collection.collectionImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = collection.collectionImageUrl,
                    contentDescription = "Immagine di copertina della collezione ${collection.name}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { collection.collectionImageUrl?.let { onImageClick(it) } },
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder con gradiente e icona
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Collections,
                        contentDescription = "Icona placeholder collezione",
                        modifier = Modifier.size(80.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // Overlay scuro per migliorare la leggibilità del testo
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

            // Nome e categoria in basso a sinistra
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = collection.category ?: "Senza categoria",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // Statistiche (oggetti, commenti, like) in alto a destra
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Conteggio oggetti
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Collections, contentDescription = "Numero oggetti", modifier = Modifier.size(18.dp), tint = Color.White)
                    Text(text = itemsCount.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                // Conteggio commenti
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Numero commenti", modifier = Modifier.size(18.dp), tint = Color.White)
                    Text(text = commentsCount.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                // Conteggio like (cliccabile solo per owner)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = if (isOwner) Modifier.clickable { onLikesCountClick() } else Modifier
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Numero like", modifier = Modifier.size(18.dp), tint = Color.White)
                    Text(text = likesCount.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }

            // Pulsante menu (solo owner) in alto a sinistra
            if (isOwner) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opzioni collezione", tint = Color.White)
                    }
                }
            }
        }

        // ─────────── Card della descrizione (se presente) ───────────
        if (!collection.description.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = collection.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // ─────────── Pulsante "Aggiungi oggetto" (solo owner) ───────────
        if (isOwner) {
            Button(
                onClick = onAddObjectClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi oggetto", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi oggetto")
            }
        }
    }
}