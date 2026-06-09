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
 * ViewModel per la gestione delle chat dell'utente corrente.
 *
 * Espone tre flussi osservabili:
 * - [messages] – messaggi della chat attualmente aperta (con un altro utente)
 * - [recentChats] – anteprime delle conversazioni recenti (chat preview)
 * - [unreadMessagesCount] – totale dei messaggi non letti (utile per badge UI)
 *
 * Delega tutte le operazioni su Firestore a [ChatRepository].
 * I flussi vengono aggiornati in tempo reale grazie ai listener Firestore.
 *
 * @param repository Repository per l'accesso ai dati delle chat.
 * @see ChatRepository
 * @see ChatMessage
 * @see ChatPreview
 */
class ChatViewModel(
    private val repository: ChatRepository,
) : ViewModel() {

    // ID dell'utente autenticato corrente (vuoto se non loggato)
    private val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    /** Flusso dei messaggi della chat attualmente aperta (ordinati cronologicamente). */
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _recentChats = MutableStateFlow<List<ChatPreview>>(emptyList())
    /** Flusso delle anteprime delle conversazioni recenti (ordinate per ultimo messaggio). */
    val recentChats: StateFlow<List<ChatPreview>> = _recentChats

    private val _isLoading = MutableStateFlow(false)
    /** Indica se i messaggi di una chat sono in fase di caricamento iniziale. */
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Numero totale di messaggi non letti in tutte le conversazioni.
     * Derivato automaticamente da [recentChats] (somma di [ChatPreview.unreadCount]).
     * Utile per mostrare un badge sulla bottom bar o sulla schermata chat.
     */
    val unreadMessagesCount: StateFlow<Int> = _recentChats
        .map { chats -> chats.sumOf { it.unreadCount } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    /**
     * Avvia l'osservazione in tempo reale dei messaggi tra l'utente corrente e [otherUserId].
     *
     * Questo metodo:
     * 1. Resetta il contatore dei non letti per questa conversazione.
     * 2. Avvia la raccolta del flusso dei messaggi (ordinati per timestamp).
     * 3. Quando arrivano nuovi messaggi, li marca automaticamente come letti.
     *
     * @param otherUserId ID dell'altro utente della conversazione.
     */
    fun observeMessages(otherUserId: String) {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            // Resetta il contatore non letti per questa conversazione prima di iniziare
            repository.resetUnreadCount(currentUserId, otherUserId)
            repository.getMessages(currentUserId, otherUserId).collect { list ->
                _messages.value = list
                // Marca come letti tutti i messaggi ricevuti dall'altro utente
                repository.markMessagesAsRead(currentUserId, otherUserId)
                _isLoading.value = false
            }
        }
    }

    /**
     * Avvia l'osservazione in tempo reale delle chat recenti (anteprime conversazioni).
     *
     * Il flusso [recentChats] si aggiorna automaticamente quando:
     * - Viene ricevuto un nuovo messaggio
     * - Un messaggio viene letto (cambia unreadCount)
     * - Una conversazione viene eliminata
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
     * Invia un messaggio di testo a un altro utente.
     *
     * Il messaggio viene creato tramite [ChatMessage.create], che imposta automaticamente
     * timestamp, let=false, e genera un ID temporaneo.
     *
     * @param otherUserId ID del destinatario.
     * @param text Testo del messaggio (non può essere vuoto o solo spazi).
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
     * Elimina l'intera conversazione con [otherUserId] per l'utente corrente.
     *
     * L'eliminazione può essere:
     * - Logica (se l'altro utente non ha ancora eliminato, i messaggi vengono solo nascosti per l'utente corrente)
     * - Fisica (se entrambi hanno eliminato, i messaggi vengono rimossi da Firestore)
     *
     * @param otherUserId ID dell'altro utente.
     * @param onComplete Callback invocato al termine dell'operazione (indipendentemente dal successo).
     * @see ChatRepository.deleteChat
     */
    fun deleteChat(otherUserId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteChat(currentUserId, otherUserId)
            onComplete()
        }
    }

    /**
     * Svuota la lista dei messaggi attualmente in [messages].
     *
     * Questo metodo dovrebbe essere chiamato quando si esce dalla schermata di chat
     * (ad esempio in un `DisposableEffect`), per evitare che la prossima apertura
     * mostri brevemente i vecchi messaggi prima che il nuovo flusso venga raccolto.
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }
}