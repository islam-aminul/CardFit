package `in`.firm.consultancy.bayaan.cardfit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Bayaan-branded theme (design-system/): flat paper page, white bordered cards, midnight authority,
 * one teal signal. Replaces the shipped v1 Material dynamic-color theme.
 *
 * The design system is light-only ("flat paper or white" backgrounds; dark midnight surfaces are
 * reserved for feature panels, not a dark mode), so the same scheme is used regardless of the system
 * dark setting and [dynamicColor] is ignored.
 */
private val BayaanColorScheme = lightColorScheme(
    // Teal is "the signal": links, focus, selection, progress, sliders. teal-600 on light surfaces.
    primary = Teal600,
    onPrimary = Color.White,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Midnight800,
    inversePrimary = Teal300,
    // Sage — the calm second voice.
    secondary = Sage600,
    onSecondary = Color.White,
    secondaryContainer = SageSoft,
    onSecondaryContainer = Sage700,
    // Midnight tonal pairing for neutral emphasis.
    tertiary = Midnight600,
    onTertiary = Color.White,
    tertiaryContainer = Midnight50,
    onTertiaryContainer = Midnight600,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    // Neutralise tonal-elevation tinting: surfaces stay flat paper/white.
    surfaceTint = Paper,
    surfaceVariant = Midnight50,
    onSurfaceVariant = TextMuted,
    surfaceBright = Color.White,
    surfaceDim = Midnight50,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Midnight50,
    outline = Midnight200,
    outlineVariant = Midnight100,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF8C1D18),
    scrim = Midnight900,
    inverseSurface = Midnight900,
    inverseOnSurface = Color.White,
)

/** Bayaan radii (tokens/shape.css): 8 chips · 12 tiles/inputs · 16 cards · 24 panels; pills are per-component. */
private val BayaanShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun CardFitTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BayaanColorScheme,
        typography = Typography,
        shapes = BayaanShapes,
        content = content,
    )
}
