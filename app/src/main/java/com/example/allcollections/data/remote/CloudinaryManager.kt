package com.example.allcollections.data.remote

import android.content.Context
import com.cloudinary.android.MediaManager
import com.example.allcollections.BuildConfig

/**
 * Singleton responsabile dell'inizializzazione dell'SDK Cloudinary per l'upload
 * e la gestione delle immagini.
 *
 * L'inizializzazione deve avvenire **una sola volta** all'avvio dell'app,
 * tipicamente nel metodo [onCreate][android.app.Application.onCreate] della
 * classe [Application] (es. [AllCollections]).
 *
 * Le credenziali (`cloud_name`, `api_key`, `api_secret`) sono lette da
 * [BuildConfig] (generato automaticamente da Gradle) e non hardcodate nel sorgente.
 *
 * ### Pattern di inizializzazione
 * Viene utilizzato il **double-checked locking** per garantire che
 * [MediaManager.init] venga chiamato al massimo una volta, anche in presenza
 * di chiamate concorrenti da thread diversi (es. accesso da più componenti
 * all'avvio dell'app).
 *
 * @see MediaManager
 * @see AllCollections
 */
object CloudinaryManager {

    /**
     * Flag che indica se [MediaManager] è già stato inizializzato.
     *
     * Dichiarato `@Volatile` per garantire la visibilità immediata del valore
     * tra thread diversi (necessario per il corretto funzionamento del
     * double-checked locking).
     */
    @Volatile
    private var isInitialized = false

    /**
     * Inizializza l'SDK Cloudinary con le credenziali definite in [BuildConfig].
     *
     * Questa funzione è **idempotente**: chiamate successive alla prima
     * non hanno effetto.
     *
     * ### Double-checked locking
     * 1. Primo controllo senza sincronizzazione (per performance).
     * 2. Sincronizzazione sul singleton.
     * 3. Secondo controllo dentro il blocco sincronizzato (per sicurezza).
     *
     * Viene utilizzato [Context.getApplicationContext] per evitare memory leak
     * legati a Activity o Fragment.
     *
     * @param context Contesto Android (anche Activity va bene, ma internamente
     *                viene convertito in applicationContext).
     */
    fun init(context: Context) {
        // Primo controllo (senza lock) – ottimizzazione
        if (isInitialized) return

        synchronized(this) {
            // Secondo controllo (con lock) – sicurezza
            if (isInitialized) return

            // Prepara la configurazione con le credenziali dai BuildConfig
            val config = mutableMapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET,
                "secure" to true       // Usa sempre URL HTTPS
            )

            // Inizializza MediaManager con il contesto applicativo (evita leak)
            MediaManager.init(context.applicationContext, config)

            // Marca come inizializzato per future chiamate
            isInitialized = true
        }
    }
}