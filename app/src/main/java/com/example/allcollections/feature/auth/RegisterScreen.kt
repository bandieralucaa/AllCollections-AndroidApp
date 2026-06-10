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
import com.example.allcollections.core.ui.ErrorText
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.core.utils.input.DatePickerField
import com.example.allcollections.core.utils.input.GenderSelector
import com.example.allcollections.feature.profile.ProfileViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Schermata di registrazione nuovo utente.
 *
 * Raccoglie tutti i dati necessari per la creazione di un account:
 * - Nome, cognome, genere, data di nascita
 * - Email, username, password (con conferma)
 *
### Validazioni eseguite (in ordine)
 * 1. **Campi obbligatori** – nessuno deve essere vuoto.
 * 2. **Età minima** – l'utente deve avere almeno 18 anni (confronto con data odierna).
 * 3. **Password corrispondente** – `password` e `confirmPassword` devono coincidere.
 * 4. **Unicità username** – verifica asincrona su Firestore tramite [ProfileViewModel.isUsernameTaken].
 *
### Flusso di registrazione
 * - Creazione account su Firebase Authentication tramite [ProfileViewModel.registerUser].
 * - Salvataggio dati profilo su Firestore ([ProfileViewModel.saveUserData]).
 * - Invio email di verifica ([ProfileViewModel.sendEmailVerification]).
 * - Navigazione a [VerifyEmailScreen] con reset della back stack.
 *
 * In caso di errore in qualsiasi fase, viene mostrato un messaggio tramite snackbar.
 *
 * @param navController Controller per la navigazione tra schermate.
 * @param profileViewModel ViewModel per operazioni di registrazione e verifica username.
 *
 * @see ProfileViewModel
 * @see DatePickerField
 * @see GenderSelector
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, profileViewModel: ProfileViewModel) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Stato dei campi del modulo
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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    /**
     * Aggiorna lo stato di corrispondenza delle password e mostra l'errore
     * solo se l'utente ha già iniziato a scrivere nella conferma.
     */
    fun updatePasswordsMatch() {
        passwordsMatch = password == confirmPassword
        if (confirmPassword.isNotEmpty()) {
            showPasswordError = !passwordsMatch
        }
    }

    Scaffold(
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

                // Nome e Cognome
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

                // Genere (dropdown)
                GenderSelector(selectedGender = gender, modifier = textFieldWidth) {
                    gender = it
                }

                // Data di nascita (DatePicker)
                DatePickerField(
                    selectedDate = dateOfBirth,
                    modifier = textFieldWidth,
                    isError = showDateError
                ) {
                    dateOfBirth = it
                    showDateError = false
                }
                if (showDateError) {
                    ErrorText(
                        text = "Devi avere almeno 18 anni per registrarti",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }

                // Email
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

                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = textFieldWidth,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                // Password (con toggle visibilità)
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

                // Conferma password (con toggle visibilità)
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
                    ErrorText(
                        text = "Le password non coincidono",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsante di registrazione (disabilitato durante l'operazione)
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
                                errorMessage = "Compila tutti i campi"
                                isRegistering = false
                                return@launch
                            }

                            // Step 2: Controllo età minima (18 anni)
                            val today = LocalDate.now()
                            if (dateOfBirth.plusYears(18).isAfter(today)) {
                                errorMessage = "Devi avere almeno 18 anni per registrarti"
                                showDateError = true
                                isRegistering = false
                                return@launch
                            }
                            showDateError = false

                            // Step 3: Controllo corrispondenza password
                            if (!passwordsMatch) {
                                errorMessage = "Le password non coincidono"
                                isRegistering = false
                                return@launch
                            }

                            // Step 4: Controllo unicità username su Firestore
                            val usernameExists = profileViewModel.isUsernameTaken(username)
                            if (usernameExists) {
                                errorMessage = "Username già in uso"
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
                                                    errorMessage = error ?: "Errore invio verifica email"
                                                }
                                                // Naviga alla schermata di verifica email
                                                navController.navigate(Screens.VerifyEmailScreen.route) {
                                                    popUpTo(Screens.RegisterScreen.route) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                                isRegistering = false
                                            }
                                        },
                                        onFailure = { msg ->
                                            errorMessage = msg
                                            isRegistering = false
                                        }
                                    )
                                },
                                onFailure = { msg ->
                                    errorMessage = msg
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