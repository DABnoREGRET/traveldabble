package com.dabber.traveldabble.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dabber.traveldabble.data.SettingsState
import com.dabber.traveldabble.data.ThemeMode

private val GlassDarkScheme = darkColorScheme(
    primary = AuroraTeal,
    onPrimary = SpaceNight,
    primaryContainer = AuroraTeal.copy(alpha = 0.24f),
    onPrimaryContainer = MistWhite,
    secondary = AuroraBlue,
    onSecondary = SpaceNight,
    secondaryContainer = AuroraBlue.copy(alpha = 0.20f),
    onSecondaryContainer = MistWhite,
    tertiary = AuroraViolet,
    onTertiary = SpaceNight,
    tertiaryContainer = AuroraViolet.copy(alpha = 0.22f),
    onTertiaryContainer = MistWhite,
    error = Danger,
    onError = SpaceNight,
    errorContainer = Danger.copy(alpha = 0.22f),
    onErrorContainer = MistWhite,
    background = SpaceNight,
    onBackground = MistWhite,
    surface = SpaceDeep,
    onSurface = MistWhite,
    surfaceVariant = SpaceSurface,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceTint = AuroraTeal,
    surfaceBright = SpaceSurfaceBright,
    surfaceDim = SpaceSurfaceDim,
    surfaceContainerLowest = SpaceSurfaceContainerLowest,
    surfaceContainerLow = SpaceSurfaceContainerLow,
    surfaceContainer = SpaceSurfaceContainer,
    surfaceContainerHigh = SpaceSurfaceContainerHigh,
    surfaceContainerHighest = SpaceSurfaceContainerHighest,
    inverseSurface = MistWhite,
    inverseOnSurface = SpaceNight,
    inversePrimary = AuroraTeal,
    outline = GlassWhiteBorder,
    outlineVariant = GlassWhite14,
    scrim = GlassDark70,
)

private val GlassLightScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF5B21B6),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceDim,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = Color(0xFF059669),
    surfaceBright = LightSurfaceBright,
    surfaceDim = LightSurfaceDim,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    inverseSurface = SpaceNight,
    inverseOnSurface = MistWhite,
    inversePrimary = Color(0xFF10B981),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = GlassDark70,
)

@Composable
fun TravelDabbleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val themeMode = SettingsState.themeMode
    val isDark = when (themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> darkTheme
    }

    val colorScheme = if (isDark) GlassDarkScheme else GlassLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TravelTypography,
        shapes = TravelShapes,
        content = content,
    )
}
