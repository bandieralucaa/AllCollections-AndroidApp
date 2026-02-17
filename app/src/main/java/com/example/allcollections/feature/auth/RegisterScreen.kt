package com.example.allcollections.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.core.utils.input.DatePickerField
import com.example.allcollections.core.utils.input.GenderSelector
import com.example.allcollections.feature.profile.ProfileViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, profileViewModel: ProfileViewModel) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // ─────────── Campi utente ───────────
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf(LocalDate.now()) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    // Visibilità password
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Stato validazione
    var passwordsMatch by remember { mutableStateOf(true) }
    var showPasswordError by remember { mutableStateOf(false) }

    // Stato per evitare click multipli
    var isRegistering by remember { mutableStateOf(false) }

    // Funzione per aggiornare lo stato delle password
    fun updatePasswordsMatch() {
        passwordsMatch = password == confirmPassword
        if (confirmPassword.isNotEmpty()) {
            showPasswordError = !passwordsMatch
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { MyTopBar(navController = navController) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val textFieldWidth = Modifier.fillMaxWidth(0.85f)

                // ─────────── Nome e Cognome ───────────
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = textFieldWidth,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    label = { Text("Cognome") },
                    modifier = textFieldWidth,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                // ─────────── Genere ───────────
                GenderSelector(selectedGender = gender, modifier = textFieldWidth) {
                    gender = it
                }

                // ─────────── Data di nascita ───────────
                DatePickerField(selectedDate = dateOfBirth, modifier = textFieldWidth) {
                    dateOfBirth = it
                }

                // ─────────── Email ───────────
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = textFieldWidth,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                // ─────────── Username ───────────
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = textFieldWidth,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                // ─────────── Password ───────────
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        updatePasswordsMatch()
                    },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val desc = if (passwordVisible) "Nascondi password" else "Mostra password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = desc)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = textFieldWidth,
                    isError = showPasswordError
                )

                // ─────────── Conferma Password ───────────
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        updatePasswordsMatch()
                    },
                    label = { Text("Conferma Password") },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val desc = if (confirmPasswordVisible) "Nascondi password" else "Mostra password"
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = icon, contentDescription = desc)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = textFieldWidth,
                    isError = showPasswordError
                )

                // Messaggio di errore se le password non coincidono
                if (showPasswordError) {
                    Text(
                        text = "Le password non coincidono",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─────────── Bottone Registrati ───────────
                Button(
                    onClick = {
                        if (isRegistering) return@Button

                        coroutineScope.launch {
                            isRegistering = true

                            // Controllo campi vuoti
                            if (name.isBlank() || surname.isBlank() || email.isBlank() ||
                                username.isBlank() || password.isBlank() || confirmPassword.isBlank() ||
                                gender.isBlank()
                            ) {
                                snackbarHostState.showSnackbar("Compila tutti i campi")
                                isRegistering = false
                                return@launch
                            }

                            // Controllo che le password coincidano
                            if (!passwordsMatch) {
                                snackbarHostState.showSnackbar("Le password non coincidono")
                                isRegistering = false
                                return@launch
                            }

                            // Controllo unicità username
                            val usernameExists = profileViewModel.isUsernameTaken(username)
                            if (usernameExists) {
                                snackbarHostState.showSnackbar("Username già in uso")
                                isRegistering = false
                                return@launch
                            }

                            // Registrazione utente su Firebase Auth
                            profileViewModel.registerUser(
                                email = email,
                                password = password,
                                onSuccess = { userId ->
                                    // Salva subito i dati base con foto default
                                    profileViewModel.saveUserData(
                                        userId = userId,
                                        name = name,
                                        surname = surname,
                                        username = username,
                                        email = email,
                                        gender = gender,
                                        dateOfBirth = dateOfBirth,
                                        onSuccess = {
                                            // Invia email di verifica
                                            profileViewModel.sendEmailVerification { success, error ->
                                                if (success) {
                                                    // Vai alla schermata di verifica email
                                                    navController.navigate(Screens.VerifyEmailScreen.route) {
                                                        popUpTo(Screens.RegisterScreen.route) { inclusive = true }
                                                    }
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Errore invio verifica: $error")
                                                    }
                                                    // Se l'email non viene inviata, comunque vai alla verifica
                                                    navController.navigate(Screens.VerifyEmailScreen.route) {
                                                        popUpTo(Screens.RegisterScreen.route) { inclusive = true }
                                                    }
                                                }
                                                isRegistering = false
                                            }
                                        },
                                        onFailure = { msg ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                            isRegistering = false
                                        }
                                    )
                                },
                                onFailure = { msg ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                    isRegistering = false
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    enabled = !isRegistering && password.isNotBlank() &&
                            confirmPassword.isNotBlank() &&
                            passwordsMatch
                ) {
                    if (isRegistering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Registrati")
                    }
                }
            }
        }
    }
}