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
import com.example.allcollections.core.ui.MyTopBar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        sendPasswordResetEmail(
                            email = email,
                            isLoading = isLoading,
                            snackbarHostState = snackbarHostState,
                            coroutineScope = coroutineScope,
                            onLoadingChange = { isLoading = it },
                            onSuccess = { navController.popBackStack() }
                        )
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
            }
        }
    }
}

/**
 * Invia l'email di reset password tramite Firebase Authentication.
 *
 * Valida che il campo email non sia vuoto prima di chiamare Firebase.
 * Mostra messaggi di esito tramite [SnackbarHostState]. In caso di
 * successo invoca [onSuccess] (tipicamente per tornare al login).
 *
 * @param email Indirizzo email inserito dall'utente.
 * @param isLoading Stato corrente del caricamento (usato per evitare doppi tap).
 * @param snackbarHostState Host per la visualizzazione dei messaggi snackbar.
 * @param coroutineScope Scope per il lancio delle coroutine di snackbar.
 * @param onLoadingChange Callback per aggiornare lo stato di caricamento.
 * @param onSuccess Callback invocato quando l'email è stata inviata con successo.
 */
private fun sendPasswordResetEmail(
    email: String,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    onLoadingChange: (Boolean) -> Unit,
    onSuccess: () -> Unit
) {
    if (isLoading) return

    if (email.isBlank()) {
        coroutineScope.launch { snackbarHostState.showSnackbar("Inserisci un'email") }
        return
    }

    onLoadingChange(true)
    FirebaseAuth.getInstance()
        .sendPasswordResetEmail(email.trim())
        .addOnCompleteListener { task ->
            onLoadingChange(false)
            if (task.isSuccessful) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        "Email di recupero inviata! Controlla la tua casella di posta"
                    )
                }
                onSuccess()
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        task.exception?.message ?: "Errore durante l'invio"
                    )
                }
            }
        }
}