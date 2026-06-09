package com.example.allcollections.feature.notification.domain

/**
 * Tipi di notifica supportati dall'app.
 *
 * Ogni valore corrisponde alla stringa salvata su Firestore nel campo "type".
 * Il tipo GENERAL è usato come fallback per valori non riconosciuti.
 *
 * @property value Stringa identificativa del tipo di notifica.
 */
enum class NotificationType(val value: String) {
    FOLLOW("follow"),
    COMMENT("comment"),
    ITEM_COMMENT("item_comment"),
    LIKE("like"),
    NEW_ITEM("new_item"),
    GENERAL("general");

    companion object {
        /**
         * Converte una stringa nel corrispondente [NotificationType].
         * Se la stringa è null o non corrisponde a nessun tipo, ritorna [GENERAL].
         *
         * @param value Stringa da convertire.
         * @return [NotificationType] corrispondente, o [GENERAL] come fallback.
         */
        fun fromString(value: String?): NotificationType {
            return entries.find { it.value == value } ?: GENERAL
        }
    }
}