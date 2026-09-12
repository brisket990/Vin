package com.xothiques.vin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System font (Roboto), but with bolder headings and tighter letter-spacing
// than Material3's defaults -- reads more like a wine-label wordmark than a
// generic settings screen. Body/label styles are left at Material3
// defaults for readability.
private val defaults = Typography()

val VinTypography = defaults.copy(
    headlineLarge = defaults.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = defaults.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineSmall = defaults.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = defaults.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = defaults.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = defaults.titleSmall.copy(fontWeight = FontWeight.SemiBold),
)

/** A serif-free, extra-bold display style for hero numbers/titles (cellar header, etc). */
val VinDisplayStyle = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = (-0.5).sp)
