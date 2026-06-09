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
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.example.allcollections.feature.profile.ProfileViewModel

/**
 * Schermata delle impostazioni dell'app.
 *
 * Mostra una lista verticale di voci cliccabili che permettono di accedere a:
 * - Modifica profilo (dati anagrafici)
 * - Modifica password
 * - Cambio immagine profilo
 * - Modifica biografia
 * - Cambio tema (chiaro/scuro/sistema)
 * - Logout
 *
 * ### Logout
 * Al logout vengono eseguiti i seguenti passaggi nell'ordine:
 * 1. [NotificationViewModel.stopObserving] – ferma l'ascolto delle notifiche in tempo reale.
 * 2. [ProfileViewModel.cleanupListeners] – rimuove i listener Firestore attivi (follow, ecc.).
 * 3. [ProfileViewModel.logout] – esegue il sign‑out da Firebase Auth.
 * 4. Navigazione a [Screens.LoginScreen] con reset completo della back stack (`popUpTo(0)`).
 *
 * Questa sequenza previene errori di permessi Firestore (accesso con utente nullo)
 * dopo il logout.
 *
 * @param navController Controller per la navigazione alle sottoschermate.
 * @param viewModel ViewModel del profilo (per logout, cleanup listener, userId).
 * @param notificationViewModel ViewModel delle notifiche (per fermare i listener al logout).
 *
 * @see ProfileViewModel
 * @see NotificationViewModel
 */
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: ProfileViewModel,
    notificationViewModel: NotificationViewModel
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId = viewModel.getCurrentUserId()

    // Lista delle voci di impostazione: (testo, azione)
    val settingsItems = listOf<Pair<String, () -> Unit>>(
        "Modifica profilo" to { navController.navigate(Screens.EditProfileScreen.route) },
        "Modifica password" to { navController.navigate(Screens.EditPasswordScreen.route) },
        "Cambia immagine del profilo" to {
            if (currentUserId != null) {
                navController.navigate(
                    Screens.PhotoProfileScreen.photoProfileRoute(
                        userId = currentUserId,
                        isRegistration = false
                    )
                )
            }
        },
        "Modifica bio" to { navController.navigate(Screens.EditBioScreen.route) },
        "Cambia tema" to { navController.navigate(Screens.ChooseThemeScreen.route) },
        "Logout" to {
            // Ordine critico: prima ferma i listener, poi logout, infine naviga
            notificationViewModel.stopObserving()
            viewModel.cleanupListeners()
            viewModel.logout()
            navController.navigate(Screens.LoginScreen.route) {
                popUpTo(0) { inclusive = true } // Resetta completamente la back stack
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { MyTopBar(navController = navController, title = "Impostazioni") }
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
 * Riga cliccabile per una singola voce di impostazione.
 *
 * Mostra il testo su tutta la larghezza, con padding verticale e orizzontale.
 * Al tap esegue l'azione associata.
 *
 * @param setting Testo della voce (es. "Modifica profilo").
 * @param onClick Callback invocato al tap sull'elemento.
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