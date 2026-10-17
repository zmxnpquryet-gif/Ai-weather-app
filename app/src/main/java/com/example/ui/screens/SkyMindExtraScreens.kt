package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.CityLocation
import com.example.model.GeminiWeatherAdvice
import com.example.model.WeatherBundle
import com.example.notification.MorningBriefingScheduler
import com.example.ui.components.GlassmorphismHomeWidgetPreviewCard
import com.example.ui.components.InteractiveOsmWeatherMap
import com.example.ui.components.SeaGlassPill
import com.example.ui.components.SeaGlassSurface
import com.example.ui.components.SkyMindBlueAccent
import com.example.ui.components.SkyMindBotAvatar
import com.example.ui.components.SkyMindGreenGood
import com.example.ui.components.SkyMindTextPrimary
import com.example.ui.components.SkyMindTextSecondary
import com.example.ui.theme.AtmosphericThemeSpec
import com.example.ui.theme.LocalSkyMindColors
import com.example.ui.theme.SkyMindThemeMode
import com.example.ui.theme.WeatherThemeCatalog
import com.example.viewmodel.QaMessage
import com.example.widget.WidgetDataSyncManager
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tab 3: Interactive Real OpenStreetMap (OSM) Weather Map & Direct Coordinate Picker + Search + Pin Manager
 */
@Composable
fun WeatherRadarMapScreen(
    weather: WeatherBundle,
    pinnedLocations: List<CityLocation>,
    isCurrentPinned: Boolean,
    searchQuery: String,
    searchResults: List<CityLocation>,
    onSearchQueryChange: (String) -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onPickCoordinates: (lat: Double, lon: Double, pinNow: Boolean) -> Unit,
    onTogglePinCurrent: () -> Unit,
    onRemovePin: (CityLocation) -> Unit,
    onRequestGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mapLayers = listOf("강수", "기온", "미세먼지", "바람")
    var selectedLayer by remember { mutableStateOf("강수") }
    var timeOffsetHours by remember { mutableFloatStateOf(0f) }

    val hourIndex = timeOffsetHours.roundToInt().coerceIn(0, (weather.hourlyItems.size - 1).coerceAtLeast(0))
    val currentSlot = weather.hourlyItems.getOrNull(hourIndex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("weather_map_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "실시간 날씨 지도 (OpenStreetMap)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "실제 지도를 드래그·확대하고 원하는 동네를 터치하거나 검색해 핀 고정하세요",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }

        // Direct Search Bar inside Map Tab - Translucent Sea Glass
        item {
            SeaGlassSurface(
                cornerRadius = 22.dp,
                tintColor = Color(0xFFD6F5F2),
                surfaceAlpha = 0.30f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = SkyMindBlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = {
                                Text(
                                    text = "도시·동네 검색 (예: 판교, 해운대, 여의도, 도쿄)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SkyMindTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkyMindBlueAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.50f),
                                focusedContainerColor = Color(0xFFE0F7FA).copy(alpha = 0.30f),
                                unfocusedContainerColor = Color(0xFFE0F7FA).copy(alpha = 0.20f),
                                focusedTextColor = SkyMindTextPrimary,
                                unfocusedTextColor = SkyMindTextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("map_city_search_input")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onRequestGps,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SkyMindBlueAccent.copy(alpha = 0.18f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "현위치",
                                tint = SkyMindBlueAccent
                            )
                        }
                    }

                    // Search Results with instant Select & Pin buttons
                    if (searchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            searchResults.forEach { resultCity ->
                                SeaGlassSurface(
                                    cornerRadius = 14.dp,
                                    tintColor = Color(0xFFCFFAFE),
                                    surfaceAlpha = 0.28f,
                                    onClick = { onSelectCity(resultCity) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = resultCity.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = SkyMindTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${resultCity.adminArea} ${resultCity.country} (${String.format(Locale.US, "%.2f", resultCity.latitude)}, ${String.format(Locale.US, "%.2f", resultCity.longitude)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SkyMindTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        SeaGlassPill(
                                            isSelected = true,
                                            activeColor = SkyMindBlueAccent,
                                            onClick = {
                                                onPickCoordinates(resultCity.latitude, resultCity.longitude, true)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "선택 + 핀",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Layer Filter Sea Glass Pills (강수 / 기온 / 미세먼지 / 바람)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mapLayers.forEach { layer ->
                    val isSelected = layer == selectedLayer
                    SeaGlassPill(
                        isSelected = isSelected,
                        activeColor = Color(0xFF0D9488),
                        modifier = Modifier.weight(1f),
                        onClick = { selectedLayer = layer }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = layer,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else SkyMindTextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Real Interactive OpenStreetMap (OSM) Card + Selected Location Callout + Radar Timeline Slider
        item {
            SeaGlassSurface(
                cornerRadius = 28.dp,
                tintColor = Color(0xFFD6F5F2),
                surfaceAlpha = 0.30f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // 1. Real OpenStreetMap Slippy Tile Map
                    InteractiveOsmWeatherMap(
                        weather = weather,
                        pinnedLocations = pinnedLocations,
                        selectedLayer = selectedLayer,
                        timeOffsetHours = timeOffsetHours,
                        onPickCoordinates = { lat, lon ->
                            onPickCoordinates(lat, lon, false)
                        },
                        onRequestGps = onRequestGps
                    )

                    // 2. Selected Location Info & Pin Action Bar (weight(1f) + softWrap=false so button never stretches!)
                    SeaGlassSurface(
                        cornerRadius = 20.dp,
                        tintColor = Color(0xFFE0F7FA),
                        surfaceAlpha = 0.34f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = SkyMindBlueAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = weather.location.displayWithOriginalName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SkyMindTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val layerDesc = when (selectedLayer) {
                                        "강수" -> "강수확률 ${currentSlot?.precipProb ?: weather.precipProbabilityNow}% · 강수량 ${currentSlot?.precipMm ?: weather.currentPrecipMm}mm"
                                        "기온" -> "기온 ${(currentSlot?.temp ?: weather.currentTemp).roundToInt()}°C (체감 ${weather.feelsLikeTemp.roundToInt()}°C)"
                                        "미세먼지" -> "미세먼지 ${weather.pm10Grade} (PM10 ${weather.pm10.roundToInt()}µg/m³)"
                                        else -> "${weather.windDirectionCardinal} ${String.format(Locale.US, "%.1f", currentSlot?.windSpeedMs ?: weather.windSpeedMs)}m/s"
                                    }
                                    Text(
                                        text = layerDesc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkyMindTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = onTogglePinCurrent,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrentPinned) Color(0xFF0D9488) else SkyMindBlueAccent
                                ),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("map_pin_location_button")
                            ) {
                                Icon(
                                    imageVector = if (isCurrentPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCurrentPinned) "핀 고정됨" else "위치 핀 고정",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // 3. Timeline Scrubber Slider inside Map Card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SkyMindBlueAccent.copy(alpha = 0.16f))
                                .clickable {
                                    timeOffsetHours = if (timeOffsetHours >= 6f) 0f else timeOffsetHours + 1f
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "시간 재생",
                                tint = SkyMindBlueAccent
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${currentSlot?.hourLabel ?: "지금"} ($selectedLayer 레이더)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SkyMindBlueAccent,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "+6시간 예보 슬라이더",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyMindTextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Slider(
                                value = timeOffsetHours,
                                onValueChange = { timeOffsetHours = it },
                                valueRange = 0f..6f,
                                steps = 5,
                                colors = SliderDefaults.colors(
                                    thumbColor = SkyMindBlueAccent,
                                    activeTrackColor = SkyMindBlueAccent
                                )
                            )
                        }
                    }
                }
            }
        }

        // User's Custom Pinned Locations Manager Card (Translucent Sea Glass)
        item {
            SeaGlassSurface(
                cornerRadius = 24.dp,
                tintColor = Color(0xFFD6F5F2),
                surfaceAlpha = 0.30f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "내가 핀 고정한 위치 (${pinnedLocations.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = SkyMindTextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = "홈 상단 표시",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyMindTextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (pinnedLocations.isEmpty()) {
                        Text(
                            text = "아직 고정한 핀 위치가 없습니다. 위 실제 지도를 터치하거나 검색한 뒤 '위치 핀 고정'을 눌러 나만의 즐겨찾는 장소를 등록해 보세요!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyMindTextSecondary
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(pinnedLocations) { pin ->
                                val isSelected = abs(pin.latitude - weather.location.latitude) < 0.01 &&
                                    abs(pin.longitude - weather.location.longitude) < 0.01
                                SeaGlassPill(
                                    isSelected = isSelected,
                                    activeColor = Color(0xFF0D9488),
                                    onClick = { onSelectCity(pin) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pin.displayWithOriginalName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) Color.White else SkyMindTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "핀 삭제",
                                            tint = if (isSelected) Color.White.copy(alpha = 0.85f) else SkyMindTextSecondary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onRemovePin(pin) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 4: "날씨 & 옷차림 비서" with Translucent Bubbles
 */
@Composable
fun AiWeatherAssistantScreen(
    weather: WeatherBundle?,
    advice: GeminiWeatherAdvice?,
    qaHistory: List<QaMessage>,
    onAskQuestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputQuestion by remember { mutableStateOf("") }

    val suggestedQuestions = listOf(
        "오늘 비 올 확률이 얼마나 되나요?",
        "오늘 저녁에 바람 많이 불까요? 어떤 겉옷 입지?",
        "주말 날씨는 어때요?",
        "근교 나들이하기 좋은 날씨인가요?",
        "미세먼지랑 자외선 괜찮나요?"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("ai_assistant_screen")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "날씨 & 옷차림 비서",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.width(8.dp))
            SeaGlassPill(
                isSelected = true,
                activeColor = Color(0xFF0D9488),
                onClick = {}
            ) {
                Text(
                    text = "실시간 기상 맞춤",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SkyMindBotAvatar(sizeDp = 64)
                    Spacer(modifier = Modifier.height(10.dp))
                    SeaGlassSurface(
                        cornerRadius = 24.dp,
                        tintColor = Color(0xFFD6F5F2),
                        surfaceAlpha = 0.30f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "안녕하세요! 오늘의 날씨와 옷차림, 어떤 게 궁금하신가요?",
                                style = MaterialTheme.typography.titleMedium,
                                color = SkyMindTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (advice != null) {
                                Text(
                                    text = "${advice.headline}\n• 추천 복장: ${advice.outfitOuter}, ${advice.outfitTop}\n• 우산: ${advice.umbrellaStatus} / 마스크: ${advice.maskStatus}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SkyMindTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestedQuestions.forEach { q ->
                        SeaGlassPill(
                            isSelected = false,
                            onClick = { onAskQuestion(q) }
                        ) {
                            Text(
                                text = q,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyMindTextPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            items(qaHistory) { qa ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        SeaGlassPill(
                            isSelected = true,
                            activeColor = SkyMindBlueAccent,
                            cornerRadius = 20.dp,
                            onClick = {}
                        ) {
                            Text(
                                text = qa.question,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        SkyMindBotAvatar(sizeDp = 36)
                        Spacer(modifier = Modifier.width(8.dp))
                        SeaGlassSurface(
                            cornerRadius = 20.dp,
                            tintColor = Color(0xFFD6F5F2),
                            surfaceAlpha = 0.30f,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = qa.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SkyMindTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = qa.timestamp,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyMindTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SeaGlassSurface(
            cornerRadius = 50.dp,
            tintColor = Color(0xFFD6F5F2),
            surfaceAlpha = 0.34f,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputQuestion,
                    onValueChange = { inputQuestion = it },
                    placeholder = {
                        Text(
                            text = "궁금한 걸 물어보세요...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyMindTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SkyMindTextPrimary,
                        unfocusedTextColor = SkyMindTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input")
                )
                IconButton(
                    onClick = {
                        if (inputQuestion.isNotBlank()) {
                            onAskQuestion(inputQuestion)
                            inputQuestion = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SkyMindBlueAccent)
                        .testTag("ai_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "전송",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tab 5: Settings, System Light/Dark Mode, Morning Briefing Push Notification, Home Widget & 30 Themes Gallery
 */
@Composable
fun ThemeGalleryAndSettingsScreen(
    activeTheme: AtmosphericThemeSpec,
    themeMode: SkyMindThemeMode,
    isAutoThemeEnabled: Boolean,
    isProcessingNanoBanana: Boolean,
    geminiThemeReason: String,
    weather: WeatherBundle?,
    advice: GeminiWeatherAdvice?,
    morningBriefingEnabled: Boolean,
    morningBriefingHour: Int,
    morningBriefingMinute: Int,
    formattedMorningBriefingTime: String,
    notificationStatusMessage: String?,
    rainAlertEnabled: Boolean,
    tempAlertEnabled: Boolean,
    dustAlertEnabled: Boolean,
    onSelectThemeMode: (SkyMindThemeMode) -> Unit,
    onToggleAutoTheme: (Boolean) -> Unit,
    onSelectTheme: (AtmosphericThemeSpec) -> Unit,
    onRegenerateNanoBanana: () -> Unit,
    onUpdateMorningBriefing: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onSendTestMorningBriefing: () -> Unit,
    onRequestPinHomeWidget: () -> Unit,
    onDismissNotificationStatus: () -> Unit,
    onToggleRainAlert: (Boolean) -> Unit,
    onToggleTempAlert: (Boolean) -> Unit,
    onToggleDustAlert: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalSkyMindColors.current

    var pendingActionAfterNotifPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingActionAfterNotifPermission?.invoke()
        pendingActionAfterNotifPermission = null
    }

    val ensureNotifPermissionThen = { action: () -> Unit ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !MorningBriefingScheduler.hasNotificationPermission(context)
        ) {
            pendingActionAfterNotifPermission = action
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    val openTimePicker = {
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                ensureNotifPermissionThen {
                    onUpdateMorningBriefing(true, selectedHour, selectedMinute)
                }
            },
            morningBriefingHour,
            morningBriefingMinute,
            false
        ).show()
    }

    val categories = remember {
        listOf("전체 (30)") + WeatherThemeCatalog.allThemes.map { it.categoryKo.substringBefore(" ·") }.distinct()
    }
    var selectedCategory by remember { mutableStateOf("전체 (30)") }

    val filteredThemes = remember(selectedCategory) {
        if (selectedCategory.startsWith("전체")) {
            WeatherThemeCatalog.allThemes
        } else {
            WeatherThemeCatalog.allThemes.filter { it.categoryKo.startsWith(selectedCategory) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("theme_settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "테마 · 위젯 · 아침 알림 설정",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }

        // Status Toast / Banner for Notification & Widget Actions
        if (!notificationStatusMessage.isNullOrBlank()) {
            item {
                SeaGlassSurface(
                    cornerRadius = 18.dp,
                    tintColor = Color(0xFF99F6E4),
                    surfaceAlpha = 0.36f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notificationStatusMessage,
                            style = MaterialTheme.typography.labelLarge,
                            color = SkyMindTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissNotificationStatus,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = SkyMindTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. Light Mode / Dark Mode Color System Card
        item {
            SeaGlassSurface(
                cornerRadius = 24.dp,
                tintColor = Color(0xFFD6F5F2),
                surfaceAlpha = 0.30f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("color_scheme_mode_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (tokens.isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = SkyMindGreenGood,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "화면 컬러 모드 (라이트 / 다크 자동 전환)",
                                style = MaterialTheme.typography.titleLarge,
                                color = SkyMindTextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "시스템 다크·라이트 테마에 맞춰 전체 UI가 자동으로 전환됩니다.",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyMindTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkyMindThemeMode.entries.forEach { mode ->
                            val isSelected = themeMode == mode
                            SeaGlassPill(
                                isSelected = isSelected,
                                activeColor = Color(0xFF0D9488),
                                onClick = { onSelectThemeMode(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_${mode.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.labelKo,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) Color.White else SkyMindTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Daily Morning Weather, Outfit & Umbrella Briefing Push Notification Card
        item {
            SeaGlassSurface(
                cornerRadius = 24.dp,
                tintColor = Color(0xFFD6F5F2),
                surfaceAlpha = 0.30f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("morning_briefing_notification_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = SkyMindGreenGood,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "아침 외출 전 날씨 · 옷차림 푸시 알림",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SkyMindTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "지정한 시간에 당일 날씨, 옷차림, 우산 필요 여부를 알려드려요",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyMindTextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = morningBriefingEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    ensureNotifPermissionThen {
                                        onUpdateMorningBriefing(true, morningBriefingHour, morningBriefingMinute)
                                    }
                                } else {
                                    onUpdateMorningBriefing(false, morningBriefingHour, morningBriefingMinute)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0D9488)
                            ),
                            modifier = Modifier.testTag("morning_briefing_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time Picker Row
                    SeaGlassSurface(
                        cornerRadius = 18.dp,
                        tintColor = Color(0xFFE0F7FA),
                        surfaceAlpha = 0.26f,
                        onClick = openTimePicker,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("morning_briefing_time_picker_row")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = SkyMindBlueAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "외출 브리핑 알림 시각",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkyMindTextSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "매일 $formattedMorningBriefingTime",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SkyMindTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            SeaGlassPill(
                                isSelected = true,
                                activeColor = SkyMindBlueAccent,
                                onClick = openTimePicker,
                                modifier = Modifier.testTag("change_briefing_time_button")
                            ) {
                                Text(
                                    text = "시간 변경",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Preset Time Chips (06:30, 07:00, 07:30, 08:00, 08:30, 09:00)
                    val presetTimes = listOf(
                        6 to 30,
                        7 to 0,
                        7 to 30,
                        8 to 0,
                        8 to 30,
                        9 to 0
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presetTimes) { (h, m) ->
                            val isSelected = morningBriefingHour == h && morningBriefingMinute == m
                            val label = String.format(Locale.KOREA, "오전 %02d:%02d", h, m)
                            SeaGlassPill(
                                isSelected = isSelected,
                                activeColor = Color(0xFF0D9488),
                                onClick = {
                                    ensureNotifPermissionThen {
                                        onUpdateMorningBriefing(true, h, m)
                                    }
                                }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else SkyMindTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Morning Briefing Push Notification Button
                    Button(
                        onClick = {
                            ensureNotifPermissionThen {
                                onSendTestMorningBriefing()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_morning_briefing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "지금 브리핑 푸시 알림 미리 받아보기 (테스트 발송)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = tokens.dividerColor)
                    Spacer(modifier = Modifier.height(6.dp))

                    AlertToggleRow("강수 · 우산 챙김 알림", rainAlertEnabled, onToggleRainAlert)
                    HorizontalDivider(color = tokens.dividerColor)
                    AlertToggleRow("급격한 기온 변화 · 겉옷 알림", tempAlertEnabled, onToggleTempAlert)
                    HorizontalDivider(color = tokens.dividerColor)
                    AlertToggleRow("미세먼지 · 자외선 주의 알림", dustAlertEnabled, onToggleDustAlert)
                }
            }
        }

        // 3. Android Home Screen Glassmorphism Weather & Outfit Widget Preview & Pin Card
        item {
            GlassmorphismHomeWidgetPreviewCard(
                weather = weather,
                advice = advice,
                onPinOrSyncWidget = onRequestPinHomeWidget
            )
        }

        // 4. 30 Seasonal/Weather Themes Gallery Card
        item {
            SeaGlassSurface(
                cornerRadius = 26.dp,
                tintColor = Color(0xFFD6F5F2),
                surfaceAlpha = 0.30f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "계절 · 날씨 감성 배경 테마 (30종)",
                            style = MaterialTheme.typography.titleLarge,
                            color = SkyMindTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "현재 날씨와 시간대, 계절 분위기에 어울리는 풍경 배경이 자동으로 적용됩니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyMindTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Auto Theme Toggle Card
                    SeaGlassSurface(
                        cornerRadius = 18.dp,
                        tintColor = Color(0xFFE0F7FA),
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
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = SkyMindGreenGood,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "날씨에 맞춰 테마 자동 변경",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = SkyMindTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "현재: ${activeTheme.nameKo}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyMindGreenGood,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = isAutoThemeEnabled,
                                    onCheckedChange = onToggleAutoTheme,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF0D9488)
                                    ),
                                    modifier = Modifier.testTag("auto_theme_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isProcessingNanoBanana) "배경 풍경 분위기를 다듬는 중..." else "배경 풍경 분위기 새로고침",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyMindTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                SeaGlassPill(
                                    isSelected = true,
                                    activeColor = SkyMindBlueAccent,
                                    onClick = onRegenerateNanoBanana
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isProcessingNanoBanana) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = "배경 새로고침",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (geminiThemeReason.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "추천 이유: $geminiThemeReason",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyMindTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Category Sea Glass Filter Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            val isSelected = cat == selectedCategory
                            SeaGlassPill(
                                isSelected = isSelected,
                                activeColor = Color(0xFF0D9488),
                                onClick = { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) Color.White else SkyMindTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val chunkedThemes = filteredThemes.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        chunkedThemes.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { themeItem ->
                                    ThemePreviewThumbnailCard(
                                        theme = themeItem,
                                        isSelected = themeItem.id == activeTheme.id,
                                        onClick = { onSelectTheme(themeItem) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewThumbnailCard(
    theme: AtmosphericThemeSpec,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SeaGlassSurface(
        modifier = modifier,
        cornerRadius = 18.dp,
        tintColor = if (isSelected) Color(0xFF99F6E4) else Color(0xFFCFFAFE),
        surfaceAlpha = if (isSelected) 0.38f else 0.24f,
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(theme.skyTop, theme.skyMid, theme.skyBottom)
                        )
                    )
            ) {
                AsyncImage(
                    model = theme.realPhotoUrl,
                    contentDescription = theme.nameKo,
                    contentScale = ContentScale.Crop,
                    alpha = 0.78f,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    theme.skyTop.copy(alpha = 0.22f),
                                    theme.skyBottom.copy(alpha = 0.42f)
                                )
                            )
                        )
                )

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "선택됨",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = theme.nameKo,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) SkyMindGreenGood else SkyMindTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = theme.subtitleKo,
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
private fun AlertToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = SkyMindTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0D9488)
            )
        )
    }
}
