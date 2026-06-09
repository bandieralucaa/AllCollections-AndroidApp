package com.example.allcollections.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import com.example.allcollections.data.model.NavItem

/**
 * Lista degli elementi della Bottom Navigation Bar, nell'ordine in cui vengono visualizzati.
 *
 * Ogni elemento associa un'icona Material (da [Icons.Default]) alla schermata di destinazione
 * corrispondente, definita nell'enum [Screens].
 *
 * L'ordine delle voci è:
 * 1. [Screens.HomeScreen] – Home
 * 2. [Screens.SearchScreen] – Ricerca
 * 3. [Screens.ChatsListScreen] – Chat
 * 4. [Screens.NotificationsScreen] – Notifiche
 * 5. [Screens.ProfileScreen] – Profilo
 *
 * Le icone selezionate sono:
 * - Home: `Icons.Default.Home`
 * - Ricerca: `Icons.Default.Search`
 * - Chat: `Icons.Default.QuestionAnswer` (fumetto)
 * - Notifiche: `Icons.Default.Notifications`
 * - Profilo: `Icons.Default.Person`
 *
 * @see BottomNavBar in AppNavigation.kt
 * @see NavItem
 * @see Screens
 */
val bottomNavItems: List<NavItem> = listOf(
    NavItem(
        icon = Icons.Default.Home,
        screen = Screens.HomeScreen
    ),
    NavItem(
        icon = Icons.Default.Search,
        screen = Screens.SearchScreen
    ),
    NavItem(
        icon = Icons.Default.QuestionAnswer,
        screen = Screens.ChatsListScreen
    ),
    NavItem(
        icon = Icons.Default.Notifications,
        screen = Screens.NotificationsScreen
    ),
    NavItem(
        icon = Icons.Default.Person,
        screen = Screens.ProfileScreen
    )
)