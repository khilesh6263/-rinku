package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BabyDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = TextPrimary,
    primaryContainer = DeepViolet,
    onPrimaryContainer = SoftLavender,
    secondary = SapphireBlue,
    onSecondary = TextPrimary,
    secondaryContainer = NebulaSurfaceBright,
    onSecondaryContainer = SkyTint,
    tertiary = NeonCyan,
    onTertiary = NebulaObsidian,
    background = NebulaObsidian,
    onBackground = TextPrimary,
    surface = NebulaSurface,
    onSurface = TextPrimary,
    surfaceVariant = NebulaSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = NebulaOutline,
    error = RoseDanger,
    onError = TextPrimary
)

val BabyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun BabyAssistantTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BabyDarkColorScheme,
        typography = BabyTypography,
        shapes = BabyShapes,
        content = content
    )
}
