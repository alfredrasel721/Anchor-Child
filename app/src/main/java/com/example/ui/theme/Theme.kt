package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AnchorChildColorScheme = lightColorScheme(
    primary = StitchPrimary,
    onPrimary = StitchOnPrimary,
    primaryContainer = StitchPrimaryContainer,
    onPrimaryContainer = StitchOnPrimaryContainer,
    secondary = StitchSecondary,
    onSecondary = StitchOnSecondary,
    secondaryContainer = StitchSecondaryContainer,
    onSecondaryContainer = StitchOnSecondaryContainer,
    tertiary = StitchTertiary,
    onTertiary = StitchOnTertiary,
    tertiaryContainer = StitchTertiaryContainer,
    error = StitchError,
    onError = StitchOnError,
    errorContainer = StitchErrorContainer,
    onErrorContainer = StitchOnErrorContainer,
    background = StitchBackground,
    onBackground = StitchOnBackground,
    surface = StitchSurface,
    onSurface = StitchOnSurface,
    surfaceVariant = StitchSurfaceContainerLow,
    onSurfaceVariant = StitchOnSurfaceVariant,
    outline = StitchOutline,
    outlineVariant = StitchOutlineVariant
)

@Composable
fun AnchorChildTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AnchorChildColorScheme,
        typography = Typography,
        content = content
    )
}
