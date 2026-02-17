package com.example.allcollections.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.feature.profile.ProfileViewModel

/**
 * Schermata principale delle impostazioni.
 *
 * Mostra le opzioni:
 * - Modifica profilo
 * - Modifica password
 * - Cambia immagine profilo
 * - Cambia tema
 * - Logout
 *
 * @param navController Controller di navigazione per passare ad altri schermi
 * @param viewModel ViewModel del profilo per operazioni come logout e ID utente
 */
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: ProfileViewModel
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Otteniamo l'ID dell'utente corrente
    val currentUserId = viewModel.getCurrentUserId()

    // Lista delle impostazioni con azione associata
    val settingsItems = listOf<Pair<String, () -> Unit>>(
        // Navigazione alle varie schermate
        "Modifica profilo" to { navController.navigate(Screens.EditProfileScreen.route) },
        "Modifica password" to { navController.navigate(Screens.EditPasswordScreen.route) },
        "Cambia immagine del profilo" to {
            if (currentUserId != null) {
                navController.navigate(
                    Screens.PhotoProfileScreen.photoProfileRoute(
                        userId = currentUserId,
                        isRegistration = false // false perché siamo in modifica, non registrazione
                    )
                )
            }
        },
        "Cambia tema" to { navController.navigate(Screens.ChooseThemeScreen.route) },
        "Logout" to {
            viewModel.cleanupListeners()
            viewModel.logout()
            navController.navigate(Screens.LoginScreen.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    )

    // Scaffold principale con topBar e snackbarHost
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { MyTopBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lista delle impostazioni cliccabili
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(settingsItems) { (setting, action) ->
                    ClickableSettingItem(setting = setting, onClick = action)
                    Divider(color = Color.Gray, thickness = 0.5.dp)
                }
            }
        }
    }
}

/**
 * Singolo elemento impostazione cliccabile.
 *
 * @param setting Testo della voce di impostazione
 * @param onClick Azione da eseguire al click
 */
@Composable
fun ClickableSettingItem(
    setting: String,
    onClick: () -> Unit
) {
    Text(
        text = setting,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .fillMaxWidth()
    )
}