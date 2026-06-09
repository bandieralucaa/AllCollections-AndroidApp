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
 * Activity principale e unica dell'app AllCollections (architettura single-Activity).
 *
 * L'app utilizza una singola Activity con navigazione Compose gestita da [AppNavigation].
 *
 * ### Responsabilità principali
 * - Inizializza Firebase tramite [FirebaseApp.initializeApp] all'avvio.
 * - Determina la schermata iniziale ([Screens.HomeScreen] o [Screens.LoginScreen])
 *   in base allo stato di autenticazione di [FirebaseAuth].
 * - Applica il tema dinamico (chiaro/scuro/sistema) leggendo lo stato da [ThemeViewModel].
 * - Gestisce la navigazione da notifica push in due scenari:
 *   - **Cold start** (app non in esecuzione) – i dati della notifica vengono processati in [onCreate].
 *   - **Foreground/background** (app già in esecuzione) – i dati vengono processati in [onNewIntent].
 *
 * @see AppNavigation
 * @see NotificationData
 */
class MainActivity : ComponentActivity() {

    /**
     * Chiamata alla creazione dell'Activity. Qui avviene l'inizializzazione di Firebase,
     * la configurazione del tema e della navigazione, e la gestione della notifica
     * in caso di avvio da cold start.
     *
     * @param savedInstanceState Stato salvato dell'istanza (non utilizzato).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            val navController = rememberNavController()
            val themeViewModel: ThemeViewModel = koinViewModel()
            val notificationViewModel: NotificationViewModel = koinViewModel()
            val themeState by themeViewModel.state.collectAsState()

            // Dati della notifica in attesa di essere processati (da cold start)
            var pendingNotificationNavigation by remember { mutableStateOf<NotificationData?>(null) }

            // Controlla se l'app è stata aperta tramite tap su una notifica (cold start)
            LaunchedEffect(Unit) {
                checkNotificationIntent(intent)?.let { data ->
                    pendingNotificationNavigation = data
                }
            }

            // Determina la destinazione iniziale in base all'autenticazione
            val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
                Screens.HomeScreen.route
            } else {
                Screens.LoginScreen.route
            }

            // Applica il tema (supporta modalità sistema, scuro, chiaro)
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
                    // Naviga alla destinazione della notifica appena il NavController è pronto
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
     * un nuovo Intent, tipicamente da un tap su una notifica push FCM.
     *
     * Aggiorna l'[Intent] corrente e ricrea il contenuto Compose per triggerare
     * la navigazione tramite [LaunchedEffect].
     *
     * @param intent Il nuovo Intent ricevuto dal sistema (contiene i dati della notifica).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Ricrea il contenuto Compose per eseguire nuovamente il LaunchedEffect
        // che processa pendingNotificationNavigation
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
     * FCM inserisce i dati nel [Bundle.extras] dell'Intent. I nomi delle chiavi
     * possono variare (es. `"collectionId"` o `"collection_id"`), quindi la funzione
     * controlla entrambe le varianti per robustezza.
     *
     * @param intent L'Intent da cui estrarre i dati (può essere `null`).
     * @return Un [NotificationData] con i campi valorizzati se l'Intent contiene
     *         almeno un dato di navigazione significativo, altrimenti `null`.
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

            // Restituisce NotificationData solo se c'è almeno un campo utile
            if (type != null || collectionId != null || itemId != null || userId != null) {
                NotificationData(type, collectionId, itemId, collectionName, userId)
            } else null
        }
    }

    /**
     * Naviga alla schermata appropriata in base al tipo di notifica ricevuta.
     *
     * ### Regole di navigazione
     * - `"follow"` → profilo pubblico dell'utente che ha seguito.
     * - `"comment"`, `"new_comment"` → dettaglio della collezione commentata.
     * - Altro / non specificato → schermata notifiche.
     *
     * Se i dati necessari per la navigazione sono assenti (es. userId per follow,
     * collectionId per comment), viene navigato a [Screens.HomeScreen] come fallback.
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