package com.example.allcollections.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * TopBar personalizzata e riutilizzabile per tutte le schermate dell'app.
 *
 * Questa barra superiore fornisce un layout standardizzato con:
 * - Pulsante di navigazione indietro (opzionale) allineato a sinistra
 * - Titolo centrato
 * - Slot per azioni personalizzate allineato a destra (es. menu, modifica, salvataggio)
 *
 * ### Comportamento del pulsante indietro
 * - Se [onBackClick] è fornito, viene eseguito al suo posto.
 * - Altrimenti, chiama [NavController.popBackStack] per tornare alla schermata precedente.
 * - Se [showBackButton] è `false`, il pulsante non viene mostrato.
 *
 * ### Esempio di utilizzo
 * ```
 * MyTopBar(
 *     navController = navController,
 *     title = "Profilo",
 *     actions = {
 *         IconButton(onClick = { /* modifica profilo */ }) {
 *             Icon(Icons.Default.Edit, contentDescription = "Modifica")
 *         }
 *     }
 * )
 * ```
 *
 * @param navController Controller di navigazione (usato per popBackStack di default).
 * @param showBackButton Se `true`, mostra il pulsante indietro. Default `true`.
 * @param onBackClick Callback personalizzato per il back. Se `null`, usa `navController.popBackStack()`.
 * @param title Titolo da centrare nella barra. Può essere vuoto.
 * @param actions Slot composable per icone o elementi sulla destra. Default vuoto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(
    navController: NavController,
    showBackButton: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    title: String = "",
    actions: @Composable () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        // Pulsante indietro (allineato a sinistra) - opzionale
        if (showBackButton) {
            IconButton(
                onClick = { onBackClick?.invoke() ?: navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Torna indietro",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Titolo centrato
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Center)
        )

        // Slot per azioni aggiuntive (allineato a destra)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        ) {
            actions()
        }
    }
}