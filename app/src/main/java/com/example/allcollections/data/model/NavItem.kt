package com.example.allcollections.data.model
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.allcollections.core.navigation.Screens

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