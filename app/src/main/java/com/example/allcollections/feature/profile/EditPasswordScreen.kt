package com.example.allcollections.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.ui.MyTopBar
import kotlinx.coroutines.delay

/**
 * Schermata per la modifica della password dell'utente corrente.
 *
 * Richiede:
 * - Password attuale (per reautenticazione su Firebase)
 * - Nuova password (minimo 6 caratteri)
 * - Conferma nuova password (deve coincidere)
 *
 * Dopo il successo, mostra un messaggio temporaneo e torna indietro automaticamente.
 *
 * @param navController Controller per la navigazione.
 * @param profileViewModel ViewModel del profilo (per il cambio password).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPasswordScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Toggle visibilità per ogni campo
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    // Reset dei messaggi quando l'utente modifica i campi
    LaunchedEffect(currentPassword, newPassword, confirmPassword) {
        errorMessage = null
        successMessage = null
    }

    Scaffold(
        topBar = { MyTopBar(navController, title = "Modifica password") }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Password attuale
                    PasswordField(
                        label = "Password attuale",
                        value = currentPassword,
                        visible = currentVisible,
                        onValueChange = { currentPassword = it },
                        onToggleVisibility = { currentVisible = !currentVisible }
                    )

                    // Nuova password
                    PasswordField(
                        label = "Nuova password",
                        value = newPassword,
                        visible = newVisible,
                        onValueChange = { newPassword = it },
                        onToggleVisibility = { newVisible = !newVisible }
                    )

                    // Conferma nuova password
                    PasswordField(
                        label = "Conferma nuova password",
                        value = confirmPassword,
                        visible = confirmVisible,
                        onValueChange = { confirmPassword = it },
                        onToggleVisibility = { confirmVisible = !confirmVisible }
                    )

                    // Pulsante di invio
                    Button(
                        onClick = {
                            errorMessage = validatePasswords(
                                currentPassword,
                                newPassword,
                                confirmPassword
                            )

                            if (errorMessage == null) {
                                isSubmitting = true
                                profileViewModel.changePassword(
                                    currentPassword,
                                    newPassword,
                                ) { success, message ->
                                    isSubmitting = false
                                    if (success) {
                                        successMessage = "Password aggiornata con successo"
                                        errorMessage = null
                                    } else {
                                        errorMessage = message ?: "Errore sconosciuto"
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Salva nuova password")
                        }
                    }

                    // Messaggio di errore (animato)
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Messaggio di successo (animato)
                    AnimatedVisibility(
                        visible = successMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = successMessage.orEmpty(),
                            color = Color(0xFF2E7D32) // verde successo
                        )
                    }
                }
            }
        }
    }

    // Naviga indietro automaticamente dopo il successo (con un breve delay)
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            delay(900)
            navController.popBackStack()
        }
    }
}

/**
 * Validazione centralizzata dei campi password.
 *
 * Controlli eseguiti:
 * - Nessun campo vuoto
 * - Nuova password ≥ 6 caratteri
 * - Nuova password e conferma coincidono
 *
 * @return Messaggio di errore, o `null` se tutto è valido.
 */
private fun validatePasswords(
    current: String,
    new: String,
    confirm: String
): String? = when {
    current.isBlank() || new.isBlank() || confirm.isBlank() ->
        "Compila tutti i campi"
    new.length < 6 ->
        "La password deve avere almeno 6 caratteri"
    new != confirm ->
        "Le password non coincidono"
    else -> null
}

/**
 * Campo password riutilizzabile con toggle di visibilità.
 *
 * @param label Etichetta del campo.
 * @param value Valore corrente.
 * @param visible Se `true`, mostra la password in chiaro.
 * @param onValueChange Callback per aggiornare il valore.
 * @param onToggleVisibility Callback per invertire la visibilità.
 */
@Composable
private fun PasswordField(
    label: String,
    value: String,
    visible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (visible) "Nascondi password" else "Mostra password"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}