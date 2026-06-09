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
 * Mostra una lista cliccabile di voci di impostazione: modifica profilo, password,
 * foto, bio, tema e logout. Il logout arresta i listener attivi ([NotificationViewModel.stopObserving],
 * [ProfileViewModel.cleanupListeners]) prima di eseguire il sign-out Firebase,
 * evitando errori di permessi Firestore su utente non autenticato.
 *
 * @param navController NavController per la navigazione alle sotto-schermate.
 * @param viewModel ViewModel del profilo, usato per logout e recupero userId.
 * @param notificationViewModel ViewModel notifiche, usato per fermare i listener al logout.
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
            notificationViewModel.stopObserving()
            viewModel.cleanupListeners()
            viewModel.logout()
            navController.navigate(Screens.LoginScreen.route) {
                popUpTo(0) { inclusive = true }
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