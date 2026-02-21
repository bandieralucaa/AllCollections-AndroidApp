package com.example.allcollections.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.allcollections.data.repository.ThemeRepository
import com.example.allcollections.core.theme.ThemeViewModel
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

val Context.dataStore by preferencesDataStore(name = "theme")

/**
 * Modulo Koin principale dell'app AllCollections.
 * Qui vengono definiti:
 * - Firebase (Firestore e Auth)
 * - DataStore per tema
 * - Repository
 * - ViewModel
 */
val appModule = module {

    // ===== FIREBASE =====
    single { Firebase.firestore }
    single { Firebase.auth }

    // ===== DATASTORE =====
    single { (get() as Context).dataStore }

    // ===== REPOSITORIES =====
    single { ThemeRepository(get()) }
    single { NotificationRepository(get()) }
    single { ChatRepository(get()) }

    // ===== VIEWMODELS =====
    viewModel { ThemeViewModel(get()) }
    viewModel { ProfileViewModel() }
    viewModel { NotificationViewModel(get()) }
    viewModel { CollectionViewModel() }
    viewModel { SearchViewModel(get()) }
    viewModel { ChatViewModel(get()) }
}