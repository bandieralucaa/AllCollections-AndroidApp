package com.example.allcollections.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.allcollections.core.navigation.AppNavigation
import com.example.allcollections.core.navigation.Screens
import com.example.allcollections.core.theme.AllCollectionsTheme
import com.example.allcollections.core.theme.ThemeMode
import com.example.allcollections.core.theme.ThemeViewModel
import com.example.allcollections.data.model.NotificationData
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.compose.koinViewModel

/**
 * Activity principale e unica dell'app AllCollections (single-Activity architecture).
 *
 * Responsabilità principali:
 * - Inizializza Firebase tramite [FirebaseApp.initializeApp].
 * - Determina la destinazione di partenza ([Screens.HomeScreen] o [Screens.LoginScreen])
 *   in base allo stato di autenticazione di [FirebaseAuth].
 * - Applica il tema dell'app (light/dark/system) leggendo lo stato da [ThemeViewModel].
 * - Gestisce la navigazione da notifica push sia a **cold start** (via [onCreate])
 *   sia con l'app già in foreground/background (via [onNewIntent]).
 *
 * Il grafo di navigazione completo è delegato ad [AppNavigation].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            val navController = rememberNavController()
            val themeViewModel: ThemeViewModel = koinViewModel()
            val notificationViewModel: NotificationViewModel = koinViewModel()
            val themeState by themeViewModel.state.collectAsState()

            // Dati della notifica in attesa di essere processata per la navigazione.
            // È uno State per triggerare il LaunchedEffect appena viene valorizzato.
            var pendingNotificationNavigation by remember { mutableStateOf<NotificationData?>(null) }

            // Controlla se l'app è stata aperta tramite tap su una notifica push (cold start)
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
                    // Naviga alla destinazione della notifica non appena il NavController
                    // è pronto e pendingNotificationNavigation viene valorizzato
                    LaunchedEffect(pendingNotificationNavigation) {
                        pendingNotificationNavigation?.let { data ->
                            navigateFromNotification(data, navController)
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

    /**
     * Chiamato quando l'app è già in esecuzione (foreground o background) e arriva
     * un nuovo Intent — tipicamente un tap su una notifica push FCM.
     *
     * Aggiorna l'[intent] dell'Activity con quello nuovo in modo che
     * [checkNotificationIntent] possa elaborare i dati corretti, e ricrea il
     * contenuto Compose per triggerare il [LaunchedEffect] di navigazione.
     *
     * @param intent Il nuovo [Intent] ricevuto dal sistema.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Ricrea il contenuto Compose così che il LaunchedEffect
        // su pendingNotificationNavigation venga ri-eseguito con i nuovi dati
        setContent {
            val navController = rememberNavController()
            val themeViewModel: ThemeViewModel = koinViewModel()
            val notificationViewModel: NotificationViewModel = koinViewModel()
            val themeState by themeViewModel.state.collectAsState()

            var pendingNotificationNavigation by remember { mutableStateOf<NotificationData?>(null) }

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
                    LaunchedEffect(pendingNotificationNavigation) {
                        pendingNotificationNavigation?.let { data ->
                            navigateFromNotification(data, navController)
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

    /**
     * Estrae i dati di navigazione da un [Intent] di notifica push FCM.
     *
     * FCM inserisce i dati del payload nei [Bundle.extras] dell'Intent con nomi
     * che possono variare leggermente (es. `"collectionId"` o `"collection_id"`),
     * quindi vengono verificati entrambi i formati per robustezza.
     *
     * @param intent L'[Intent] da cui estrarre i dati; può essere `null`.
     * @return Un [NotificationData] con i campi valorizzati se l'Intent contiene
     *   almeno un dato di navigazione, altrimenti `null`.
     */
    private fun checkNotificationIntent(intent: Intent?): NotificationData? {
        return intent?.extras?.let { extras ->
            val type = extras.getString("type") ?: extras.getString("notification_type")
            val collectionId = extras.getString("collectionId") ?: extras.getString("collection_id")
            val itemId = extras.getString("itemId") ?: extras.getString("item_id")
            val collectionName = extras.getString("collectionName") ?: extras.getString("collection_name")
            val userId = extras.getString("userId") ?: extras.getString("user_id")

            Log.d(
                "MainActivity",
                "Notifica ricevuta: type=$type, collectionId=$collectionId, " +
                        "itemId=$itemId, userId=$userId"
            )

            // Restituisce NotificationData solo se è presente almeno un campo rilevante
            if (type != null || collectionId != null || itemId != null || userId != null) {
                NotificationData(type, collectionId, itemId, collectionName, userId)
            } else null
        }
    }

    /**
     * Naviga alla schermata corretta in base al tipo di notifica ricevuta.
     *
     * | Tipo notifica           | Destinazione                                  |
     * |-------------------------|-----------------------------------------------|
     * | `"follow"`              | Profilo pubblico dell'utente che ha seguito   |
     * | `"comment"`, `"new_comment"` | Dettaglio della collezione commentata    |
     * | Altro / non specificato | Schermata notifiche                           |
     *
     * @param data Dati estratti dall'Intent della notifica.
     * @param navController Controller per eseguire la navigazione.
     */
    private fun navigateFromNotification(
        data: NotificationData,
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
                data.collectionId?.let {
                    navController.navigate(Screens.CollectionDetailScreen.collectionDetailRoute(it))
                } ?: navController.navigate(Screens.HomeScreen.route)
            }
            else -> {
                navController.navigate(Screens.NotificationsScreen.route)
            }
        }
    }
}
