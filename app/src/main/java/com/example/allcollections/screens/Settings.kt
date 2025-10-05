package com.example.allcollections.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.navigation.MyTopBar
import com.example.allcollections.navigation.Screens
import com.example.allcollections.viewModel.ProfileViewModel

@Composable
fun Settings(navController: NavController, viewModel: ProfileViewModel) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val settingsItems = listOf<Pair<String, () -> Unit>>(
        "Modifica profilo" to {
            navController.navigate(Screens.EditProfile.name)
        },
        "Modifica password" to {
            navController.navigate(Screens.EditPassword.name)
        },
        "Cambia immagine del profilo" to {
            navController.navigate(Screens.EditPhotoProfile.name)
        },
        "Cambia tema" to {
            navController.navigate(Screens.ChooseTheme.name)
        },
        "Logout" to {
            viewModel.logout {
                navController.navigate(Screens.Login.name)
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MyTopBar(navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(settingsItems.size) { index ->
                    val (setting, action) = settingsItems[index]
                    ClickableSettingItem(setting = setting, onClick = action)
                    Divider(color = Color.Gray, thickness = 0.5.dp)
                }
            }

        }
    }
}

@Composable
fun ClickableSettingItem(setting: String, onClick: () -> Unit) {
    Text(
        text = setting,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .fillMaxWidth()
    )
}
