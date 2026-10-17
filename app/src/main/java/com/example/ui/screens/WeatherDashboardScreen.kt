package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.CityLocation
import com.example.model.WeatherBundle
import com.example.ui.components.AtmosphericSceneryCanvas
import com.example.ui.components.DailyForecastSection
import com.example.ui.components.DetailedWeatherMetricsGrid
import com.example.ui.components.FavoritePlaceCategory
import com.example.ui.components.GeminiStylistSection
import com.example.ui.components.GlassmorphismHomeWidgetPreviewCard
import com.example.ui.components.HomeAiGreetingBubble
import com.example.ui.components.HourlyForecastSection
import com.example.ui.components.HourlyTempPrecipChartSection
import com.example.ui.components.MyFavoritePlacesDialog
import com.example.ui.components.MyFavoritePlacesSection
import com.example.ui.components.SeaGlassPill
import com.example.ui.components.SeaGlassSurface
import com.example.ui.components.SkyMindBlueAccent
import com.example.ui.components.SkyMindTextPrimary
import com.example.ui.components.SkyMindTextSecondary
import com.example.ui.components.weatherIconForCode
import com.example.ui.theme.AtmosphericThemeSpec
import com.example.ui.theme.LocalSkyMindColors
import com.example.viewmodel.LocationPermissionState
import com.example.viewmodel.SkyMindTab
import com.example.viewmodel.WeatherViewModel
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun WeatherDashboardScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSearchBar by remember { mutableStateOf(false) }
    var favoriteDialogTarget by remember { mutableStateOf<CityLocation?>(null) }

    if (uiState.currentTab != SkyMindTab.HOME) {
        BackHandler {
            viewModel.selectTab(SkyMindTab.HOME)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(context, granted)
    }

    LaunchedEffect(Unit) {
        viewModel.initializeContext(context)
        if (viewModel.hasLocationPermission(context)) {
            viewModel.onLocationPermissionResult(context, true)
        } else if (uiState.locationPermissionState == LocationPermissionState.NOT_CHECKED) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val triggerGpsPermissionRequest = {
        if (viewModel.hasLocationPermission(context)) {
            viewModel.fetchCurrentGpsLocation(context)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val openAppSystemSettings = {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    // '나의 장소' 즐겨찾기 추가 · 이름 변경 모달
    favoriteDialogTarget?.let { targetLocation ->
        MyFavoritePlacesDialog(
            initialTargetLocation = targetLocation,
            favoritePlaces = uiState.pinnedLocations,
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onSaveFavoritePlace = { loc, alias, catTag, selectNow ->
                viewModel.saveOrUpdateFavoritePlace(context, loc, alias, catTag, selectNow)
            },
            onSelectFavoritePlace = viewModel::selectCity,
            onRemoveFavoritePlace = { loc ->
                viewModel.removePinnedLocation(context, loc)
            },
            onDismiss = { favoriteDialogTarget = null }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Dynamic 30-Theme Atmospheric Scenery Backdrop with Live Weather Animation
        AtmosphericSceneryCanvas(
            theme = uiState.activeTheme,
            smudgedPhotoBitmap = uiState.smudgedBackdropBitmap,
            weatherCode = uiState.weatherBundle?.weatherCode,
            isDay = uiState.weatherBundle?.isDay ?: true,
            windSpeedMs = uiState.weatherBundle?.windSpeedMs ?: 2.0,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Main Foreground Content + Translucent Bottom Navigation Bar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.currentTab) {
                    SkyMindTab.HOME -> {
                        HomeTabContent(
                            uiState = uiState,
                            showSearchBar = showSearchBar,
                            onToggleSearch = { showSearchBar = !showSearchBar },
                            onSearchQueryChange = viewModel::onSearchQueryChange,
                            onSelectCity = { city ->
                                showSearchBar = false
                                viewModel.selectCity(city)
                            },
                            onTogglePinCurrent = {
                                viewModel.togglePinLocation(context, uiState.selectedLocation)
                            },
                            onOpenFavoriteDialog = { target ->
                                favoriteDialogTarget = target ?: uiState.selectedLocation
                            },
                            onQuickSaveFavorite = { loc, alias, catTag ->
                                viewModel.saveOrUpdateFavoritePlace(context, loc, alias, catTag, selectAfterSave = false)
                            },
                            onRemovePin = { city ->
                                viewModel.removePinnedLocation(context, city)
                            },
                            onOpenMapPicker = {
                                viewModel.selectTab(SkyMindTab.RADAR_MAP)
                            },
                            onRequestGps = triggerGpsPermissionRequest,
                            onOpenAppSettings = openAppSystemSettings,
                            onDismissGpsBanner = viewModel::clearGpsStatusMessage,
                            onRefresh = viewModel::refreshAll,
                            onSelectPersona = viewModel::selectPersona,
                            onAskQuestion = viewModel::askFollowUp,
                            onOpenAiTab = { viewModel.selectTab(SkyMindTab.AI_ASSISTANT) },
                            onOpenThemeTab = { viewModel.selectTab(SkyMindTab.MORE_THEMES) },
                            onPinHomeWidget = { viewModel.requestPinHomeWidget(context) }
                        )
                    }

                    SkyMindTab.FORECAST -> {
                        ForecastTabContent(
                            weather = uiState.weatherBundle,
                            timeSlotTips = uiState.geminiAdvice?.timeSlotTips ?: emptyList(),
                            onRefresh = viewModel::refreshAll
                        )
                    }

                    SkyMindTab.RADAR_MAP -> {
                        val weather = uiState.weatherBundle
                        if (weather != null) {
                            WeatherRadarMapScreen(
                                weather = weather,
                                pinnedLocations = uiState.pinnedLocations,
                                isCurrentPinned = uiState.isCurrentSelectedLocationPinned,
                                searchQuery = uiState.searchQuery,
                                searchResults = uiState.searchResults,
                                onSearchQueryChange = viewModel::onSearchQueryChange,
                                onSelectCity = viewModel::selectCity,
                                onPickCoordinates = { lat, lon, pinNow ->
                                    viewModel.pickLocationFromMapCoordinates(context, lat, lon, pinNow)
                                },
                                onTogglePinCurrent = {
                                    viewModel.togglePinLocation(context, uiState.selectedLocation)
                                },
                                onRemovePin = { city ->
                                    viewModel.removePinnedLocation(context, city)
                                },
                                onRequestGps = triggerGpsPermissionRequest
                            )
                        }
                    }

                    SkyMindTab.AI_ASSISTANT -> {
                        AiWeatherAssistantScreen(
                            weather = uiState.weatherBundle,
                            advice = uiState.geminiAdvice,
                            qaHistory = uiState.qaHistory,
                            onAskQuestion = viewModel::askFollowUp
                        )
                    }

                    SkyMindTab.MORE_THEMES -> {
                        ThemeGalleryAndSettingsScreen(
                            activeTheme = uiState.activeTheme,
                            themeMode = uiState.themeMode,
                            isAutoThemeEnabled = uiState.isAutoThemeEnabled,
                            isProcessingNanoBanana = uiState.isProcessingNanoBanana,
                            geminiThemeReason = uiState.geminiAdvice?.themeSelectionReason ?: "",
                            weather = uiState.weatherBundle,
                            advice = uiState.geminiAdvice,
                            morningBriefingEnabled = uiState.morningBriefingEnabled,
                            morningBriefingHour = uiState.morningBriefingHour,
                            morningBriefingMinute = uiState.morningBriefingMinute,
                            formattedMorningBriefingTime = uiState.formattedMorningBriefingTime,
                            notificationStatusMessage = uiState.notificationStatusMessage,
                            rainAlertEnabled = uiState.rainAlertEnabled,
                            tempAlertEnabled = uiState.tempChangeAlertEnabled,
                            dustAlertEnabled = uiState.dustAlertEnabled,
                            onSelectThemeMode = viewModel::setThemeMode,
                            onToggleAutoTheme = viewModel::setAutoThemeEnabled,
                            onSelectTheme = viewModel::selectManualTheme,
                            onRegenerateNanoBanana = viewModel::regenerateCurrentBackdropWithNanoBanana,
                            onUpdateMorningBriefing = { enabled, hour, minute ->
                                viewModel.updateMorningBriefingSchedule(context, enabled, hour, minute)
                            },
                            onSendTestMorningBriefing = {
                                viewModel.sendTestMorningBriefingNow(context)
                            },
                            onRequestPinHomeWidget = {
                                viewModel.requestPinHomeWidget(context)
                            },
                            onDismissNotificationStatus = viewModel::clearNotificationStatusMessage,
                            onToggleRainAlert = viewModel::toggleRainAlert,
                            onToggleTempAlert = viewModel::toggleTempAlert,
                            onToggleDustAlert = viewModel::toggleDustAlert
                        )
                    }
                }
            }

            // 3. Translucent Floating Bottom Navigation Bar
            SeaGlassBottomNavigationBar(
                currentTab = uiState.currentTab,
                activeAccent = Color(0xFF0D9488),
                onSelectTab = viewModel::selectTab
            )
        }
    }
}

@Composable
private fun HomeTabContent(
    uiState: com.example.viewmodel.WeatherUiState,
    showSearchBar: Boolean,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onTogglePinCurrent: () -> Unit,
    onOpenFavoriteDialog: (CityLocation?) -> Unit,
    onQuickSaveFavorite: (CityLocation, String, String) -> Unit,
    onRemovePin: (CityLocation) -> Unit,
    onOpenMapPicker: () -> Unit,
    onRequestGps: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismissGpsBanner: () -> Unit,
    onRefresh: () -> Unit,
    onSelectPersona: (String) -> Unit,
    onAskQuestion: (String) -> Unit,
    onOpenAiTab: () -> Unit,
    onOpenThemeTab: () -> Unit,
    onPinHomeWidget: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("weather_dashboard_scroll"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SkyMindTopHeroHeader(
                selectedCity = uiState.selectedLocation,
                pinnedLocations = uiState.pinnedLocations,
                isCurrentPinned = uiState.isCurrentSelectedLocationPinned,
                activeTheme = uiState.activeTheme,
                isFetchingGps = uiState.isFetchingGps,
                locationPermissionState = uiState.locationPermissionState,
                gpsStatusMessage = uiState.gpsStatusMessage,
                showSearchBar = showSearchBar,
                searchQuery = uiState.searchQuery,
                searchResults = uiState.searchResults,
                onToggleSearch = onToggleSearch,
                onSearchQueryChange = onSearchQueryChange,
                onSelectCity = onSelectCity,
                onTogglePinCurrent = onTogglePinCurrent,
                onOpenFavoriteDialog = onOpenFavoriteDialog,
                onRemovePin = onRemovePin,
                onOpenMapPicker = onOpenMapPicker,
                onRequestGps = onRequestGps,
                onOpenAppSettings = onOpenAppSettings,
                onDismissGpsBanner = onDismissGpsBanner,
                onOpenThemeTab = onOpenThemeTab,
                onRefresh = onRefresh
            )
        }

        if (uiState.isLoadingWeather && uiState.weatherBundle == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${uiState.selectedLocation.name} 실시간 날씨와 계절 풍경을 불러오고 있어요...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            }
        } else if (uiState.errorMessage != null && uiState.weatherBundle == null) {
            item {
                SeaGlassSurface(
                    cornerRadius = 24.dp,
                    tintColor = Color(0xFFD6F5F2),
                    surfaceAlpha = 0.32f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SkyMindTextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = SkyMindBlueAccent),
                            modifier = Modifier.testTag("retry_weather_button")
                        ) {
                            Text("다시 시도", color = Color.White, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        uiState.weatherBundle?.let { weather ->
            item {
                SkyMindHeroTemperatureDisplay(
                    weather = weather,
                    theme = uiState.activeTheme
                )
            }

            item {
                HomeAiGreetingBubble(
                    advice = uiState.geminiAdvice,
                    activeTheme = uiState.activeTheme,
                    isAutoTheme = uiState.isAutoThemeEnabled,
                    isGenerating = uiState.isGeneratingAdvice,
                    onClickExpandAi = onOpenAiTab,
                    onClickThemeGallery = onOpenThemeTab
                )
            }

            item {
                GeminiStylistSection(
                    weather = weather,
                    advice = uiState.geminiAdvice,
                    isGenerating = uiState.isGeneratingAdvice,
                    selectedPersona = uiState.selectedPersona,
                    qaHistory = uiState.qaHistory,
                    onSelectPersona = onSelectPersona,
                    onRefreshAdvice = onRefresh,
                    onAskQuestion = onAskQuestion
                )
            }

            item {
                MyFavoritePlacesSection(
                    selectedLocation = uiState.selectedLocation,
                    favoritePlaces = uiState.pinnedLocations,
                    isCurrentSaved = uiState.isCurrentSelectedLocationPinned,
                    onSelectPlace = onSelectCity,
                    onOpenAddOrEditDialog = onOpenFavoriteDialog,
                    onQuickSaveWithPreset = onQuickSaveFavorite,
                    onRemovePlace = onRemovePin
                )
            }

            item {
                HourlyForecastSection(
                    hourlyItems = weather.hourlyItems,
                    timeSlotTips = uiState.geminiAdvice?.timeSlotTips ?: emptyList()
                )
            }

            item {
                DetailedWeatherMetricsGrid(weather = weather)
            }

            item {
                DailyForecastSection(dailyItems = weather.dailyItems)
            }

            item {
                HourlyTempPrecipChartSection(
                    hourlyItems = weather.hourlyItems,
                    locationName = weather.location.displayTitle
                )
            }

            item {
                GlassmorphismHomeWidgetPreviewCard(
                    weather = weather,
                    advice = uiState.geminiAdvice,
                    onPinOrSyncWidget = onPinHomeWidget
                )
            }
        }
    }
}

@Composable
private fun ForecastTabContent(
    weather: WeatherBundle?,
    timeSlotTips: List<String>,
    onRefresh: () -> Unit
) {
    if (weather == null) return
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("forecast_tab_scroll"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "시간별 · 주간 예보",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${weather.location.displayWithOriginalName} · ${weather.updatedAt}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "새로고침",
                        tint = Color.White
                    )
                }
            }
        }

        item {
            HourlyTempPrecipChartSection(
                hourlyItems = weather.hourlyItems,
                locationName = weather.location.displayTitle
            )
        }

        item {
            HourlyForecastSection(
                hourlyItems = weather.hourlyItems,
                timeSlotTips = timeSlotTips
            )
        }

        item {
            DailyForecastSection(dailyItems = weather.dailyItems)
        }

        item {
            DetailedWeatherMetricsGrid(weather = weather)
        }
    }
}

@Composable
private fun SkyMindTopHeroHeader(
    selectedCity: CityLocation,
    pinnedLocations: List<CityLocation>,
    isCurrentPinned: Boolean,
    activeTheme: AtmosphericThemeSpec,
    isFetchingGps: Boolean,
    locationPermissionState: LocationPermissionState,
    gpsStatusMessage: String?,
    showSearchBar: Boolean,
    searchQuery: String,
    searchResults: List<CityLocation>,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onTogglePinCurrent: () -> Unit,
    onOpenFavoriteDialog: (CityLocation?) -> Unit,
    onRemovePin: (CityLocation) -> Unit,
    onOpenMapPicker: () -> Unit,
    onRequestGps: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismissGpsBanner: () -> Unit,
    onOpenThemeTab: () -> Unit,
    onRefresh: () -> Unit
) {
    val isDark = LocalSkyMindColors.current.isDark
    val heroTextColor = if (isDark) Color(0xFFF0FDFA) else if (activeTheme.isDarkTextOnHero) Color(0xFF1E293B) else Color.White

    Column(modifier = Modifier.fillMaxWidth()) {
        // Brand Row (weight(1f) on left prevents subtitle from squeezing right action buttons)
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
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SkyMind",
                        style = MaterialTheme.typography.headlineMedium,
                        color = heroTextColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${activeTheme.nameKo} · ${activeTheme.subtitleKo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = heroTextColor.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRequestGps,
                    modifier = Modifier.testTag("gps_location_button")
                ) {
                    if (isFetchingGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = heroTextColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = stringResource(R.string.current_location),
                            tint = heroTextColor
                        )
                    }
                }
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.testTag("toggle_search_button")
                ) {
                    Icon(
                        imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_city_hint),
                        tint = heroTextColor
                    )
                }
                IconButton(
                    onClick = onOpenThemeTab,
                    modifier = Modifier.testTag("open_theme_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "테마 설정",
                        tint = heroTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Location Pill + Pin Current Button + Map Picker Button (Scrollable LazyRow so text NEVER wraps vertically!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyRow(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Current Location Display Pill
                item {
                    SeaGlassPill(
                        isSelected = true,
                        activeColor = Color(0xFF0D9488),
                        onClick = onToggleSearch
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedCity.isCurrentGps) Icons.Default.GpsFixed else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedCity.displayWithOriginalName,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // '나의 장소' 즐겨찾기 추가 / 이름 붙이기 버튼
                item {
                    SeaGlassPill(
                        isSelected = isCurrentPinned,
                        activeColor = Color(0xFF0284C7),
                        onClick = { onOpenFavoriteDialog(selectedCity) },
                        modifier = Modifier.testTag("pin_current_location_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCurrentPinned) Icons.Default.Star else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isCurrentPinned) Color.White else SkyMindTextPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCurrentPinned) "나의 장소 수정" else "나의 장소 저장",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrentPinned) Color.White else SkyMindTextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Direct Map Coordinate Picker Button
                item {
                    SeaGlassPill(
                        isSelected = false,
                        onClick = onOpenMapPicker,
                        modifier = Modifier.testTag("open_map_picker_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLocationAlt,
                                contentDescription = null,
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "지도에서 위치 집기",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyMindTextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("refresh_weather_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.refresh_weather),
                    tint = heroTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expandable City Search Bar (Translucent Sea Glass)
        AnimatedVisibility(visible = showSearchBar) {
            SeaGlassSurface(
                cornerRadius = 22.dp,
                tintColor = Color(0xFFD6F5F2),
                surfaceAlpha = 0.34f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_city_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyMindTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0D9488),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.55f),
                            focusedContainerColor = Color(0xFFE0F7FA).copy(alpha = 0.32f),
                            unfocusedContainerColor = Color(0xFFE0F7FA).copy(alpha = 0.22f),
                            focusedTextColor = SkyMindTextPrimary,
                            unfocusedTextColor = SkyMindTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("city_search_input")
                    )

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
                                                text = "${resultCity.adminArea} ${resultCity.country}".trim(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SkyMindTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "선택",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFF0D9488),
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

        Spacer(modifier = Modifier.height(8.dp))

        // User's Custom Pinned Locations Bar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("user_pinned_cities_row")
        ) {
            // GPS Current Location Quick Pebble
            item {
                SeaGlassPill(
                    isSelected = selectedCity.isCurrentGps,
                    activeColor = Color(0xFF0D9488),
                    onClick = onRequestGps,
                    modifier = Modifier.testTag("quick_gps_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = if (selectedCity.isCurrentGps) Color.White else Color(0xFF0D9488),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "내 현위치",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedCity.isCurrentGps) Color.White else SkyMindTextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // User's '나의 장소' Favorite Locations Chips
            items(pinnedLocations) { pin ->
                val isSelected = !selectedCity.isCurrentGps &&
                    abs(pin.latitude - selectedCity.latitude) < 0.01 &&
                    abs(pin.longitude - selectedCity.longitude) < 0.01
                val cat = FavoritePlaceCategory.fromTag(pin.categoryTag)

                SeaGlassPill(
                    isSelected = isSelected,
                    activeColor = cat.accentColor,
                    onClick = { onSelectCity(pin) }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 11.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else cat.accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pin.displayTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else SkyMindTextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "이름 수정",
                            tint = if (isSelected) Color.White.copy(alpha = 0.85f) else SkyMindBlueAccent,
                            modifier = Modifier
                                .size(13.dp)
                                .clickable { onOpenFavoriteDialog(pin) }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "나의 장소 삭제",
                            tint = if (isSelected) Color.White.copy(alpha = 0.85f) else SkyMindTextSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRemovePin(pin) }
                        )
                    }
                }
            }

            if (pinnedLocations.isEmpty()) {
                item {
                    SeaGlassPill(
                        isSelected = false,
                        onClick = { onOpenFavoriteDialog(selectedCity) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ '${selectedCity.name}' 이름 붙여 나의 장소에 추가",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyMindTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Location Permission Denied Rationale Banner
        AnimatedVisibility(visible = locationPermissionState == LocationPermissionState.DENIED) {
            SeaGlassSurface(
                cornerRadius = 20.dp,
                tintColor = Color(0xFFD6F5F2),
                surfaceAlpha = 0.34f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("location_permission_rationale_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "현위치 자동 인식을 위해 위치 권한을 켜주세요",
                            style = MaterialTheme.typography.titleMedium,
                            color = SkyMindTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "권한 없이도 '지도에서 위치 집기'로 원하는 동네를 직접 터치해 선택하고 핀 고정할 수 있어요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkyMindTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onRequestGps,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("request_location_permission_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("위치 권한 허용", color = Color.White, maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = onOpenMapPicker,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("지도에서 찍기", color = SkyMindTextPrimary, maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = onOpenAppSettings,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("open_app_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "설정",
                                tint = SkyMindTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Status Feedback Pill
        AnimatedVisibility(visible = !gpsStatusMessage.isNullOrBlank() && locationPermissionState != LocationPermissionState.DENIED) {
            SeaGlassSurface(
                cornerRadius = 16.dp,
                tintColor = Color(0xFFD6F5F2),
                surfaceAlpha = 0.34f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = gpsStatusMessage ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            color = SkyMindTextPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismissGpsBanner,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = SkyMindTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkyMindHeroTemperatureDisplay(
    weather: WeatherBundle,
    theme: AtmosphericThemeSpec
) {
    val isDark = LocalSkyMindColors.current.isDark
    val heroTextColor = if (isDark) Color(0xFFF0FDFA) else if (theme.isDarkTextOnHero) Color(0xFF1E293B) else Color.White
    val (weatherIcon, iconTint) = weatherIconForCode(weather.weatherCode)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("current_weather_hero_card"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left Column given weight(1f) so it NEVER squeezes the right stats card into a narrow vertical strip!
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${weather.currentTemp.roundToInt()}°",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 70.sp),
                    color = heroTextColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = weather.weatherSummary,
                    tint = iconTint,
                    modifier = Modifier.size(44.dp)
                )
            }
            Text(
                text = weather.weatherSummary,
                style = MaterialTheme.typography.headlineMedium,
                color = heroTextColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "체감 ${weather.feelsLikeTemp.roundToInt()}° · 최고 ${weather.highTemp.roundToInt()}° / 최저 ${weather.lowTemp.roundToInt()}°",
                style = MaterialTheme.typography.bodyMedium,
                color = heroTextColor.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right: Translucent Sea Glass Frosted Pebble Stats Card
        SeaGlassSurface(
            cornerRadius = 20.dp,
            tintColor = Color(0xFFD6F5F2),
            surfaceAlpha = 0.30f
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                HeroRightStatItem(
                    icon = Icons.Default.WaterDrop,
                    label = "습도",
                    value = "${weather.humidityPercent}% (${weather.humidityGrade})"
                )
                HeroRightStatItem(
                    icon = Icons.Default.Speed,
                    label = "기압",
                    value = "${weather.seaLevelPressureHpa.roundToInt()} hPa"
                )
                HeroRightStatItem(
                    icon = Icons.Default.Air,
                    label = "바람",
                    value = "${String.format(Locale.US, "%.1f", weather.windSpeedMs)} m/s"
                )
                HeroRightStatItem(
                    icon = Icons.Default.WbSunny,
                    label = "자외선",
                    value = weather.uvGrade
                )
            }
        }
    }
}

@Composable
private fun HeroRightStatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF0D9488),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SkyMindTextSecondary,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = SkyMindTextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Translucent Floating Bottom Navigation Bar
 */
@Composable
private fun SeaGlassBottomNavigationBar(
    currentTab: SkyMindTab,
    activeAccent: Color,
    onSelectTab: (SkyMindTab) -> Unit
) {
    SeaGlassSurface(
        cornerRadius = 28.dp,
        tintColor = Color(0xFFD6F5F2),
        surfaceAlpha = 0.34f,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("skymind_bottom_nav")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkyMindTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val icon = when (tab) {
                    SkyMindTab.HOME -> Icons.Default.Home
                    SkyMindTab.FORECAST -> Icons.Default.CalendarMonth
                    SkyMindTab.RADAR_MAP -> Icons.Default.Map
                    SkyMindTab.AI_ASSISTANT -> Icons.Default.AutoAwesome
                    SkyMindTab.MORE_THEMES -> Icons.Default.MoreHoriz
                }

                SeaGlassPill(
                    isSelected = isSelected,
                    activeColor = activeAccent,
                    cornerRadius = 20.dp,
                    onClick = { onSelectTab(tab) },
                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.labelKo,
                            tint = if (isSelected) Color.White else SkyMindTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = tab.labelKo,
                                style = MaterialTheme.typography.labelLarge,
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
