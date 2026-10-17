package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DailyForecastItem
import com.example.model.HourlyForecastItem
import kotlin.math.roundToInt

@Composable
fun HourlyForecastSection(
    hourlyItems: List<HourlyForecastItem>,
    timeSlotTips: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val displayItems = hourlyItems.take(12)
    val minTemp = displayItems.minOfOrNull { it.temp } ?: 10.0
    val maxTemp = displayItems.maxOfOrNull { it.temp } ?: 25.0
    val tempSpan = (maxTemp - minTemp).coerceAtLeast(1.0)

    SeaGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hourly_forecast_card"),
        cornerRadius = 26.dp,
        tintColor = Color(0xFFD6F5F2),
        surfaceAlpha = 0.30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "시간별 예보",
                    style = MaterialTheme.typography.titleLarge,
                    color = SkyMindTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                SeaGlassPill(
                    isSelected = false,
                    onClick = {}
                ) {
                    Text(
                        text = "오늘 · 내일",
                        style = MaterialTheme.typography.labelLarge,
                        color = SkyMindBlueAccent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("hourly_forecast_list")
            ) {
                itemsIndexed(displayItems) { idx, item ->
                    val (icon, tint) = weatherIconForCode(item.weatherCode)
                    val isNow = idx == 0
                    val normalizedY = 1f - ((item.temp - minTemp) / tempSpan).toFloat().coerceIn(0f, 1f)

                    SeaGlassSurface(
                        cornerRadius = 18.dp,
                        tintColor = if (isNow) Color(0xFF99F6E4) else Color(0xFFCFFAFE),
                        surfaceAlpha = if (isNow) 0.38f else 0.22f,
                        modifier = Modifier.width(74.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.hourLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isNow) SkyMindGreenGood else SkyMindTextSecondary,
                                fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${item.temp.roundToInt()}°",
                                style = MaterialTheme.typography.titleMedium,
                                color = SkyMindTextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )

                            val blueAccent = SkyMindBlueAccent
                            Box(
                                modifier = Modifier
                                    .height(26.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(60.dp, 24.dp)) {
                                    val dotY = 4.dp.toPx() + normalizedY * 14.dp.toPx()
                                    drawLine(
                                        color = blueAccent.copy(alpha = 0.40f),
                                        start = Offset(0f, dotY),
                                        end = Offset(size.width, dotY),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                    drawCircle(
                                        color = blueAccent,
                                        radius = 4.dp.toPx(),
                                        center = Offset(size.width / 2f, dotY)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = if (item.precipProb >= 30) SkyMindCyanRain else SkyMindTextSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${item.precipProb}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.precipProb >= 30) SkyMindCyanRain else SkyMindTextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            if (timeSlotTips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "시간대별 날씨 포인트",
                    style = MaterialTheme.typography.titleMedium,
                    color = SkyMindTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeSlotTips.forEachIndexed { i, tip ->
                        val (tipIcon, tipTint) = when (i) {
                            0 -> Icons.Default.WbSunny to SkyMindSunOrange
                            1 -> Icons.Default.DeviceThermostat to SkyMindDangerRed
                            else -> Icons.Default.Air to SkyMindBlueAccent
                        }
                        SeaGlassSurface(
                            cornerRadius = 16.dp,
                            tintColor = Color(0xFFE0F7FA),
                            surfaceAlpha = 0.22f,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(tipTint.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tipIcon,
                                        contentDescription = null,
                                        tint = tipTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SkyMindTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyForecastSection(
    dailyItems: List<DailyForecastItem>,
    modifier: Modifier = Modifier
) {
    val globalMin = dailyItems.minOfOrNull { it.minTemp } ?: 0.0
    val globalMax = dailyItems.maxOfOrNull { it.maxTemp } ?: 30.0
    val totalRange = (globalMax - globalMin).coerceAtLeast(1.0)

    SeaGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_forecast_card"),
        cornerRadius = 26.dp,
        tintColor = Color(0xFFD6F5F2),
        surfaceAlpha = 0.30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "주간 예보",
                style = MaterialTheme.typography.titleLarge,
                color = SkyMindTextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dailyItems.forEachIndexed { index, day ->
                    val (icon, tint) = weatherIconForCode(day.weatherCode)

                    SeaGlassSurface(
                        cornerRadius = 16.dp,
                        tintColor = if (index == 0) Color(0xFF99F6E4) else Color(0xFFCFFAFE),
                        surfaceAlpha = if (index == 0) 0.32f else 0.20f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.width(64.dp)) {
                                Text(
                                    text = day.dayOfWeek,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (index == 0) SkyMindGreenGood else SkyMindTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = day.dateLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyMindTextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Icon(
                                imageVector = icon,
                                contentDescription = day.weatherDescription,
                                tint = tint,
                                modifier = Modifier.size(26.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${day.minTemp.roundToInt()}°",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SkyMindTextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(6.dp))

                                val startFraction = ((day.minTemp - globalMin) / totalRange).toFloat().coerceIn(0f, 0.85f)
                                val widthFraction = ((day.maxTemp - day.minTemp) / totalRange).toFloat().coerceIn(0.15f, 1f - startFraction)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.45f))
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        if (startFraction > 0.01f) {
                                            Spacer(modifier = Modifier.weight(startFraction))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(widthFraction.coerceAtLeast(0.1f))
                                                .height(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(SkyMindBlueAccent, SkyMindSunOrange)
                                                    )
                                                )
                                        )
                                        val endSpace = (1f - startFraction - widthFraction).coerceAtLeast(0.01f)
                                        if (endSpace > 0.02f) {
                                            Spacer(modifier = Modifier.weight(endSpace))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${day.maxTemp.roundToInt()}°",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SkyMindTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Row(
                                modifier = Modifier.width(56.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = if (day.precipProbMax >= 40) SkyMindCyanRain else SkyMindTextSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${day.precipProbMax}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (day.precipProbMax >= 40) SkyMindCyanRain else SkyMindTextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            SeaGlassSurface(
                cornerRadius = 18.dp,
                tintColor = Color(0xFFB2F5EA),
                surfaceAlpha = 0.28f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = SkyMindGreenGood,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "이번 주는 일교차가 커요!",
                            style = MaterialTheme.typography.titleMedium,
                            color = SkyMindTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "아침에는 쌀쌀하고 낮에는 포근하니 입고 벗기 편한 겉옷을 챙겨 다니세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyMindTextSecondary
                        )
                    }
                }
            }
        }
    }
}
