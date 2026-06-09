package com.example.allcollections.core.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.allcollections.core.theme.ThemeMode
import com.example.allcollections.core.theme.ThemeState
import com.example.allcollections.feature.auth.*
import com.example.allcollections.feature.chat.presentation.ChatScreen
import com.example.allcollections.feature.chat.presentation.ChatViewModel
import com.example.allcollections.feature.chat.presentation.ChatsListScreen
import com.example.allcollections.feature.collection.*
import com.example.allcollections.feature.home.HomeScreen
import com.example.allcollections.feature.notification.NotificationsScreen
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.example.allcollections.feature.profile.*
import com.example.allcollections.feature.publicProfile.PublicProfileScreen
import com.example.allcollections.feature.search.SearchScreen
import com.example.allcollections.feature.settings.*
import org.koin.androidx.compose.koinViewModel

/**
 * Grafo di navigazione principale dell'app.
 *
 * Definisce tutte le rotte disponibili tramite [NavHost] e gestisce la
 * [BottomNavBar] (nascosta nelle schermate di autenticazione e foto profilo).
 * Le destinazioni sono raggruppate per area funzionale in funzioni di estensione
 * private su [NavGraphBuilder]:
 * - [authNav] — login, registrazione, verifica email, password dimenticata
 * - [homeNav] — feed principale
 * - [searchNav] — ricerca utenti e collezioni
 * - [notificationsNav] — lista notifiche
 * - [profileNav] — profilo personale e pubblico, modifica dati
 * - [collectionNav] — creazione, dettaglio, modifica collezioni e oggetti
 * - [chatNav] — lista chat e singola conversazione
 * - [settingsNav] — impostazioni app e scelta tema
 *
 * @param navController Controller della navigazione condiviso con tutta l'app.
 * @param startDestination Rotta iniziale, determinata in [MainActivity] in base allo stato auth.
 * @param themeState Stato corrente del tema, passato alla schermata di scelta tema.
 * @param onThemeSelected Callback invocato quando l'utente cambia il tema.
 * @param notificationViewModel ViewModel condiviso per badge notifiche e navigazione da notifica.
 * @param chatViewModel ViewModel condiviso per il conteggio messaggi non letti nel badge chat.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    themeState: ThemeState,
    onThemeSelected: (ThemeMode) -> Unit,
    notificationViewModel: NotificationViewModel = koinViewModel(),
    chatViewModel: ChatViewModel = koinViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val hasUnreadNotifications by notificationViewModel
        .hasUnreadNotifications
        .collectAsState(initial = false)

    val unreadMessagesCount by chatViewModel.unreadMessagesCount.collectAsState(initial = 0)

    LaunchedEffect(Unit) {
        chatViewModel.observeRecentChats()
    }

    Scaffold(
        bottomBar = {
            // La bottom bar è nascosta nelle schermate auth e nella schermata foto profilo
            val hideBottomBarRoutes = listOf(
                Screens.LoginScreen.route,
                Screens.RegisterScreen.route,
                Screens.VerifyEmailScreen.route,
                Screens.ForgotPasswordScreen.route,
                Screens.PhotoProfileScreen.route
            )
            if (currentDestination?.route != null &&
                hideBottomBarRoutes.none { currentDestination.route!!.startsWith(it) }
            ) {
                BottomNavBar(
                    currentDestination = currentDestination,
                    navController = navController,
                    hasUnreadNotifications = hasUnreadNotifications,
                    unreadMessagesCount = unreadMessagesCount
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            authNav(navController)
            homeNav(navController)
            searchNav(navController)
            notificationsNav(navController, notificationViewModel)
            profileNav(navController)
            collectionNav(navController)
            settingsNav(navController, themeState, onThemeSelected, notificationViewModel)
            chatNav(navController)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM BAR
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Bottom Navigation Bar dell'app.
 *
 * Mostra i badge per notifiche non lette e messaggi non letti.
 * La selezione attiva è determinata confrontando la gerarchia della destinazione
 * corrente con la rotta di ciascun elemento.
 *
 * @param currentDestination Destinazione corrente nel back stack.
 * @param navController Controller per la navigazione al tap su un elemento.
 * @param hasUnreadNotifications `true` se ci sono notifiche non lette (mostra badge rosso).
 * @param unreadMessagesCount Numero di messaggi non letti (mostra badge con contatore).
 */
@Composable
private fun BottomNavBar(
    currentDestination: androidx.navigation.NavDestination?,
    navController: NavHostController,
    hasUnreadNotifications: Boolean,
    unreadMessagesCount: Int
) {
    NavigationBar(modifier = Modifier.height(72.dp)) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    when {
                        item.screen == Screens.NotificationsScreen && hasUnreadNotifications -> {
                            BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) }) {
                                Icon(item.icon, contentDescription = null, modifier = Modifier.size(28.dp))
                            }
                        }
                        item.screen == Screens.ChatsListScreen && unreadMessagesCount > 0 -> {
                            BadgedBox(badge = {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text(unreadMessagesCount.toString())
                                }
                            }) {
                                Icon(item.icon, contentDescription = null, modifier = Modifier.size(28.dp))
                            }
                        }
                        else -> Icon(item.icon, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AUTH
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Registra le destinazioni del flusso di autenticazione:
 * login, registrazione, verifica email e recupero password.
 */
private fun NavGraphBuilder.authNav(navController: NavHostController) {
    composable(Screens.LoginScreen.route) {
        LoginScreen(navController, koinViewModel())
    }
    composable(Screens.RegisterScreen.route) {
        RegisterScreen(navController, koinViewModel())
    }
    composable(Screens.VerifyEmailScreen.route) {
        VerifyEmailScreen(navController, koinViewModel())
    }
    composable(Screens.ForgotPasswordScreen.route) {
        ForgotPasswordScreen(navController)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME
// ─────────────────────────────────────────────────────────────────────────────

/** Registra la destinazione della schermata home (feed principale). */
private fun NavGraphBuilder.homeNav(navController: NavHostController) {
    composable(Screens.HomeScreen.route) {
        HomeScreen(navController, koinViewModel(), koinViewModel())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEARCH
// ─────────────────────────────────────────────────────────────────────────────

/** Registra la destinazione della schermata di ricerca utenti e collezioni. */
private fun NavGraphBuilder.searchNav(navController: NavHostController) {
    composable(Screens.SearchScreen.route) {
        SearchScreen(
            viewModel = koinViewModel(),
            searchViewModel = koinViewModel(),
            navController = navController
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NOTIFICATIONS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Registra la destinazione della schermata notifiche.
 *
 * Il [NotificationViewModel] è passato dall'esterno perché è condiviso con
 * [AppNavigation] per il badge della bottom bar.
 */
private fun NavGraphBuilder.notificationsNav(
    navController: NavHostController,
    notificationViewModel: NotificationViewModel
) {
    composable(Screens.NotificationsScreen.route) {
        NotificationsScreen(navController, notificationViewModel)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Registra le destinazioni relative al profilo utente:
 * profilo personale, modifica profilo/password/bio, foto profilo e profilo pubblico.
 */
private fun NavGraphBuilder.profileNav(navController: NavHostController) {
    composable(Screens.ProfileScreen.route) {
        ProfileScreen(navController)
    }
    composable(Screens.EditProfileScreen.route) {
        EditProfileScreen(navController)
    }
    composable(Screens.EditPasswordScreen.route) {
        EditPasswordScreen(navController, koinViewModel())
    }
    composable(
        route = Screens.PublicProfileScreen.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) { entry ->
        val userId = requireNotNull(entry.arguments?.getString("userId"))
        PublicProfileScreen(userId, navController)
    }
    composable(
        route = Screens.PhotoProfileScreen.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType },
            navArgument("isRegistration") { type = NavType.BoolType }
        )
    ) { entry ->
        val userId = requireNotNull(entry.arguments?.getString("userId"))
        val isRegistration = requireNotNull(entry.arguments?.getBoolean("isRegistration"))
        PhotoProfileScreen(
            navController = navController,
            userId = userId,
            profileViewModel = koinViewModel(),
            isRegistration = isRegistration
        )
    }
    composable(Screens.EditBioScreen.route) {
        EditBioScreen(navController, koinViewModel())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTIONS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Registra le destinazioni relative alle collezioni:
 * lista, aggiunta, dettaglio, aggiunta immagine/oggetto, modifica collezione e oggetto.
 */
private fun NavGraphBuilder.collectionNav(navController: NavHostController) {
    composable(Screens.MyCollectionsScreen.route) {
        MyCollectionsScreen(navController, koinViewModel())
    }
    composable(Screens.AddCollectionScreen.route) {
        AddCollectionScreen(navController)
    }
    composable(
        route = Screens.CollectionDetailScreen.route,
        arguments = listOf(
            navArgument("collectionId") { type = NavType.StringType },
            navArgument("itemId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { entry ->
        val collectionId = requireNotNull(entry.arguments?.getString("collectionId"))
        val itemId = entry.arguments?.getString("itemId")
        CollectionDetailScreen(navController, collectionId, itemId = itemId, koinViewModel())
    }
    composable(
        route = Screens.AddCollectionImageScreen.route,
        arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
    ) { entry ->
        val collectionId = requireNotNull(entry.arguments?.getString("collectionId"))
        AddCollectionImageScreen(collectionId, navController, koinViewModel())
    }
    composable(
        route = Screens.AddCollectionObjectScreen.route,
        arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
    ) { entry ->
        val collectionId = requireNotNull(entry.arguments?.getString("collectionId"))
        AddCollectionObjectScreen(navController, collectionId, koinViewModel())
    }
    composable(
        route = Screens.EditCollectionItemScreen.route,
        arguments = listOf(
            navArgument("collectionId") { type = NavType.StringType },
            navArgument("itemId") { type = NavType.StringType }
        )
    ) { entry ->
        val collectionId = requireNotNull(entry.arguments?.getString("collectionId"))
        val itemId = requireNotNull(entry.arguments?.getString("itemId"))
        EditCollectionItemScreen(navController, collectionId, itemId, koinViewModel())
    }
    composable(
        route = Screens.EditCollectionScreen.route,
        arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
    ) { entry ->
        val collectionId = requireNotNull(entry.arguments?.getString("collectionId"))
        EditCollectionScreen(navController, collectionId, koinViewModel())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHAT
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Registra le destinazioni della funzionalità chat:
 * lista di tutte le conversazioni e singola schermata di chat.
 */
private fun NavGraphBuilder.chatNav(navController: NavHostController) {
    composable(Screens.ChatsListScreen.route) {
        ChatsListScreen(navController)
    }
    composable(
        route = Screens.ChatScreen.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) { entry ->
        val userId = requireNotNull(entry.arguments?.getString("userId"))
        ChatScreen(
            otherUserId = userId,
            navController = navController
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SETTINGS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Registra le destinazioni delle impostazioni:
 * schermata principale impostazioni e scelta tema.
 *
 * Il [NotificationViewModel] è passato dall'esterno perché condiviso con [AppNavigation].
 */
private fun NavGraphBuilder.settingsNav(
    navController: NavHostController,
    themeState: ThemeState,
    onThemeSelected: (ThemeMode) -> Unit,
    notificationViewModel: NotificationViewModel
) {
    composable(Screens.SettingsScreen.route) {
        SettingsScreen(navController, koinViewModel(), notificationViewModel)
    }
    composable(Screens.ChooseThemeScreen.route) {
        ChooseTheme(themeState, onThemeSelected, navController)
    }
}
