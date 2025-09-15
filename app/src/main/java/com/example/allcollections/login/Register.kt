package com.example.allcollections.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.navigation.Screens
import com.example.allcollections.profile.UserData
import com.example.allcollections.viewModel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Register(navController: NavController, profileViewModel: ProfileViewModel) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf(LocalDate.now()) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Maschio") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrazione") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    label = { Text("Cognome") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                GenderSelector(selectedGender = gender) { gender = it }

                DatePickerField(dateOfBirth) { dateOfBirth = it }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email
                    )
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Button(onClick = {
                    if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank() || username.isBlank()) {
                        errorMessage = "Compila tutti i campi"
                        return@Button
                    }

                    FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                profileViewModel.pendingUserData = UserData(
                                    name = name,
                                    surname = surname,
                                    dateOfBirth = dateOfBirth,
                                    email = email,
                                    gender = gender,
                                    username = username
                                )
                                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                navController.navigate("${Screens.PhotoProfile.name}/$userId")
                            } else {
                                errorMessage = "Errore nella registrazione: ${task.exception?.message}"
                            }
                        }
                }) {
                    Text("Prosegui")
                }

                errorMessage?.let {
                    Text(text = it, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun DatePickerField(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
        onValueChange = {},
        label = { Text("Data di nascita") },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Seleziona data")
            }
        },
        readOnly = true
    )

    if (showDialog) {
        val picker = android.app.DatePickerDialog(context)
        picker.setOnDateSetListener { _, year, month, day ->
            onDateSelected(LocalDate.of(year, month + 1, day))
            showDialog = false
        }
        picker.show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderSelector(selectedGender: String, onGenderSelected: (String) -> Unit) {
    val options = listOf("Maschio", "Femmina", "Altro", "Non binario", "Preferisco non dichiarare")
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(selectedGender) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(),
            label = { Text("Genere") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        selectedText = option
                        onGenderSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

