package com.greenrou.kanata.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AnimeLight_Primary,
    onPrimary = AnimeLight_OnPrimary,
    primaryContainer = AnimeLight_PrimaryContainer,
    onPrimaryContainer = AnimeLight_OnPrimaryContainer,
    secondary = AnimeLight_Secondary,
    onSecondary = AnimeLight_OnSecondary,
    secondaryContainer = AnimeLight_SecondaryContainer,
    onSecondaryContainer = AnimeLight_OnSecondaryContainer,
    tertiary = AnimeLight_Tertiary,
    onTertiary = AnimeLight_OnTertiary,
    tertiaryContainer = AnimeLight_TertiaryContainer,
    onTertiaryContainer = AnimeLight_OnTertiaryContainer,
    background = AnimeLight_Background,
    onBackground = AnimeLight_OnBackground,
    surface = AnimeLight_Surface,
    onSurface = AnimeLight_OnSurface,
    surfaceVariant = AnimeLight_SurfaceVariant,
    onSurfaceVariant = AnimeLight_OnSurfaceVariant,
    surfaceContainer = AnimeLight_SurfaceContainer,
    surfaceContainerHigh = AnimeLight_SurfaceContainerHigh,
    surfaceContainerHighest = AnimeLight_SurfaceContainerHighest,
    outline = AnimeLight_Outline,
    outlineVariant = AnimeLight_OutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = AnimeDark_Primary,
    onPrimary = AnimeDark_OnPrimary,
    primaryContainer = AnimeDark_PrimaryContainer,
    onPrimaryContainer = AnimeDark_OnPrimaryContainer,
    secondary = AnimeDark_Secondary,
    onSecondary = AnimeDark_OnSecondary,
    secondaryContainer = AnimeDark_SecondaryContainer,
    onSecondaryContainer = AnimeDark_OnSecondaryContainer,
    tertiary = AnimeDark_Tertiary,
    onTertiary = AnimeDark_OnTertiary,
    tertiaryContainer = AnimeDark_TertiaryContainer,
    onTertiaryContainer = AnimeDark_OnTertiaryContainer,
    background = AnimeDark_Background,
    onBackground = AnimeDark_OnBackground,
    surface = AnimeDark_Surface,
    onSurface = AnimeDark_OnSurface,
    surfaceVariant = AnimeDark_SurfaceVariant,
    onSurfaceVariant = AnimeDark_OnSurfaceVariant,
    surfaceContainer = AnimeDark_SurfaceContainer,
    surfaceContainerHigh = AnimeDark_SurfaceContainerHigh,
    surfaceContainerHighest = AnimeDark_SurfaceContainerHighest,
    outline = AnimeDark_Outline,
    outlineVariant = AnimeDark_OutlineVariant,
)

@Composable
fun KanataTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
