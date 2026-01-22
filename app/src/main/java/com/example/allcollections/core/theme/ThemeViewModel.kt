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
 * Stato osservabile del tema dell'applicazione.
 *
 * Contiene solo la modalità tema corrente (Light / Dark / System).
 */
data class ThemeState(val theme: ThemeMode)

/**
 * ViewModel per la gestione del tema dell'app.
 *
 * Interagisce con ThemeRepository per leggere e salvare la preferenza utente.
 */
class ThemeViewModel(
    private val repository: ThemeRepository
) : ViewModel() {

    /**
     * Stato osservabile del tema.
     *
     * StateFlow aggiornato automaticamente quando il repository cambia.
     */
    val state: StateFlow<ThemeState> = repository.theme
        .map { theme -> ThemeState(theme) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000), // Timeout 5 secondi
            initialValue = ThemeState(ThemeMode.System)
        )

    /**
     * Cambia il tema dell'applicazione e lo salva nel repository.
     *
     * @param theme Nuovo tema da applicare.
     */
    fun changeTheme(theme: ThemeMode) = viewModelScope.launch {
        repository.setTheme(theme)
    }

    /**
     * Toggle tra tema chiaro e scuro.
     *
     * - Light -> Dark
     * - Dark -> Light
     * - System -> Dark (default per toggle)
     */
    fun toggleTheme() = viewModelScope.launch {
        val current = state.value.theme
        val newTheme = when (current) {
            ThemeMode.Light -> ThemeMode.Dark
            ThemeMode.Dark -> ThemeMode.Light
            ThemeMode.System -> ThemeMode.Dark
        }
        repository.setTheme(newTheme)
    }
}
