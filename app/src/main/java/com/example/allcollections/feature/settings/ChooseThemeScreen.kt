package com.example.allcollections.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.core.theme.ThemeMode
import com.example.allcollections.core.theme.ThemeState

/**
 * Schermata per la selezione del tema dell'app.
 *
 * Mostra i tre temi disponibili ([ThemeMode]) come radio button selezionabili.
 * Usa `selectableGroup` per l'accessibilità e rispetta la semantica corretta
 * per i screen reader con `Role.RadioButton`.
 *
 * @param state Stato corrente del tema con il valore attivo.
 * @param onThemeSelected Callback invocato quando l'utente seleziona un tema.
 * @param navController NavController per tornare indietro.
 */
@Composable
fun ChooseTheme(
    state: ThemeState,
    onThemeSelected: (theme: ThemeMode) -> Unit,
    navController: NavController
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { MyTopBar(navController = navController, title = "Cambia tema") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .selectableGroup()
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ThemeMode.entries.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (theme == state.theme),
                            onClick = { onThemeSelected(theme) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (theme == state.theme),
                        onClick = null
                    )
                    Text(
                        text = theme.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}