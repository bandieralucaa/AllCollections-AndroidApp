package com.example.allcollections.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.allcollections.ui.theme.ThemeMode
import kotlinx.coroutines.flow.map

class ThemeRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
    }

    val theme = dataStore.data
        .map { preferences ->
            try {
                ThemeMode.valueOf(preferences[THEME_KEY] ?: "System")
            } catch (_: Exception) {
                ThemeMode.System
            }
        }

    suspend fun setTheme(theme: ThemeMode) =
        dataStore.edit { it[THEME_KEY] = theme.toString() }
}