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
 * Responsabilità di inizializzazione in [onCreate]:
 * 1. **Koin** — avvia il container di Dependency Injection con il modulo [appModule].
 * 2. **Cloudinary** — inizializza l'SDK per la gestione delle immagini remote
 *    (una sola volta grazie al double-checked locking in [CloudinaryManager]).
 *
 * Registrata nel manifest con `android:name=".app.AllCollections"`.
 *
 * @see AppModule per la definizione delle dipendenze Koin.
 * @see CloudinaryManager per i dettagli sull'inizializzazione Cloudinary.
 */
class AllCollections : Application() {

    override fun onCreate() {
        super.onCreate()

        // Avvia Koin con il contesto dell'applicazione
        startKoin {
            androidContext(this@AllCollections)
            modules(appModule)
        }

        // Inizializza Cloudinary per la gestione delle immagini remote
        CloudinaryManager.init(this)
    }
}
