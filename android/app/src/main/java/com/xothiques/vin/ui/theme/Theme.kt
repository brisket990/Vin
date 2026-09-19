package com.xothiques.vin.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Bordeaux40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Blush90,
    onPrimaryContainer = Bordeaux30,
    secondary = Gold40,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Cream90,
    onSecondaryContainer = Gold40,
    tertiary = WineColorRose,
    background = CreamBackground,
    onBackground = WarmInk,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = WarmInk,
    surfaceVariant = WarmLight90,
    onSurfaceVariant = WarmMuted,
    outline = WarmOutline,
)

private val DarkColors = darkColorScheme(
    primary = Bordeaux80,
    onPrimary = Bordeaux20,
    primaryContainer = BordeauxContainerDark,
    onPrimaryContainer = Blush90,
    secondary = Gold80,
    onSecondary = Color20(),
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = Gold80,
    tertiary = WineColorRose,
    background = WarmDarkBackground,
    onBackground = WarmPale,
    surface = WarmDarkSurface,
    onSurface = WarmPale,
    surfaceVariant = WarmDarkVariant,
    onSurfaceVariant = WarmDarkMuted,
    outline = WarmDarkOutline,
)

// Small helper kept private to this file: darkColorScheme wants an onSecondary
// with good contrast against the light Gold80 secondary.
private fun Color20() = androidx.compose.ui.graphics.Color(0xFF241A00)

@Composable
fun VinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default: Material You dynamic color would replace this app's
    // bordeaux/cream wine palette with colors extracted from the phone's
    // wallpaper, which is not the intended look.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VinTypography,
        shapes = VinShapes,
        content = content,
    )
}

/** Maps a Bottle.color enum value (as returned by the API) to a display color. */
fun wineColorFor(color: String) = when (color) {
    "red" -> WineColorRed
    "white" -> WineColorWhite
    "rose" -> WineColorRose
    "sparkling" -> WineColorSparkling
    "sweet" -> WineColorSweet
    "fortified" -> WineColorFortified
    else -> WineColorRed
}

/** Maps a Bottle.color enum value to its French display label -- shared by
 *  the cave screen's "Ma collection" breakdown and the filtered bottle list
 *  it links to, so both always agree on the wording. */
val WINE_COLOR_LABELS: Map<String, String> = mapOf(
    "red" to "Vins Rouges",
    "white" to "Vins Blancs",
    "rose" to "Vins Rosés",
    "sparkling" to "Vins Effervescents",
    "sweet" to "Vins Doux",
    "fortified" to "Vins Fortifiés",
)
