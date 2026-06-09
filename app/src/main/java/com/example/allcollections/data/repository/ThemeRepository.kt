package com.example.allcollections.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.allcollections.core.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Repository responsabile della persistenza e del recupero della preferenza tema.
 *
 * Utilizza [DataStore] per leggere e scrivere il tema scelto dall'utente in modo
 * asincrono e sicuro. Espone un [Flow] osservabile così che la UI si aggiorni
 * automaticamente al cambio di tema senza bisogno di polling.
 *
 * @param dataStore Istanza di [DataStore] iniettata tramite Koin (vedi `AppModule`).
 */
class ThemeRepository(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        /** Chiave DataStore con cui viene salvata la preferenza del tema. */
        private val THEME_KEY = stringPreferencesKey("app_theme")
    }

    /**
     * Flusso osservabile del tema corrente selezionato dall'utente.
     *
     * In caso di errore di I/O durante la lettura del DataStore, emette
     * preferenze vuote e ricade sul valore di default [ThemeMode.fromString]
     * (tipicamente [ThemeMode.System]). Tutti gli altri errori vengono
     * rilanciati normalmente.
     *
     * @return [Flow] che emette il [ThemeMode] corrente ad ogni aggiornamento.
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
     *
     * La scrittura avviene su un dispatcher IO gestito internamente da DataStore,
     * quindi questa funzione va chiamata da una coroutine (es. da un ViewModel).
     *
     * @param theme Il [ThemeMode] da salvare.
     */
    suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
}
