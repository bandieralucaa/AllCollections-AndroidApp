package com.example.allcollections.core.theme

/**
 * Modalità tema disponibili per l'app AllCollections.
 *
 * Definisce se l'app usa tema chiaro, scuro o segue le impostazioni di sistema.
 */
enum class ThemeMode {

    /** Tema chiaro (light mode) */
    Light,

    /** Tema scuro (dark mode) */
    Dark,

    /** Segue le impostazioni tema del sistema operativo */
    System;

    /** Nome leggibile dall'utente */
    val displayName: String
        get() = when (this) {
            Light -> "Chiaro"
            Dark -> "Scuro"
            System -> "Sistema"
        }

    /** Nome tecnico per salvataggio/preferenze (lowercase) */
    val technicalName: String
        get() = name.lowercase()

    /** Icona rappresentativa (può essere stringa o resource ID) */
    val iconName: String
        get() = when (this) {
            Light -> "sun"
            Dark -> "moon"
            System -> "settings"
        }

    /** Descrizione dettagliata per tooltip o help */
    val description: String
        get() = when (this) {
            Light -> "Tema chiaro per ambienti luminosi"
            Dark -> "Tema scuro per ridurre l'affaticamento degli occhi"
            System -> "Segue le impostazioni tema del tuo dispositivo"
        }

    companion object {
        /** Converte una stringa in ThemeMode (case-insensitive). Default = System */
        fun fromString(name: String?): ThemeMode = when (name?.lowercase()) {
            "light", "chiaro" -> Light
            "dark", "scuro" -> Dark
            "system", "sistema" -> System
            else -> System
        }

        /** Restituisce la lista completa dei temi disponibili */
        fun all(): List<ThemeMode> = listOf(Light, Dark, System)

        /** Verifica se una stringa rappresenta un tema valido */
        fun isValid(name: String?): Boolean =
            name?.lowercase() in listOf("light", "dark", "system", "chiaro", "scuro", "sistema")
    }
}

/** Tema opposto (solo per Light/Dark), null se System */
val ThemeMode.opposite: ThemeMode?
    get() = when (this) {
        ThemeMode.Light -> ThemeMode.Dark
        ThemeMode.Dark -> ThemeMode.Light
        ThemeMode.System -> null
    }

/** Verifica se il tema è fisso (non System) */
val ThemeMode.isFixed: Boolean
    get() = this != ThemeMode.System

/** Nome dello schema Material Design da usare */
val ThemeMode.materialSchemeName: String
    get() = when (this) {
        ThemeMode.Light -> "light"
        ThemeMode.Dark -> "dark"
        ThemeMode.System -> "dynamic"
    }
