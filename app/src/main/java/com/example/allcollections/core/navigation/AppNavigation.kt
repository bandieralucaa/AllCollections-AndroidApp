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
import com.example.allcollections.feature.notification.presentation.NotificationsScreen
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.example.allcollections.feature.profile.*
import com.example.allcollections.feature.publicProfile.PublicProfileScreen
import com.example.allcollections.feature.search.SearchScreen
import com.example.allcollections.feature.settings.*
import org.koin.androidx.compose.koinViewModel

/**
 * Grafo di navigazione principale dell'applicazione.
 *
 * Questo componente gestisce tutta la navigazione tramite [NavHost], includendo
 * la [BottomNavBar] (che viene nascosta automaticamente in determinate schermate).
 *
 * Le destinazioni sono organizzate in blocchi logici separati (auth, home, search,
 * notifications, profile, collection, chat, settings), ciascuno definito in una
 * funzione di estensione su [NavGraphBuilder] per mantenere il codice modulare.
 *
 * ### Rotte e argomenti
 * - Le rotte sono definite in [Screens] (oggetto companion).
 * - Le rotte con parametri usano `navArgument` con tipi esplicitamente dichiarati.
 * - I ViewModel condivisi ([notificationViewModel], [chatViewModel]) vengono passati
 *   dall'esterno per mantenere lo stesso stato tra bottom bar e schermate.
 *
 * @param navController Controller di navigazione (creato in [MainActivity]).
 * @param startDestination Rotta iniziale (dipende dallo stato di autenticazione).
 * @param themeState Stato corrente del tema (per la schermata impostazioni).
 * @param onThemeSelected Callback per cambiare tema (dal menu impostazioni).
 * @param notificationViewModel ViewModel delle notifiche (condiviso per badge e listener).
 * @param chatViewModel ViewModel delle chat (condiviso per badge e recent chats).
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

    // Osserva i badge per la bottom bar
    val hasUnreadNotifications by notificationViewModel
        .hasUnreadNotifications
        .collectAsState(initial = false)

    val unreadMessagesCount by chatViewModel.unreadMessagesCount.collectAsState(initial = 0)

    // Avvia l'osservazione delle chat recenti per aggiornare il badge
    LaunchedEffect(Unit) {
        chatViewModel.observeRecentChats()
    }

    Scaffold(
        bottomBar = {
            // Nasconde la bottom bar nelle schermate di autenticazione e nella selezione foto profilo
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
 * Bottom Navigation Bar con cinque voci predefinite (Home, Search, Notifications, Profile, Chats).
 *
 * Mostra un badge rosso sulle notifiche se [hasUnreadNotifications] è `true`.
 * Mostra un badge numerico sulle chat se [unreadMessagesCount] > 0.
 * La voce selezionata viene evidenziata confrontando la destinazione corrente con la gerarchia.
 *
 * @param currentDestination Destinazione attuale nel back stack di navigazione.
 * @param navController Controller per navigare alla rotta selezionata.
 * @param hasUnreadNotifications Se `true`, mostra un badge rosso sull'icona delle notifiche.
 * @param unreadMessagesCount Numero di messaggi non letti (mostrato nel badge delle chat).
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
            // Verifica se la destinazione corrente corrisponde a questa voce
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (selected) {
                        when (item.screen) {
                            Screens.ChatsListScreen -> navController.popBackStack(Screens.ChatsListScreen.route, inclusive = false)
                            Screens.NotificationsScreen -> navController.popBackStack(Screens.NotificationsScreen.route, inclusive = false)
                            Screens.ProfileScreen -> navController.popBackStack(Screens.ProfileScreen.route, inclusive = false)
                            Screens.HomeScreen -> navController.popBackStack(Screens.HomeScreen.route, inclusive = false)
                            Screens.SearchScreen -> navController.popBackStack(Screens.SearchScreen.route, inclusive = false)
                            else -> {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    } else {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    when {
                        // Badge per notifiche non lette
                        item.screen == Screens.NotificationsScreen && hasUnreadNotifications -> {
                            BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) }) {
                                Icon(item.icon, contentDescription = null, modifier = Modifier.size(28.dp))
                            }
                        }
                        // Badge numerico per messaggi non letti
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
 * Registra le destinazioni del flusso di autenticazione.
 *
 * - [Screens.LoginScreen] – login con email/password.
 * - [Screens.RegisterScreen] – registrazione nuovo utente.
 * - [Screens.VerifyEmailScreen] – verifica email dopo registrazione.
 * - [Screens.ForgotPasswordScreen] – recupero password tramite email.
 *
 * @param navController Controller per la navigazione tra schermate.
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

/**
 * Registra la destinazione della schermata home (feed principale).
 *
 * @param navController Controller per la navigazione.
 */
private fun NavGraphBuilder.homeNav(navController: NavHostController) {
    composable(Screens.HomeScreen.route) {
        HomeScreen(navController, koinViewModel(), koinViewModel())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEARCH
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Registra la destinazione della schermata di ricerca (utenti e collezioni).
 *
 * @param navController Controller per la navigazione.
 */
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
 * Registra la destinazione della schermata delle notifiche.
 *
 * Il [NotificationViewModel] viene passato dall'esterno perché condiviso
 * con [AppNavigation] per il badge della bottom bar.
 *
 * @param navController Controller per la navigazione.
 * @param notificationViewModel ViewModel condiviso delle notifiche.
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
 * Registra le destinazioni relative al profilo utente.
 *
 * - [Screens.ProfileScreen] – profilo personale (dettagli e statistiche).
 * - [Screens.EditProfileScreen] – modifica dati anagrafici.
 * - [Screens.EditPasswordScreen] – cambio password.
 * - [Screens.PublicProfileScreen] – profilo pubblico di un altro utente (con parametro `userId`).
 * - [Screens.PhotoProfileScreen] – upload/modifica foto profilo (con parametri `userId` e `isRegistration`).
 * - [Screens.EditBioScreen] – modifica biografia.
 *
 * @param navController Controller per la navigazione.
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
 * Registra tutte le destinazioni relative alla gestione delle collezioni.
 *
 * Rotte e argomenti:
 * - [Screens.MyCollectionsScreen] – lista delle collezioni dell'utente.
 * - [Screens.AddCollectionScreen] – creazione nuova collezione.
 * - [Screens.CollectionDetailScreen] – dettaglio collezione (con `collectionId` e `itemId` opzionale).
 * - [Screens.AddCollectionImageScreen] – upload immagine copertina (con `collectionId`).
 * - [Screens.AddCollectionObjectScreen] – aggiunta oggetto (con `collectionId`).
 * - [Screens.EditCollectionItemScreen] – modifica oggetto (con `collectionId` e `itemId`).
 * - [Screens.EditCollectionScreen] – modifica dati collezione (con `collectionId`).
 *
 * @param navController Controller per la navigazione.
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
 * Registra le destinazioni per la messaggistica istantanea.
 *
 * - [Screens.ChatsListScreen] – elenco delle conversazioni recenti.
 * - [Screens.ChatScreen] – schermata di chat con un singolo utente (parametro `userId`).
 *
 * @param navController Controller per la navigazione.
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
 * Registra le destinazioni delle impostazioni dell'app.
 *
 * - [Screens.SettingsScreen] – impostazioni principali (logout, navigazione a scelta tema, ecc.).
 * - [Screens.ChooseThemeScreen] – scelta del tema (chiaro, scuro, sistema).
 *
 * @param navController Controller per la navigazione.
 * @param themeState Stato corrente del tema (per pre-selezionare l'opzione attiva).
 * @param onThemeSelected Callback per cambiare il tema (passato al ViewModel).
 * @param notificationViewModel ViewModel condiviso per eliminare notifiche o altre operazioni.
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