package com.example.allcollections.core.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allcollections.data.repository.ThemeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Stato UI del tema dell'applicazione.
 *
 * Wrapper semplice del [ThemeMode] corrente, utilizzato per uniformare
 * la gestione dello stato UI con gli altri ViewModel dell'app.
 *
 * @property theme Modalità tema attualmente attiva (chiaro, scuro, sistema).
 */
data class ThemeState(val theme: ThemeMode)

/**
 * ViewModel per la gestione del tema dell'applicazione (chiaro, scuro, sistema).
 *
 * Si interfaccia con [ThemeRepository] per leggere e salvare la preferenza
 * dell'utente in DataStore. Espone uno [StateFlow] osservabile che la UI può
 * utilizzare per applicare il tema e per mostrare l'opzione selezionata
 * nella schermata di scelta tema.
 *
 * ### Comportamento del flusso
 * - Il flusso [state] viene inizializzato con [ThemeMode.System] come valore
 *   predefinito, in attesa che DataStore emetta il valore persistito.
 * - Utilizza [SharingStarted.WhileSubscribed] con un timeout di 5 secondi:
 *   la sottoscrizione rimane attiva durante le riconfigurazioni dell'Activity
 *   (es. rotazione schermo) ma viene cancellata se nessun osservatore rimane
 *   per più di 5 secondi (ottimizzazione delle risorse).
 *
 * @param repository Repository che gestisce la persistenza del tema (DataStore).
 * @see ThemeRepository
 * @see ThemeMode
 * @see ThemeState
 */
class ThemeViewModel(
    private val repository: ThemeRepository
) : ViewModel() {

    /**
     * Stato osservabile del tema corrente.
     *
     * La UI dovrebbe collezionare questo flusso per applicare il tema
     * (usando `DynamicColor` o `MaterialTheme`) e per evidenziare
     * l'opzione attiva nella schermata delle impostazioni.
     *
     * Esempio di utilizzo in un composable:
     * ```
     * val themeState by viewModel.state.collectAsState()
     * // themeState.theme contiene il ThemeMode corrente
     * ```
     */
    val state: StateFlow<ThemeState> = repository.theme
        .map { theme -> ThemeState(theme) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeState(ThemeMode.System)
        )

    /**
     * Cambia il tema dell'applicazione e lo persiste in modo permanente.
     *
     * La modifica viene salvata su DataStore tramite [ThemeRepository.setTheme]
     * e automaticamente propagata a tutti gli osservatori di [state].
     *
     * @param theme Nuova modalità tema da applicare (Light, Dark, System).
     */
    fun changeTheme(theme: ThemeMode) = viewModelScope.launch {
        repository.setTheme(theme)
    }
}