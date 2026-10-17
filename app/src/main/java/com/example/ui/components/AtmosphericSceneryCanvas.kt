package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AtmosphericThemeSpec
import com.example.ui.theme.LocalSkyMindColors
import com.example.ui.theme.SceneryMood
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Weather animation state mapped from WMO weatherCode (or theme mood fallback)
 */
enum class WeatherAnimationCondition {
    CLEAR,
    RAIN,
    SNOW,
    CLOUDY
}

fun resolveWeatherAnimationCondition(
    weatherCode: Int?,
    themeMood: SceneryMood
): WeatherAnimationCondition {
    // If the active theme specifically depicts rain, snow, or cloudy mist, or live weatherCode matches:
    when (themeMood) {
        SceneryMood.RAIN_UMBRELLA -> return WeatherAnimationCondition.RAIN
        SceneryMood.WINTER_SNOW -> return WeatherAnimationCondition.SNOW
        SceneryMood.CLOUDY_MIST, SceneryMood.DUST_HAZE -> return WeatherAnimationCondition.CLOUDY
        else -> Unit
    }
    if (weatherCode != null) {
        return when (weatherCode) {
            0, 1 -> WeatherAnimationCondition.CLEAR
            2, 3, 45, 48 -> WeatherAnimationCondition.CLOUDY
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99 -> WeatherAnimationCondition.RAIN
            71, 73, 75, 77, 85, 86 -> WeatherAnimationCondition.SNOW
            else -> WeatherAnimationCondition.CLOUDY
        }
    }
    return WeatherAnimationCondition.CLEAR
}

private data class RainDropParticle(
    val xSeed: Float,
    val yOffset: Float,
    val speedMultiplier: Float,
    val lengthDp: Float,
    val alpha: Float,
    val strokeWidthDp: Float
)

private data class SnowFlakeParticle(
    val xSeed: Float,
    val yOffset: Float,
    val speedMultiplier: Float,
    val radiusDp: Float,
    val swayAmplitudeDp: Float,
    val swayFrequency: Float,
    val alpha: Float
)

private data class SunBokehParticle(
    val xRatio: Float,
    val yRatio: Float,
    val baseRadiusDp: Float,
    val phaseOffset: Float,
    val driftRangeDp: Float
)

/**
 * Translucent Sea Glass Surface Container (Light & Dark Mode Adaptive):
 * - Light Mode: Frosted sea-foam / aqua / ice tinting (alpha 0.18f~0.42f) with bright white specular rim.
 * - Dark Mode: Deep bioluminescent abyssal teal glass with cyan-mint specular refraction rim.
 */
@Composable
fun SeaGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    tintColor: Color = Color(0xFFCFFAFE),
    surfaceAlpha: Float = 0.28f,
    shadowElevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalSkyMindColors.current
    val shape = RoundedCornerShape(cornerRadius)

    val baseGlassBrush = if (tokens.isDark) {
        val darkClampedAlpha = (surfaceAlpha + 0.26f).coerceIn(0.42f, 0.72f)
        val midTint = lerp(Color(0xFF0B1F30), tintColor, 0.18f)
        Brush.linearGradient(
            colors = listOf(
                tokens.seaGlassTopTint.copy(alpha = (darkClampedAlpha + 0.06f).coerceAtMost(0.78f)),
                midTint.copy(alpha = darkClampedAlpha),
                tokens.seaGlassBottomTint.copy(alpha = (darkClampedAlpha - 0.04f).coerceAtLeast(0.38f))
            )
        )
    } else {
        val clampedAlpha = surfaceAlpha.coerceIn(0.16f, 0.42f)
        val topAlpha = (clampedAlpha + 0.08f).coerceIn(0.20f, 0.46f)
        val bottomAlpha = (clampedAlpha - 0.05f).coerceIn(0.14f, 0.36f)
        Brush.linearGradient(
            colors = listOf(
                tokens.seaGlassTopTint.copy(alpha = topAlpha),
                tintColor.copy(alpha = clampedAlpha),
                tokens.seaGlassBottomTint.copy(alpha = bottomAlpha)
            )
        )
    }

    val specularRimBrush = if (tokens.isDark) {
        Brush.linearGradient(
            colors = listOf(
                tokens.seaGlassRimHighlight,
                tokens.seaGlassRimSubtle,
                lerp(Color(0xFF0D9488), tintColor, 0.3f).copy(alpha = 0.32f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                tokens.seaGlassRimHighlight,
                tokens.seaGlassRimSubtle,
                tintColor.copy(alpha = 0.42f)
            )
        )
    }

    val clickMod = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = baseGlassBrush, shape = shape)
            .border(width = 1.2.dp, brush = specularRimBrush, shape = shape)
            .then(clickMod),
        content = content
    )
}

/**
 * Translucent Sea Glass Pill / Pebble Button (Light & Dark Mode Adaptive):
 */
@Composable
fun SeaGlassPill(
    isSelected: Boolean,
    activeColor: Color = Color(0xFF0D9488),
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 50.dp,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalSkyMindColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val fillBrush = if (isSelected) {
        Brush.linearGradient(
            colors = listOf(
                activeColor.copy(alpha = if (tokens.isDark) 0.78f else 0.68f),
                activeColor.copy(alpha = if (tokens.isDark) 0.58f else 0.48f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                tokens.pillUnselectedTop,
                tokens.pillUnselectedBottom
            )
        )
    }

    val borderBrush = if (isSelected) {
        Brush.linearGradient(
            colors = listOf(
                if (tokens.isDark) Color(0xFF99F6E4).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f),
                activeColor.copy(alpha = 0.50f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                tokens.seaGlassRimHighlight,
                tokens.seaGlassRimSubtle
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = fillBrush, shape = shape)
            .border(width = 1.1.dp, brush = borderBrush, shape = shape)
            .clickable { onClick() },
        content = content
    )
}

/**
 * Renders the simplified/smudged real photograph as the full-bleed background,
 * with a live weather-condition animation overlay (맑음, 비, 눈, 흐림):
 * - RAIN (비): Falling rain streaks from top to bottom with wind slant, water splashes/ripples, and thunder glow.
 * - SNOW (눈): Drifting multi-layered snowflakes falling from top to bottom with gentle horizontal sway.
 * - CLEAR (맑음): Rotating warm sunbeams, floating light bokeh orbs, and twinkling stars at night.
 * - CLOUDY (흐림): Multi-layered translucent mist and cloud banks drifting softly across the background photo.
 */
@Composable
fun AtmosphericSceneryCanvas(
    theme: AtmosphericThemeSpec,
    smudgedPhotoBitmap: Bitmap? = null,
    weatherCode: Int? = null,
    isDay: Boolean = true,
    windSpeedMs: Double = 2.0,
    modifier: Modifier = Modifier
) {
    val weatherCondition = remember(weatherCode, theme.mood) {
        resolveWeatherAnimationCondition(weatherCode, theme.mood)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "weather_scenery_animation")

    // Continuous 0f..1f master phase for falling particles (rain / snow)
    val fallProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fall_progress"
    )

    // Slow continuous 0f..1f phase for drifting clouds, sun rays, and bokeh
    val slowDriftPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slow_drift_phase"
    )

    // Subtle breathing / Ken-Burns motion on the background photo itself
    val photoScale by infiniteTransition.animateFloat(
        initialValue = 1.01f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "photo_scale"
    )

    val photoPanX by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "photo_pan_x"
    )

    // Pre-allocated particle pools for zero-allocation 60fps Canvas rendering
    val rainParticles = remember {
        List(72) { idx ->
            val seed1 = ((idx * 37 + 13) % 100) / 100f
            val seed2 = ((idx * 61 + 29) % 100) / 100f
            val layer = idx % 3
            RainDropParticle(
                xSeed = seed1,
                yOffset = seed2,
                speedMultiplier = when (layer) {
                    0 -> 1.0f
                    1 -> 2.0f
                    else -> 3.0f
                },
                lengthDp = when (layer) {
                    0 -> 20f
                    1 -> 30f
                    else -> 42f
                },
                alpha = when (layer) {
                    0 -> 0.32f
                    1 -> 0.52f
                    else -> 0.74f
                },
                strokeWidthDp = when (layer) {
                    0 -> 1.2f
                    1 -> 1.7f
                    else -> 2.3f
                }
            )
        }
    }

    val snowParticles = remember {
        List(56) { idx ->
            val seed1 = ((idx * 43 + 17) % 100) / 100f
            val seed2 = ((idx * 71 + 11) % 100) / 100f
            val layer = idx % 3
            SnowFlakeParticle(
                xSeed = seed1,
                yOffset = seed2,
                speedMultiplier = if (layer == 0) 1.0f else 2.0f,
                radiusDp = when (layer) {
                    0 -> 2.4f
                    1 -> 4.0f
                    else -> 6.0f
                },
                swayAmplitudeDp = when (layer) {
                    0 -> 8f
                    1 -> 15f
                    else -> 22f
                },
                swayFrequency = 1.0f + (idx % 4) * 0.5f,
                alpha = when (layer) {
                    0 -> 0.48f
                    1 -> 0.72f
                    else -> 0.90f
                }
            )
        }
    }

    val sunBokehParticles = remember {
        List(16) { idx ->
            SunBokehParticle(
                xRatio = 0.08f + ((idx * 29) % 84) / 100f,
                yRatio = 0.06f + ((idx * 47) % 72) / 100f,
                baseRadiusDp = 12f + (idx % 5) * 9f,
                phaseOffset = (idx * 0.39f),
                driftRangeDp = 10f + (idx % 3) * 8f
            )
        }
    }

    val tokens = LocalSkyMindColors.current

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Base Sky Gradient
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(theme.skyTop, theme.skyMid, theme.skyBottom)
                ),
                size = size
            )
        }

        // 2. Animated Smudged Background Photo with Gentle Breathing Scale & Pan
        if (smudgedPhotoBitmap != null) {
            Image(
                bitmap = smudgedPhotoBitmap.asImageBitmap(),
                contentDescription = theme.nameKo,
                contentScale = ContentScale.Crop,
                alpha = if (tokens.isDark) 0.72f else 0.90f,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = photoScale
                        scaleY = photoScale
                        translationX = photoPanX
                    }
            )
        }

        // 3. Weather-Condition Live Animation Overlay (맑음, 비, 눈, 흐림)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft theme tint refraction over the background photo
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        theme.skyTop.copy(alpha = if (smudgedPhotoBitmap != null) 0.20f else 0.72f),
                        theme.skyMid.copy(alpha = if (smudgedPhotoBitmap != null) 0.16f else 0.62f),
                        theme.skyBottom.copy(alpha = if (smudgedPhotoBitmap != null) 0.26f else 0.82f)
                    )
                ),
                size = size
            )

            // Dark Mode Nocturnal Abyssal Veil so light text and dark sea-glass cards have optimal contrast
            if (tokens.isDark) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF040D1A).copy(alpha = 0.52f),
                            Color(0xFF071626).copy(alpha = 0.44f),
                            Color(0xFF061220).copy(alpha = 0.58f)
                        )
                    ),
                    size = size
                )
            }

            if (smudgedPhotoBitmap == null) {
                val horizonY = h * 0.44f
                val frontHill = Path().apply {
                    moveTo(0f, horizonY + 12.dp.toPx())
                    quadraticTo(w * 0.35f, horizonY - 18.dp.toPx(), w * 0.68f, horizonY + 16.dp.toPx())
                    quadraticTo(w * 0.88f, horizonY + 28.dp.toPx(), w, horizonY + 6.dp.toPx())
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = frontHill,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            theme.horizonHillFront.copy(alpha = 0.42f),
                            theme.skyBottom.copy(alpha = 0.78f)
                        ),
                        startY = horizonY,
                        endY = h
                    )
                )
            }

            // Dynamic Weather Overlay Dispatch
            when (weatherCondition) {
                WeatherAnimationCondition.RAIN -> {
                    drawRainOverlayAnimation(
                        w = w,
                        h = h,
                        fallProgress = fallProgress,
                        slowDriftPhase = slowDriftPhase,
                        windSpeedMs = windSpeedMs,
                        isThunderstorm = (weatherCode ?: 0) >= 95,
                        particles = rainParticles
                    )
                }

                WeatherAnimationCondition.SNOW -> {
                    drawSnowOverlayAnimation(
                        w = w,
                        h = h,
                        fallProgress = fallProgress,
                        slowDriftPhase = slowDriftPhase,
                        windSpeedMs = windSpeedMs,
                        particles = snowParticles
                    )
                }

                WeatherAnimationCondition.CLOUDY -> {
                    drawCloudyMistOverlayAnimation(
                        w = w,
                        h = h,
                        slowDriftPhase = slowDriftPhase,
                        theme = theme
                    )
                }

                WeatherAnimationCondition.CLEAR -> {
                    drawClearSkyOverlayAnimation(
                        w = w,
                        h = h,
                        slowDriftPhase = slowDriftPhase,
                        isDay = isDay,
                        theme = theme,
                        bokehParticles = sunBokehParticles
                    )
                }
            }
        }
    }
}

/**
 * RAIN (비) Overlay:
 * - Multi-layered raindrops falling from top to bottom of the screen
 * - Wind-driven diagonal slant
 * - Expanding water ripple rings at various heights
 * - Subtle distant cloud & occasional sheet-lightning shimmer during thunderstorms
 */
private fun DrawScope.drawRainOverlayAnimation(
    w: Float,
    h: Float,
    fallProgress: Float,
    slowDriftPhase: Float,
    windSpeedMs: Double,
    isThunderstorm: Boolean,
    particles: List<RainDropParticle>
) {
    // Cool rainy atmosphere veil at top
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A).copy(alpha = 0.22f),
                Color(0xFF1E3A8A).copy(alpha = 0.08f),
                Color.Transparent
            )
        ),
        size = size
    )

    // Optional thunderstorm sheet lightning flash
    if (isThunderstorm) {
        val flashWave = sin(slowDriftPhase * 2f * PI.toFloat() * 7f)
        if (flashWave > 0.92f) {
            val flashAlpha = ((flashWave - 0.92f) / 0.08f) * 0.28f
            drawRect(
                color = Color(0xFFE0F2FE).copy(alpha = flashAlpha),
                size = size
            )
        }
    }

    // Wind slant in pixels per drop length
    val windSlantFactor = (windSpeedMs.toFloat().coerceIn(0f, 14f) / 14f) * 0.32f + 0.08f

    particles.forEach { drop ->
        val rawY = (drop.yOffset + fallProgress * drop.speedMultiplier) % 1.0f
        val startY = rawY * (h + 120.dp.toPx()) - 60.dp.toPx()
        val dropLenPx = drop.lengthDp.dp.toPx()
        val dx = -dropLenPx * windSlantFactor

        // Wrap X softly so slanted drops cover full width
        val shiftedXRatio = (drop.xSeed - rawY * windSlantFactor * 0.25f + 1.0f) % 1.0f
        val startX = shiftedXRatio * w
        val endX = startX + dx
        val endY = startY + dropLenPx

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color(0xFFE0F7FA).copy(alpha = drop.alpha),
                    Color.White.copy(alpha = (drop.alpha + 0.15f).coerceAtMost(0.95f))
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = drop.strokeWidthDp.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Expanding raindrop splash ripples across the lower half of the screen
    for (rIdx in 0 until 8) {
        val ripplePhase = (fallProgress * 1.5f + rIdx * 0.13f) % 1.0f
        val rippleX = w * (0.10f + ((rIdx * 31) % 80) / 100f)
        val rippleY = h * (0.58f + ((rIdx * 19) % 36) / 100f)
        val maxRadiusX = 22.dp.toPx()
        val maxRadiusY = 7.dp.toPx()
        val currentRx = maxRadiusX * ripplePhase
        val currentRy = maxRadiusY * ripplePhase
        val rippleAlpha = (1f - ripplePhase) * 0.42f

        drawOval(
            color = Color(0xFFE0F7FA).copy(alpha = rippleAlpha),
            topLeft = Offset(rippleX - currentRx, rippleY - currentRy),
            size = Size(currentRx * 2f, currentRy * 2f),
            style = Stroke(width = 1.3.dp.toPx())
        )
    }
}

/**
 * SNOW (눈) Overlay:
 * - Soft snowflakes falling from top to bottom with gentle sinusoidal horizontal sway
 * - Multi-size depth layers with glowing frost halos on foreground flakes
 */
private fun DrawScope.drawSnowOverlayAnimation(
    w: Float,
    h: Float,
    fallProgress: Float,
    slowDriftPhase: Float,
    windSpeedMs: Double,
    particles: List<SnowFlakeParticle>
) {
    // Frosty top glow
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color(0xFFE0F2FE).copy(alpha = 0.08f),
                Color.Transparent
            )
        ),
        size = size
    )

    val windDrift = (windSpeedMs.toFloat().coerceIn(0f, 12f) / 12f) * 0.12f

    particles.forEachIndexed { idx, flake ->
        val rawY = (flake.yOffset + fallProgress * flake.speedMultiplier * 0.5f + slowDriftPhase * 0.5f) % 1.0f
        val y = rawY * (h + 60.dp.toPx()) - 30.dp.toPx()

        val swayAngle = (rawY * flake.swayFrequency * 2f * PI.toFloat()) + idx
        val swayPx = sin(swayAngle) * flake.swayAmplitudeDp.dp.toPx()
        val baseX = ((flake.xSeed + rawY * windDrift) % 1.0f) * w
        val x = (baseX + swayPx + w) % w
        val radiusPx = flake.radiusDp.dp.toPx()

        // Outer soft frost halo for larger snowflakes
        if (flake.radiusDp >= 4.0f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = flake.alpha * 0.55f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = radiusPx * 2.3f
                ),
                radius = radiusPx * 2.3f,
                center = Offset(x, y)
            )
        }

        drawCircle(
            color = Color.White.copy(alpha = flake.alpha),
            radius = radiusPx,
            center = Offset(x, y)
        )
    }
}

/**
 * CLOUDY (흐림 / 안개) Overlay:
 * - Multi-layered translucent cloud and mist banks drifting horizontally across the background photo
 */
private fun DrawScope.drawCloudyMistOverlayAnimation(
    w: Float,
    h: Float,
    slowDriftPhase: Float,
    theme: AtmosphericThemeSpec
) {
    val twoPi = 2f * PI.toFloat()

    // Soft diffused overcast top light
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.sunMoonColor.copy(alpha = 0.24f),
                Color.Transparent
            ),
            center = Offset(w * 0.72f, h * 0.16f),
            radius = 160.dp.toPx()
        ),
        radius = 160.dp.toPx(),
        center = Offset(w * 0.72f, h * 0.16f)
    )

    // 5 Drifting translucent cloud/mist bands at different altitudes
    for (band in 0 until 5) {
        val direction = if (band % 2 == 0) 1f else -1f
        val driftOffset = sin(slowDriftPhase * twoPi + band * 1.1f) * (w * 0.14f) * direction
        val centerX = w * (0.20f + band * 0.16f) + driftOffset
        val centerY = h * (0.12f + band * 0.13f)
        val cloudW = w * (0.72f + (band % 3) * 0.14f)
        val cloudH = (58 + (band % 3) * 22).dp.toPx()

        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color(0xFFE0F2FE).copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = cloudW * 0.55f
            ),
            topLeft = Offset(centerX - cloudW / 2f, centerY - cloudH / 2f),
            size = Size(cloudW, cloudH),
            cornerRadius = CornerRadius(cloudH / 2f, cloudH / 2f)
        )
    }
}

/**
 * CLEAR (맑음) Overlay:
 * - Daytime: Rotating warm sunbeams, pulsing solar halo, and floating warm light bokeh particles
 * - Nighttime: Soft lunar halo and gently twinkling starlight across the upper sky
 */
private fun DrawScope.drawClearSkyOverlayAnimation(
    w: Float,
    h: Float,
    slowDriftPhase: Float,
    isDay: Boolean,
    theme: AtmosphericThemeSpec,
    bokehParticles: List<SunBokehParticle>
) {
    val twoPi = 2f * PI.toFloat()
    val sunCenter = Offset(w * 0.80f, h * 0.15f)
    val pulse = 0.90f + 0.14f * sin(slowDriftPhase * twoPi * 2f)

    // Pulsing Sun / Moon radial glow
    val glowRadius = 155.dp.toPx() * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.sunMoonColor.copy(alpha = if (isDay) 0.44f else 0.34f),
                theme.sunMoonColor.copy(alpha = 0.12f),
                Color.Transparent
            ),
            center = sunCenter,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = sunCenter
    )

    if (isDay) {
        // Rotating soft sunbeams
        val rotationDeg = slowDriftPhase * 360f
        rotate(degrees = rotationDeg, pivot = sunCenter) {
            val rayCount = 8
            for (i in 0 until rayCount) {
                val angleRad = (i * (360.0 / rayCount)) * (PI / 180.0)
                val rayLen = 210.dp.toPx()
                val endX = sunCenter.x + cos(angleRad).toFloat() * rayLen
                val endY = sunCenter.y + sin(angleRad).toFloat() * rayLen
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.24f),
                            theme.sunMoonColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        start = sunCenter,
                        end = Offset(endX, endY)
                    ),
                    start = sunCenter,
                    end = Offset(endX, endY),
                    strokeWidth = 14.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Floating sunlit bokeh orbs
        bokehParticles.forEach { orb ->
            val phase = slowDriftPhase * twoPi + orb.phaseOffset
            val dx = cos(phase) * orb.driftRangeDp.dp.toPx()
            val dy = sin(phase * 1.5f) * orb.driftRangeDp.dp.toPx()
            val orbCenter = Offset(w * orb.xRatio + dx, h * orb.yRatio + dy)
            val orbAlpha = (0.12f + 0.10f * sin(phase * 2f)).coerceIn(0.04f, 0.26f)
            val rPx = orb.baseRadiusDp.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = orbAlpha),
                        theme.sunMoonColor.copy(alpha = orbAlpha * 0.5f),
                        Color.Transparent
                    ),
                    center = orbCenter,
                    radius = rPx
                ),
                radius = rPx,
                center = orbCenter
            )
        }
    } else {
        // Clear Night: Twinkling Stars
        bokehParticles.forEachIndexed { idx, star ->
            val twinklePhase = slowDriftPhase * twoPi * 3f + star.phaseOffset * 4f
            val starAlpha = (0.35f + 0.55f * ((sin(twinklePhase) + 1f) / 2f)).coerceIn(0.15f, 0.92f)
            val starX = w * star.xRatio
            val starY = h * (star.yRatio * 0.55f)
            val starRadius = (if (idx % 3 == 0) 2.6f else 1.7f).dp.toPx()

            drawCircle(
                color = Color.White.copy(alpha = starAlpha),
                radius = starRadius,
                center = Offset(starX, starY)
            )
        }
    }
}
