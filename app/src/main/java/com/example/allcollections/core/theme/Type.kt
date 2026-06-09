package com.example.allcollections.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Scala tipografica dell'app AllCollections.
 *
 * Segue le specifiche Material Design 3 per `Typography`.
 * Usa [FontFamily.SansSerif] (Inter su dispositivi moderni) con dimensioni
 * e `lineHeight` ottimizzate per la leggibilità su mobile.
 *
 * Referenziato in [AllCollectionsTheme] come parametro `typography` di [MaterialTheme].
 */
val Typography = Typography(

    // ─────────── Body — testi principali ───────────

    /** Testo principale, usato per descrizioni e contenuti lunghi. */
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    ),

    /** Testo secondario, usato per sottotitoli e dettagli. */
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),

    /** Testo di supporto, usato per caption, label di input e hint. */
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp
    ),

    // ─────────── Titles — titoli e intestazioni ───────────

    /** Titolo grande, usato nelle TopBar e nelle intestazioni di schermata. */
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    /** Titolo medio, usato per nomi di sezione e card header. */
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),

    /** Titolo piccolo, usato per label e etichette di categoria. */
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)
