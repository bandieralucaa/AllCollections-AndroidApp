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
 * Carosello orizzontale degli oggetti di una collezione.
 *
 * Questo componente permette di navigare tra gli oggetti di una collezione
 * tramite:
 * - Frecce laterali sinistra/destra
 * - Swipe orizzontale (drag) sul contenuto dell'oggetto
 * - Indicatori a pallini (dot) che mostrano la posizione corrente
 *
 * L'oggetto corrente viene visualizzato tramite [CarouselItemCard], che gestisce
 * la visualizzazione dei dettagli, commenti e azioni di modifica/eliminazione.
 *
 * @param items Lista degli oggetti della collezione.
 * @param currentIndex Indice corrente (0‑based) dell'oggetto visualizzato.
 * @param onIndexChange Callback invocato quando l'utente passa a un altro oggetto.
 * @param isOwner Se l'utente corrente è il proprietario della collezione (abilita modifica/eliminazione).
 * @param onEdit Callback per modificare l'oggetto corrente.
 * @param onDelete Callback per eliminare l'oggetto corrente.
 * @param onImageClick Callback quando si clicca sull'immagine dell'oggetto (per fullscreen).
 * @param itemComments Lista dei commenti relativi all'oggetto corrente.
 * @param usernames Mappa `userId -> username` per i commenti.
 * @param userPhotos Mappa `userId -> URL foto profilo` per i commenti.
 * @param currentUserId ID dell'utente corrente (per verificare se può modificare/eliminare commenti).
 * @param onAddItemComment Callback per aggiungere un commento all'oggetto corrente.
 * @param onDeleteItemComment Callback per eliminare un commento.
 * @param onEditItemComment Callback per modificare un commento (commento, nuovo testo).
 * @param navController NavController per la navigazione verso i profili pubblici.
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
    // Se non ci sono oggetti, non mostrare nulla
    if (items.isEmpty()) return

    // Assicura che l'indice corrente sia valido
    val safeIndex = currentIndex.coerceIn(0, items.lastIndex)
    val currentItem = items[safeIndex]

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Indicatore di posizione testuale (es. "Oggetto 2 di 5")
        Text(
            text = "Oggetto ${safeIndex + 1} di ${items.size}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Box che contiene la card dell'oggetto + le frecce laterali
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    // Rileva swipe orizzontale per cambiare oggetto
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 0) {
                            // Swipe verso destra → oggetto precedente
                            if (safeIndex > 0) onIndexChange(safeIndex - 1)
                        } else {
                            // Swipe verso sinistra → oggetto successivo
                            if (safeIndex < items.lastIndex) onIndexChange(safeIndex + 1)
                        }
                    }
                }
        ) {
            // Card principale dell'oggetto corrente
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

            // Pulsante freccia sinistra (oggetto precedente)
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

            // Pulsante freccia destra (oggetto successivo)
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

        // Indicatori a pallini (dot) per la posizione
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