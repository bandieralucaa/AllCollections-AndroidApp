package com.example.allcollections.app

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.allcollections.core.navigation.AppNavigation
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.theme.AllCollectionsTheme
import com.example.allcollections.core.theme.ThemeMode
import com.example.allcollections.core.theme.ThemeViewModel
import com.example.allcollections.feature.notification.NotificationData
import com.example.allcollections.feature.notification.NotificationViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * MainActivity gestisce:
 * - Tema dell'app (light/dark/system)
 * - Navigazione principale usando NavController
 * - Controllo login e scelta startDestination
 * - Gestione notifiche push (FCM)
 */
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) getFCMToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        requestNotificationPermission()

        setContent {
            val navController = rememberNavController()
            val themeViewModel: ThemeViewModel = koinViewModel()
            val notificationViewModel: NotificationViewModel = koinViewModel()
            val context = LocalContext.current

            val themeState by themeViewModel.state.collectAsState()

            val startDestination = FirebaseAuth.getInstance().currentUser?.let {
                Screens.HomeScreen.route
            } ?: Screens.LoginScreen.route

            var pendingNotificationNavigation by remember { mutableStateOf<NotificationData?>(null) }

            // Controlla se l'app è stata aperta da una notifica
            LaunchedEffect(Unit) {
                checkNotificationIntent(intent)?.let { data ->
                    pendingNotificationNavigation = data
                }
            }

            AllCollectionsTheme(darkTheme = getDarkTheme(themeState.theme)) {
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
                        onThemeSelected = themeViewModel::changeTheme
                    )

                    // Ottieni token FCM se permesso già concesso
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            getFCMToken()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun getDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                getFCMToken()
            }
        } else {
            getFCMToken()
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let { token -> saveTokenToUserProfile(token) }
            }
        }
    }

    private fun saveTokenToUserProfile(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        lifecycleScope.launch(Dispatchers.IO) {
            if (userId != null) {
                // UTENTE LOGGATO: salva nel profilo utente
                val userData = mapOf(
                    "fcmToken" to token,
                    "fcmTokenUpdated" to Timestamp.now()
                )

                Firebase.firestore.collection("users")
                    .document(userId)
                    .update(userData)
            } else {
                // UTENTE NON LOGGATO: salva in collection separata
                val deviceData = mapOf(
                    "fcmToken" to token,
                    "createdAt" to Timestamp.now(),
                    "deviceModel" to Build.MODEL,
                    "androidVersion" to Build.VERSION.SDK_INT.toString()
                )

                Firebase.firestore.collection("device_tokens")
                    .add(deviceData)
            }
        }
    }


    private fun checkNotificationIntent(intent: Intent?): NotificationData? {
        return intent?.extras?.let { extras ->
            val type = extras.getString("type") ?: extras.getString("notification_type")
            val collectionId = extras.getString("collectionId") ?: extras.getString("collection_id")
            val itemId = extras.getString("itemId") ?: extras.getString("item_id")
            val collectionName = extras.getString("collectionName") ?: extras.getString("collection_name")

            if (type != null || collectionId != null || itemId != null) {
                NotificationData(type, collectionId, itemId, collectionName)
            } else null
        }
    }

    private fun navigateFromNotification(
        data: NotificationData,
        viewModel: NotificationViewModel,
        navController: NavHostController
    ) {
        when (data.type) {
            "push_new_item", "new_item" -> {
                data.collectionId?.let { navController.navigate("collection_detail/$it") }
                    ?: navController.navigate(Screens.HomeScreen.route)
            }
            "push_new_comment", "new_comment", "comment" -> {
                data.itemId?.let { navController.navigate("item_detail/$it") }
                    ?: data.collectionId?.let { navController.navigate("collection_detail/$it") }
                    ?: navController.navigate(Screens.HomeScreen.route)
            }
            "follow" -> {
                data.collectionId?.let { navController.navigate("publicProfile/$it") }
                    ?: navController.navigate(Screens.HomeScreen.route)
            }
            else -> navController.navigate(Screens.NotificationsScreen.route)
        }

        viewModel.checkUnreadNotifications()
    }
}
