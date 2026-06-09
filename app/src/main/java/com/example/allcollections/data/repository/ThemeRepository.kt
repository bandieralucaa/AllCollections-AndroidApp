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
 * Utilizza [DataStore] per leggere/scrivere il tema scelto dall'utente in modo
 * asincrono e sicuro. Espone un [Flow] osservabile così che la UI si aggiorni
 * automaticamente senza polling.
 *
 * ### Comportamento in caso di errore di I/O
 * Il flusso [theme] emette un valore di fallback (tema di sistema) e poi si
 * arresta permanentemente. Se desideri un ripristino automatico, considera
 * l'aggiunta di un operatore `.retry()` prima del `.catch`.
 *
 * @see ThemeMode per i valori supportati
 * @see androidx.datastore.preferences.core.Preferences
 *
 * @param dataStore Istanza di [DataStore] iniettata (es. tramite Koin in [AppModule]).
 */
class ThemeRepository(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        /** Chiave DataStore per salvare il tema. */
        private val THEME_KEY = stringPreferencesKey("app_theme")
    }

    /**
     * Flusso osservabile del tema corrente.
     *
     * In caso di [IOException] durante la lettura (es. file corrotto o permessi),
     * il flusso emette un [emptyPreferences] come fallback e poi termina.
     * Se il DataStore è sano, il flusso continua a emettere ogni aggiornamento.
     *
     * @return [Flow] che emette il [ThemeMode] corrente o il default ([ThemeMode.System]).
     */
    val theme: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                // Fallback: emetti preferenze vuote per evitare crash
                // Nota: il flusso finirà qui senza riprovare automaticamente
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ThemeMode.fromString(preferences[THEME_KEY])
        }

    /**
     * Salva in modo persistente il tema selezionato.
     *
     * La scrittura avviene su un dispatcher IO (gestito da DataStore).
     * Chiamare questa funzione da una coroutine (es. ViewModel) con `viewModelScope.launch`.
     *
     * @param theme Il [ThemeMode] da salvare.
     */
    suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
}