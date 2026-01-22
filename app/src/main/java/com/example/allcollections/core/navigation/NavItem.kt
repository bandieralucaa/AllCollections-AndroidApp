package com.example.allcollections.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Modello dati per un elemento della Bottom Navigation.
 *
 * @property icon Icona mostrata nella barra di navigazione.
 * @property screen Schermata associata all'elemento.
 */
data class NavItem(
    val icon: ImageVector,
    val screen: Screens
)

/**
 * Lista dei principali elementi della Bottom Navigation.
 * L'ordine definito qui sarà quello visualizzato nell'interfaccia.
 *
 * Nota: gli screen devono corrispondere ai Composable aggiornati con 'Screen'.
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
        icon = Icons.Default.Notifications,
        screen = Screens.NotificationsScreen
    ),
    NavItem(
        icon = Icons.Default.Person,
        screen = Screens.ProfileScreen
    )
)
