package com.example.allcollections.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette colori principale dell'app AllCollections.
 *
 * I colori sono organizzati in due set — chiaro (`Light`) e scuro (`Dark`) —
 * e seguono le linee guida Material Design 3. Vengono referenziati in [Theme.kt]
 * per costruire gli schemi colore di Material 3.
 *
 * ### Convenzione di nomenclatura
 * ```
 * <Ruolo><Tema>
 * ```
 * - `Ruolo`: Primary, Secondary, Tertiary, Background, Surface, OnPrimary, OnSecondary, OnBackground, OnSurface, Error.
 * - `Tema`: Light o Dark.
 *
 * Esempio: `PrimaryLight` = colore primario per il tema chiaro.
 *
 * @see Theme.kt
 * @see androidx.compose.material3.ColorScheme
 */

// ─────────── Light Theme ───────────

/** Colore primario per il tema chiaro (blu). */
val PrimaryLight = Color(0xFF1E88E5)

/** Colore secondario per il tema chiaro (blu più scuro). */
val SecondaryLight = Color(0xFF0288D1)

/** Colore terziario per il tema chiaro (verde acqua per accenti). */
val TertiaryLight = Color(0xFF26A69A)

/** Colore di sfondo per il tema chiaro (grigio chiaro neutro). */
val BackgroundLight = Color(0xFFF5F5F5)

/** Colore delle superfici (card, dialog, ecc.) per il tema chiaro (bianco). */
val SurfaceLight = Color(0xFFFFFFFF)

/** Colore del testo sopra il colore primario nel tema chiaro (bianco). */
val OnPrimaryLight = Color(0xFFFFFFFF)

/** Colore del testo sopra il colore secondario nel tema chiaro (bianco). */
val OnSecondaryLight = Color(0xFFFFFFFF)

/** Colore del testo sopra lo sfondo nel tema chiaro (nero). */
val OnBackgroundLight = Color(0xFF000000)

/** Colore del testo sopra le superfici nel tema chiaro (nero). */
val OnSurfaceLight = Color(0xFF000000)

/** Colore per indicare errori nel tema chiaro (rosso). */
val ErrorLight = Color(0xFFD32F2F)

// ─────────── Dark Theme ───────────

/** Colore primario per il tema scuro (blu chiaro, si staglia sullo sfondo scuro). */
val PrimaryDark = Color(0xFF90CAF9)

/** Colore secondario per il tema scuro (blu più scuro ma comunque leggibile). */
val SecondaryDark = Color(0xFF64B5F6)

/** Colore terziario per il tema scuro (verde acqua brillante). */
val TertiaryDark = Color(0xFF4DB6AC)

/** Sfondo principale per il tema scuro (grigio scuro Material standard). */
val BackgroundDark = Color(0xFF121212)

/** Superfici elevate (card, menu) nel tema scuro (grigio leggermente più chiaro). */
val SurfaceDark = Color(0xFF1E1E1E)

/** Colore del testo sopra il colore primario nel tema scuro (nero, per contrasto). */
val OnPrimaryDark = Color(0xFF000000)

/** Colore del testo sopra il colore secondario nel tema scuro (nero). */
val OnSecondaryDark = Color(0xFF000000)

/** Colore del testo sopra lo sfondo scuro (bianco). */
val OnBackgroundDark = Color(0xFFFFFFFF)

/** Colore del testo sopra le superfici scure (bianco). */
val OnSurfaceDark = Color(0xFFFFFFFF)

/** Colore per indicare errori nel tema scuro (rosso chiaro). */
val ErrorDark = Color(0xFFEF5350)