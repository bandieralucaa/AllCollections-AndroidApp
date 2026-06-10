package com.example.allcollections.feature.auth

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.allcollections.R
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.ErrorText
import com.example.allcollections.feature.profile.ProfileViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * Schermata di login dell'applicazione.
 *
 * Permette all'utente di accedere con email e password tramite Firebase Authentication.
 *
 * ### Comportamento
 * - Se l'utente è già autenticato (Firebase auth ha un utente corrente), viene reindirizzato
 *   automaticamente alla [Screens.HomeScreen] con reset completo della back stack.
 * - Il pulsante "Accedi" è abilitato **solo** quando entrambi i campi (email e password)
 *   sono non vuoti, per ridurre chiamate Firebase inutili.
 * - La password può essere mostrata/nascosta tramite l'icona di toggle.
 * - In caso di errore di autenticazione, viene mostrato un messaggio sotto il pulsante.
 *
 * ### Navigazione
 * - "Password dimenticata?" → [Screens.ForgotPasswordScreen]
 * - "Registrati" → [Screens.RegisterScreen]
 * - Login riuscito → [Screens.HomeScreen] con `popUpTo` che rimuove LoginScreen dalla back stack.
 *
 * @param navController Controller per la navigazione tra schermate.
 * @param viewModel ViewModel del profilo, che espone il metodo [ProfileViewModel.login].
 *
 * @see ProfileViewModel
 * @see Screens
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: ProfileViewModel
) {
    // Stato UI locale
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentUser = Firebase.auth.currentUser

    // Se l'utente è già loggato, naviga direttamente alla Home e rimuove il login dalla back stack
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate(Screens.HomeScreen.route) {
                popUpTo(Screens.LoginScreen.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo dell'app
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo AllCollections",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AllCollections",
                fontSize = 32.sp,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Campo email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("campo_email"),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo password con toggle visibilità
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("campo_password"),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility
                    else Icons.Default.VisibilityOff
                    val desc = if (passwordVisible) "Nascondi password" else "Mostra password"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = icon, contentDescription = desc)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Link "Password dimenticata?"
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { navController.navigate(Screens.ForgotPasswordScreen.route) }
                ) {
                    Text(
                        text = "Password dimenticata?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pulsante di login (abilitato solo se campi non vuoti)
            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    Log.d("LoginScreen", "Tentativo login con email=$trimmedEmail")
                    viewModel.login(trimmedEmail, password) { success, error ->
                        if (success) {
                            navController.navigate(Screens.HomeScreen.route) {
                                popUpTo(Screens.LoginScreen.route) { inclusive = true }
                            }
                        } else {
                            errorMessage = error ?: "Errore durante il login"
                        }
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottone_accedi")
            ) {
                Text("Accedi")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Link per la registrazione
            TextButton(onClick = { navController.navigate(Screens.RegisterScreen.route) }) {
                Text("Non hai un account? Registrati")
            }

            // Messaggio di errore (se presente)
            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                ErrorText(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}