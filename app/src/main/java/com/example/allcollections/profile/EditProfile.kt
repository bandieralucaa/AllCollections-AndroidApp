package com.example.allcollections.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.allcollections.viewModel.ProfileViewModel
import java.time.LocalDate
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.example.allcollections.navigation.MyTopBar
import com.example.allcollections.utils.DatePickerField
import com.example.allcollections.utils.GenderSelector

@Composable
fun EditProfile(navController: NavController) {
    val profileViewModel: ProfileViewModel = viewModel()
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf(LocalDate.now()) }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(true) {
        val userData = profileViewModel.getUserData()
        name = userData.name
        surname = userData.surname
        dateOfBirth = userData.dateOfBirth
        email = userData.email
        gender = userData.gender
        username = userData.username
    }

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
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .fillMaxWidth(0.85f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    label = { Text("Cognome") },
                    modifier = Modifier.fillMaxWidth()
                )

                GenderSelector(
                    selectedGender = gender,
                    modifier = Modifier.fillMaxWidth()
                ) { gender = it }

                DatePickerField(
                    selectedDate = dateOfBirth,
                    modifier = Modifier.fillMaxWidth()
                ) { dateOfBirth = it }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Indirizzo email") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(onClick = {
                    profileViewModel.updateUserData(
                        name, surname, dateOfBirth, email, gender, username
                    ) { success, error ->
                        if (success) navController.navigateUp()
                        else errorMessage = error
                    }
                }) {
                    Text("Salva modifiche")
                }

                errorMessage?.let {
                    Text(text = it, color = Color.Red)
                }
            }
        }
    }
}