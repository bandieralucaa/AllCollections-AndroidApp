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
 * TopBar personalizzata riusabile per tutte le schermate dell'app.
 *
 * Mostra un titolo centrato, un pulsante "indietro" opzionale a sinistra
 * e uno slot per azioni aggiuntive a destra (es. icone di menu o modifica).
 *
 * Il back button chiama [onBackClick] se fornito, altrimenti esegue
 * [NavController.popBackStack] come comportamento di default.
 *
 * @param navController Controller della navigazione, usato per il back di default.
 * @param showBackButton Se `true` mostra il pulsante indietro; default `true`.
 * @param onBackClick Callback personalizzato per il back button; se `null` usa `popBackStack`.
 * @param title Testo del titolo centrato nella barra.
 * @param actions Slot Composable per azioni aggiuntive allineate a destra (default vuoto).
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
        // ─────────── Pulsante indietro (sinistra) ───────────
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

        // ─────────── Titolo centrato ───────────
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Center)
        )

        // ─────────── Azioni (destra) ───────────
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        ) {
            actions()
        }
    }
}
