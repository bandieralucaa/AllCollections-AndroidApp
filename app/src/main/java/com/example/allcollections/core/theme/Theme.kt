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

/**
 * Schema colori per la modalità scura, costruito dalla palette definita in [Color.kt].
 * Utilizza le costanti di colore specifiche per il tema scuro.
 */
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

/**
 * Schema colori per la modalità chiara, costruito dalla palette definita in [Color.kt].
 * Utilizza le costanti di colore specifiche per il tema chiaro.
 */
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
 * Questa funzione applica la palette colori, la tipografia personalizzata e la configurazione
 * della status bar all'intera gerarchia di composizione. Supporta:
 * - **Modalità scura/chiara** (automatica o forzata tramite [darkTheme]).
 * - **Dynamic Color (Material You)** su Android 12+ (API 31+), che estrae i colori
 *   dallo sfondo del dispositivo per un'esperienza personalizzata.
 * - **Configurazione automatica della status bar**: il colore di sfondo viene sincronizzato
 *   con il colore `primary` del tema corrente, e le icone vengono adattate
 *   (chiare su sfondo scuro, scure su sfondo chiaro).
 *
 * ### Utilizzo tipico
 * ```
 * AllCollectionsTheme(
 *     darkTheme = viewModel.isDarkTheme.value,
 *     dynamicColor = true
 * ) {
 *     // Contenuto dell'app
 * }
 * ```
 *
 * @param darkTheme Se `true` forza la modalità scura, altrimenti segue le impostazioni di sistema.
 *   Default: [isSystemInDarkTheme].
 * @param dynamicColor Se `true` (default) e il dispositivo ha Android 12+, usa i Dynamic Color
 *   di Material You. Su versioni precedenti o se `false`, usa la palette statica.
 * @param content Il contenuto Composable a cui applicare il tema.
 *
 * @see Color.kt per la definizione delle palette statiche
 * @see Typography per la tipografia personalizzata
 */
@Composable
fun AllCollectionsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Seleziona la combinazione di colori appropriata in base ai parametri
    val colorScheme = when {
        // Dynamic Color su Android 12+ (API 31+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Palette statica per tema scuro
        darkTheme -> DarkColorScheme
        // Palette statica per tema chiaro
        else -> LightColorScheme
    }

    // Configura la status bar (solo in runtime, non nell'editor di layout)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            // Imposta le icone della status bar come scure quando il tema è chiaro
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Applica il tema Material 3 con i colori e la tipografia selezionati
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}