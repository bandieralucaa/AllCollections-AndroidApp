package com.example.allcollections.data.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.allcollections.core.navigation.Screens

/**
 * Modello dati per un elemento della Bottom Navigation Bar.
 *
 * Associa un'icona alla schermata corrispondente, usato per costruire
 * dinamicamente la barra di navigazione inferiore.
 *
 * @property icon Icona vettoriale mostrata nella barra di navigazione.
 * @property screen Schermata di destinazione associata all'elemento.
 */
data class NavItem(
    val icon: ImageVector,
    val screen: Screens
)
