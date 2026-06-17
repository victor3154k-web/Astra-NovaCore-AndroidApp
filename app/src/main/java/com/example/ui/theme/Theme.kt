package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CustomDarkColorScheme = darkColorScheme(
    primary = GoldMetallic,
    onPrimary = Black,
    secondary = White,
    onSecondary = Black,
    tertiary = BrightGold,
    background = Black,
    onBackground = White,
    surface = DarkBackground,
    onSurface = OffWhite,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = LightGray,
    outline = BorderGray
)

private val CustomLightColorScheme = lightColorScheme(
    primary = GoldMetallic,
    onPrimary = Black,
    secondary = Black,
    onSecondary = White,
    tertiary = DeepGold,
    background = OffWhite,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = LightGray,
    onSurfaceVariant = DarkBackground,
    outline = LightGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark layout for the cinematic visual style
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our exact custom palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CustomDarkColorScheme else CustomLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
