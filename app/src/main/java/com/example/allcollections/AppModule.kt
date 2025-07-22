package com.example.allcollections

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.allcollections.repositories.ThemeRepository
import com.example.allcollections.viewModel.ThemeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val Context.dataStore by preferencesDataStore("theme")

val appModule = module {
    single { get<Context>().dataStore }

    single { ThemeRepository(get()) }

    viewModel { ThemeViewModel(get()) }
}