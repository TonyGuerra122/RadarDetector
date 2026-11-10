package com.tonyguerra.radardetector.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RadarColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = PureBlack,
    primaryContainer = DeepGreen,
    onPrimaryContainer = NeonGreen,
    secondary = RadarGreen,
    onSecondary = PureBlack,
    secondaryContainer = CarbonOutline,
    onSecondaryContainer = TextPrimary,
    tertiary = MutedGreen,
    onTertiary = PureBlack,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = Carbon,
    onSurface = TextPrimary,
    surfaceVariant = CarbonLight,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = Carbon,
    surfaceContainerHigh = CarbonLight,
    outline = CarbonOutline,
    outlineVariant = CarbonOutline,
    error = AlertRed,
    onError = PureBlack,
)

/** O app usa sempre o tema escuro verde e preto (sem cores dinâmicas). */
@Composable
fun RadarDetectorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RadarColorScheme,
        typography = Typography,
        content = content
    )
}
