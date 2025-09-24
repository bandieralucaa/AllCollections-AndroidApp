package com.example.allcollections

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.allcollections.navigation.AppNavigation
import com.example.allcollections.navigation.Screens
import com.example.allcollections.ui.theme.AllCollectionsTheme
import com.example.allcollections.ui.theme.ThemeMode
import com.example.allcollections.viewModel.CollectionViewModel
import com.example.allcollections.viewModel.NotificationViewModel
import com.example.allcollections.viewModel.ProfileViewModel
import com.example.allcollections.viewModel.ThemeViewModel
import com.example.allcollections.viewModel.ViewModelContainer
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.initialize
import org.koin.androidx.compose.koinViewModel


@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.initialize(this)


        setContent {
            val navController = rememberNavController()
            val profileViewModel: ProfileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
            val collectionViewModel: CollectionViewModel = ViewModelProvider(this).get(CollectionViewModel::class.java)
            val themeViewModel = koinViewModel<ThemeViewModel>()
            val notificationViewModel: NotificationViewModel = ViewModelProvider(this).get(NotificationViewModel::class.java)
            val viewModelContainer = ViewModelContainer(profileViewModel, collectionViewModel, themeViewModel, notificationViewModel)
            val themeState by themeViewModel.state.collectAsState()
            val currentUser = FirebaseAuth.getInstance().currentUser

            val startDestination = if (currentUser != null) {
                Screens.Home.name
            } else {
                Screens.Login.name
            }

            AllCollectionsTheme(
                darkTheme = when (themeState.theme) {
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                    ThemeMode.System -> isSystemInDarkTheme()
                }
            ) {
                Surface( // Imposta lo sfondo in base al tema selezionato
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(navController, viewModelContainer, startDestination, themeState, themeViewModel::changeTheme)
                }
            }
        }
    }
}
