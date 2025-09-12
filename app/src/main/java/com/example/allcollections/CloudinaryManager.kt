package com.example.allcollections

import android.content.Context
import com.cloudinary.android.MediaManager

object CloudinaryManager {
    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            val config = hashMapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET,
                "secure" to "true"
            )
            MediaManager.init(context, config)
            isInitialized = true
        }
    }
}
