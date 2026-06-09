package com.example.allcollections.data.remote

import android.content.Context
import com.cloudinary.android.MediaManager
import com.example.allcollections.BuildConfig

/**
 * Singleton responsabile dell'inizializzazione dell'SDK Cloudinary.
 *
 * Deve essere inizializzato **una sola volta** all'avvio dell'app,
 * tipicamente nel metodo `onCreate` della classe [Application].
 * Chiamate successive a [init] vengono ignorate in modo sicuro.
 *
 * Le credenziali (`cloud_name`, `api_key`, `api_secret`) sono lette da
 * [BuildConfig] e non hardcodate nel sorgente.
 */
object CloudinaryManager {

    /**
     * Flag che indica se [MediaManager] è già stato inizializzato.
     *
     * Dichiarato `@Volatile` per garantire la visibilità immediata
     * del valore tra thread diversi (necessario per il double-checked locking).
     */
    @Volatile
    private var isInitialized = false

    /**
     * Inizializza Cloudinary con le credenziali da [BuildConfig].
     *
     * Utilizza il pattern **double-checked locking** per garantire che
     * [MediaManager] venga inizializzato al massimo una volta, anche in
     * presenza di chiamate concorrenti da thread diversi.
     *
     * @param context Contesto Android; viene usato [Context.getApplicationContext]
     *   internamente per evitare memory leak legati ad Activity o Fragment.
     */
    fun init(context: Context) {
        if (isInitialized) return

        synchronized(this) {
            if (isInitialized) return

            val config = mutableMapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET,
                "secure" to true
            )

            MediaManager.init(context.applicationContext, config)
            isInitialized = true
        }
    }
}
