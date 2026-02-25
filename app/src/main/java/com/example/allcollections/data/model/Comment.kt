package com.example.allcollections.data.model

import com.example.allcollections.core.utils.time.formatRelativeTime
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Modello dati per un commento su una collezione o su un singolo oggetto.
 * Usa Firebase Timestamp per ordinamento efficiente su Firestore.
 *
 * @property id ID del documento Firestore
 * @property collectionId ID della collezione a cui appartiene il commento
 * @property itemId ID dell'oggetto a cui appartiene il commento (null = commento sulla collezione)
 * @property userId ID dell'utente che ha scritto il commento
 * @property text Testo del commento
 * @property timestamp Data/ora di creazione (Firebase Timestamp)
 * @property username Nome utente per visualizzazione veloce senza query aggiuntive
 */
data class Comment(
    val id: String = "",
    val collectionId: String = "",
    val itemId: String = "",
    val userId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val username: String = ""
) {
    /** Verifica se il commento ha testo valido */
    val hasText: Boolean
        get() = text.isNotBlank()

    /** Data di creazione come oggetto Date */
    val createdAt: Date
        get() = timestamp.toDate()

    /** Verifica se il commento appartiene a un oggetto specifico */
    val isItemComment: Boolean
        get() = itemId.isNotBlank()

    /**
     * Restituisce un testo abbreviato per anteprime
     * @param maxLength Lunghezza massima (default: 100)
     */
    fun getShortText(maxLength: Int = 100): String {
        val safeMax = maxOf(3, maxLength)
        return if (text.length <= safeMax) text else text.take(safeMax - 3) + "..."
    }

    /** Verifica se il commento è recente (ultime 24 ore) */
    fun isRecent(): Boolean {
        val now = Timestamp.now()
        val dayInSeconds = 24 * 60 * 60L
        return now.seconds - timestamp.seconds < dayInSeconds
    }

    companion object {
        /** Commento vuoto / placeholder */
        fun empty(): Comment = Comment()

        /** Crea un nuovo commento sulla collezione, pronto per il salvataggio su Firestore */
        fun create(
            collectionId: String,
            userId: String,
            text: String,
            username: String = ""
        ): Comment = Comment(
            id = "",
            collectionId = collectionId,
            itemId = "",
            userId = userId,
            text = text.trim(),
            timestamp = Timestamp.now(),
            username = username
        )

        /** Crea un nuovo commento su un singolo oggetto, pronto per il salvataggio su Firestore */
        fun createForItem(
            collectionId: String,
            itemId: String,
            userId: String,
            text: String,
            username: String = ""
        ): Comment = Comment(
            id = "",
            collectionId = collectionId,
            itemId = itemId,
            userId = userId,
            text = text.trim(),
            timestamp = Timestamp.now(),
            username = username
        )
    }
}

/** Extension per validazione rapida prima del salvataggio */
val Comment.isValid: Boolean
    get() = collectionId.isNotBlank() && userId.isNotBlank() && text.isNotBlank() && text.length <= 500

/** Extension per ottenere display name ottimizzato per UI */
val Comment.displayName: String
    get() = username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Utente"

/** Extension per formattazione relativa della data (coerente con altre parti dell'app) */
val Comment.formattedTime: String
    get() = formatRelativeTime(timestamp.toDate())