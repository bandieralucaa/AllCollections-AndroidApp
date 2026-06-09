package com.example.allcollections.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.feature.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Schermata di verifica email post-registrazione.
 *
 * Mostra l'indirizzo email a cui è stata inviata la verifica e offre
 * due azioni all'utente:
 * - **Verifica manuale**: tap su "Ho verificato la mia email" → ricarica il profilo
 *   e naviga a [Screens.PhotoProfileScreen] se l'email risulta verificata.
 * - **Reinvio email**: disponibile dopo un cooldown di 60 secondi per evitare spam.
 *
 * Un polling automatico ogni 3 secondi controlla in background se l'utente
 * ha già cliccato il link, navigando automaticamente senza richiedere
 * un'azione esplicita.
 *
 * @param navController Controller per la navigazione.
 * @param profileViewModel ViewModel usato per ricaricare il profilo e verificare lo stato email.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyEmailScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isResending by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: ""

    // Countdown per il bottone "Reinvia email" (cooldown 60 secondi)
    LaunchedEffect(secondsRemaining) {
        if (secondsRemaining > 0 && !canResend) {
            delay(1000)
            secondsRemaining--
        } else {
            canResend = true
        }
    }

    // Polling automatico ogni 3 secondi: naviga alla schermata foto se l'email è verificata
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            profileViewModel.reloadUser { success ->
                if (success && profileViewModel.isEmailVerified()) {
                    currentUser?.uid?.let { userId ->
                        navController.navigate(
                            Screens.PhotoProfileScreen.createRoute(userId, "true")
                        ) {
                            popUpTo(Screens.VerifyEmailScreen.route) { inclusive = true }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MyTopBar(
                navController = navController,
                title = "Verifica email"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Verifica la tua email",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            // Card informativa con l'indirizzo email
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Abbiamo inviato un email di verifica a:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Clicca sul link nell'email per attivare il tuo account.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pulsante verifica manuale
            Button(
                onClick = {
                    isChecking = true
                    profileViewModel.reloadUser { success ->
                        isChecking = false
                        if (success && profileViewModel.isEmailVerified()) {
                            currentUser?.uid?.let { userId ->
                                navController.navigate(
                                    Screens.PhotoProfileScreen.createRoute(userId, "true")
                                ) {
                                    popUpTo(Screens.VerifyEmailScreen.route) { inclusive = true }
                                }
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Email non ancora verificata")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isChecking
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Ho verificato la mia email")
                }
            }

            // Pulsante reinvio email con countdown
            OutlinedButton(
                onClick = {
                    isResending = true
                    profileViewModel.sendEmailVerification { success, error ->
                        isResending = false
                        if (success) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Email di verifica reinviata!")
                            }
                            // Reset del cooldown
                            secondsRemaining = 60
                            canResend = false
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Errore: ${error ?: "riprova più tardi"}")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canResend && !isResending
            ) {
                if (isResending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(if (canResend) "Reinvia email" else "Reinvia tra $secondsRemaining s")
                }
            }

            // Link per usare un'altra email: effettua il logout e torna al login
            TextButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.popBackStack(
                        Screens.LoginScreen.route,
                        inclusive = false
                    )
                }
            ) {
                Text("Usa un'altra email")
            }

            Text(
                text = "Controlla anche la cartella spam",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}