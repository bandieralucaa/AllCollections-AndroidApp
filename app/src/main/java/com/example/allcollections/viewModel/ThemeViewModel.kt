package com.example.allcollections.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allcollections.apptheme.ThemeMode
import com.example.allcollections.repositories.ThemeRepository
import com.example.allcollections.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThemeState(val theme: ThemeMode)

class ThemeViewModel(
    private val repository: ThemeRepository
) : ViewModel() {
    val state = repository.theme.map { ThemeState(it) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = ThemeState(ThemeMode.System)
    )

    fun changeTheme(theme: ThemeMode) = viewModelScope.launch {
        repository.setTheme(theme)
    }
}