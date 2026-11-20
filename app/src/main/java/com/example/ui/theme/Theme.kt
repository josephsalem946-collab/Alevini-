package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalDataSaverMode = staticCompositionLocalOf { false }

private val AleviniLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = GoldAccent,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = GoldOnContainer,
    tertiary = SkyInfo,
    onTertiary = Color.White,
    tertiaryContainer = SkyContainer,
    onTertiaryContainer = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceBright,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline.copy(alpha = 0.65f),
    error = RubyError,
    onError = Color.White,
    errorContainer = RubyContainer,
    onErrorContainer = TextPrimary,
)

private val AleviniNoTeardropSelectionColors = TextSelectionColors(
    handleColor = Color.Transparent,
    backgroundColor = EmeraldPrimary.copy(alpha = 0.24f),
)

val AleviniShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AleviniTheme(
    isDataSaverEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTextSelectionColors provides AleviniNoTeardropSelectionColors,
        LocalDataSaverMode provides isDataSaverEnabled,
    ) {
        MaterialTheme(
            colorScheme = AleviniLightColorScheme,
            typography = AleviniTypography,
            shapes = AleviniShapes,
            content = content,
        )
    }
}
