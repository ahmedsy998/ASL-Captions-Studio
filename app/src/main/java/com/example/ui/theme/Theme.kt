package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val PremiumDarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = DarkSilver,
    onPrimaryContainer = SoftIvory,
    secondary = PrimaryOrangeLight,
    onSecondary = DeepCharcoal,
    secondaryContainer = DarkSilverSurface,
    onSecondaryContainer = SoftIvory,
    tertiary = StudioTealSlate,
    onTertiary = SoftIvory,
    background = DeepCharcoal,
    onBackground = SoftIvory,
    surface = DarkSilverSurface,
    onSurface = SoftIvory,
    surfaceVariant = DarkSilver,
    onSurfaceVariant = SilverGray,
    outline = SilverGrayMuted,
    error = StudioError,
    onError = Color.White
)

private val PremiumLightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = SoftIvoryCard,
    onPrimaryContainer = DeepCharcoal,
    secondary = DarkSilver,
    onSecondary = SoftIvory,
    secondaryContainer = SoftIvoryCard,
    onSecondaryContainer = DarkSilver,
    tertiary = StudioTealSlate,
    onTertiary = Color.White,
    background = SoftIvory,
    onBackground = DeepCharcoal,
    surface = SoftIvoryElevated,
    onSurface = DeepCharcoal,
    surfaceVariant = SoftIvoryCard,
    onSurfaceVariant = DarkSilver,
    outline = SilverGray,
    error = StudioError,
    onError = Color.White
)

@Composable
fun ASLCaptionsStudioTheme(
    darkTheme: Boolean = true,
    isRtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PremiumDarkColorScheme else PremiumLightColorScheme
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Keep backwards-compatible alias for default template tests if needed
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ASLCaptionsStudioTheme(darkTheme = darkTheme, isRtl = true, content = content)
}
