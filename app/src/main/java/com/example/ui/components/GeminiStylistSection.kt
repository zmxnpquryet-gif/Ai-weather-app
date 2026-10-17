package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GeminiWeatherAdvice
import com.example.model.WeatherBundle
import com.example.ui.theme.AtmosphericThemeSpec
import com.example.viewmodel.PersonaStyles
import com.example.viewmodel.QaMessage
import kotlin.math.roundToInt

/**
 * Cute SkyMind Mascot Avatar matching the blue smiling cloud/robot bubble.
 */
@Composable
fun SkyMindBotAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 44
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.85f), Color(0xFF0284C7).copy(alpha = 0.85f))
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((sizeDp * 0.65f).dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = Color(0xFF0F2942),
                topLeft = Offset(w * 0.1f, h * 0.22f),
                size = Size(w * 0.8f, h * 0.56f),
                cornerRadius = CornerRadius(w * 0.25f, w * 0.25f)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.09f,
                center = Offset(w * 0.34f, h * 0.48f)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.09f,
                center = Offset(w * 0.66f, h * 0.48f)
            )
            drawCircle(
                color = Color(0xFFF472B6).copy(alpha = 0.75f),
                radius = w * 0.06f,
                center = Offset(w * 0.24f, h * 0.60f)
            )
            drawCircle(
                color = Color(0xFFF472B6).copy(alpha = 0.75f),
                radius = w * 0.06f,
                center = Offset(w * 0.76f, h * 0.60f)
            )
        }
    }
}

/**
 * Compact Weather Briefing Speech Bubble Card displayed right below the Hero section on the Home screen.
 * Uses translucent SeaGlassSurface instead of opaque Card.
 */
@Composable
fun HomeAiGreetingBubble(
    advice: GeminiWeatherAdvice?,
    activeTheme: AtmosphericThemeSpec,
    isAutoTheme: Boolean,
    isGenerating: Boolean,
    onClickExpandAi: () -> Unit,
    onClickThemeGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    SeaGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_ai_greeting_bubble"),
        cornerRadius = 26.dp,
        tintColor = Color(0xFFD6F5F2),
        surfaceAlpha = 0.30f,
        onClick = onClickExpandAi
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SkyMindBotAvatar(sizeDp = 46)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = advice?.headline ?: "오늘의 날씨와 옷차림을 분석하고 있어요!",
                        style = MaterialTheme.typography.titleMedium,
                        color = SkyMindTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = advice?.summaryComment ?: "기온, 바람, 자외선, 미세먼지, 강수확률을 종합하고 있어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyMindTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Translucent Theme Pill Banner
            SeaGlassSurface(
                cornerRadius = 16.dp,
                tintColor = Color(0xFFB2F5EA),
                surfaceAlpha = 0.26f,
                onClick = onClickThemeGallery,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAutoTheme) {
                                "날씨 맞춤 테마: ${activeTheme.nameKo}"
                            } else {
                                "선택한 테마: ${activeTheme.nameKo}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = SkyMindTextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "테마 변경 >",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyMindTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Lifestyle & Outfit Recommendation Section ("오늘의 옷차림 & 외출 가이드")
 * Every sub-card, chip, checklist row, and activity badge uses translucent SeaGlassSurface/SeaGlassPill!
 */
@Composable
fun GeminiStylistSection(
    weather: WeatherBundle?,
    advice: GeminiWeatherAdvice?,
    isGenerating: Boolean,
    selectedPersona: String,
    qaHistory: List<QaMessage>,
    onSelectPersona: (String) -> Unit,
    onRefreshAdvice: () -> Unit,
    onAskQuestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SeaGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gemini_stylist_card"),
        cornerRadius = 26.dp,
        tintColor = Color(0xFFD6F5F2),
        surfaceAlpha = 0.30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
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
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = null,
                        tint = SkyMindBlueAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "오늘의 옷차림 & 외출 가이드",
                            style = MaterialTheme.typography.titleLarge,
                            color = SkyMindTextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "기온 · 바람 · 자외선 · 미세먼지 · 강수량 맞춤 추천",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyMindTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshAdvice,
                    modifier = Modifier.testTag("refresh_gemini_button")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = SkyMindBlueAccent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_weather),
                            tint = SkyMindTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Persona Sea Glass Pill Tabs (의상 / 외출 / 운동 / 여행 등)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
                modifier = Modifier.testTag("persona_selector_row")
            ) {
                items(PersonaStyles) { persona ->
                    val isSelected = persona == selectedPersona
                    SeaGlassPill(
                        isSelected = isSelected,
                        activeColor = Color(0xFF0D9488),
                        onClick = { onSelectPersona(persona) }
                    ) {
                        Text(
                            text = persona,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else SkyMindTextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (advice != null) {
                // Featured Outfit Card - Translucent Sea Glass
                SeaGlassSurface(
                    cornerRadius = 22.dp,
                    tintColor = Color(0xFFE0F7FA),
                    surfaceAlpha = 0.26f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "오늘의 추천 복장",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SkyMindBlueAccent,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = advice.outfitOuter,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SkyMindTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (weather != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                SeaGlassPill(
                                    isSelected = false,
                                    cornerRadius = 14.dp,
                                    onClick = {}
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = SkyMindSunOrange,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${weather.lowTemp.roundToInt()}° / ${weather.highTemp.roundToInt()}°",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SkyMindTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = advice.stylingReason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyMindTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4-part outfit grid - Translucent Sea Glass SubCards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutfitSubCard(
                                label = "상의 (Top)",
                                item = advice.outfitTop,
                                modifier = Modifier.weight(1f)
                            )
                            OutfitSubCard(
                                label = "하의 (Bottom)",
                                item = advice.outfitBottom,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutfitSubCard(
                            label = "신발 & 외출 필수 소품",
                            item = advice.outfitShoesAndAcc,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3-Pillar Essential Checklist (우산 지참 / 미세먼지 마스크 / 자외선 선크림) - Translucent Sea Glass
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("decision_badges_column")
                ) {
                    DecisionCheckRow(
                        icon = Icons.Default.Umbrella,
                        categoryTitle = "우산 챙길까?",
                        statusBadge = advice.umbrellaStatus,
                        reasonText = advice.umbrellaReason,
                        accentColor = SkyMindCyanRain,
                        tintColor = Color(0xFFCCFBF1)
                    )
                    DecisionCheckRow(
                        icon = Icons.Default.HealthAndSafety,
                        categoryTitle = "미세먼지 마스크",
                        statusBadge = advice.maskStatus,
                        reasonText = advice.maskReason,
                        accentColor = statusColorForGrade(advice.maskStatus),
                        tintColor = Color(0xFFD1FAE5)
                    )
                    DecisionCheckRow(
                        icon = Icons.Default.WbSunny,
                        categoryTitle = "자외선 케어",
                        statusBadge = advice.uvCareStatus,
                        reasonText = advice.uvCareReason,
                        accentColor = SkyMindSunOrange,
                        tintColor = Color(0xFFFEF3C7)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Today's Recommended Activities Row ("오늘의 추천 활동: 산책 / 피크닉 / 카페 투어")
                Text(
                    text = "오늘의 추천 활동",
                    style = MaterialTheme.typography.titleMedium,
                    color = SkyMindTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(8.dp))
                val isOutdoorGood = (weather?.precipProbabilityMaxToday ?: 0) < 40 && (weather?.pm10 ?: 20.0) <= 80
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActivityStatusBadge(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        title = "산책",
                        status = if (isOutdoorGood) "좋음" else "실내권장",
                        color = if (isOutdoorGood) SkyMindGreenGood else SkyMindSunOrange,
                        modifier = Modifier.weight(1f)
                    )
                    ActivityStatusBadge(
                        icon = Icons.Default.Park,
                        title = "피크닉",
                        status = if (isOutdoorGood) "좋음" else "보통",
                        color = if (isOutdoorGood) SkyMindGreenGood else SkyMindSunOrange,
                        modifier = Modifier.weight(1f)
                    )
                    ActivityStatusBadge(
                        icon = Icons.Default.LocalCafe,
                        title = "카페 투어",
                        status = "좋음",
                        color = SkyMindGreenGood,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityStatusBadge(
    icon: ImageVector,
    title: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    SeaGlassSurface(
        modifier = modifier,
        cornerRadius = 18.dp,
        tintColor = Color(0xFFD1FAE5),
        surfaceAlpha = 0.26f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = SkyMindTextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun DecisionCheckRow(
    icon: ImageVector,
    categoryTitle: String,
    statusBadge: String,
    reasonText: String,
    accentColor: Color,
    tintColor: Color
) {
    SeaGlassSurface(
        cornerRadius = 18.dp,
        tintColor = tintColor,
        surfaceAlpha = 0.26f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        imageVector = icon,
                        contentDescription = categoryTitle,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = SkyMindTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                SeaGlassPill(
                    isSelected = false,
                    cornerRadius = 50.dp,
                    onClick = {}
                ) {
                    Text(
                        text = statusBadge,
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reasonText,
                style = MaterialTheme.typography.bodyMedium,
                color = SkyMindTextSecondary
            )
        }
    }
}

@Composable
private fun OutfitSubCard(
    label: String,
    item: String,
    modifier: Modifier = Modifier
) {
    SeaGlassSurface(
        modifier = modifier,
        cornerRadius = 16.dp,
        tintColor = Color(0xFFCFFAFE),
        surfaceAlpha = 0.24f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SkyMindBlueAccent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item,
                style = MaterialTheme.typography.bodyMedium,
                color = SkyMindTextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
