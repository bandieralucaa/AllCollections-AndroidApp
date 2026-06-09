package com.example.allcollections.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.core.ui.MyTopBar

/**
 * Schermata per la modifica della biografia (bio) del profilo utente.
 *
 * Carica la bio attuale dell'utente, permette di modificarla con un limite
 * di 150 caratteri, mostra un contatore in tempo reale e salva le modifiche
 * su Firestore tramite [ProfileViewModel.saveBio].
 *
 * Al termine del salvataggio, torna alla schermata precedente (profilo).
 *
 * @param navController Controller per la navigazione (per tornare indietro).
 * @param viewModel ViewModel del profilo (per recuperare e salvare la bio).
 */
@Composable
fun EditBioScreen(
    navController: NavController,
    viewModel: ProfileViewModel
) {
    var bio by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val maxChars = 150

    // Carica la bio corrente al primo avvio
    LaunchedEffect(Unit) {
        val userData = viewModel.getUserData()
        bio = userData?.bio ?: ""
        isLoading = false
    }

    Scaffold(
        topBar = { MyTopBar(navController = navController, title = "Modifica bio") }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Scrivi qualcosa su di te",
                style = MaterialTheme.typography.titleMedium
            )

            // Campo di testo per la bio con limite caratteri
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= maxChars) bio = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Es. Colleziono fumetti vintage e vinili 🎵") },
                minLines = 4,
                maxLines = 6,
                supportingText = {
                    Text(
                        text = "${bio.length}/$maxChars",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bio.length >= maxChars) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            // Messaggio di errore (se presente)
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Pulsante di salvataggio
            Button(
                onClick = {
                    isSaving = true
                    viewModel.saveBio(bio) { success, error ->
                        isSaving = false
                        if (success) {
                            navController.popBackStack()
                        } else {
                            errorMessage = error ?: "Errore salvataggio"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Salva")
                }
            }
        }
    }
}