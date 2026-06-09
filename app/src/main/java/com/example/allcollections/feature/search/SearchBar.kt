package com.example.allcollections.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Barra di ricerca personalizzata con icona lente, campo testo e pulsante "X" per svuotare.
 *
 * Utilizza [BasicTextField] per massima flessibilità e integra un'icona di ricerca a sinistra,
 * un campo di testo con placeholder personalizzabile e un pulsante di cancellazione (X)
 * che appare solo quando il campo non è vuoto.
 *
 * La barra è racchiusa in un [Surface] con angoli arrotondati e ombra leggera.
 *
 * @param query Testo corrente nel campo di ricerca.
 * @param onQueryChange Callback invocato ad ogni modifica del testo.
 * @param onClear Callback invocato quando si preme il pulsante "X".
 * @param modifier Modificatore opzionale per personalizzare dimensioni e posizionamento.
 * @param placeholder Testo placeholder mostrato quando il campo è vuoto (default: "Cerca collezioni o utenti...").
 * @param enabled Se `false` disabilita l'input (es. durante il caricamento).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cerca collezioni o utenti...",
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp)),
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icona lente di ricerca
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cerca",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Campo di testo libero (senza decorazioni Material)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                },
                singleLine = true
            )

            // Pulsante "X" per cancellare il testo (visibile solo quando c'è testo)
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Cancella ricerca",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}