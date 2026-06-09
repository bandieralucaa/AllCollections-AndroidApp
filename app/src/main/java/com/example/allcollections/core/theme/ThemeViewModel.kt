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
 * Wrappa il [ThemeMode] corrente in una data class per uniformità
 * con gli altri stati UI dell'app.
 *
 * @property theme Modalità tema attualmente attiva.
 */
data class ThemeState(val theme: ThemeMode)

/**
 * ViewModel per la gestione del tema dell'app.
 *
 * Legge la preferenza tema da [ThemeRepository] e la espone come [StateFlow],
 * così che la UI si aggiorni automaticamente ad ogni cambio senza polling.
 * Utilizza [SharingStarted.WhileSubscribed] con timeout di 5 secondi per
 * non cancellare la sottoscrizione durante le riconfigurazioni di Activity.
 *
 * @param repository Repository da cui leggere e salvare la preferenza tema.
 */
class ThemeViewModel(
    private val repository: ThemeRepository
) : ViewModel() {

    /**
     * Stato osservabile del tema corrente.
     *
     * Inizia con [ThemeMode.System] come valore di default finché DataStore
     * non ha emesso il valore persistito.
     */
    val state: StateFlow<ThemeState> = repository.theme
        .map { theme -> ThemeState(theme) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeState(ThemeMode.System)
        )

    /**
     * Cambia il tema dell'applicazione e lo persiste tramite [ThemeRepository].
     *
     * @param theme Nuovo [ThemeMode] da applicare.
     */
    fun changeTheme(theme: ThemeMode) = viewModelScope.launch {
        repository.setTheme(theme)
    }
}
