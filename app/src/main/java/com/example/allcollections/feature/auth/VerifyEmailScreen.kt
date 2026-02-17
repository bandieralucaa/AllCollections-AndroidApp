package com.example.allcollections.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.feature.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyEmailScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isResending by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: ""

    // Timer per il pulsante "Reinvia"
    LaunchedEffect(secondsRemaining) {
        if (secondsRemaining > 0 && !canResend) {
            delay(1000)
            secondsRemaining--
        } else {
            canResend = true
        }
    }

    // Controllo automatico ogni 3 secondi se l'email è stata verificata
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            profileViewModel.reloadUser { success ->
                if (success && profileViewModel.isEmailVerified()) {
                    // Email verificata! Vai alla foto profilo
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
            // Icona grande
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Titolo
            Text(
                text = "Verifica la tua email",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            // Istruzioni
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

            // Pulsante "Ho verificato"
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

            // Pulsante "Reinvia email"
            OutlinedButton(
                onClick = {
                    isResending = true
                    profileViewModel.sendEmailVerification { success, error ->
                        isResending = false
                        if (success) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Email di verifica reinviata!")
                            }
                            // Reset timer
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

            // Pulsante per tornare al login (se l'utente ha sbagliato email)
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

            // Nota informativa
            Text(
                text = "Controlla anche la cartella spam",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}