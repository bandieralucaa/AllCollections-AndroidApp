package com.example.allcollections.core.theme

/**
 * Modalità tema disponibili nell'app AllCollections.
 *
 * Queste modalità permettono all'utente di scegliere l'aspetto visivo dell'app:
 * - [Light]: tema chiaro fisso
 * - [Dark]: tema scuro fisso
 * - [System]: segue automaticamente il tema del sistema operativo
 *
 * Le preferenze vengono salvate tramite [ThemeRepository] e osservate da [ThemeViewModel]
 * per aggiornare dinamicamente la UI.
 *
 * @see ThemeRepository
 * @see ThemeViewModel
 */
enum class ThemeMode {

    /** Forza la modalità chiara indipendentemente dalle impostazioni di sistema. */
    Light,

    /** Forza la modalità scura indipendentemente dalle impostazioni di sistema. */
    Dark,

    /** Segue automaticamente le impostazioni tema del sistema operativo. */
    System;

    /**
     * Descrizione leggibile del tema, utilizzata in tooltip, testi di aiuto o
     * nella schermata di scelta tema per spiegare all'utente l'effetto di ogni opzione.
     *
     * @return Stringa descrittiva localizzata (in italiano, adatta all'UI).
     */
    val description: String
        get() = when (this) {
            Light -> "Tema chiaro per ambienti luminosi"
            Dark -> "Tema scuro per ridurre l'affaticamento degli occhi"
            System -> "Segue le impostazioni tema del tuo dispositivo"
        }

    companion object {
        /**
         * Converte una stringa nel corrispondente [ThemeMode] (case‑insensitive).
         *
         * Supporta sia i nomi inglesi (`"light"`, `"dark"`, `"system"`) sia
         * le traduzioni italiane (`"chiaro"`, `"scuro"`, `"sistema"`).
         * Se la stringa è `null` o non corrisponde a nessun valore riconosciuto,
         * ritorna [System] come fallback sicuro.
         *
         * @param name Stringa da convertire (può essere `null`).
         * @return Il [ThemeMode] corrispondente, o [System] se non riconosciuto.
         *
         * @sample
         * ThemeMode.fromString("dark") // Dark
         * ThemeMode.fromString("SCURO") // Dark
         * ThemeMode.fromString("qualunque") // System
         */
        fun fromString(name: String?): ThemeMode = when (name?.lowercase()) {
            "light", "chiaro" -> Light
            "dark", "scuro" -> Dark
            "system", "sistema" -> System
            else -> System
        }

        /**
         * Restituisce la lista completa dei temi disponibili nell'ordine di visualizzazione
         * consigliato per la UI (Light, Dark, System).
         *
         * @return Lista immutabile di tutti i valori [ThemeMode].
         */
        fun all(): List<ThemeMode> = listOf(Light, Dark, System)
    }
}