package com.example.allcollections.data.remote

import android.content.Context
import com.cloudinary.android.MediaManager
import com.example.allcollections.BuildConfig

/**
 * Singleton responsabile dell'inizializzazione di Cloudinary.
 *
 * Deve essere inizializzato UNA SOLA VOLTA all'avvio dell'app
 * (tipicamente nella classe Application).
 */
object CloudinaryManager {

    /** Flag per evitare inizializzazioni multiple */
    @Volatile
    private var isInitialized = false

    /**
     * Inizializza Cloudinary se non è già stato inizializzato.
     *
     * Deve essere chiamato con applicationContext
     * per evitare memory leak.
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

    /**
     * Verifica se Cloudinary è già inizializzato.
     * Utile per debug o controlli di sicurezza.
     */
    fun isReady(): Boolean = isInitialized
}
