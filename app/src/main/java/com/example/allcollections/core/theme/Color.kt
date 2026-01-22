package com.example.allcollections.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette principale dell'app AllCollections.
 * Pensata per leggibilità, stile moderno e coerenza tra light/dark mode.
 */

// ─────────── Light Theme Colors ───────────
val PrimaryLight = Color(0xFF1E88E5)       // Blu principale moderno
val SecondaryLight = Color(0xFF0288D1)     // Blu secondario
val TertiaryLight = Color(0xFF26A69A)      // Verde acqua per accenti
val BackgroundLight = Color(0xFFF5F5F5)    // Grigio chiaro, neutro
val SurfaceLight = Color(0xFFFFFFFF)       // Bianco per card e superfici
val OnPrimaryLight = Color(0xFFFFFFFF)     // Colore testo sopra primary
val OnSecondaryLight = Color(0xFFFFFFFF)   // Colore testo sopra secondary
val OnBackgroundLight = Color(0xFF000000)  // Testo su sfondo chiaro
val OnSurfaceLight = Color(0xFF000000)     // Testo su surface chiaro
val ErrorLight = Color(0xFFD32F2F)         // Rosso per errori

// ─────────── Dark Theme Colors ───────────
val PrimaryDark = Color(0xFF90CAF9)        // Blu chiaro su dark
val SecondaryDark = Color(0xFF64B5F6)      // Blu secondario
val TertiaryDark = Color(0xFF4DB6AC)       // Verde acqua
val BackgroundDark = Color(0xFF121212)     // Sfondo scuro
val SurfaceDark = Color(0xFF1E1E1E)        // Superfici scure
val OnPrimaryDark = Color(0xFF000000)      // Testo sopra primary dark
val OnSecondaryDark = Color(0xFF000000)    // Testo sopra secondary dark
val OnBackgroundDark = Color(0xFFFFFFFF)   // Testo su background scuro
val OnSurfaceDark = Color(0xFFFFFFFF)      // Testo su surface scuro
val ErrorDark = Color(0xFFEF5350)          // Rosso per errori

// ─────────── Accent Colors ───────────
val AccentOrangeLight = Color(0xFFFFB74D)  // CTA o highlights light
val AccentOrangeDark = Color(0xFFFFA726)   // CTA o highlights dark
val SuccessGreenLight = Color(0xFF66BB6A)  // Azioni riuscite / positivo
val SuccessGreenDark = Color(0xFF81C784)   // Azioni riuscite dark
val WarningYellowLight = Color(0xFFFFF176) // Avvisi / warning
val WarningYellowDark = Color(0xFFFFEE58)  // Avvisi dark
