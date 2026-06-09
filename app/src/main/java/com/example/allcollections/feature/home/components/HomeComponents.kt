package com.example.allcollections.feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.data.model.CollectionCardLayout
import com.example.allcollections.data.model.UserCollection
import com.example.allcollections.feature.collection.components.CollectionCard
import com.example.allcollections.feature.home.HomeFilter

// ─────────────────────────────────────────────────────────────────────────────
// Filtri
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pulsante che apre il dialog di selezione filtro.
 *
 * Mostra un'icona di filtro e, se il filtro attivo non è [HomeFilter.All],
 * visualizza un piccolo badge di notifica in sovraimpressione.
 *
 * @param activeFilter Filtro attualmente selezionato.
 * @param onClick Callback invocato al tap sul pulsante (per mostrare il dialog).
 */
@Composable
fun FilterButton(activeFilter: HomeFilter, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.FilterList, contentDescription = "Filtra collezioni")
        }
        // Badge visibile solo se il filtro non è "Tutte"
        if (activeFilter != HomeFilter.All) {
            Badge(modifier = Modifier.offset(x = (-8).dp, y = 8.dp))
        }
    }
}

/**
 * Dialog per la selezione del filtro delle collezioni.
 *
 * Presenta tre opzioni tramite [FilterOption]:
 * - Tutte le collezioni
 * - Solo utenti seguiti
 * - Collezioni preferite (con like)
 *
 * @param activeFilter Filtro attualmente selezionato (per evidenziare l'opzione corretta).
 * @param onDismiss Callback per chiudere il dialog.
 * @param onSelectAll Callback quando si seleziona "Tutte le collezioni".
 * @param onSelectFollowed Callback quando si seleziona "Solo utenti seguiti".
 * @param onSelectLiked Callback quando si seleziona "Collezioni preferite".
 */
@Composable
fun FilterDialog(
    activeFilter: HomeFilter,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectFollowed: () -> Unit,
    onSelectLiked: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtra collezioni") },
        text = {
            Column {
                FilterOption("Tutte le collezioni", activeFilter == HomeFilter.All, onSelectAll)
                FilterOption("Solo utenti seguiti", activeFilter == HomeFilter.Followed, onSelectFollowed)
                FilterOption("Collezioni preferite", activeFilter == HomeFilter.Liked, onSelectLiked)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}

/**
 * Singola opzione di filtro all'interno di [FilterDialog].
 *
 * Combina un [RadioButton] con un'etichetta testuale. L'intera riga è cliccabile.
 *
 * @param text Etichetta dell'opzione.
 * @param selected Se `true`, il radio button è selezionato.
 * @param onClick Callback invocato al tap sull'opzione.
 */
@Composable
fun FilterOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Griglia e stati
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Griglia a due colonne che mostra le collezioni come card.
 *
 * Ogni card è renderizzata da [CollectionCard] e supporta like, click per navigare
 * ai dettagli della collezione, click sull'username per il profilo pubblico.
 *
 * @param collections Lista delle collezioni da visualizzare.
 * @param navController Controller per la navigazione.
 * @param likedMap Mappa `collectionId -> liked` (true se l'utente ha messo like).
 * @param likesCountMap Mappa `collectionId -> conteggio like`.
 * @param onLikeClick Callback invocato al tap sul pulsante like di una collezione.
 */
@Composable
fun CollectionsGrid(
    collections: List<UserCollection>,
    navController: NavController,
    likedMap: Map<String, Boolean>,
    likesCountMap: Map<String, Int>,
    onLikeClick: (UserCollection) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(collections) { collection ->
            CollectionCard(
                collection = collection,
                layoutType = CollectionCardLayout.Vertical,
                showMenu = false,
                onCardClick = { collectionId ->
                    navController.navigate(Screens.CollectionDetailScreen.createRoute(collectionId))
                },
                onUsernameClick = { userId ->
                    navController.navigate(Screens.PublicProfileScreen.createRoute(userId))
                },
                onMyProfileClick = {
                    navController.navigate(Screens.ProfileScreen.route)
                },
                hasLiked = likedMap[collection.id] ?: false,
                likesCount = likesCountMap[collection.id] ?: 0,
                onLikeClick = { onLikeClick(collection) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Vista di caricamento mostrata mentre le collezioni vengono caricate.
 *
 * Visualizza un [CircularProgressIndicator] centrato e un messaggio testuale.
 */
@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Caricamento collezioni...")
        }
    }
}

/**
 * Vista mostrata quando la lista delle collezioni è vuota.
 *
 * Il messaggio varia in base al filtro attivo e allo stato di autenticazione.
 *
 * @param activeFilter Filtro attualmente selezionato.
 * @param currentUserId ID dell'utente corrente (può essere `null` se non autenticato).
 */
@Composable
fun EmptyView(activeFilter: HomeFilter, currentUserId: String?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            val message = when {
                activeFilter == HomeFilter.Liked ->
                    "Non hai ancora messo like a nessuna collezione"
                activeFilter == HomeFilter.Followed && currentUserId != null ->
                    "Non segui ancora nessun utente con collezioni pubbliche"
                activeFilter == HomeFilter.Followed && currentUserId == null ->
                    "Accedi per vedere le collezioni degli utenti che segui"
                currentUserId != null ->
                    "Nessun'altra collezione pubblica disponibile."
                else ->
                    "Nessuna collezione pubblica disponibile"
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}