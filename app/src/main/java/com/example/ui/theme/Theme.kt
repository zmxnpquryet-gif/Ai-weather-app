package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightSeaGlassColorScheme = lightColorScheme(
    primary = LightGreenGood,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = LightTextPrimary,
    secondary = LightBlueAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightSunOrange,
    onTertiary = Color.White,
    background = LightSkyBackground,
    onBackground = LightTextPrimary,
    surface = LightSeaGlassSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSeaGlassCard,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFF7DD3FC),
    error = LightDangerRed,
    onError = Color.White
)

private val DarkSeaGlassColorScheme = darkColorScheme(
    primary = DarkGreenGood,
    onPrimary = TextDarkOnAccent,
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkBlueAccent,
    onSecondary = TextDarkOnAccent,
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkSunOrange,
    onTertiary = TextDarkOnAccent,
    background = NightSkyDark,
    onBackground = DarkTextPrimary,
    surface = NightSkySurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = NightSkyCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = NightSkyBorder,
    error = DarkDangerRed,
    onError = TextDarkOnAccent
)

@Composable
fun MyApplicationTheme(
    themeMode: SkyMindThemeMode = SkyMindThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        SkyMindThemeMode.SYSTEM -> systemDark
        SkyMindThemeMode.LIGHT -> false
        SkyMindThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val m3ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkSeaGlassColorScheme
        else -> LightSeaGlassColorScheme
    }

    val skyMindTokens = if (useDarkTheme) DarkSkyMindColorTokens else LightSkyMindColorTokens

    CompositionLocalProvider(LocalSkyMindColors provides skyMindTokens) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = Typography,
            content = content
        )
    }
}
