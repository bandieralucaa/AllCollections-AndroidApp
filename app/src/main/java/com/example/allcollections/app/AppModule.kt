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
 * Dichiarata a livello di file (top-level) così da essere accessibile ovunque
 * sia disponibile un [Context]. Koin la recupera tramite `get<Context>()` nel
 * blocco `single` dedicato al DataStore.
 *
 * Il DataStore è thread-safe e gestisce automaticamente la scrittura asincrona
 * su dispatcher IO, senza bisogno di lock manuali.
 */
val Context.dataStore by preferencesDataStore(name = "theme")

/**
 * Modulo Koin principale dell'app AllCollections.
 *
 * Dichiara tutte le dipendenze dell'applicazione come singleton o ViewModel:
 *
 * - **Firebase** — istanze singleton di Firestore e Auth, condivise da tutti i repository.
 * - **DataStore** — istanza singleton del DataStore per la persistenza del tema.
 * - **Repository** — singleton per l'accesso ai dati (Firebase, DataStore).
 * - **ViewModel** — istanze con ciclo di vita legato alla composizione Compose
 *   tramite `koinViewModel()`.
 *
 * Registrato in [AllCollections.onCreate] tramite `startKoin { modules(appModule) }`.
 */
val appModule = module {

    // ─────────── Firebase ───────────
    /** Istanza singleton di FirebaseFirestore, usata da tutti i repository. */
    single { Firebase.firestore }

    /** Istanza singleton di FirebaseAuth, usata da AuthViewModel e ProfileViewModel. */
    single { Firebase.auth }

    // ─────────── DataStore ───────────
    /**
     * Istanza singleton del DataStore per la preferenza tema.
     * Risolve il [Context] dal container Koin (registrato da `androidContext()` in [AllCollections]).
     */
    single { (get() as Context).dataStore }

    // ─────────── Repository ───────────
    single { ThemeRepository(get()) }
    single { NotificationRepository(get()) }
    single { ChatRepository(get()) }

    // ─────────── ViewModel ───────────
    viewModel { ThemeViewModel(get()) }
    viewModel { ProfileViewModel() }
    viewModel { NotificationViewModel(get()) }
    viewModel { CollectionViewModel() }
    viewModel { SearchViewModel(get()) }
    viewModel { ChatViewModel(get()) }
}
