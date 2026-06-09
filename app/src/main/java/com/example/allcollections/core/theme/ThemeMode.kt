package com.example.allcollections.core.theme

/**
 * Modalità tema disponibili nell'app AllCollections.
 *
 * Usato da [ThemeRepository] per la persistenza e da [ThemeViewModel]
 * per esporre lo stato alla UI.
 */
enum class ThemeMode {

    /** Forza la modalità chiara indipendentemente dalle impostazioni di sistema. */
    Light,

    /** Forza la modalità scura indipendentemente dalle impostazioni di sistema. */
    Dark,

    /** Segue automaticamente le impostazioni tema del sistema operativo. */
    System;

    /**
     * Descrizione leggibile del tema, usata in tooltip o testi di aiuto nella UI.
     */
    val description: String
        get() = when (this) {
            Light -> "Tema chiaro per ambienti luminosi"
            Dark -> "Tema scuro per ridurre l'affaticamento degli occhi"
            System -> "Segue le impostazioni tema del tuo dispositivo"
        }

    companion object {

        /**
         * Converte una stringa nel corrispondente [ThemeMode] (case-insensitive).
         *
         * Accetta sia i valori in inglese (es. `"dark"`) che in italiano (es. `"scuro"`).
         * Se la stringa è `null` o non riconosciuta, ritorna [System] come fallback.
         *
         * @param name Stringa da convertire.
         * @return Il [ThemeMode] corrispondente, o [System] se non riconosciuto.
         */
        fun fromString(name: String?): ThemeMode = when (name?.lowercase()) {
            "light", "chiaro" -> Light
            "dark", "scuro" -> Dark
            "system", "sistema" -> System
            else -> System
        }

        /**
         * Restituisce la lista completa dei temi disponibili nell'ordine di visualizzazione.
         *
         * @return Lista ordinata di tutti i valori di [ThemeMode].
         */
        fun all(): List<ThemeMode> = listOf(Light, Dark, System)
    }
}
