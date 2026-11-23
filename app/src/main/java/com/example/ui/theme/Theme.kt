package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = MintLight,
    onPrimary = ForestGreenDark,
    primaryContainer = ForestGreen,
    onPrimaryContainer = MintTint,
    secondary = TangerineLight,
    onSecondary = EspressoInk,
    secondaryContainer = HarvestTangerine,
    onSecondaryContainer = WarmApricot,
    tertiary = GoldAmber,
    background = PineNight,
    onBackground = CreamInk,
    surface = MossCard,
    onSurface = CreamInk,
    surfaceVariant = MossElevated,
    onSurfaceVariant = SageMuted,
    outline = PineOutline,
    error = ErrorCrimson,
    onError = CrispWhite
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = CrispWhite,
    primaryContainer = MintTint,
    onPrimaryContainer = ForestGreenDark,
    secondary = HarvestTangerine,
    onSecondary = CrispWhite,
    secondaryContainer = WarmApricot,
    onSecondaryContainer = EspressoInk,
    tertiary = GoldAmber,
    tertiaryContainer = SoftAmber,
    background = WarmCream,
    onBackground = EspressoInk,
    surface = CrispWhite,
    onSurface = EspressoInk,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedStone,
    outline = WarmOutline,
    error = ErrorCrimson,
    onError = CrispWhite,
    errorContainer = ErrorBlush
)

val FreshCartShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FreshCartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FreshCartTypography,
        shapes = FreshCartShapes,
        content = content
    )
}
