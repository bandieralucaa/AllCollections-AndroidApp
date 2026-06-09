package com.example.allcollections.feature.notification.domain

/**
 * Tipi di notifica supportati dall'app.
 *
 * Ogni valore corrisponde alla stringa salvata su Firestore nel campo "type".
 * Il tipo GENERAL è usato come fallback per valori non riconosciuti.
 */
enum class NotificationType(val value: String) {
    FOLLOW("follow"),
    COMMENT("comment"),
    ITEM_COMMENT("item_comment"),
    LIKE("like"),
    NEW_ITEM("new_item"),
    GENERAL("general");

    companion object {
        fun fromString(value: String?): NotificationType {
            return entries.find { it.value == value } ?: GENERAL
        }
    }
}