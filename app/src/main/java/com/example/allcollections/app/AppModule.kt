package com.example.allcollections.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.allcollections.core.theme.ThemeViewModel
import com.example.allcollections.data.repository.ThemeRepository
import com.example.allcollections.feature.chat.data.ChatRepository
import com.example.allcollections.feature.chat.presentation.ChatViewModel
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.notification.data.NotificationRepository
import com.example.allcollections.feature.notification.presentation.NotificationViewModel
import com.example.allcollections.feature.profile.ProfileViewModel
import com.example.allcollections.feature.search.SearchViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Estensione di [Context] che espone un'istanza singleton di DataStore<Preferences>
 * con il file di preferenze `"theme"`.
 *
### Utilizzo
 * ```kotlin
 * val dataStore = context.dataStore
 * ```
 *
 * Il DataStore è thread-safe e gestisce automaticamente la scrittura asincrona
 * su dispatcher IO, senza bisogno di lock manuali. La dichiarazione è top-level
 * (non all'interno di un modulo Koin) per poter essere utilizzata direttamente
 * dove necessario, ma viene anche iniettata tramite Koin nel [ThemeRepository].
 *
 * @see androidx.datastore.preferences.core.Preferences
 * @see preferencesDataStore
 */
val Context.dataStore by preferencesDataStore(name = "theme")

/**
 * Modulo Koin principale dell'app AllCollections.
 *
 * Dichiara tutte le dipendenze dell'applicazione come singleton o ViewModel.
 *
 * ### Componenti definiti:
 * - **Firebase** – istanze singleton di [Firebase.firestore] e [Firebase.auth].
 * - **DataStore** – istanza singleton del DataStore per la persistenza del tema.
 * - **Repository** – singleton per l'accesso ai dati (Theme, Notification, Chat).
 * - **ViewModel** – istanze con ciclo di vita legato alla composizione Compose
 *   (utilizzabili tramite `koinViewModel()` nei composable).
 *
 * ### Registrazione
 * Il modulo viene caricato in [AllCollections.onCreate] tramite
 * `startKoin { modules(appModule) }`.
 *
 * @see AllCollections
 * @see org.koin.dsl.module
 * @see viewModel
 */
val appModule = module {

    // ─────────── Firebase ───────────
    // Istanza singleton di Firestore, condivisa da tutti i repository.
    single { Firebase.firestore }

    // Istanza singleton di Firebase Auth.
    single { Firebase.auth }

    // ─────────── DataStore ───────────
    // Istanza singleton del DataStore per il tema.
    // Risolve il Context dal container Koin (registrato con androidContext() in AllCollections).
    single { (get() as Context).dataStore }

    // ─────────── Repository ───────────
    // Repository per il tema (DataStore).
    single { ThemeRepository(get()) }

    // Repository per le notifiche (Firestore).
    single { NotificationRepository(get()) }

    // Repository per le chat (Firestore).
    single { ChatRepository(get()) }

    // ─────────── ViewModel ───────────
    // ViewModel del tema (dipende da ThemeRepository).
    viewModel { ThemeViewModel(get()) }

    // ViewModel del profilo (senza dipendenze esterne, usa Firebase Auth internamente).
    viewModel { ProfileViewModel() }

    // ViewModel delle notifiche (dipende da NotificationRepository).
    viewModel { NotificationViewModel(get()) }

    // ViewModel delle collezioni (senza dipendenze).
    viewModel { CollectionViewModel() }

    // ViewModel della ricerca (dipende da CollectionViewModel).
    viewModel { SearchViewModel(get()) }

    // ViewModel delle chat (dipende da ChatRepository).
    viewModel { ChatViewModel(get()) }
}