package com.example.allcollections.feature.notification.components

import com.example.allcollections.data.model.UserData
import java.util.Date

/**
 * Modello dati per una notifica dell'applicazione.
 * Contiene tutte le informazioni necessarie per visualizzare una notifica.
 * Supporta sia notifiche interne (follow, comment) che notifiche push.
 */
data class NotificationItem(
    val notificationId: String,
    val user: UserData,
    val timestamp: Date,
    val read: Boolean,
    val type: String, // "follow", "comment", "push_general", "push_new_item", "push_new_comment"
    val collectionId: String? = null,
    val collectionName: String? = null,
    val commentText: String? = null,
    // Campi specifici per notifiche push (possono essere null per notifiche interne)
    val pushTitle: String? = null,        // Titolo specifico per push
    val pushMessage: String? = null,      // Messaggio completo per push
    val isPushNotification: Boolean = false // Flag per identificare notifiche push
) {
    /** Tipo di notifica */
    val isComment: Boolean get() = type == "comment"
    val isFollow: Boolean get() = type == "follow"
    val isPush: Boolean get() = isPushNotification || type.startsWith("push_")
    val isUnread: Boolean get() = !read

    // Distinzione tra tipi di push
    val isPushGeneral: Boolean get() = type == TYPE_PUSH_GENERAL
    val isPushNewItem: Boolean get() = type == TYPE_PUSH_NEW_ITEM
    val isPushNewComment: Boolean get() = type == TYPE_PUSH_NEW_COMMENT

    /** Titolo da mostrare nella card della notifica */
    val title: String
        get() = when {
            isPush && pushTitle != null -> pushTitle
            isComment -> "${user.username} ha commentato"
            isFollow -> "${user.username} ti segue"
            else -> pushTitle ?: "Nuova notifica"
        }

    /** Messaggio da mostrare nella card della notifica */
    val message: String
        get() = when {
            isPush && pushMessage != null -> pushMessage
            isComment && collectionName != null ->
                commentText?.take(50)?.let { "$it..." } ?: "Ha commentato: $collectionName"
            isComment -> commentText?.take(50)?.let { "$it..." } ?: "Nuovo commento"
            isFollow -> "Ora segue le tue collezioni"
            else -> pushMessage ?: "Nuova attività"
        }

    companion object {
        /** Crea una notifica vuota di placeholder */
        fun empty() = NotificationItem(
            notificationId = "",
            user = UserData.empty(),
            timestamp = Date(),
            read = true,
            type = "follow"
        )

        // Costanti per tipi di notifica
        const val TYPE_FOLLOW = "follow"
        const val TYPE_COMMENT = "comment"
        const val TYPE_PUSH_GENERAL = "push_general"
        const val TYPE_PUSH_NEW_ITEM = "push_new_item"
        const val TYPE_PUSH_NEW_COMMENT = "push_new_comment"
    }
}

/** Estensione per sapere se la notifica è recente (ultime 24 ore) */
val NotificationItem.isRecent: Boolean
    get() {
        val now = Date()
        val dayInMillis = 24 * 60 * 60 * 1000L
        return now.time - timestamp.time < dayInMillis
    }

/** Estensione per ottenere tempo relativo (es. "5 min fa", "2 ore fa") */
val NotificationItem.relativeTime: String
    get() {
        val now = Date()
        val diff = now.time - timestamp.time
        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        val days = diff / (24 * 60 * 60 * 1000)

        return when {
            minutes < 1 -> "Adesso"
            minutes < 60 -> "$minutes min fa"
            hours < 24 -> "$hours ore fa"
            days < 7 -> "$days giorni fa"
            else -> {
                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                dateFormat.format(timestamp)
            }
        }
    }

/** Estensione per icona in base al tipo di notifica */
val NotificationItem.iconResId: Int
    get() = when {
        isFollow -> android.R.drawable.ic_menu_my_calendar
        isComment -> android.R.drawable.ic_menu_edit
        isPushNewItem -> android.R.drawable.ic_menu_add
        isPushNewComment -> android.R.drawable.ic_menu_today
        else -> android.R.drawable.ic_dialog_info
    }