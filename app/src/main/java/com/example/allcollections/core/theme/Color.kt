package com.example.allcollections.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette colori principale dell'app AllCollections.
 *
 * I colori sono organizzati in due set — light e dark — e seguono le linee guida
 * Material Design 3. Vengono referenziati in [Theme.kt] per costruire i [ColorScheme].
 *
 * Convenzione nomi: `<Ruolo><Tema>` (es. `PrimaryLight`, `ErrorDark`).
 */

// ─────────── Light Theme ───────────

val PrimaryLight = Color(0xFF1E88E5)        // Blu principale
val SecondaryLight = Color(0xFF0288D1)      // Blu secondario
val TertiaryLight = Color(0xFF26A69A)       // Verde acqua per accenti
val BackgroundLight = Color(0xFFF5F5F5)     // Grigio chiaro neutro
val SurfaceLight = Color(0xFFFFFFFF)        // Bianco per card e superfici
val OnPrimaryLight = Color(0xFFFFFFFF)      // Testo su primary light
val OnSecondaryLight = Color(0xFFFFFFFF)    // Testo su secondary light
val OnBackgroundLight = Color(0xFF000000)   // Testo su sfondo chiaro
val OnSurfaceLight = Color(0xFF000000)      // Testo su surface chiaro
val ErrorLight = Color(0xFFD32F2F)          // Rosso errore light

// ─────────── Dark Theme ───────────

val PrimaryDark = Color(0xFF90CAF9)         // Blu chiaro su sfondo scuro
val SecondaryDark = Color(0xFF64B5F6)       // Blu secondario dark
val TertiaryDark = Color(0xFF4DB6AC)        // Verde acqua dark
val BackgroundDark = Color(0xFF121212)      // Sfondo scuro standard Material
val SurfaceDark = Color(0xFF1E1E1E)         // Superfici elevate in dark mode
val OnPrimaryDark = Color(0xFF000000)       // Testo su primary dark
val OnSecondaryDark = Color(0xFF000000)     // Testo su secondary dark
val OnBackgroundDark = Color(0xFFFFFFFF)    // Testo su background scuro
val OnSurfaceDark = Color(0xFFFFFFFF)       // Testo su surface scuro
val ErrorDark = Color(0xFFEF5350)           // Rosso errore dark
