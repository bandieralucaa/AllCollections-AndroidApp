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
 * TopBar personalizzata per l'app AllCollections.
 *
 * Mostra un titolo centrato e opzionalmente un bottone "indietro".
 * - Il back button di default chiama navController.popBackStack().
 * - Può essere fornita una callback personalizzata con onBackClick.
 *
 * @param navController Controller della navigazione.
 * @param showBackButton Mostra o nasconde il back button (default = true).
 * @param onBackClick Callback opzionale quando si clicca il back button.
 * @param title Titolo centrato della TopBar.
 * @param actions Azioni aggiuntive da mostrare a destra (opzionale).
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
            .height(56.dp) // Altezza standard AppBar Material3
    ) {
        // ─────────── Bottone Indietro ───────────
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