package com.example.allcollections.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allcollections.data.model.ChatMessage
import com.example.allcollections.data.model.ChatPreview
import com.example.allcollections.feature.chat.data.ChatRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel per la gestione delle chat.
 *
 * Espone tre flussi osservabili:
 * - [messages] — messaggi della chat attualmente aperta.
 * - [recentChats] — lista delle conversazioni recenti con preview.
 * - [unreadMessagesCount] — totale messaggi non letti (usato per il badge nella bottom bar).
 *
 * Delega tutte le operazioni su Firestore a [ChatRepository].
 *
 * @param repository Repository per l'accesso ai dati delle chat.
 */
class ChatViewModel(
    private val repository: ChatRepository,
) : ViewModel() {

    private val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    /** Messaggi della chat attualmente aperta, in ordine cronologico ascendente. */
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _recentChats = MutableStateFlow<List<ChatPreview>>(emptyList())

    /** Lista delle conversazioni recenti, ordinata per messaggio più recente. */
    val recentChats: StateFlow<List<ChatPreview>> = _recentChats

    private val _isLoading = MutableStateFlow(false)

    /** `true` mentre i messaggi di una chat sono in caricamento. */
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Contatore totale dei messaggi non letti in tutte le conversazioni.
     * Derivato da [recentChats] tramite una mappatura lazy.
     */
    val unreadMessagesCount: StateFlow<Int> = _recentChats
        .map { chats -> chats.sumOf { it.unreadCount } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    /**
     * Avvia l'ascolto in tempo reale dei messaggi con [otherUserId].
     *
     * Azzera il contatore non letti e marca i messaggi come letti appena
     * arrivano. Non fa nulla se [currentUserId] è vuoto (utente non autenticato).
     *
     * @param otherUserId ID dell'altro utente della conversazione.
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
     * Avvia l'ascolto in tempo reale delle chat recenti dell'utente corrente.
     *
     * Non fa nulla se [currentUserId] è vuoto (utente non autenticato).
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
     * Invia un messaggio di testo a [otherUserId].
     *
     * Non fa nulla se il testo è vuoto o se l'utente non è autenticato.
     *
     * @param otherUserId ID del destinatario.
     * @param text Testo del messaggio da inviare.
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
     * Elimina la conversazione con [otherUserId].
     *
     * L'eliminazione può essere logica (solo per l'utente corrente) o fisica
     * (se anche l'altro utente ha già eliminato la chat). Vedi [ChatRepository.deleteChat].
     *
     * @param otherUserId ID dell'altro utente della conversazione da eliminare.
     * @param onComplete Callback invocato al completamento dell'eliminazione.
     */
    fun deleteChat(otherUserId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteChat(currentUserId, otherUserId)
            onComplete()
        }
    }

    /**
     * Svuota la lista dei messaggi correnti.
     *
     * Chiamato da [DisposableEffect] quando si esce dalla schermata chat,
     * evitando che i messaggi vecchi ricompaiano brevemente alla prossima apertura.
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }
}