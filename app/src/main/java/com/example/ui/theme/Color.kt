package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class SkyMindThemeMode(val labelKo: String) {
    SYSTEM("시스템 설정 따름"),
    LIGHT("라이트 모드"),
    DARK("다크 모드")
}

// --- Light Mode Sea Glass Palette ---
val LightSkyBackground = Color(0xFFE0F7FA)
val LightSeaGlassSurface = Color(0xFFE6FFFA)
val LightSeaGlassCard = Color(0xFFD6F5F2)
val LightSeaGlassBorder = Color(0xFFFFFFFF)

val LightTextPrimary = Color(0xFF0B2239)
val LightTextSecondary = Color(0xFF2C4A63)
val LightBlueAccent = Color(0xFF0284C7)
val LightCyanRain = Color(0xFF0E7490)
val LightSunOrange = Color(0xFFD97706)
val LightGreenGood = Color(0xFF0D9488)
val LightDangerRed = Color(0xFFE11D48)

// --- Dark Mode Bioluminescent Abyssal Sea Glass Palette ---
val NightSkyDark = Color(0xFF07131E)
val NightSkySurface = Color(0xFF0D2233)
val NightSkyCard = Color(0xFF133046)
val NightSkyBorder = Color(0xFF5EEAD4)

val DarkTextPrimary = Color(0xFFF0FDFA)
val DarkTextSecondary = Color(0xFFBAE6FD)
val DarkBlueAccent = Color(0xFF38BDF8)
val DarkCyanRain = Color(0xFF22D3EE)
val DarkSunOrange = Color(0xFFFBBF24)
val DarkGreenGood = Color(0xFF2DD4BF)
val DarkDangerRed = Color(0xFFFB7185)

// Legacy compatibility tokens
val SkyBluePrimary = Color(0xFF38BDF8)
val SkyBlueSecondary = Color(0xFF818CF8)
val SunGoldAccent = Color(0xFFFBBF24)
val RainCyanAccent = Color(0xFF22D3EE)
val StatusGoodGreen = Color(0xFF34D399)
val StatusModerateYellow = Color(0xFFFBBF24)
val StatusWarningOrange = Color(0xFFFB923C)
val StatusDangerRed = Color(0xFFF87171)
val StatusExtremePurple = Color(0xFFC084FC)
val TextPrimaryLight = Color(0xFFF8FAFC)
val TextSecondaryMuted = Color(0xFF94A3B8)
val TextDarkOnAccent = Color(0xFF0F172A)

@Immutable
data class SkyMindColorTokens(
    val isDark: Boolean,
    val textPrimary: Color,
    val textSecondary: Color,
    val blueAccent: Color,
    val cyanRain: Color,
    val sunOrange: Color,
    val greenGood: Color,
    val dangerRed: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val seaGlassTopTint: Color,
    val seaGlassBottomTint: Color,
    val seaGlassRimHighlight: Color,
    val seaGlassRimSubtle: Color,
    val pillUnselectedTop: Color,
    val pillUnselectedBottom: Color,
    val dividerColor: Color,
    val progressTrackColor: Color,
    val nightBackdropVeilAlpha: Float
)

val LightSkyMindColorTokens = SkyMindColorTokens(
    isDark = false,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    blueAccent = LightBlueAccent,
    cyanRain = LightCyanRain,
    sunOrange = LightSunOrange,
    greenGood = LightGreenGood,
    dangerRed = LightDangerRed,
    cardBg = Color(0x48E8F8F5),
    cardBorder = Color(0x88FFFFFF),
    seaGlassTopTint = Color(0xFFE6FFFA),
    seaGlassBottomTint = Color(0xFFB2F5EA),
    seaGlassRimHighlight = Color.White.copy(alpha = 0.72f),
    seaGlassRimSubtle = Color(0xFFE0F7FA).copy(alpha = 0.30f),
    pillUnselectedTop = Color(0xFFE0F7FA).copy(alpha = 0.34f),
    pillUnselectedBottom = Color(0xFFB2F5EA).copy(alpha = 0.24f),
    dividerColor = Color.White.copy(alpha = 0.38f),
    progressTrackColor = Color.White.copy(alpha = 0.45f),
    nightBackdropVeilAlpha = 0f
)

val DarkSkyMindColorTokens = SkyMindColorTokens(
    isDark = true,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    blueAccent = DarkBlueAccent,
    cyanRain = DarkCyanRain,
    sunOrange = DarkSunOrange,
    greenGood = DarkGreenGood,
    dangerRed = DarkDangerRed,
    cardBg = Color(0x780C2233),
    cardBorder = Color(0x665EEAD4),
    seaGlassTopTint = Color(0xFF102A3F),
    seaGlassBottomTint = Color(0xFF081724),
    seaGlassRimHighlight = Color(0xFF5EEAD4).copy(alpha = 0.48f),
    seaGlassRimSubtle = Color(0xFF38BDF8).copy(alpha = 0.24f),
    pillUnselectedTop = Color(0xFF15344D).copy(alpha = 0.62f),
    pillUnselectedBottom = Color(0xFF0B1F30).copy(alpha = 0.50f),
    dividerColor = Color(0xFF38BDF8).copy(alpha = 0.24f),
    progressTrackColor = Color(0xFF091926).copy(alpha = 0.65f),
    nightBackdropVeilAlpha = 0.44f
)

val LocalSkyMindColors = staticCompositionLocalOf { LightSkyMindColorTokens }
