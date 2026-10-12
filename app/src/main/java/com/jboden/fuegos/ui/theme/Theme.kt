package com.jboden.fuegos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceVariant = SurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    primary = PrimaryCopper,
    onPrimary = OnPrimaryCopper,
    primaryContainer = PrimaryContainerCopper,
    onPrimaryContainer = OnPrimaryContainerCopper,
    secondary = SecondaryGraphite,
    onSecondary = OnSecondaryGraphite,
    secondaryContainer = SecondaryContainerGraphite,
    onSecondaryContainer = OnSecondaryContainerGraphite,
    tertiary = TertiaryGreen,
    onTertiary = OnTertiaryGreen,
    tertiaryContainer = TertiaryContainerGreen,
    onTertiaryContainer = OnTertiaryContainerGreen,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed
)

@Composable
fun FuegosTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
