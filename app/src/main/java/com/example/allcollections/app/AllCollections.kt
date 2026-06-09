package com.example.allcollections.app

import android.app.Application
import com.example.allcollections.data.remote.CloudinaryManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Classe [Application] dell'app AllCollections.
 *
 * È il punto di ingresso globale dell'applicazione; viene istanziata dal sistema
 * **prima** di qualsiasi Activity, Service o BroadcastReceiver.
 *
 * ### Inizializzazioni eseguite in [onCreate]
 * 1. **Koin (Dependency Injection)** – avvia il container di dependency injection
 *    con il modulo [appModule], fornendo il contesto dell'applicazione a tutti i componenti.
 * 2. **Cloudinary** – inizializza l'SDK per la gestione delle immagini remote
 *    (upload, download, trasformazioni). L'inizializzazione è thread‑safe grazie
 *    al double‑checked locking implementato in [CloudinaryManager.init].
 *
 * ### Registrazione nel manifest
 * ```xml
 * <application
 *     android:name=".app.AllCollections"
 *     ... >
 * ```
 *
 * @see AppModule per la definizione dei moduli Koin (repository, viewModel, ecc.).
 * @see CloudinaryManager per l'inizializzazione dell'SDK Cloudinary.
 * @see Application per il ciclo di vita dell'applicazione Android.
 */
class AllCollections : Application() {

    /**
     * Chiamata all'avvio dell'applicazione. Qui vengono eseguite tutte le inizializzazioni
     * globali che devono avvenire prima della creazione della prima Activity.
     */
    override fun onCreate() {
        super.onCreate()

        // Inizializza Koin (Dependency Injection)
        startKoin {
            // Fornisce il contesto dell'applicazione a tutti i moduli Koin
            androidContext(this@AllCollections)
            // Carica i moduli definiti in AppModule.kt
            modules(appModule)
        }

        // Inizializza l'SDK Cloudinary per la gestione delle immagini
        // L'inizializzazione è idempotente e sicura in multi‑threading.
        CloudinaryManager.init(this)
    }
}