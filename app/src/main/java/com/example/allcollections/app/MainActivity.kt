package com.example.allcollections.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.allcollections.core.navigation.AppNavigation
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.theme.AllCollectionsTheme
import com.example.allcollections.core.theme.ThemeMode
import com.example.allcollections.core.theme.ThemeViewModel
import com.example.allcollections.feature.notification.data.FCMTokenManager
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) initializeFCMToken()
    }

    @SuppressLint("StateFlowValueCalledInComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        checkNotificationPermission()

        setContent {
            val navController = rememberNavController()
            val themeViewModel: ThemeViewModel = koinViewModel()
            val notificationViewModel: NotificationViewModel = koinViewModel()

            val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
                Screens.HomeScreen.route
            } else {
                Screens.LoginScreen.route
            }

            AllCollectionsTheme(
                darkTheme = when (themeViewModel.state.value.theme) {
                    ThemeMode.Dark -> true
                    ThemeMode.Light -> false
                    ThemeMode.System -> isSystemInDarkTheme()
                }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination,
                        themeState = themeViewModel.state.value,
                        onThemeSelected = themeViewModel::changeTheme,
                        notificationViewModel = notificationViewModel
                    )
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                initializeFCMToken()
            }
        } else {
            initializeFCMToken()
        }
    }

    private fun initializeFCMToken() {
        lifecycleScope.launch {
            val tokenManager = FCMTokenManager(Firebase.auth, Firebase.firestore)
            tokenManager.initializeToken(this@MainActivity)
        }
    }
}