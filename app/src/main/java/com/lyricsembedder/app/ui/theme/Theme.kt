package com.lyricsembedder.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lyricsembedder.app.data.model.ThemeMode

private val FallbackDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF002A78),
    primaryContainer = Color(0xFF1E3F99),
    onPrimaryContainer = Color(0xFFDBE1FF),
    secondary = Color(0xFFC2C5DD),
    onSecondary = Color(0xFF2B3042),
    secondaryContainer = Color(0xFF424659),
    onSecondaryContainer = Color(0xFFDEE1F9),
    tertiary = Color(0xFFE3BADA),
    onTertiary = Color(0xFF432740),
    background = Color(0xFF10131A),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF12151C),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF1E222D),
    onSurfaceVariant = Color(0xFFC5C6D0)
)

private val FallbackLightColorScheme = lightColorScheme(
    primary = Color(0xFF3959B4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE1FF),
    onPrimaryContainer = Color(0xFF001849),
    secondary = Color(0xFF595E72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDEE1F9),
    onSecondaryContainer = Color(0xFF161B2C),
    tertiary = Color(0xFF745470),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF45464F)
)

/**
 * Material 3 + Material You (Android 12+ Dynamic Color) theme,
 * with graceful fallback palette for Android 5.0 - Android 11.
 */
@Composable
fun LyricsEmbedderTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FallbackDarkColorScheme
        else -> FallbackLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
