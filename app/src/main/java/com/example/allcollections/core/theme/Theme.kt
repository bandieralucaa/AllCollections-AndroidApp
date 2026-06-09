package com.example.allcollections.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─────────── Color Schemes ───────────

/** Schema colori per la modalità scura, costruito dalla palette in [Color.kt]. */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorDark,
    onPrimary = OnPrimaryDark,
    onSecondary = OnSecondaryDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark
)

/** Schema colori per la modalità chiara, costruito dalla palette in [Color.kt]. */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    tertiary = TertiaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = ErrorLight,
    onPrimary = OnPrimaryLight,
    onSecondary = OnSecondaryLight,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight
)

/**
 * Tema principale dell'app AllCollections.
 *
 * Applica la palette colori, la tipografia e la configurazione della status bar.
 * Su Android 12+ (API 31) supporta i **Dynamic Color** (Material You): i colori vengono
 * estratti dallo sfondo del dispositivo; su versioni precedenti viene usata la palette statica.
 *
 * La status bar viene colorata con il colore `primary` del tema corrente tramite [SideEffect],
 * e le icone vengono adattate (chiare su dark, scure su light).
 *
 * @param darkTheme Se `true` forza la dark mode; default segue il sistema con [isSystemInDarkTheme].
 * @param dynamicColor Se `true` usa i Dynamic Color su Android 12+; default `true`.
 * @param content Contenuto Composable a cui applicare il tema.
 */
@Composable
fun AllCollectionsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
