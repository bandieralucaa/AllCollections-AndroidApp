package com.example.allcollections.feature.auth

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.allcollections.core.navigation.Screens
import com.google.firebase.auth.FirebaseAuth

/**
 * Composable protettivo per schermate che richiedono l'autenticazione.
 *
 * Se l'utente non è loggato, viene automaticamente reindirizzato
 * alla schermata di login. Altrimenti mostra il contenuto della schermata.
 *
 * @param navController Controller di navigazione per eventuale redirect.
 * @param content Contenuto della schermata protetta.
 */
@Composable
fun RequireLoginScreen(
    navController: NavController,
    content: @Composable () -> Unit
) {
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(firebaseAuth.currentUser) }

    // Aggiorna lo stato dell'utente in tempo reale
    DisposableEffect(firebaseAuth) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        firebaseAuth.addAuthStateListener(listener)
        onDispose { firebaseAuth.removeAuthStateListener(listener) }
    }

    // Reindirizza alla Login se l'utente non è autenticato
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate(Screens.LoginScreen.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Mostra il contenuto solo se l'utente è loggato
    currentUser?.let { content() }
}
