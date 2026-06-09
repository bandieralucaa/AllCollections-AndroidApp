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
 * Ogni elemento associa un'icona Material alla schermata di destinazione corrispondente.
 * Usata in [AppNavigation] per costruire dinamicamente la [BottomNavBar].
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
