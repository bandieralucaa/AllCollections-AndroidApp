package com.example.allcollections.feature.notification.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allcollections.data.model.Notification
import com.example.allcollections.feature.notification.data.NotificationRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel per la gestione delle notifiche dell'utente corrente.
 *
 * Osserva in tempo reale le notifiche su Firestore, le arricchisce con i dati del mittente
 * e espone lo stato delle notifiche non lette. Gestisce l'invio di notifiche per
 * follow, commenti, like e nuovi oggetti tramite il [NotificationRepository].
 *
 * @param repository Repository che gestisce la logica di persistenza e invio notifiche.
 * @see NotificationRepository
 * @see Notification
 */
class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val userId: String?
        get() = Firebase.auth.currentUser?.uid

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())

    /**
     * Flusso osservabile delle notifiche dell'utente corrente, arricchite con i dati del mittente.
     * Aggiornato in tempo reale da Firestore.
     */
    val notifications: StateFlow<List<Notification>> = _notifications
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Indica se esiste almeno una notifica non letta.
     * Utile per mostrare un badge nella UI.
     */
    val hasUnreadNotifications: StateFlow<Boolean> = notifications
        .map { list -> list.any { !it.read } }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private var notificationJob: Job? = null

    init {
        observeNotifications()
    }

    /**
     * Avvia l'osservazione delle notifiche in tempo reale per l'utente corrente.
     * Se l'utente non è autenticato, non fa nulla.
     * In caso di eccezione (es. permessi), logga l'errore ma non interrompe il flusso.
     */
    private fun observeNotifications() {
        val currentUserId = userId ?: return
        notificationJob?.cancel()
        notificationJob = viewModelScope.launch {
            try {
                repository.observeNotifications(currentUserId).collect { rawNotifications ->
                    val enriched = repository.enrichWithSenders(rawNotifications)
                    _notifications.value = enriched
                }
            } catch (e: Exception) {
                // Se è un errore di permessi, ignoriamo silenziosamente (dopo logout)
                if (e is FirebaseFirestoreException &&
                    e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    // Non fare nulla
                } else {
                    Log.e("NotificationViewModel", "Errore durante osservazione notifiche: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Arresta l'osservazione delle notifiche e cancella la lista corrente.
     * Utile quando l'utente si disconnette.
     */
    fun stopObserving() {
        notificationJob?.cancel()
        notificationJob = null
        _notifications.value = emptyList()
    }

    /**
     * Invia una notifica di follow a un utente.
     *
     * @param recipientId ID dell'utente che riceve il follow.
     */
    fun sendFollowNotification(recipientId: String) {
        val senderId = userId ?: return
        viewModelScope.launch {
            repository.sendFollowNotification(recipientId, senderId)
        }
    }

    /**
     * Invia una notifica di commento su una collezione.
     *
     * @param recipientId ID del proprietario della collezione.
     * @param collectionId ID della collezione commentata.
     * @param collectionName Nome della collezione.
     * @param commentText Testo del commento (opzionale, mostrato nella notifica).
     */
    fun sendCommentNotification(
        recipientId: String,
        collectionId: String,
        collectionName: String,
        commentText: String? = null
    ) {
        val senderId = userId ?: return
        viewModelScope.launch {
            repository.sendCommentNotification(
                recipientId = recipientId,
                senderId = senderId,
                collectionId = collectionId,
                collectionName = collectionName,
                commentText = commentText
            )
        }
    }

    /**
     * Invia una notifica di commento su un singolo oggetto di una collezione.
     *
     * @param recipientId ID del proprietario della collezione.
     * @param collectionId ID della collezione.
     * @param collectionName Nome della collezione.
     * @param itemId ID dell'oggetto commentato.
     * @param itemDescription Descrizione dell'oggetto (opzionale, mostrata nella notifica).
     * @param commentText Testo del commento (opzionale).
     */
    fun sendItemCommentNotification(
        recipientId: String,
        collectionId: String,
        collectionName: String,
        itemId: String,
        itemDescription: String?,
        commentText: String? = null
    ) {
        val senderId = userId ?: return
        viewModelScope.launch {
            repository.sendItemCommentNotification(
                recipientId = recipientId,
                senderId = senderId,
                collectionId = collectionId,
                collectionName = collectionName,
                itemId = itemId,
                itemDescription = itemDescription,
                commentText = commentText
            )
        }
    }

    /**
     * Segna una notifica come letta.
     *
     * @param notificationId ID della notifica da marcare come letta.
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }

    /**
     * Elimina tutte le notifiche dell'utente corrente.
     */
    fun deleteAll() {
        val currentUserId = userId ?: return
        viewModelScope.launch {
            repository.deleteAll(currentUserId)
        }
    }

    /**
     * Invia una notifica di like su una collezione.
     *
     * @param recipientId ID del proprietario della collezione.
     * @param collectionId ID della collezione ricevente il like.
     * @param collectionName Nome della collezione.
     */
    fun sendLikeNotification(
        recipientId: String,
        collectionId: String,
        collectionName: String
    ) {
        val senderId = userId ?: return
        viewModelScope.launch {
            repository.sendLikeNotification(
                recipientId = recipientId,
                senderId = senderId,
                collectionId = collectionId,
                collectionName = collectionName
            )
        }
    }

    /**
     * Invia una notifica di nuovo oggetto aggiunto a una collezione.
     * Tipicamente inviata a tutti gli utenti che hanno messo like alla collezione.
     *
     * @param recipientId ID dell'utente da notificare.
     * @param collectionId ID della collezione che ha ricevuto il nuovo oggetto.
     * @param collectionName Nome della collezione.
     */
    fun sendNewItemNotification(
        recipientId: String,
        collectionId: String,
        collectionName: String
    ) {
        val senderId = userId ?: return
        viewModelScope.launch {
            repository.sendNewItemNotification(
                recipientId = recipientId,
                senderId = senderId,
                collectionId = collectionId,
                collectionName = collectionName
            )
        }
    }

    /**
     * Cleanup: cancella il job di osservazione quando il ViewModel viene distrutto.
     */
    override fun onCleared() {
        super.onCleared()
        stopObserving()
    }
}