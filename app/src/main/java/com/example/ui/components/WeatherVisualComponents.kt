package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.WeatherBundle
import com.example.ui.theme.LocalSkyMindColors
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Adaptive Light/Dark Mode Translucent Sea Glass Palette Tokens
val SkyMindCardBg: Color
    @Composable get() = LocalSkyMindColors.current.cardBg
val SkyMindCardBorder: Color
    @Composable get() = LocalSkyMindColors.current.cardBorder
val SkyMindTextPrimary: Color
    @Composable get() = LocalSkyMindColors.current.textPrimary
val SkyMindTextSecondary: Color
    @Composable get() = LocalSkyMindColors.current.textSecondary
val SkyMindBlueAccent: Color
    @Composable get() = LocalSkyMindColors.current.blueAccent
val SkyMindCyanRain: Color
    @Composable get() = LocalSkyMindColors.current.cyanRain
val SkyMindSunOrange: Color
    @Composable get() = LocalSkyMindColors.current.sunOrange
val SkyMindGreenGood: Color
    @Composable get() = LocalSkyMindColors.current.greenGood
val SkyMindDangerRed: Color
    @Composable get() = LocalSkyMindColors.current.dangerRed

fun weatherIconForCode(code: Int): Pair<ImageVector, Color> = when (code) {
    0, 1 -> Icons.Default.WbSunny to Color(0xFFFFB300)
    2, 3, 45, 48 -> Icons.Default.Cloud to Color(0xFF38BDF8)
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Default.Umbrella to Color(0xFF0284C7)
    71, 73, 75, 77, 85, 86 -> Icons.Default.Grain to Color(0xFF60A5FA)
    95, 96, 99 -> Icons.Default.Thunderstorm to Color(0xFFF59E0B)
    else -> Icons.Default.Cloud to Color(0xFF38BDF8)
}

@Composable
fun statusColorForGrade(grade: String): Color = when {
    grade.contains("좋음") || grade.contains("낮음") || grade.contains("상쾌") || grade.contains("쾌적") || grade.contains("안심") || grade.contains("불필요") || grade.contains("표준") -> SkyMindGreenGood
    grade.contains("보통") || grade.contains("권장") || grade.contains("다소") || grade.contains("고기압") -> SkyMindSunOrange
    grade.contains("높음") && !grade.contains("매우") -> if (LocalSkyMindColors.current.isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
    grade.contains("나쁨") && !grade.contains("매우") -> if (LocalSkyMindColors.current.isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
    grade.contains("저기압") || grade.contains("건조 주의") -> if (LocalSkyMindColors.current.isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
    grade.contains("매우") || grade.contains("필수") || grade.contains("위험") -> SkyMindDangerRed
    else -> SkyMindBlueAccent
}

@Composable
fun DetailedWeatherMetricsGrid(
    weather: WeatherBundle,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("detailed_metrics_grid"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "날씨 상세 정보",
            style = MaterialTheme.typography.titleLarge,
            color = SkyMindTextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Row 1: Precipitation & Wind
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrecipitationCard(
                weather = weather,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_card_precipitation")
            )
            WindCompassCard(
                weather = weather,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_card_wind")
            )
        }

        // Row 2: Humidity & Atmospheric Pressure (New Dedicated Cards with Icons & Gauges)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HumidityMetricCard(
                weather = weather,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_card_humidity")
            )
            PressureMetricCard(
                weather = weather,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_card_pressure")
            )
        }

        // Row 3: UV Index & Air Quality
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UvIndexCard(
                weather = weather,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_card_uv")
            )
            AirQualityCard(
                weather = weather,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_card_air_quality")
            )
        }

        // Row 4: Sunrise / Sunset / Cloud Cover Summary
        AtmosphereExtraCard(
            weather = weather,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("metric_card_atmosphere")
        )
    }
}

/**
 * Dedicated Humidity Metric Card with Droplet/Opacity Icons, Dew Point, Daily Range & Comfort Progress
 */
@Composable
private fun HumidityMetricCard(
    weather: WeatherBundle,
    modifier: Modifier = Modifier
) {
    val gradeColor = statusColorForGrade(weather.humidityGrade)

    SeaGlassSurface(
        modifier = modifier,
        cornerRadius = 22.dp,
        tintColor = Color(0xFFCFFAFE),
        surfaceAlpha = 0.30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Opacity,
                    contentDescription = "습도",
                    tint = SkyMindCyanRain,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "습도 · 이슬점",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyMindTextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${weather.humidityPercent}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = SkyMindTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = SkyMindBlueAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "이슬점 ${weather.dewPointC.roundToInt()}°C",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyMindBlueAccent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                SeaGlassPill(
                    isSelected = false,
                    cornerRadius = 10.dp,
                    onClick = {}
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = gradeColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = weather.humidityGrade,
                            style = MaterialTheme.typography.labelSmall,
                            color = gradeColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (weather.humidityPercent / 100f).coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = SkyMindCyanRain,
                trackColor = Color.White.copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "오늘 ${weather.humidityMinToday}%~${weather.humidityMaxToday}% · ${weather.humidityAdviceShort}",
                style = MaterialTheme.typography.labelSmall,
                color = SkyMindTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Dedicated Atmospheric Pressure Metric Card with Barometer Gauge, Trend Icon, Surface & Sea-Level hPa
 */
@Composable
private fun PressureMetricCard(
    weather: WeatherBundle,
    modifier: Modifier = Modifier
) {
    val gradeColor = statusColorForGrade(weather.pressureGrade)
    val trendIcon = when {
        weather.pressureDiff3h >= 1.0 -> Icons.AutoMirrored.Filled.TrendingUp
        weather.pressureDiff3h <= -1.0 -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }
    val trendText = when {
        weather.pressureDiff3h >= 0.5 -> String.format(Locale.US, "+%.1f hPa", weather.pressureDiff3h)
        weather.pressureDiff3h <= -0.5 -> String.format(Locale.US, "%.1f hPa", weather.pressureDiff3h)
        else -> "변화 완만"
    }

    SeaGlassSurface(
        modifier = modifier,
        cornerRadius = 22.dp,
        tintColor = Color(0xFFE0F2FE),
        surfaceAlpha = 0.30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "기압",
                    tint = SkyMindBlueAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "기압 · 대기 안정도",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyMindTextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${weather.seaLevelPressureHpa.roundToInt()} hPa",
                        style = MaterialTheme.typography.titleLarge,
                        color = SkyMindTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = trendIcon,
                            contentDescription = null,
                            tint = gradeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${weather.pressureGrade} ($trendText)",
                            style = MaterialTheme.typography.labelSmall,
                            color = gradeColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Barometer Dial Gauge
                val dialCyan = SkyMindCyanRain
                val dialGreen = SkyMindGreenGood
                val dialBlue = SkyMindBlueAccent
                Box(
                    modifier = Modifier.size(48.dp, 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(modifier = Modifier.size(46.dp, 28.dp)) {
                        val strokeWidth = 5.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, (size.height - strokeWidth / 2) * 2)
                        drawArc(
                            color = Color.White.copy(alpha = 0.5f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Normalize pressure between 985 hPa and 1040 hPa
                        val normalized = ((weather.seaLevelPressureHpa - 985.0) / (1040.0 - 985.0))
                            .toFloat()
                            .coerceIn(0.08f, 1f)
                        drawArc(
                            brush = Brush.horizontalGradient(
                                listOf(dialCyan, dialGreen, dialBlue)
                            ),
                            startAngle = 180f,
                            sweepAngle = 180f * normalized,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Compress,
                    contentDescription = null,
                    tint = SkyMindTextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "지면 ${weather.pressureHpa.roundToInt()}hPa · ${weather.pressureAdviceShort}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyMindTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WindCompassCard(
    weather: WeatherBundle,
    modifier: Modifier = Modifier
) {
    SeaGlassSurface(
        modifier = modifier,
        cornerRadius = 22.dp,
        tintColor = Color(0xFFD0F0FD),
        surfaceAlpha = 0.30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = stringResource(R.string.wind_metric_title),
                    tint = SkyMindBlueAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "바람 · 풍속/풍향",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyMindTextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", weather.windSpeedMs)} m/s",
                        style = MaterialTheme.typography.titleLarge,
                        color = SkyMindTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "(${weather.windDirectionCardinal.substringBefore(" ")})",
                        style = MaterialTheme.typography.labelLarge,
                        color = SkyMindBlueAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "돌풍 ${String.format(Locale.US, "%.1f", weather.windGustsMs)} m/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyMindTextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                val sunOrange = SkyMindSunOrange
                val textSecondary = SkyMindTextSecondary
                val blueAccent = SkyMindBlueAccent
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(46.dp)) {
                        val radius = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        drawCircle(
                            color = Color.White.copy(alpha = 0.7f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        for (i in 0 until 8) {
                            val angleRad = Math.toRadians((i * 45.0) - 90.0)
                            val outer = Offset(
                                x = center.x + (radius - 2.dp.toPx()) * cos(angleRad).toFloat(),
                                y = center.y + (radius - 2.dp.toPx()) * sin(angleRad).toFloat()
                            )
                            val inner = Offset(
                                x = center.x + (radius - 6.dp.toPx()) * cos(angleRad).toFloat(),
                                y = center.y + (radius - 6.dp.toPx()) * sin(angleRad).toFloat()
                            )
                            drawLine(
                                color = if (i == 0) sunOrange else textSecondary.copy(alpha = 0.5f),
                                start = inner,
                                end = outer,
                                strokeWidth = 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        rotate(degrees = weather.windDirectionDeg.toFloat(), pivot = center) {
                            val arrowPath = Path().apply {
                                moveTo(center.x, center.y - radius + 8.dp.toPx())
                                lineTo(center.x - 5.dp.toPx(), center.y + 7.dp.toPx())
                                lineTo(center.x, center.y + 2.dp.toPx())
                                lineTo(center.x + 5.dp.toPx(), center.y + 7.dp.toPx())
                                close()
                            }
                            drawPath(path = arrowPath, color = blueAccent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = weather.windFeelDescription,
                style = MaterialTheme.typography.labelSmall,
                color = SkyMindTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PrecipitationCard(
    weather: WeatherBundle,
    modifier: Modifier = Modifier
) {
    val maxProb = maxOf(weather.precipProbabilityNow, weather.precipProbabilityMaxToday)

    SeaGlassSurface(
        modifier = modifier,
        cornerRadius = 22.dp,
        tintColor = Color(0xFFCCFBF1),
        surfaceAlpha = 0.30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = stringResource(R.string.precipitation_metric_title),
                    tint = SkyMindCyanRain,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "비 확률 · 강수량",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyMindTextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$maxProb%",
                        style = MaterialTheme.typography.titleLarge,
                        color = SkyMindTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "현재 ${weather.precipProbabilityNow}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyMindBlueAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                SeaGlassPill(
                    isSelected = false,
                    cornerRadius = 10.dp,
                    onClick = {}
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", weather.todayTotalPrecipMm)}mm",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyMindCyanRain,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (maxProb / 100f).coerceIn(0.04f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = SkyMindCyanRain,
                trackColor = Color.White.copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "시간당 ${String.format(Locale.US, "%.1f", weather.currentPrecipMm)}mm · 누적 ${String.format(Locale.US, "%.1f", weather.todayTotalPrecipMm)}mm",
                style = MaterialTheme.typography.labelSmall,
                color = SkyMindTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UvIndexCard(
    weather: WeatherBundle,
    modifier: Modifier = Modifier
) {
    val peakUv = maxOf(weather.uvIndexCurrent, weather.uvIndexMaxToday)
    val badgeColor = statusColorForGrade(weather.uvGrade)

    SeaGlassSurface(
        modifier = modifier,
        cornerRadius = 22.dp,
        tintColor = Color(0xFFFEF3C7),
        surfaceAlpha = 0.28f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = stringResource(R.string.uv_metric_title),
                    tint = SkyMindSunOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "자외선",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyMindTextSecondary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weather.uvGrade,
                        style = MaterialTheme.typography.titleLarge,
                        color = SkyMindTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "(지수 ${peakUv.roundToInt()})",
                        style = MaterialTheme.typography.labelLarge,
                        color = badgeColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                val uvGreen = SkyMindGreenGood
                val uvOrange = SkyMindSunOrange
                val uvRed = SkyMindDangerRed
                Box(
                    modifier = Modifier.size(50.dp, 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(modifier = Modifier.size(48.dp, 28.dp)) {
                        val strokeWidth = 5.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, (size.height - strokeWidth / 2) * 2)
                        drawArc(
                            color = Color.White.copy(alpha = 0.5f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        val progress = (peakUv / 11.0).toFloat().coerceIn(0.08f, 1f)
                        drawArc(
                            brush = Brush.horizontalGradient(
                                listOf(uvGreen, uvOrange, uvRed)
                            ),
                            startAngle = 180f,
                            sweepAngle = 180f * progress,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = weather.uvAdviceShort,
                style = MaterialTheme.typography.labelSmall,
                color = SkyMindTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AirQualityCard(
    weather: WeatherBundle,
    modifier: Modifier = Modifier
) {
    val pm10Color = statusColorForGrade(weather.pm10Grade)
    val pm25Color = statusColorForGrade(weather.pm25Grade)

    SeaGlassSurface(
        modifier = modifier,
        cornerRadius = 22.dp,
        tintColor = Color(0xFFD1FAE5),
        surfaceAlpha = 0.30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EnergySavingsLeaf,
                    contentDescription = stringResource(R.string.air_quality_metric_title),
                    tint = pm10Color,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "오늘의 미세먼지",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyMindTextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PM10 ${weather.pm10.roundToInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyMindTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                SeaGlassPill(
                    isSelected = false,
                    cornerRadius = 8.dp,
                    onClick = {}
                ) {
                    Text(
                        text = weather.pm10Grade,
                        style = MaterialTheme.typography.labelSmall,
                        color = pm10Color,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PM2.5 ${weather.pm25.roundToInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyMindTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                SeaGlassPill(
                    isSelected = false,
                    cornerRadius = 8.dp,
                    onClick = {}
                ) {
                    Text(
                        text = weather.pm25Grade,
                        style = MaterialTheme.typography.labelSmall,
                        color = pm25Color,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "통합대기 AQI ${weather.aqiScore} (µg/m³)",
                style = MaterialTheme.typography.labelSmall,
                color = SkyMindTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AtmosphereExtraCard(
    weather: WeatherBundle,
    modifier: Modifier = Modifier
) {
    SeaGlassSurface(
        modifier = modifier,
        cornerRadius = 22.dp,
        tintColor = Color(0xFFE0F2FE),
        surfaceAlpha = 0.30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "일출 / 일몰 · 구름양",
                style = MaterialTheme.typography.labelLarge,
                color = SkyMindTextSecondary,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "일출",
                        tint = SkyMindSunOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("일출", style = MaterialTheme.typography.labelSmall, color = SkyMindTextSecondary, maxLines = 1, softWrap = false)
                        Text("오전 ${weather.sunriseTime}", style = MaterialTheme.typography.titleMedium, color = SkyMindTextPrimary, maxLines = 1, softWrap = false)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbTwilight,
                        contentDescription = "일몰",
                        tint = Color(0xFFDB2777),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("일몰", style = MaterialTheme.typography.labelSmall, color = SkyMindTextSecondary, maxLines = 1, softWrap = false)
                        Text("오후 ${weather.sunsetTime}", style = MaterialTheme.typography.titleMedium, color = SkyMindTextPrimary, maxLines = 1, softWrap = false)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "구름양",
                        tint = SkyMindBlueAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("구름양", style = MaterialTheme.typography.labelSmall, color = SkyMindTextSecondary, maxLines = 1, softWrap = false)
                        Text("${weather.cloudCoverPercent}%", style = MaterialTheme.typography.titleMedium, color = SkyMindTextPrimary, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
