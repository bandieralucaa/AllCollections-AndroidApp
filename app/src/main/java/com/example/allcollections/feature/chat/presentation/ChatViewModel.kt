package com.example.allcollections.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allcollections.data.model.ChatMessage
import com.example.allcollections.feature.chat.data.ChatPreview
import com.example.allcollections.feature.chat.data.ChatRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
) : ViewModel() {

    private val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _recentChats = MutableStateFlow<List<ChatPreview>>(emptyList())
    val recentChats: StateFlow<List<ChatPreview>> = _recentChats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val unreadMessagesCount: StateFlow<Int> = _recentChats
        .map { chats -> chats.sumOf { it.unreadCount } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    /**
     * Osserva i messaggi di una chat specifica
     */
    fun observeMessages(otherUserId: String) {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            repository.resetUnreadCount(currentUserId, otherUserId)
            repository.getMessages(currentUserId, otherUserId).collect { list ->
                _messages.value = list
                repository.markMessagesAsRead(currentUserId, otherUserId)
                _isLoading.value = false
            }
        }
    }

    /**
     * Osserva tutte le chat recenti dell'utente loggato
     */
    fun observeRecentChats() {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            repository.getRecentChats(currentUserId).collect { list ->
                _recentChats.value = list
            }
        }
    }

    /**
     * Invia un messaggio
     */
    fun sendMessage(otherUserId: String, text: String) {
        if (text.isBlank() || currentUserId.isEmpty()) return

        val message = ChatMessage.create(
            senderId = currentUserId,
            receiverId = otherUserId,
            text = text
        )

        viewModelScope.launch {
            repository.sendMessage(message)
        }
    }

    /**
     * Elimina l'intera chat con un utente
     */
    fun deleteChat(otherUserId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteChat(currentUserId, otherUserId)
            onComplete()
        }
    }

    /**
     * Pulisce i dati quando si esce da una chat
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }
}