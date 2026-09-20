package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BinanceGold,
    onPrimary = Color.Black,
    primaryContainer = BinanceGoldSubtle,
    onPrimaryContainer = BinanceGold,
    secondary = BullishGreen,
    onSecondary = Color.Black,
    secondaryContainer = BullishGreenSubtle,
    onSecondaryContainer = BullishGreen,
    tertiary = NeutralBlue,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderActive,
    error = BearishRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BinanceGold,
    onPrimary = Color.Black,
    secondary = BullishGreen,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    error = BearishRed,
    onError = Color.White
)

@Composable
fun TradePilotTheme(
    darkTheme: Boolean = true, // Default to professional terminal dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
