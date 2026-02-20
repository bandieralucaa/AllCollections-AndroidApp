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
            val hideBottomBarRoutes = listOf(
                Screens.LoginScreen.route,
                Screens.RegisterScreen.route,
                Screens.VerifyEmailScreen.route,
                Screens.ForgotPasswordScreen.route
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

/* ---------------- BOTTOM BAR ---------------- */
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
                                Icon(item.icon, null, Modifier.size(28.dp))
                            }
                        }
                        item.screen == Screens.ChatsListScreen && unreadMessagesCount > 0 -> {
                            BadgedBox(badge = {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text(unreadMessagesCount.toString())
                                }
                            }) {
                                Icon(item.icon, null, Modifier.size(28.dp))
                            }
                        }
                        else -> Icon(item.icon, null, Modifier.size(28.dp))
                    }
                }
            )
        }
    }
}

/* ---------------- AUTH NAVIGATION ---------------- */
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

/* ---------------- HOME NAVIGATION ---------------- */
private fun NavGraphBuilder.homeNav(navController: NavHostController) {
    composable(Screens.HomeScreen.route) {
        HomeScreen(navController, koinViewModel(), koinViewModel())
    }
}

/* ---------------- SEARCH NAVIGATION ---------------- */
private fun NavGraphBuilder.searchNav(navController: NavHostController) {
    composable(Screens.SearchScreen.route) {
        SearchScreen(
            viewModel = koinViewModel(),
            searchViewModel = koinViewModel(),
            navController = navController
        )
    }
}

/* ---------------- NOTIFICATIONS NAVIGATION ---------------- */
private fun NavGraphBuilder.notificationsNav(
    navController: NavHostController,
    notificationViewModel: NotificationViewModel
) {
    composable(Screens.NotificationsScreen.route) {
        NotificationsScreen(navController, notificationViewModel)
    }
}

/* ---------------- PROFILE NAVIGATION ---------------- */
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

    // Public profile
    composable(
        route = Screens.PublicProfileScreen.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) { entry ->
        val userId = requireNotNull(entry.arguments?.getString("userId"))
        PublicProfileScreen(userId, navController)
    }

    // PhotoProfileScreen (registrazione o edit)
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

/* ---------------- COLLECTION NAVIGATION ---------------- */
private fun NavGraphBuilder.collectionNav(navController: NavHostController) {
    composable(Screens.MyCollectionsScreen.route) {
        MyCollectionsScreen(navController, koinViewModel())
    }

    composable(Screens.AddCollectionScreen.route) {
        AddCollectionScreen(navController)
    }

    composable(
        route = Screens.CollectionDetailScreen.route,
        arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
    ) { entry ->
        val collectionId = requireNotNull(entry.arguments?.getString("collectionId"))
        CollectionDetailScreen(navController, collectionId, koinViewModel())
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

/* ---------------- CHAT NAVIGATION ---------------- */
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

/* ---------------- SETTINGS NAVIGATION ---------------- */
private fun NavGraphBuilder.settingsNav(
    navController: NavHostController,
    themeState: ThemeState,
    onThemeSelected: (ThemeMode) -> Unit,
    notificationViewModel: NotificationViewModel  // aggiunto
) {
    composable(Screens.SettingsScreen.route) {
        SettingsScreen(navController, koinViewModel(), notificationViewModel)
    }
    composable(Screens.ChooseThemeScreen.route) {
        ChooseTheme(themeState, onThemeSelected, navController)
    }
}