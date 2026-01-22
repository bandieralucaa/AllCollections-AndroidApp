package com.example.allcollections.feature.auth

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.allcollections.feature.profile.ProfileViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * Schermata di login dell'app AllCollections.
 *
 * Permette all'utente di inserire email e password per autenticarsi.
 * Se l'utente è già loggato, viene reindirizzato automaticamente alla HomeScreen.
 * Mostra eventuali messaggi di errore se il login fallisce.
 *
 * Componenti principali:
 * - Logo e titolo
 * - Campo email
 * - Campo password con toggle visibilità
 * - Bottone "Accedi"
 * - Link per registrazione
 * - Messaggio di errore
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: ProfileViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentUser = Firebase.auth.currentUser

    // Redirect automatico se l'utente è già loggato
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate(Screens.HomeScreen.route) {
                popUpTo(Screens.LoginScreen.route) { inclusive = true }
            }
        }
    }

    // Layout principale con sfondo gradiente leggero
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE0F7FA), Color(0xFFFFFFFF))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
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

            // Email input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password input con toggle visibilità
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    val desc = if (passwordVisible) "Nascondi password" else "Mostra password"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = icon, contentDescription = desc)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bottone Login
            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    viewModel.login(trimmedEmail, password) { success, error ->
                        Log.d("LoginScreen", "Tentativo login con email=$trimmedEmail")
                        if (success) {
                            navController.navigate(Screens.HomeScreen.route) {
                                popUpTo(Screens.LoginScreen.route) { inclusive = true }
                            }
                        } else {
                            errorMessage = error ?: "Errore durante il login"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Accedi")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Link registrazione
            TextButton(onClick = { navController.navigate(Screens.RegisterScreen.route) }) {
                Text("Non hai un account? Registrati")
            }

            // Messaggio errore
            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
