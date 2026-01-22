package com.example.allcollections.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.allcollections.data.repository.ThemeRepository
import com.example.allcollections.core.theme.ThemeViewModel
import com.example.allcollections.feature.collection.CollectionViewModel
import com.example.allcollections.feature.notification.NotificationViewModel
import com.example.allcollections.feature.profile.ProfileViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val Context.dataStore by preferencesDataStore(name = "theme")

/**
 * Modulo Koin principale dell'app AllCollections.
 * Qui vengono definiti:
 * - DataStore per tema
 * - Repository
 * - ViewModel
 */
val appModule = module {

    // DataStore per salvare le preferenze relative al tema
    single { (get() as Context).dataStore }

    // Repository che gestisce lettura e scrittura delle preferenze del tema
    single { ThemeRepository(get()) }

    // ViewModel del tema
    viewModel { ThemeViewModel(get()) }

    // Altri ViewModel dell'app
    viewModel { ProfileViewModel() }
    viewModel { NotificationViewModel() }
    viewModel { CollectionViewModel() }
}
