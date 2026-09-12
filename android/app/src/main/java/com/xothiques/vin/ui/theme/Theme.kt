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
    primary = WineRed40,
    secondary = Gold40,
    background = Neutral99,
)

private val DarkColors = darkColorScheme(
    primary = WineRed80,
    secondary = Gold80,
    background = Neutral10,
)

@Composable
fun VinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
