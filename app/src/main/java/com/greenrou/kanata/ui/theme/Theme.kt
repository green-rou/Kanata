package com.greenrou.kanata.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val sharedLightSecondary = lightColorScheme(
    secondary = AnimeLight_Secondary,
    onSecondary = AnimeLight_OnSecondary,
    secondaryContainer = AnimeLight_SecondaryContainer,
    onSecondaryContainer = AnimeLight_OnSecondaryContainer,
    tertiary = AnimeLight_Tertiary,
    onTertiary = AnimeLight_OnTertiary,
    tertiaryContainer = AnimeLight_TertiaryContainer,
    onTertiaryContainer = AnimeLight_OnTertiaryContainer,
    outline = AnimeLight_Outline,
)

private val sharedDarkSecondary = darkColorScheme(
    secondary = AnimeDark_Secondary,
    onSecondary = AnimeDark_OnSecondary,
    secondaryContainer = AnimeDark_SecondaryContainer,
    onSecondaryContainer = AnimeDark_OnSecondaryContainer,
    tertiary = AnimeDark_Tertiary,
    onTertiary = AnimeDark_OnTertiary,
    tertiaryContainer = AnimeDark_TertiaryContainer,
    onTertiaryContainer = AnimeDark_OnTertiaryContainer,
    outline = AnimeDark_Outline,
)

private fun lightScheme(accentColor: String) = when (accentColor) {
    "Gray" -> lightColorScheme(
        primary = GrayLight_Primary, onPrimary = GrayLight_OnPrimary,
        primaryContainer = GrayLight_PrimaryContainer, onPrimaryContainer = GrayLight_OnPrimaryContainer,
        background = GrayLight_Background, onBackground = GrayLight_OnBackground,
        surface = GrayLight_Surface, onSurface = GrayLight_OnSurface,
        surfaceVariant = GrayLight_SurfaceVariant, onSurfaceVariant = GrayLight_OnSurfaceVariant,
        surfaceContainer = GrayLight_SurfaceContainer,
        surfaceContainerHigh = GrayLight_SurfaceContainerHigh,
        surfaceContainerHighest = GrayLight_SurfaceContainerHighest,
        outlineVariant = GrayLight_OutlineVariant,
        secondary = AnimeLight_Secondary, onSecondary = AnimeLight_OnSecondary,
        secondaryContainer = AnimeLight_SecondaryContainer, onSecondaryContainer = AnimeLight_OnSecondaryContainer,
        tertiary = AnimeLight_Tertiary, onTertiary = AnimeLight_OnTertiary,
        tertiaryContainer = AnimeLight_TertiaryContainer, onTertiaryContainer = AnimeLight_OnTertiaryContainer,
        outline = AnimeLight_Outline,
    )
    "Blue" -> lightColorScheme(
        primary = BlueLight_Primary, onPrimary = BlueLight_OnPrimary,
        primaryContainer = BlueLight_PrimaryContainer, onPrimaryContainer = BlueLight_OnPrimaryContainer,
        background = BlueLight_Background, onBackground = BlueLight_OnBackground,
        surface = BlueLight_Surface, onSurface = BlueLight_OnSurface,
        surfaceVariant = BlueLight_SurfaceVariant, onSurfaceVariant = BlueLight_OnSurfaceVariant,
        surfaceContainer = BlueLight_SurfaceContainer,
        surfaceContainerHigh = BlueLight_SurfaceContainerHigh,
        surfaceContainerHighest = BlueLight_SurfaceContainerHighest,
        outlineVariant = BlueLight_OutlineVariant,
        secondary = AnimeLight_Secondary, onSecondary = AnimeLight_OnSecondary,
        secondaryContainer = AnimeLight_SecondaryContainer, onSecondaryContainer = AnimeLight_OnSecondaryContainer,
        tertiary = AnimeLight_Tertiary, onTertiary = AnimeLight_OnTertiary,
        tertiaryContainer = AnimeLight_TertiaryContainer, onTertiaryContainer = AnimeLight_OnTertiaryContainer,
        outline = AnimeLight_Outline,
    )
    "Red" -> lightColorScheme(
        primary = RedLight_Primary, onPrimary = RedLight_OnPrimary,
        primaryContainer = RedLight_PrimaryContainer, onPrimaryContainer = RedLight_OnPrimaryContainer,
        background = RedLight_Background, onBackground = RedLight_OnBackground,
        surface = RedLight_Surface, onSurface = RedLight_OnSurface,
        surfaceVariant = RedLight_SurfaceVariant, onSurfaceVariant = RedLight_OnSurfaceVariant,
        surfaceContainer = RedLight_SurfaceContainer,
        surfaceContainerHigh = RedLight_SurfaceContainerHigh,
        surfaceContainerHighest = RedLight_SurfaceContainerHighest,
        outlineVariant = RedLight_OutlineVariant,
        secondary = AnimeLight_Secondary, onSecondary = AnimeLight_OnSecondary,
        secondaryContainer = AnimeLight_SecondaryContainer, onSecondaryContainer = AnimeLight_OnSecondaryContainer,
        tertiary = AnimeLight_Tertiary, onTertiary = AnimeLight_OnTertiary,
        tertiaryContainer = AnimeLight_TertiaryContainer, onTertiaryContainer = AnimeLight_OnTertiaryContainer,
        outline = AnimeLight_Outline,
    )
    "Orange" -> lightColorScheme(
        primary = OrangeLight_Primary, onPrimary = OrangeLight_OnPrimary,
        primaryContainer = OrangeLight_PrimaryContainer, onPrimaryContainer = OrangeLight_OnPrimaryContainer,
        background = OrangeLight_Background, onBackground = OrangeLight_OnBackground,
        surface = OrangeLight_Surface, onSurface = OrangeLight_OnSurface,
        surfaceVariant = OrangeLight_SurfaceVariant, onSurfaceVariant = OrangeLight_OnSurfaceVariant,
        surfaceContainer = OrangeLight_SurfaceContainer,
        surfaceContainerHigh = OrangeLight_SurfaceContainerHigh,
        surfaceContainerHighest = OrangeLight_SurfaceContainerHighest,
        outlineVariant = OrangeLight_OutlineVariant,
        secondary = AnimeLight_Secondary, onSecondary = AnimeLight_OnSecondary,
        secondaryContainer = AnimeLight_SecondaryContainer, onSecondaryContainer = AnimeLight_OnSecondaryContainer,
        tertiary = AnimeLight_Tertiary, onTertiary = AnimeLight_OnTertiary,
        tertiaryContainer = AnimeLight_TertiaryContainer, onTertiaryContainer = AnimeLight_OnTertiaryContainer,
        outline = AnimeLight_Outline,
    )
    else -> lightColorScheme( // Green
        primary = AnimeLight_Primary, onPrimary = AnimeLight_OnPrimary,
        primaryContainer = AnimeLight_PrimaryContainer, onPrimaryContainer = AnimeLight_OnPrimaryContainer,
        background = AnimeLight_Background, onBackground = AnimeLight_OnBackground,
        surface = AnimeLight_Surface, onSurface = AnimeLight_OnSurface,
        surfaceVariant = AnimeLight_SurfaceVariant, onSurfaceVariant = AnimeLight_OnSurfaceVariant,
        surfaceContainer = AnimeLight_SurfaceContainer,
        surfaceContainerHigh = AnimeLight_SurfaceContainerHigh,
        surfaceContainerHighest = AnimeLight_SurfaceContainerHighest,
        outlineVariant = AnimeLight_OutlineVariant,
        secondary = AnimeLight_Secondary, onSecondary = AnimeLight_OnSecondary,
        secondaryContainer = AnimeLight_SecondaryContainer, onSecondaryContainer = AnimeLight_OnSecondaryContainer,
        tertiary = AnimeLight_Tertiary, onTertiary = AnimeLight_OnTertiary,
        tertiaryContainer = AnimeLight_TertiaryContainer, onTertiaryContainer = AnimeLight_OnTertiaryContainer,
        outline = AnimeLight_Outline,
    )
}

private fun darkScheme(accentColor: String) = when (accentColor) {
    "Gray" -> darkColorScheme(
        primary = GrayDark_Primary, onPrimary = GrayDark_OnPrimary,
        primaryContainer = GrayDark_PrimaryContainer, onPrimaryContainer = GrayDark_OnPrimaryContainer,
        background = GrayDark_Background, onBackground = GrayDark_OnBackground,
        surface = GrayDark_Surface, onSurface = GrayDark_OnSurface,
        surfaceVariant = GrayDark_SurfaceVariant, onSurfaceVariant = GrayDark_OnSurfaceVariant,
        surfaceContainer = GrayDark_SurfaceContainer,
        surfaceContainerHigh = GrayDark_SurfaceContainerHigh,
        surfaceContainerHighest = GrayDark_SurfaceContainerHighest,
        outlineVariant = GrayDark_OutlineVariant,
        secondary = AnimeDark_Secondary, onSecondary = AnimeDark_OnSecondary,
        secondaryContainer = AnimeDark_SecondaryContainer, onSecondaryContainer = AnimeDark_OnSecondaryContainer,
        tertiary = AnimeDark_Tertiary, onTertiary = AnimeDark_OnTertiary,
        tertiaryContainer = AnimeDark_TertiaryContainer, onTertiaryContainer = AnimeDark_OnTertiaryContainer,
        outline = AnimeDark_Outline,
    )
    "Blue" -> darkColorScheme(
        primary = BlueDark_Primary, onPrimary = BlueDark_OnPrimary,
        primaryContainer = BlueDark_PrimaryContainer, onPrimaryContainer = BlueDark_OnPrimaryContainer,
        background = BlueDark_Background, onBackground = BlueDark_OnBackground,
        surface = BlueDark_Surface, onSurface = BlueDark_OnSurface,
        surfaceVariant = BlueDark_SurfaceVariant, onSurfaceVariant = BlueDark_OnSurfaceVariant,
        surfaceContainer = BlueDark_SurfaceContainer,
        surfaceContainerHigh = BlueDark_SurfaceContainerHigh,
        surfaceContainerHighest = BlueDark_SurfaceContainerHighest,
        outlineVariant = BlueDark_OutlineVariant,
        secondary = AnimeDark_Secondary, onSecondary = AnimeDark_OnSecondary,
        secondaryContainer = AnimeDark_SecondaryContainer, onSecondaryContainer = AnimeDark_OnSecondaryContainer,
        tertiary = AnimeDark_Tertiary, onTertiary = AnimeDark_OnTertiary,
        tertiaryContainer = AnimeDark_TertiaryContainer, onTertiaryContainer = AnimeDark_OnTertiaryContainer,
        outline = AnimeDark_Outline,
    )
    "Red" -> darkColorScheme(
        primary = RedDark_Primary, onPrimary = RedDark_OnPrimary,
        primaryContainer = RedDark_PrimaryContainer, onPrimaryContainer = RedDark_OnPrimaryContainer,
        background = RedDark_Background, onBackground = RedDark_OnBackground,
        surface = RedDark_Surface, onSurface = RedDark_OnSurface,
        surfaceVariant = RedDark_SurfaceVariant, onSurfaceVariant = RedDark_OnSurfaceVariant,
        surfaceContainer = RedDark_SurfaceContainer,
        surfaceContainerHigh = RedDark_SurfaceContainerHigh,
        surfaceContainerHighest = RedDark_SurfaceContainerHighest,
        outlineVariant = RedDark_OutlineVariant,
        secondary = AnimeDark_Secondary, onSecondary = AnimeDark_OnSecondary,
        secondaryContainer = AnimeDark_SecondaryContainer, onSecondaryContainer = AnimeDark_OnSecondaryContainer,
        tertiary = AnimeDark_Tertiary, onTertiary = AnimeDark_OnTertiary,
        tertiaryContainer = AnimeDark_TertiaryContainer, onTertiaryContainer = AnimeDark_OnTertiaryContainer,
        outline = AnimeDark_Outline,
    )
    "Orange" -> darkColorScheme(
        primary = OrangeDark_Primary, onPrimary = OrangeDark_OnPrimary,
        primaryContainer = OrangeDark_PrimaryContainer, onPrimaryContainer = OrangeDark_OnPrimaryContainer,
        background = OrangeDark_Background, onBackground = OrangeDark_OnBackground,
        surface = OrangeDark_Surface, onSurface = OrangeDark_OnSurface,
        surfaceVariant = OrangeDark_SurfaceVariant, onSurfaceVariant = OrangeDark_OnSurfaceVariant,
        surfaceContainer = OrangeDark_SurfaceContainer,
        surfaceContainerHigh = OrangeDark_SurfaceContainerHigh,
        surfaceContainerHighest = OrangeDark_SurfaceContainerHighest,
        outlineVariant = OrangeDark_OutlineVariant,
        secondary = AnimeDark_Secondary, onSecondary = AnimeDark_OnSecondary,
        secondaryContainer = AnimeDark_SecondaryContainer, onSecondaryContainer = AnimeDark_OnSecondaryContainer,
        tertiary = AnimeDark_Tertiary, onTertiary = AnimeDark_OnTertiary,
        tertiaryContainer = AnimeDark_TertiaryContainer, onTertiaryContainer = AnimeDark_OnTertiaryContainer,
        outline = AnimeDark_Outline,
    )
    else -> darkColorScheme( // Green
        primary = AnimeDark_Primary, onPrimary = AnimeDark_OnPrimary,
        primaryContainer = AnimeDark_PrimaryContainer, onPrimaryContainer = AnimeDark_OnPrimaryContainer,
        background = AnimeDark_Background, onBackground = AnimeDark_OnBackground,
        surface = AnimeDark_Surface, onSurface = AnimeDark_OnSurface,
        surfaceVariant = AnimeDark_SurfaceVariant, onSurfaceVariant = AnimeDark_OnSurfaceVariant,
        surfaceContainer = AnimeDark_SurfaceContainer,
        surfaceContainerHigh = AnimeDark_SurfaceContainerHigh,
        surfaceContainerHighest = AnimeDark_SurfaceContainerHighest,
        outlineVariant = AnimeDark_OutlineVariant,
        secondary = AnimeDark_Secondary, onSecondary = AnimeDark_OnSecondary,
        secondaryContainer = AnimeDark_SecondaryContainer, onSecondaryContainer = AnimeDark_OnSecondaryContainer,
        tertiary = AnimeDark_Tertiary, onTertiary = AnimeDark_OnTertiary,
        tertiaryContainer = AnimeDark_TertiaryContainer, onTertiaryContainer = AnimeDark_OnTertiaryContainer,
        outline = AnimeDark_Outline,
    )
}

@Composable
fun KanataTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: String = "Green",
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkScheme(accentColor) else lightScheme(accentColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
