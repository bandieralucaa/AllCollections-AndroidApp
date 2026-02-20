package com.example.allcollections.feature.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allcollections.feature.notification.data.NotificationRepository
import com.example.allcollections.feature.notification.domain.Notification
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val userId: String?
        get() = Firebase.auth.currentUser?.uid

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val hasUnreadNotifications = combine(_notifications) { list ->
        list.firstOrNull()?.any { !it.read } ?: false
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private var notificationJob: Job? = null

    init {
        observeNotifications()
    }

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
                // Ignora errori di permessi
            }
        }
    }

    fun stopObserving() {
        notificationJob?.cancel()
        notificationJob = null
        _notifications.value = emptyList()
    }

    fun sendFollowNotification(recipientId: String) {
        val senderId = userId ?: return
        viewModelScope.launch {
            repository.sendFollowNotification(recipientId, senderId)
        }
    }

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

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }

    fun deleteAll() {
        val currentUserId = userId ?: return
        viewModelScope.launch {
            repository.deleteAll(currentUserId)
        }
    }

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
}