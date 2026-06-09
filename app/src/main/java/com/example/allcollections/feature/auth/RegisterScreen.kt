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

/**
 * Schermata di registrazione.
 *
 * Raccoglie i dati dell'utente (nome, cognome, genere, data di nascita,
 * email, username e password) ed esegue le seguenti validazioni:
 * - Campi obbligatori non vuoti
 * - Età minima 18 anni
 * - Corrispondenza tra password e conferma password
 * - Unicità dello username su Firestore (query asincrona)
 *
 * Al successo crea l'account su Firebase Authentication, salva i dati
 * su Firestore e invia l'email di verifica, quindi naviga a [VerifyEmailScreen].
 *
 * @param navController Controller per la navigazione tra schermate.
 * @param profileViewModel ViewModel usato per registrazione, salvataggio dati e verifica username.
 */
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
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var passwordsMatch by remember { mutableStateOf(true) }
    var showPasswordError by remember { mutableStateOf(false) }
    var showDateError by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }

    /**
     * Aggiorna [passwordsMatch] al cambio di uno dei due campi password
     * e mostra l'errore solo se l'utente ha già scritto nella conferma.
     */
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
                DatePickerField(
                    selectedDate = dateOfBirth,
                    modifier = textFieldWidth,
                    isError = showDateError
                ) {
                    dateOfBirth = it
                    showDateError = false
                }
                if (showDateError) {
                    Text(
                        text = "Devi avere almeno 18 anni per registrarti",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
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

                            // Step 1: Controllo campi obbligatori
                            if (name.isBlank() || surname.isBlank() || email.isBlank() ||
                                username.isBlank() || password.isBlank() || confirmPassword.isBlank() ||
                                gender.isBlank()
                            ) {
                                snackbarHostState.showSnackbar("Compila tutti i campi")
                                isRegistering = false
                                return@launch
                            }

                            // Step 2: Controllo età minima (18 anni)
                            val today = LocalDate.now()
                            if (dateOfBirth.plusYears(18).isAfter(today)) {
                                snackbarHostState.showSnackbar("Devi avere almeno 18 anni per registrarti")
                                showDateError = true
                                isRegistering = false
                                return@launch
                            }
                            showDateError = false

                            // Step 3: Controllo corrispondenza password
                            if (!passwordsMatch) {
                                snackbarHostState.showSnackbar("Le password non coincidono")
                                isRegistering = false
                                return@launch
                            }

                            // Step 4: Controllo unicità username su Firestore
                            val usernameExists = profileViewModel.isUsernameTaken(username)
                            if (usernameExists) {
                                snackbarHostState.showSnackbar("Username già in uso")
                                isRegistering = false
                                return@launch
                            }

                            // Step 5: Creazione account Firebase Auth
                            profileViewModel.registerUser(
                                email = email,
                                password = password,
                                onSuccess = { userId ->
                                    // Step 6: Salvataggio dati profilo su Firestore
                                    profileViewModel.saveUserData(
                                        userId = userId,
                                        name = name,
                                        surname = surname,
                                        username = username,
                                        email = email,
                                        gender = gender,
                                        dateOfBirth = dateOfBirth,
                                        onSuccess = {
                                            // Step 7: Invio email di verifica
                                            profileViewModel.sendEmailVerification { success, error ->
                                                if (!success) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Errore invio verifica: $error")
                                                    }
                                                }
                                                // Naviga alla schermata verifica anche se l'invio fallisce
                                                navController.navigate(Screens.VerifyEmailScreen.route) {
                                                    popUpTo(Screens.RegisterScreen.route) { inclusive = true }
                                                }
                                                isRegistering = false
                                            }
                                        },
                                        onFailure = { msg ->
                                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                            isRegistering = false
                                        }
                                    )
                                },
                                onFailure = { msg ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
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