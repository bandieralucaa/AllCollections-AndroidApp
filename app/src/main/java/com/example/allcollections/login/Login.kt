package com.example.allcollections.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.allcollections.R
import com.example.allcollections.navigation.Screens
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.util.Log
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.allcollections.viewModel.ProfileViewModel

@Composable
fun Login(navController: NavController, viewModel: ProfileViewModel) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentUser = Firebase.auth.currentUser

    val cleanedEmail = email.trim()
    val cleanedPassword = password.trim()


    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate(Screens.Home.name) {
                popUpTo(Screens.Login.name) { inclusive = true }
            }
        }
    }



    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(150.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "AllCollections", fontSize = 28.sp)

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            label = { Text(text = "Indirizzo email") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrect = false
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            viewModel.login(cleanedEmail, cleanedPassword) { success, error ->
                Log.d("LOGIN", "Tentativo con email: '$cleanedEmail' e password: '$cleanedPassword'")
                if (success) {
                    navController.navigate(Screens.Home.name) {
                        popUpTo(Screens.Login.name) {
                            inclusive = true
                        }
                    }
                } else {
                    errorMessage = error ?: "Errore durante il login"
                }
            }
        }) {
            Text(text = "Accedi")
        }

        TextButton(onClick = { navController.navigate(Screens.Register.name) }) {
            Text("Non hai un account? Registrati")
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
