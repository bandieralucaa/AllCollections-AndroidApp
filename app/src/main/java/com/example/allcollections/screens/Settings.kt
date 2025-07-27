package com.example.allcollections.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.allcollections.navigation.Screens
import com.example.allcollections.viewModel.ProfileViewModel

@Composable
fun Settings(navController: NavController, viewModel: ProfileViewModel) {
    val settingsItems = listOf<Pair<String, () -> Unit>>(
        "Modifica profilo" to {
            navController.navigate(Screens.EditProfile.name)
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(settingsItems.size) { index ->
                val (setting, action) = settingsItems[index]
                ClickableSettingItem(setting = setting, onClick = action)
                if (index < settingsItems.size - 1) {
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
