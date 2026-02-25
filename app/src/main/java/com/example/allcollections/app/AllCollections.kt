package com.example.allcollections.app

import android.app.Application
import com.example.allcollections.data.remote.CloudinaryManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Classe Application dell'app AllCollections.
 * Punto di inizializzazione globale dell'app:
 * - Dependency Injection tramite Koin
 * - Librerie globali (es. Cloudinary)
 */
class AllCollections : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inizializza Koin per Dependency Injection
        startKoin {
            androidContext(this@AllCollections)
            modules(appModule)
        }

        // Inizializza Cloudinary per la gestione delle immagini remote
        CloudinaryManager.init(this)
    }
}
