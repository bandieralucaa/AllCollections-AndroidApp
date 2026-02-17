package com.example.allcollections.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.navigation.NavHostController
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

data class NotificationData(
    val type: String?,
    val collectionId: String?,
    val itemId: String?,
    val collectionName: String?,
    val userId: String?
)

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) initializeFCMToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        checkNotificationPermission()

        setContent {
            val navController = rememberNavController()
            val themeViewModel: ThemeViewModel = koinViewModel()
            val notificationViewModel: NotificationViewModel = koinViewModel()
            val themeState by themeViewModel.state.collectAsState()

            var pendingNotificationNavigation by remember { mutableStateOf<NotificationData?>(null) }

            // Controlla se l'app è stata aperta da una notifica
            LaunchedEffect(Unit) {
                checkNotificationIntent(intent)?.let { data ->
                    pendingNotificationNavigation = data
                }
            }

            val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
                Screens.HomeScreen.route
            } else {
                Screens.LoginScreen.route
            }

            AllCollectionsTheme(
                darkTheme = when (themeState.theme) {
                    ThemeMode.Dark -> true
                    ThemeMode.Light -> false
                    ThemeMode.System -> isSystemInDarkTheme()
                }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Esegui navigazione pendente se presente
                    LaunchedEffect(pendingNotificationNavigation) {
                        pendingNotificationNavigation?.let { data ->
                            navigateFromNotification(data, notificationViewModel, navController)
                            pendingNotificationNavigation = null
                        }
                    }

                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination,
                        themeState = themeState,
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

    private fun checkNotificationIntent(intent: Intent?): NotificationData? {
        return intent?.extras?.let { extras ->
            val type = extras.getString("type") ?: extras.getString("notification_type")
            val collectionId = extras.getString("collectionId") ?: extras.getString("collection_id")
            val itemId = extras.getString("itemId") ?: extras.getString("item_id")
            val collectionName = extras.getString("collectionName") ?: extras.getString("collection_name")
            val userId = extras.getString("userId") ?: extras.getString("user_id")

            Log.d("MainActivity", "Notifica ricevuta: type=$type, collectionId=$collectionId, itemId=$itemId, userId=$userId")

            if (type != null || collectionId != null || itemId != null || userId != null) {
                NotificationData(type, collectionId, itemId, collectionName, userId)
            } else null
        }
    }

    private fun navigateFromNotification(
        data: NotificationData,
        viewModel: NotificationViewModel,
        navController: NavHostController
    ) {
        Log.d("MainActivity", "Navigazione da notifica: $data")

        when (data.type) {
            "follow" -> {
                data.userId?.let {
                    navController.navigate(Screens.PublicProfileScreen.createRoute(it))
                } ?: navController.navigate(Screens.HomeScreen.route)
            }
            "comment", "new_comment" -> {
                when {
                    data.collectionId != null -> {
                        navController.navigate(Screens.CollectionDetailScreen.collectionDetailRoute(data.collectionId))
                    }
                    else -> navController.navigate(Screens.HomeScreen.route)
                }
            }
            else -> {
                navController.navigate(Screens.NotificationsScreen.route)
            }
        }
    }
}