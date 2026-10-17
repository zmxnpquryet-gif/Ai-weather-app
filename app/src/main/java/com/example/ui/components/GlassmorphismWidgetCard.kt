package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GeminiWeatherAdvice
import com.example.model.WeatherBundle
import com.example.ui.theme.LocalSkyMindColors
import com.example.widget.WidgetDataSyncManager
import kotlin.math.roundToInt

/**
 * Interactive Glassmorphism Home Screen Widget Preview & Quick-Pin Card.
 * Mirrors the exact 3-zone Glassmorphism layout of the Android Home Screen Widget:
 * 1) Zone 1: Frosted Glass Current Location Pill + Umbrella Status Badge
 * 2) Zone 2: Prominent Current Temperature Hero Display + Weather & Sub-metrics
 * 3) Zone 3: Elevated Glassmorphism Short Outfit Guide Card + Outer/Top/Bottom Glass Tags
 */
@Composable
fun GlassmorphismHomeWidgetPreviewCard(
    weather: WeatherBundle?,
    advice: GeminiWeatherAdvice?,
    onPinOrSyncWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSkyMindColors.current

    val locDisplay = weather?.location?.displayWithOriginalName ?: "서울"
    val gpsSuffix = if (weather?.location?.isCurrentGps == true) " · 현위치" else ""
    val tempC = weather?.currentTemp?.roundToInt() ?: 18
    val feelsC = weather?.feelsLikeTemp?.roundToInt() ?: 17
    val highC = weather?.highTemp?.roundToInt() ?: 22
    val lowC = weather?.lowTemp?.roundToInt() ?: 12
    val summary = weather?.weatherSummary ?: "맑고 쾌청함"
    val humidity = weather?.humidityPercent ?: 50
    val pm10Grade = weather?.pm10Grade ?: "좋음"
    val umbrella = advice?.umbrellaStatus?.takeIf { it.isNotBlank() }
        ?: if ((weather?.precipProbabilityMaxToday ?: 0) >= 40) "접이식 우산 권장" else "우산 불필요"
    val outer = advice?.outfitOuter?.takeIf { it.isNotBlank() } ?: "가벼운 트렌치코트"
    val top = advice?.outfitTop?.takeIf { it.isNotBlank() } ?: "산뜻한 셔츠/니트"
    val bottom = advice?.outfitBottom?.takeIf { it.isNotBlank() } ?: "데님/슬랙스"
    val shoes = advice?.outfitShoesAndAcc?.takeIf { it.isNotBlank() } ?: "레더 스니커즈"
    val shortGuide = WidgetDataSyncManager.buildConciseOutfitGuide(weather, advice, outer, top)
    val emoji = WidgetDataSyncManager.weatherEmojiForCode(weather?.weatherCode ?: 0)

    // Glassmorphism gradient & border tokens for Light / Dark Mode
    val outerGlassGradient = if (tokens.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0E2B3E).copy(alpha = 0.78f),
                Color(0xFF13364E).copy(alpha = 0.64f),
                Color(0xFF0A1D2B).copy(alpha = 0.74f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF).copy(alpha = 0.74f),
                Color(0xFFE0F7FA).copy(alpha = 0.62f),
                Color(0xFFF0FDFA).copy(alpha = 0.68f)
            )
        )
    }

    val specularRimBrush = if (tokens.isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF5EEAD4).copy(alpha = 0.58f),
                Color(0xFF38BDF8).copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f),
                Color(0xFF99F6E4).copy(alpha = 0.55f)
            )
        )
    }

    val innerOutfitGlassBrush = if (tokens.isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF163D57).copy(alpha = 0.76f),
                Color(0xFF0F2B3F).copy(alpha = 0.66f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.82f),
                Color(0xFFE6FAF8).copy(alpha = 0.68f)
            )
        )
    }

    val pillFillColor = if (tokens.isDark) {
        Color(0xFF18435E).copy(alpha = 0.68f)
    } else {
        Color.White.copy(alpha = 0.76f)
    }

    val pillBorderColor = if (tokens.isDark) {
        Color(0xFF5EEAD4).copy(alpha = 0.42f)
    } else {
        Color.White.copy(alpha = 0.90f)
    }

    SeaGlassSurface(
        cornerRadius = 26.dp,
        tintColor = Color(0xFFD6F5F2),
        surfaceAlpha = 0.30f,
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_widget_preview_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Section Title Row
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
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = SkyMindBlueAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "홈 화면 글래스모피즘 위젯",
                            style = MaterialTheme.typography.titleLarge,
                            color = SkyMindTextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "현재 위치 · 온도 · 짧은 옷차림 가이드를 홈 화면에서 바로 확인하세요",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyMindTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // === Glassmorphism Widget Frame ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(outerGlassGradient)
                    .border(
                        width = 1.5.dp,
                        brush = specularRimBrush,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(14.dp)
                    .testTag("glassmorphism_widget_live_frame")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // ZONE 1: Frosted Glass Current Location Pill + Umbrella Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Location Frosted Glass Pill
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(50))
                                .background(pillFillColor)
                                .border(1.dp, pillBorderColor, RoundedCornerShape(50))
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SkyMindGreenGood,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$locDisplay$gpsSuffix",
                                style = MaterialTheme.typography.labelLarge,
                                color = SkyMindTextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Umbrella Status Badge + Sync Icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF0D9488).copy(alpha = 0.88f))
                                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Umbrella,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = umbrella,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(pillFillColor)
                                    .border(1.dp, pillBorderColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "위젯 갱신",
                                    tint = SkyMindBlueAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ZONE 2: Prominent Current Temperature + Weather Summary & Sub-Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$emoji ${tempC}°C",
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                            color = SkyMindTextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$summary · 체감 ${feelsC}°C",
                                style = MaterialTheme.typography.titleMedium,
                                color = SkyMindTextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "최고 ${highC}° / 최저 ${lowC}° · 습도 ${humidity}% · 미세먼지 $pm10Grade",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyMindTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ZONE 3: Elevated Glassmorphism Short Outfit Guide Panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(innerOutfitGlassBrush)
                            .border(
                                width = 1.2.dp,
                                brush = specularRimBrush,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Short 1-Line Outfit Guide Headline
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = SkyMindGreenGood,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = shortGuide,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SkyMindGreenGood,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(7.dp))

                            // Structured 3-Pill Outfit Breakdown (겉옷 / 상의 / 하의)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassOutfitItemChip(
                                    text = "🧥 $outer",
                                    fillColor = pillFillColor,
                                    borderColor = pillBorderColor,
                                    modifier = Modifier.weight(1f)
                                )
                                GlassOutfitItemChip(
                                    text = "👕 $top",
                                    fillColor = pillFillColor,
                                    borderColor = pillBorderColor,
                                    modifier = Modifier.weight(1f)
                                )
                                GlassOutfitItemChip(
                                    text = "👖 $bottom",
                                    fillColor = pillFillColor,
                                    borderColor = pillBorderColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            // Shoes & Styling Tip Footer
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Checkroom,
                                    contentDescription = null,
                                    tint = SkyMindBlueAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "신발/소품: $shoes · 홈 화면 위젯 실시간 연동",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyMindTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onPinOrSyncWidget,
                colors = ButtonDefaults.buttonColors(containerColor = SkyMindBlueAccent),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_home_widget_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "홈 화면에 글래스모피즘 위젯 추가 / 데이터 동기화",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun GlassOutfitItemChip(
    text: String,
    fillColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(fillColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = SkyMindTextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
