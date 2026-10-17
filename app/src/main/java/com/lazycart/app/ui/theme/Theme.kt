package com.lazycart.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = OnSageGreen,
    primaryContainer = SageGreenContainer,
    onPrimaryContainer = OnSageGreenContainer,
    secondary = Terracotta,
    onSecondary = OnTerracotta,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = GoldenAmber,
    onTertiary = OnGoldenAmber,
    tertiaryContainer = GoldenAmberContainer,
    onTertiaryContainer = OnGoldenAmberContainer,
    error = ErrorRed,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = SageGreenDark,
    onPrimary = OnSageGreenDark,
    primaryContainer = SageGreenContainerDark,
    onPrimaryContainer = OnSageGreenContainerDark,
    secondary = TerracottaDark,
    onSecondary = OnTerracottaDark,
    secondaryContainer = TerracottaContainerDark,
    onSecondaryContainer = OnTerracottaContainerDark,
    tertiary = GoldenAmberDark,
    onTertiary = OnGoldenAmberDark,
    tertiaryContainer = GoldenAmberContainerDark,
    onTertiaryContainer = OnGoldenAmberContainerDark,
    error = ErrorRedDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

/**
 * LazyCart Material 3 theme.
 *
 * Dynamic colour (Material You) is supported on API 31+ but falls back to our
 * hand-picked palette on older versions. This keeps branding consistent on most devices.
 */
@Composable
fun LazyCartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to true to enable Material You on API 31+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LazyCartTypography,
        content = content
    )
}
