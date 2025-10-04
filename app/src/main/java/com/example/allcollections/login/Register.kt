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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.allcollections.navigation.MyTopBar
import com.example.allcollections.utils.DatePickerField
import com.example.allcollections.utils.GenderSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Register(navController: NavController, profileViewModel: ProfileViewModel) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf(LocalDate.now()) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MyTopBar(navController = navController)
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
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val textFieldWidth = Modifier.fillMaxWidth(0.8f)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = textFieldWidth,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    label = { Text("Cognome") },
                    modifier = textFieldWidth,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                GenderSelector(
                    selectedGender = gender,
                    modifier = textFieldWidth
                ) { gender = it }

                DatePickerField(
                    selectedDate = dateOfBirth,
                    modifier = textFieldWidth
                ) { dateOfBirth = it }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = textFieldWidth,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Email
                    )
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = textFieldWidth,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val description = if (passwordVisible) "Nascondi password" else "Mostra password"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = description)
                        }
                    },
                    singleLine = true,
                    modifier = textFieldWidth,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = {
                    profileViewModel.registerUser(
                        name, surname, dateOfBirth, email, gender, username, password,
                        onSuccess = { userId ->
                            navController.navigate("${Screens.PhotoProfile.name}/$userId")
                        },
                        onFailure = { message ->
                            errorMessage = message
                        }
                    )
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


