package com.xothiques.vin.ui.theme

import androidx.compose.ui.graphics.Color

// Wine-cellar palette: deep bordeaux primary, warm cream neutrals, soft
// blush containers for icon badges. Designed to read as a wine label, not
// a generic Material app -- see the mockups this was built against.

// --- Bordeaux (primary) ---
val Bordeaux40 = Color(0xFF6D2332) // light-theme primary
val Bordeaux30 = Color(0xFF54202C) // light-theme onPrimaryContainer / pressed states
val Bordeaux80 = Color(0xFFE8B4B8) // dark-theme primary
val Bordeaux20 = Color(0xFF4A121C) // dark-theme onPrimary

val Blush90 = Color(0xFFF6DEE1) // light-theme primaryContainer (icon badges)
val BordeauxContainerDark = Color(0xFF5C1F2B) // dark-theme primaryContainer

// --- Gold (secondary accent) ---
val Gold80 = Color(0xFFE6D2A8)
val Gold40 = Color(0xFF8A6D3B)
val Cream90 = Color(0xFFF3E8D2)
val GoldContainerDark = Color(0xFF4A3B1E)

// --- Warm neutrals ---
val CreamBackground = Color(0xFFF7F1EE) // light-theme background
val WarmInk = Color(0xFF241416) // light-theme onBackground/onSurface
val WarmDarkBackground = Color(0xFF1C1315) // dark-theme background
val WarmDarkSurface = Color(0xFF241A1C) // dark-theme surface (cards lift off bg)
val WarmLight90 = Color(0xFFF0E7E4) // light-theme surfaceVariant
val WarmMuted = Color(0xFF6E5C59) // light-theme onSurfaceVariant
val WarmOutline = Color(0xFFD8C9C6)
val WarmPale = Color(0xFFF0E4E2) // dark-theme onBackground/onSurface
val WarmDarkVariant = Color(0xFF3A2A2C) // dark-theme surfaceVariant
val WarmDarkMuted = Color(0xFFD8C4C1) // dark-theme onSurfaceVariant
val WarmDarkOutline = Color(0xFF5C4A47)

// Wine color -> UI accent, used for cellar grid cells and bottle chips.
val WineColorRed = Color(0xFF7D2E3B)
val WineColorWhite = Color(0xFFDCC98A)
val WineColorRose = Color(0xFFE5A6A0)
val WineColorSparkling = Color(0xFFC9A227)
val WineColorSweet = Color(0xFFB8860B)
val WineColorFortified = Color(0xFF5C3A21)
