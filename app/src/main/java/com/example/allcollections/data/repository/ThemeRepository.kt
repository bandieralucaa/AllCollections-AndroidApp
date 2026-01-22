package com.example.allcollections.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.allcollections.core.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Repository responsabile della gestione del tema dell'applicazione.
 *
 * - Legge e scrive la preferenza del tema tramite DataStore
 * - Espone un Flow osservabile per aggiornare automaticamente la UI
 * - Fornisce utility per toggle, reset e verifica stato
 */
class ThemeRepository(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        /** Chiave DataStore per il tema */
        private val THEME_KEY = stringPreferencesKey("app_theme")
    }

    /**
     * Flusso osservabile del tema corrente.
     * In caso di errore IO, ritorna preferenze vuote e fallback a System.
     */
    val theme: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ThemeMode.fromString(preferences[THEME_KEY])
        }

    /**
     * Salva in modo persistente il tema selezionato dall'utente.
     */
    suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    /**
     * Recupera il tema corrente in modo sincrono.
     * Usato quando serve il valore immediato (es. startup).
     */
    suspend fun getCurrentTheme(): ThemeMode = try {
        val preferences = dataStore.data.first()
        ThemeMode.fromString(preferences[THEME_KEY])
    } catch (_: Exception) {
        ThemeMode.System
    }

    /**
     * Reimposta il tema al valore di default (segue il sistema).
     */
    suspend fun resetToDefault() {
        setTheme(ThemeMode.System)
    }

    /**
     * Verifica se l'utente ha impostato un tema personalizzato.
     */
    suspend fun hasCustomTheme(): Boolean =
        getCurrentTheme() != ThemeMode.System

    /**
     * Effettua il toggle tra Light e Dark.
     * Se il tema è System, viene considerato Dark come default di toggle.
     *
     * @return Il nuovo tema applicato
     */
    suspend fun toggleLightDark(): ThemeMode {
        val newTheme = when (getCurrentTheme()) {
            ThemeMode.Light -> ThemeMode.Dark
            ThemeMode.Dark,
            ThemeMode.System -> ThemeMode.Light
        }
        setTheme(newTheme)
        return newTheme
    }
}
