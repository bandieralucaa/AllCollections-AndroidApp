package com.example.allcollections.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.ui.ErrorText
import com.example.allcollections.core.ui.MyTopBar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Schermata di recupero password.
 *
 * Permette all'utente di inserire la propria email per ricevere il link di reset
 * password tramite Firebase Authentication. Al successo dell'invio, torna
 * automaticamente alla schermata di login tramite [NavController.popBackStack].
 *
 * @param navController Controller per la navigazione (usato per tornare al login).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            MyTopBar(
                navController = navController,
                title = "Recupera password"
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Inserisci la tua email per ricevere il link di recupero password",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(0.85f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !isLoading
                )

                Button(
                    onClick = {
                        if (isLoading) return@Button
                        if (email.isBlank()) {
                            errorMessage = "Inserisci un'email"
                            return@Button
                        }
                        isLoading = true
                        FirebaseAuth.getInstance()
                            .sendPasswordResetEmail(email.trim())
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Email di recupero inviata! Controlla la tua casella di posta"
                                        )
                                    }
                                    navController.popBackStack()
                                } else {
                                    errorMessage = task.exception?.message ?: "Errore durante l'invio"
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Invia email di recupero")
                    }
                }

                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Torna al login")
                }

                errorMessage?.let {
                    ErrorText(
                        text = it,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}