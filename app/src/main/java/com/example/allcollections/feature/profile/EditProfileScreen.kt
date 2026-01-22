package com.example.allcollections.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.allcollections.core.ui.MyTopBar
import com.example.allcollections.core.utils.input.DatePickerField
import com.example.allcollections.core.utils.input.GenderSelector
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Screen di modifica del profilo utente.
 *
 * Funzionalità:
 * - Caricamento dati utente
 * - Modifica dati anagrafici
 * - Validazioni base lato UI
 * - Feedback tramite Snackbar
 * - UX pulita e coerente
 */
@Composable
fun EditProfileScreen(
    navController: NavController
) {
    // ----------------------------------------
    // ViewModel & coroutine
    // ----------------------------------------
    val profileViewModel: ProfileViewModel = viewModel()
    val scope = rememberCoroutineScope()

    // ----------------------------------------
    // UI state
    // ----------------------------------------
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(false) }

    // ----------------------------------------
    // Form state
    // ----------------------------------------
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf(LocalDate.now()) }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    // ----------------------------------------
    // Load user data on first composition
    // ----------------------------------------
    LaunchedEffect(Unit) {
        isLoading = true
        profileViewModel.getUserData()?.let { user ->
            name = user.name
            surname = user.surname
            dateOfBirth = user.dateOfBirthAsLocalDate ?: LocalDate.now()
            email = user.email
            gender = user.gender
            username = user.username
        }
        isLoading = false
    }

    // ----------------------------------------
    // Scaffold
    // ----------------------------------------
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MyTopBar(
                navController = navController,
                title = "Modifica profilo"
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                shape = MaterialTheme.shapes.large
            ) {

                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // -----------------------------
                    // Nome
                    // -----------------------------
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // -----------------------------
                    // Cognome
                    // -----------------------------
                    OutlinedTextField(
                        value = surname,
                        onValueChange = { surname = it },
                        label = { Text("Cognome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // -----------------------------
                    // Genere
                    // -----------------------------
                    GenderSelector(
                        selectedGender = gender,
                        modifier = Modifier.fillMaxWidth()
                    ) { gender = it }

                    // -----------------------------
                    // Data di nascita
                    // -----------------------------
                    DatePickerField(
                        selectedDate = dateOfBirth,
                        modifier = Modifier.fillMaxWidth()
                    ) { dateOfBirth = it }

                    // -----------------------------
                    // Email
                    // -----------------------------
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // -----------------------------
                    // Username
                    // -----------------------------
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // -----------------------------
                    // Save button
                    // -----------------------------
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        onClick = {
                            // Validazione base lato UI
                            if (name.isBlank() || surname.isBlank() || email.isBlank() || username.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Compila tutti i campi")
                                }
                                return@Button
                            }

                            isLoading = true

                            profileViewModel.updateUserData(
                                name = name,
                                surname = surname,
                                dateOfBirth = dateOfBirth,
                                email = email,
                                gender = gender,
                                username = username
                            ) { success, error ->

                                isLoading = false

                                scope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar("Profilo aggiornato")
                                        navController.navigateUp()
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            error ?: "Errore aggiornamento profilo"
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Salva modifiche")
                        }
                    }
                }
            }
        }
    }
}
