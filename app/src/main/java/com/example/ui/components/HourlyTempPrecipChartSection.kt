package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.HourlyForecastItem
import com.example.ui.theme.LocalSkyMindColors
import java.util.Locale
import kotlin.math.roundToInt

enum class WeatherChartSeriesMode(val labelKo: String) {
    COMPOSED("기온 + 강수량 복합"),
    TEMPERATURE_ONLY("기온 (°C) 추이"),
    PRECIPITATION_ONLY("강수량 (mm) · 확률")
}

/**
 * Interactive Composed Dual-Axis Chart (Cubic Spline Area Chart for Temperature +
 * Rounded Bar & Probability Curve for Precipitation + Cartesian Grid + Interactive Scrubbing Tooltip)
 * displayed at the bottom of the Main Dashboard and Forecast screen.
 */
@Composable
fun HourlyTempPrecipChartSection(
    hourlyItems: List<HourlyForecastItem>,
    locationName: String = "",
    modifier: Modifier = Modifier
) {
    if (hourlyItems.isEmpty()) return

    var hourSpanCount by remember { mutableIntStateOf(12) }
    var seriesMode by remember { mutableStateOf(WeatherChartSeriesMode.COMPOSED) }

    val sliceItems = remember(hourlyItems, hourSpanCount) {
        hourlyItems.take(hourSpanCount.coerceAtMost(hourlyItems.size))
    }

    var selectedIndex by remember(sliceItems.size) { mutableIntStateOf(0) }
    val safeSelectedIndex = selectedIndex.coerceIn(0, (sliceItems.size - 1).coerceAtLeast(0))
    val activeItem = sliceItems.getOrNull(safeSelectedIndex) ?: sliceItems.first()

    // Calculate temperature & precipitation domain bounds
    val rawMinTemp = sliceItems.minOfOrNull { it.temp } ?: 10.0
    val rawMaxTemp = sliceItems.maxOfOrNull { it.temp } ?: 25.0
    val minTempAxis = (rawMinTemp - 2.0).roundToInt().toDouble()
    val maxTempAxis = (rawMaxTemp + 2.0).roundToInt().toDouble().coerceAtLeast(minTempAxis + 4.0)
    val tempRange = (maxTempAxis - minTempAxis).coerceAtLeast(4.0)

    val maxPrecipMmRaw = sliceItems.maxOfOrNull { it.precipMm } ?: 0.0
    // Minimum scale of 5.0 mm so small rain bars are proportionally clear and readable
    val maxPrecipAxisMm = maxOf(maxPrecipMmRaw * 1.25, 5.0)
    val totalPrecipMmInSpan = sliceItems.sumOf { it.precipMm }
    val maxProbInSpan = sliceItems.maxOfOrNull { it.precipProb } ?: 0

    val highestTempItem = remember(sliceItems) { sliceItems.maxByOrNull { it.temp } }
    val lowestTempItem = remember(sliceItems) { sliceItems.minByOrNull { it.temp } }
    val peakRainItem = remember(sliceItems) {
        sliceItems.maxByOrNull { it.precipMm * 100.0 + it.precipProb }
    }

    // Capture adaptive colors outside Canvas
    val tokens = LocalSkyMindColors.current
    val tempLineColor = SkyMindSunOrange
    val precipBarColor = SkyMindCyanRain
    val probLineColor = SkyMindBlueAccent
    val goodGreenColor = SkyMindGreenGood
    val primaryTextColor = SkyMindTextPrimary
    val secondaryTextColor = SkyMindTextSecondary
    val gridLineColor = if (tokens.isDark) {
        Color(0xFF99F6E4).copy(alpha = 0.18f)
    } else {
        Color(0xFF0F172A).copy(alpha = 0.12f)
    }
    val crosshairColor = if (tokens.isDark) {
        Color(0xFFF0FDFA).copy(alpha = 0.65f)
    } else {
        Color(0xFF0D9488).copy(alpha = 0.65f)
    }

    SeaGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hourly_temp_precip_chart_card"),
        cornerRadius = 26.dp,
        tintColor = Color(0xFFD6F5F2),
        surfaceAlpha = 0.32f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. Header Row + 12h / 24h Range Toggle
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
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = goodGreenColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "시간대별 기온 · 강수량 변화 그래프",
                            style = MaterialTheme.typography.titleLarge,
                            color = primaryTextColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (locationName.isNotBlank()) {
                                "$locationName · 차트를 터치하거나 좌우로 드래그해 시간별 수치를 확인하세요"
                            } else {
                                "차트를 터치하거나 좌우로 드래그해 시간별 수치를 확인하세요"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 12H / 24H Span Selector
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(12, 24).forEach { span ->
                        val isSelected = hourSpanCount == span
                        SeaGlassPill(
                            isSelected = isSelected,
                            activeColor = Color(0xFF0D9488),
                            onClick = { hourSpanCount = span },
                            modifier = Modifier.testTag("chart_span_${span}h_chip")
                        ) {
                            Text(
                                text = "${span}시간",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else primaryTextColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Series Filter Tabs (Composed / Temperature / Precipitation)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(WeatherChartSeriesMode.entries) { mode ->
                    val isSelected = seriesMode == mode
                    SeaGlassPill(
                        isSelected = isSelected,
                        activeColor = when (mode) {
                            WeatherChartSeriesMode.COMPOSED -> Color(0xFF0D9488)
                            WeatherChartSeriesMode.TEMPERATURE_ONLY -> tempLineColor
                            WeatherChartSeriesMode.PRECIPITATION_ONLY -> probLineColor
                        },
                        onClick = { seriesMode = mode },
                        modifier = Modifier.testTag("chart_mode_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = mode.labelKo,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else primaryTextColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Interactive Scrubbing Tooltip Card (Displays exact metrics at selectedIndex)
            val (activeIcon, activeTint) = weatherIconForCode(activeItem.weatherCode)
            SeaGlassSurface(
                cornerRadius = 18.dp,
                tintColor = Color(0xFFCFFAFE),
                surfaceAlpha = 0.34f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_interactive_tooltip_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(activeTint.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activeIcon,
                                contentDescription = null,
                                tint = activeTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeItem.hourLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = primaryTextColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "습도 ${activeItem.humidityPercent}% · 바람 ${String.format(Locale.US, "%.1f", activeItem.windSpeedMs)}m/s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Temperature Readout
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(tempLineColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "기온 ${String.format(Locale.US, "%.1f", activeItem.temp)}°C",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = primaryTextColor,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                // Precipitation Readout
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(precipBarColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "강수 ${String.format(Locale.US, "%.1f", activeItem.precipMm)}mm (${activeItem.precipProb}%)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (activeItem.precipProb >= 30 || activeItem.precipMm > 0.0) precipBarColor else secondaryTextColor,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "터치하여 시간 선택",
                        tint = secondaryTextColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Main Dual-Axis Composed Chart Area (Left Y-Axis + Interactive Canvas + Right Y-Axis)
            SeaGlassSurface(
                cornerRadius = 20.dp,
                tintColor = Color(0xFFE0F7FA),
                surfaceAlpha = 0.24f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    // Legend Row at top of chart box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (seriesMode != WeatherChartSeriesMode.PRECIPITATION_ONLY) {
                                ChartLegendItem(color = tempLineColor, label = "기온 (°C)")
                            }
                            if (seriesMode != WeatherChartSeriesMode.TEMPERATURE_ONLY) {
                                ChartLegendItem(color = precipBarColor, label = "강수량 (mm)")
                                ChartLegendItem(color = probLineColor, label = "강수확률 (%)")
                            }
                        }
                        Text(
                            text = "최고 ${rawMaxTemp.roundToInt()}° / 최저 ${rawMinTemp.roundToInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Plot Row: Left Y-Axis (°C) + Interactive Chart Canvas + Right Y-Axis (mm / %)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Y-Axis Labels (Temperature °C)
                        val midTempAxis = ((maxTempAxis + minTempAxis) / 2.0).roundToInt()
                        Column(
                            modifier = Modifier
                                .width(34.dp)
                                .fillMaxHeight()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "${maxTempAxis.roundToInt()}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = tempLineColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "${midTempAxis}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryTextColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "${minTempAxis.roundToInt()}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryTextColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Center Interactive Composed Chart Canvas
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("hourly_temp_precip_canvas")
                                    .pointerInput(sliceItems.size) {
                                        detectTapGestures { tapOffset ->
                                            val count = sliceItems.size
                                            if (count > 1) {
                                                val ratio = (tapOffset.x / size.width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                                                selectedIndex = (ratio * (count - 1)).roundToInt().coerceIn(0, count - 1)
                                            }
                                        }
                                    }
                                    .pointerInput(sliceItems.size) {
                                        detectHorizontalDragGestures { change, _ ->
                                            change.consume()
                                            val count = sliceItems.size
                                            if (count > 1) {
                                                val ratio = (change.position.x / size.width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                                                selectedIndex = (ratio * (count - 1)).roundToInt().coerceIn(0, count - 1)
                                            }
                                        }
                                    }
                            ) {
                                val count = sliceItems.size
                                if (count == 0) return@Canvas

                                val topPad = 16.dp.toPx()
                                val bottomPad = 14.dp.toPx()
                                val leftPad = 10.dp.toPx()
                                val rightPad = 10.dp.toPx()
                                val plotW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                                val plotH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                                val bottomY = topPad + plotH

                                fun xForIndex(idx: Int): Float {
                                    return if (count <= 1) {
                                        leftPad + plotW / 2f
                                    } else {
                                        leftPad + (idx.toFloat() / (count - 1).toFloat()) * plotW
                                    }
                                }

                                fun yForTemp(temp: Double): Float {
                                    val norm = ((temp - minTempAxis) / tempRange).toFloat().coerceIn(0f, 1f)
                                    return bottomY - norm * plotH
                                }

                                fun yForProb(prob: Int): Float {
                                    val norm = (prob / 100f).coerceIn(0f, 1f)
                                    return bottomY - norm * (plotH * 0.72f)
                                }

                                // 1) Horizontal & Vertical Cartesian Dashed Grid Lines
                                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                for (g in 0..3) {
                                    val gy = topPad + (g / 3f) * plotH
                                    drawLine(
                                        color = gridLineColor,
                                        start = Offset(leftPad, gy),
                                        end = Offset(size.width - rightPad, gy),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = if (g in 1..2) dashEffect else null
                                    )
                                }

                                val stepIndices = if (count <= 12) (0 until count step 2) else (0 until count step 4)
                                for (idx in stepIndices) {
                                    val gx = xForIndex(idx)
                                    drawLine(
                                        color = gridLineColor.copy(alpha = gridLineColor.alpha * 0.65f),
                                        start = Offset(gx, topPad),
                                        end = Offset(gx, bottomY),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = dashEffect
                                    )
                                }

                                // 2) Precipitation Bars & Probability Curve (when COMPOSED or PRECIPITATION_ONLY)
                                if (seriesMode != WeatherChartSeriesMode.TEMPERATURE_ONLY) {
                                    val slotWidth = (plotW / count.coerceAtLeast(1)).coerceAtMost(24.dp.toPx())
                                    val barWidth = (slotWidth * 0.56f).coerceAtLeast(6.dp.toPx())

                                    sliceItems.forEachIndexed { idx, item ->
                                        val cx = xForIndex(idx)
                                        // Height reflects actual precipitation mm or probability baseline
                                        val mmFraction = (item.precipMm / maxPrecipAxisMm).toFloat().coerceIn(0f, 1f)
                                        val probBaselineFraction = (item.precipProb / 100f) * 0.35f
                                        val effectiveFraction = maxOf(mmFraction, probBaselineFraction)
                                        val maxBarAreaH = if (seriesMode == WeatherChartSeriesMode.PRECIPITATION_ONLY) {
                                            plotH * 0.85f
                                        } else {
                                            plotH * 0.55f
                                        }
                                        val barH = (effectiveFraction * maxBarAreaH).coerceAtLeast(3.dp.toPx())
                                        val isBarSelected = idx == safeSelectedIndex

                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    precipBarColor.copy(alpha = if (isBarSelected) 0.92f else 0.62f),
                                                    probLineColor.copy(alpha = if (isBarSelected) 0.55f else 0.24f)
                                                ),
                                                startY = bottomY - barH,
                                                endY = bottomY
                                            ),
                                            topLeft = Offset(cx - barWidth / 2f, bottomY - barH),
                                            size = Size(barWidth, barH),
                                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                        )
                                    }

                                    // Smooth Precipitation Probability Line
                                    if (count > 1) {
                                        val probPoints = sliceItems.mapIndexed { idx, item ->
                                            Offset(xForIndex(idx), yForProb(item.precipProb))
                                        }
                                        val probPath = Path().apply {
                                            moveTo(probPoints.first().x, probPoints.first().y)
                                            for (i in 0 until probPoints.size - 1) {
                                                val p0 = probPoints[i]
                                                val p1 = probPoints[i + 1]
                                                val ctrlX = (p0.x + p1.x) / 2f
                                                cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                            }
                                        }
                                        drawPath(
                                            path = probPath,
                                            color = probLineColor.copy(alpha = 0.78f),
                                            style = Stroke(
                                                width = 2.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                                            )
                                        )
                                    }
                                }

                                // 3) Smooth Cubic Spline Temperature Curve + Gradient Area Fill
                                if (seriesMode != WeatherChartSeriesMode.PRECIPITATION_ONLY && count > 1) {
                                    val tempPoints = sliceItems.mapIndexed { idx, item ->
                                        Offset(xForIndex(idx), yForTemp(item.temp))
                                    }

                                    val linePath = Path().apply {
                                        moveTo(tempPoints.first().x, tempPoints.first().y)
                                        for (i in 0 until tempPoints.size - 1) {
                                            val p0 = tempPoints[i]
                                            val p1 = tempPoints[i + 1]
                                            val ctrlX = (p0.x + p1.x) / 2f
                                            cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                        }
                                    }

                                    val areaPath = Path().apply {
                                        addPath(linePath)
                                        lineTo(tempPoints.last().x, bottomY)
                                        lineTo(tempPoints.first().x, bottomY)
                                        close()
                                    }

                                    drawPath(
                                        path = areaPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                tempLineColor.copy(alpha = 0.36f),
                                                tempLineColor.copy(alpha = 0.08f),
                                                Color.Transparent
                                            ),
                                            startY = topPad,
                                            endY = bottomY
                                        )
                                    )

                                    drawPath(
                                        path = linePath,
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(tempLineColor, goodGreenColor, tempLineColor)
                                        ),
                                        style = Stroke(
                                            width = 3.2.dp.toPx(),
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )

                                    // Draw Dots on Temperature Curve
                                    tempPoints.forEachIndexed { idx, pt ->
                                        val isActive = idx == safeSelectedIndex
                                        if (isActive || count <= 12 || idx % 2 == 0) {
                                            drawCircle(
                                                color = Color.White,
                                                radius = if (isActive) 6.5.dp.toPx() else 3.8.dp.toPx(),
                                                center = pt
                                            )
                                            drawCircle(
                                                color = if (isActive) goodGreenColor else tempLineColor,
                                                radius = if (isActive) 4.5.dp.toPx() else 2.5.dp.toPx(),
                                                center = pt
                                            )
                                        }
                                    }
                                }

                                // 4) Interactive Selected Hour Vertical Crosshair Line
                                val activeX = xForIndex(safeSelectedIndex)
                                drawLine(
                                    color = crosshairColor,
                                    start = Offset(activeX, topPad),
                                    end = Offset(activeX, bottomY),
                                    strokeWidth = 1.8.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Right Y-Axis Labels (Precipitation mm / %)
                        Column(
                            modifier = Modifier
                                .width(42.dp)
                                .fillMaxHeight()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.0f", maxPrecipAxisMm)}mm",
                                style = MaterialTheme.typography.labelSmall,
                                color = precipBarColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "50%",
                                style = MaterialTheme.typography.labelSmall,
                                color = probLineColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "0mm",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryTextColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // X-Axis Interactive Hour Labels Row (Tap any label to jump crosshair)
                    val tickStep = if (sliceItems.size <= 12) 2 else 4
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp, end = 44.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (idx in sliceItems.indices step tickStep) {
                            val item = sliceItems[idx]
                            val isSelectedTick = kotlin.math.abs(idx - safeSelectedIndex) < (tickStep / 2).coerceAtLeast(1)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedIndex = idx }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.hourLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelectedTick) goodGreenColor else secondaryTextColor,
                                    fontWeight = if (isSelectedTick) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${item.temp.roundToInt()}°",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelectedTick) primaryTextColor else secondaryTextColor.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Key Summary Highlights Row (Peak Temp, Low Temp, Expected Rain Summary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartInsightMiniTile(
                    icon = Icons.Default.DeviceThermostat,
                    iconTint = tempLineColor,
                    title = "최고 기온 시각",
                    value = highestTempItem?.let { "${it.hourLabel} (${it.temp.roundToInt()}°C)" } ?: "-",
                    modifier = Modifier.weight(1f)
                )
                ChartInsightMiniTile(
                    icon = Icons.Default.Air,
                    iconTint = probLineColor,
                    title = "최저 기온 시각",
                    value = lowestTempItem?.let { "${it.hourLabel} (${it.temp.roundToInt()}°C)" } ?: "-",
                    modifier = Modifier.weight(1f)
                )
                ChartInsightMiniTile(
                    icon = if (maxProbInSpan >= 40 || totalPrecipMmInSpan > 0.2) Icons.Default.Umbrella else Icons.Default.WaterDrop,
                    iconTint = precipBarColor,
                    title = "누적 강수 · 최대 확률",
                    value = "${String.format(Locale.US, "%.1f", totalPrecipMmInSpan)}mm · ${maxProbInSpan}%",
                    modifier = Modifier.weight(1f)
                )
            }

            if (peakRainItem != null && (peakRainItem.precipProb >= 30 || peakRainItem.precipMm > 0.0)) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = tokens.dividerColor)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Umbrella,
                        contentDescription = null,
                        tint = precipBarColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${peakRainItem.hourLabel} 무렵 강수확률 ${peakRainItem.precipProb}% (예상 ${String.format(Locale.US, "%.1f", peakRainItem.precipMm)}mm)로 가장 높으니 외출 시간대에 참고하세요.",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryTextColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SkyMindTextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ChartInsightMiniTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    SeaGlassSurface(
        cornerRadius = 16.dp,
        tintColor = Color(0xFFCFFAFE),
        surfaceAlpha = 0.24f,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyMindTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = SkyMindTextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
