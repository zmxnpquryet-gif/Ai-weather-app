package com.example.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CityLocation
import com.example.model.DefaultStartingLocation
import com.example.model.GeminiWeatherAdvice
import com.example.model.WeatherBundle
import com.example.notification.MorningBriefingConfig
import com.example.notification.MorningBriefingScheduler
import com.example.repository.NanoBananaBackdropManager
import com.example.repository.PinnedLocationStore
import com.example.repository.WeatherRepository
import com.example.ui.theme.AtmosphericThemeSpec
import com.example.ui.theme.SkyMindThemeMode
import com.example.ui.theme.WeatherThemeCatalog
import com.example.widget.SkyMindWeatherWidgetProvider
import com.example.widget.WidgetDataSyncManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class SkyMindTab(val labelKo: String) {
    HOME("홈"),
    FORECAST("예보"),
    RADAR_MAP("지도"),
    AI_ASSISTANT("AI"),
    MORE_THEMES("더보기")
}

enum class LocationPermissionState {
    NOT_CHECKED,
    GRANTED,
    DENIED
}

data class QaMessage(
    val question: String,
    val answer: String,
    val timestamp: String = SimpleDateFormat("a h:mm", Locale.KOREA).format(Date()),
    val isLoading: Boolean = false
)

data class WeatherUiState(
    val currentTab: SkyMindTab = SkyMindTab.HOME,
    val selectedLocation: CityLocation = DefaultStartingLocation,
    val pinnedLocations: List<CityLocation> = emptyList(),
    val isLoadingWeather: Boolean = true,
    val weatherBundle: WeatherBundle? = null,
    val errorMessage: String? = null,
    val selectedPersona: String = "의상 · 일상 캐주얼",
    val isGeneratingAdvice: Boolean = false,
    val geminiAdvice: GeminiWeatherAdvice? = null,
    val isAutoThemeEnabled: Boolean = true,
    val activeTheme: AtmosphericThemeSpec = WeatherThemeCatalog.allThemes.first(),
    val smudgedBackdropBitmap: Bitmap? = null,
    val isProcessingNanoBanana: Boolean = false,
    val searchQuery: String = "",
    val isSearchingCities: Boolean = false,
    val searchResults: List<CityLocation> = emptyList(),
    val qaHistory: List<QaMessage> = emptyList(),
    val isFetchingGps: Boolean = false,
    val locationPermissionState: LocationPermissionState = LocationPermissionState.NOT_CHECKED,
    val gpsStatusMessage: String? = null,
    val rainAlertEnabled: Boolean = true,
    val tempChangeAlertEnabled: Boolean = true,
    val dustAlertEnabled: Boolean = true,
    val themeMode: SkyMindThemeMode = SkyMindThemeMode.SYSTEM,
    val morningBriefingEnabled: Boolean = true,
    val morningBriefingHour: Int = 7,
    val morningBriefingMinute: Int = 30,
    val notificationStatusMessage: String? = null
) {
    val isCurrentSelectedLocationPinned: Boolean
        get() = pinnedLocations.any {
            abs(it.latitude - selectedLocation.latitude) < 0.01 &&
                abs(it.longitude - selectedLocation.longitude) < 0.01
        }

    val formattedMorningBriefingTime: String
        get() = MorningBriefingConfig(
            enabled = morningBriefingEnabled,
            hour = morningBriefingHour,
            minute = morningBriefingMinute
        ).formattedTimeKo
}

val PersonaStyles = listOf(
    "의상 · 일상 캐주얼",
    "외출 · 나들이 데이트",
    "운동 · 야외 러닝",
    "여행 · 장거리 이동",
    "출퇴근 · 비즈니스룩",
    "추위 민감 체질"
)

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var appContext: Context? = null

    init {
        loadWeatherAndAdvice(DefaultStartingLocation)
    }

    fun initializeContext(context: Context) {
        if (appContext != null) return
        val appCtx = context.applicationContext
        appContext = appCtx

        // Load saved morning briefing push notification schedule
        val briefingConfig = MorningBriefingScheduler.loadConfig(appCtx)
        val savedThemeModeName = appCtx.getSharedPreferences("skymind_ui_prefs", Context.MODE_PRIVATE)
            .getString("theme_mode", SkyMindThemeMode.SYSTEM.name) ?: SkyMindThemeMode.SYSTEM.name
        val resolvedThemeMode = runCatching { SkyMindThemeMode.valueOf(savedThemeModeName) }
            .getOrDefault(SkyMindThemeMode.SYSTEM)

        _uiState.update {
            it.copy(
                themeMode = resolvedThemeMode,
                morningBriefingEnabled = briefingConfig.enabled,
                morningBriefingHour = briefingConfig.hour,
                morningBriefingMinute = briefingConfig.minute
            )
        }

        if (briefingConfig.enabled) {
            MorningBriefingScheduler.ensureNotificationChannel(appCtx)
            MorningBriefingScheduler.scheduleNextDailyAlarm(appCtx, briefingConfig.hour, briefingConfig.minute)
        }

        // Observe user's custom pinned locations from DataStore
        viewModelScope.launch {
            PinnedLocationStore.observePinnedCities(appCtx).collect { savedPins ->
                _uiState.update { it.copy(pinnedLocations = savedPins) }
            }
        }

        // Load or generate smudged real photo for initial theme
        refreshBackdropForTheme(_uiState.value.activeTheme, forceNanoBanana = false)

        // Sync widget if weather is already loaded
        _uiState.value.weatherBundle?.let { w ->
            WidgetDataSyncManager.saveAndBroadcast(appCtx, w, _uiState.value.geminiAdvice)
        }
    }

    private fun refreshBackdropForTheme(theme: AtmosphericThemeSpec, forceNanoBanana: Boolean = false) {
        val ctx = appContext ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingNanoBanana = true) }
            val bmp = NanoBananaBackdropManager.getOrGenerateSmudgedBackdrop(
                context = ctx,
                theme = theme,
                forceNanoBananaApi = forceNanoBanana
            )
            _uiState.update {
                it.copy(
                    smudgedBackdropBitmap = bmp,
                    isProcessingNanoBanana = false
                )
            }
        }
    }

    fun regenerateCurrentBackdropWithNanoBanana() {
        refreshBackdropForTheme(_uiState.value.activeTheme, forceNanoBanana = true)
    }

    fun togglePinLocation(context: Context, location: CityLocation = _uiState.value.selectedLocation) {
        viewModelScope.launch {
            val currentPins = _uiState.value.pinnedLocations.toMutableList()
            val existingIndex = currentPins.indexOfFirst {
                abs(it.latitude - location.latitude) < 0.01 &&
                    abs(it.longitude - location.longitude) < 0.01
            }
            if (existingIndex >= 0) {
                val removed = currentPins.removeAt(existingIndex)
                _uiState.update {
                    it.copy(
                        pinnedLocations = currentPins,
                        gpsStatusMessage = "'${removed.displayTitle}' 장소가 나의 장소 즐겨찾기에서 해제되었습니다."
                    )
                }
            } else {
                val cleanPin = location.copy(isCurrentGps = false)
                currentPins.add(0, cleanPin)
                _uiState.update {
                    it.copy(
                        pinnedLocations = currentPins,
                        gpsStatusMessage = "'${cleanPin.displayTitle}' 위치를 나의 장소 즐겨찾기에 추가했습니다!"
                    )
                }
            }
            PinnedLocationStore.savePinnedCities(context.applicationContext, currentPins)
        }
    }

    /**
     * Saves a frequently visited place to '나의 장소' 즐겨찾기 with a user-defined custom name (alias)
     * and category tag (e.g., "집", "회사", "카페", "학교", "운동", "여행", "일반"), or updates its name if already saved.
     */
    fun saveOrUpdateFavoritePlace(
        context: Context,
        location: CityLocation,
        customAlias: String,
        categoryTag: String = "일반",
        selectAfterSave: Boolean = false
    ) {
        viewModelScope.launch {
            val trimmedAlias = customAlias.trim()
            val updatedItem = location.copy(
                isCurrentGps = false,
                customAlias = trimmedAlias,
                categoryTag = categoryTag.ifBlank { "일반" }
            )
            val currentPins = _uiState.value.pinnedLocations.toMutableList()
            val existingIndex = currentPins.indexOfFirst {
                abs(it.latitude - location.latitude) < 0.01 &&
                    abs(it.longitude - location.longitude) < 0.01
            }
            if (existingIndex >= 0) {
                currentPins[existingIndex] = updatedItem
            } else {
                currentPins.add(0, updatedItem)
            }

            val currentSelected = _uiState.value.selectedLocation
            val isSameAsSelected = abs(currentSelected.latitude - location.latitude) < 0.01 &&
                abs(currentSelected.longitude - location.longitude) < 0.01

            val newSelectedLocation = if (isSameAsSelected) {
                currentSelected.copy(
                    customAlias = trimmedAlias,
                    categoryTag = updatedItem.categoryTag
                )
            } else {
                currentSelected
            }

            _uiState.update {
                it.copy(
                    pinnedLocations = currentPins,
                    selectedLocation = newSelectedLocation,
                    gpsStatusMessage = "'${updatedItem.displayWithOriginalName}' 나의 장소 즐겨찾기에 저장되었습니다!"
                )
            }
            PinnedLocationStore.savePinnedCities(context.applicationContext, currentPins)

            if (selectAfterSave && !isSameAsSelected) {
                selectCity(updatedItem)
            }
        }
    }

    fun removePinnedLocation(context: Context, location: CityLocation) {
        viewModelScope.launch {
            val updated = _uiState.value.pinnedLocations.filterNot {
                abs(it.latitude - location.latitude) < 0.01 &&
                    abs(it.longitude - location.longitude) < 0.01
            }
            _uiState.update {
                it.copy(
                    pinnedLocations = updated,
                    gpsStatusMessage = "'${location.displayTitle}' 장소를 나의 장소에서 삭제했습니다."
                )
            }
            PinnedLocationStore.savePinnedCities(context.applicationContext, updated)
        }
    }

    /**
     * Called when the user taps or drags directly on the interactive Map to pick a custom coordinate!
     * Reverse-geocodes the tapped (latitude, longitude) into a Korean district/city name,
     * fetches live weather + Gemini advice, and optionally pins it.
     */
    fun pickLocationFromMapCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double,
        pinImmediately: Boolean = false
    ) {
        viewModelScope.launch {
            val resolvedCity = withContext(Dispatchers.IO) {
                resolveCoordinateToCityLocation(context, latitude, longitude, isGps = false)
            }
            selectCity(resolvedCity)
            if (pinImmediately) {
                togglePinLocation(context, resolvedCity)
            } else {
                _uiState.update {
                    it.copy(gpsStatusMessage = "지도에서 선택한 '${resolvedCity.name}' 날씨를 불러옵니다.")
                }
            }
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun onLocationPermissionResult(context: Context, isGranted: Boolean) {
        if (isGranted) {
            _uiState.update {
                it.copy(
                    locationPermissionState = LocationPermissionState.GRANTED,
                    gpsStatusMessage = "현재 위치 좌표를 확인하고 있어요..."
                )
            }
            fetchCurrentGpsLocation(context)
        } else {
            _uiState.update {
                it.copy(
                    locationPermissionState = LocationPermissionState.DENIED,
                    isFetchingGps = false,
                    gpsStatusMessage = "위치 권한이 거부되어 지도에서 직접 위치를 찍거나 검색해서 선택하실 수 있습니다."
                )
            }
        }
    }

    fun clearGpsStatusMessage() {
        _uiState.update { it.copy(gpsStatusMessage = null) }
    }

    fun selectTab(tab: SkyMindTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setAutoThemeEnabled(enabled: Boolean) {
        val currentState = _uiState.value
        val updatedTheme = if (enabled) {
            val geminiId = currentState.geminiAdvice?.selectedThemeId
            if (!geminiId.isNullOrBlank()) {
                WeatherThemeCatalog.findById(geminiId)
            } else {
                currentState.weatherBundle?.let { w ->
                    WeatherThemeCatalog.selectBestThemeForWeather(
                        w.weatherCode, w.currentTemp, w.isDay, w.pm10, w.pm25, w.windSpeedMs,
                        maxOf(w.uvIndexCurrent, w.uvIndexMaxToday)
                    )
                } ?: currentState.activeTheme
            }
        } else {
            currentState.activeTheme
        }
        _uiState.update { it.copy(isAutoThemeEnabled = enabled, activeTheme = updatedTheme) }
        refreshBackdropForTheme(updatedTheme)
    }

    fun selectManualTheme(theme: AtmosphericThemeSpec) {
        _uiState.update {
            it.copy(
                isAutoThemeEnabled = false,
                activeTheme = theme
            )
        }
        refreshBackdropForTheme(theme)
    }

    fun toggleRainAlert(enabled: Boolean) {
        _uiState.update { it.copy(rainAlertEnabled = enabled) }
    }

    fun toggleTempAlert(enabled: Boolean) {
        _uiState.update { it.copy(tempChangeAlertEnabled = enabled) }
    }

    fun toggleDustAlert(enabled: Boolean) {
        _uiState.update { it.copy(dustAlertEnabled = enabled) }
    }

    fun setThemeMode(mode: SkyMindThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        appContext?.getSharedPreferences("skymind_ui_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("theme_mode", mode.name)
            ?.apply()
    }

    fun updateMorningBriefingSchedule(
        context: Context,
        enabled: Boolean,
        hour: Int = _uiState.value.morningBriefingHour,
        minute: Int = _uiState.value.morningBriefingMinute
    ) {
        val saved = MorningBriefingScheduler.saveAndSchedule(context, enabled, hour, minute)
        val statusMsg = if (saved.enabled) {
            "매일 ${saved.formattedTimeKo}에 당일 날씨·옷차림·우산 브리핑 푸시 알림이 예약되었습니다."
        } else {
            "아침 날씨·옷차림 브리핑 푸시 알림이 해제되었습니다."
        }
        _uiState.update {
            it.copy(
                morningBriefingEnabled = saved.enabled,
                morningBriefingHour = saved.hour,
                morningBriefingMinute = saved.minute,
                notificationStatusMessage = statusMsg
            )
        }
    }

    fun sendTestMorningBriefingNow(context: Context) {
        viewModelScope.launch {
            _uiState.value.weatherBundle?.let { w ->
                WidgetDataSyncManager.saveAndBroadcast(context, w, _uiState.value.geminiAdvice)
            }
            val posted = MorningBriefingScheduler.fetchAndShowMorningBriefingNotification(context)
            _uiState.update {
                it.copy(
                    notificationStatusMessage = if (posted) {
                        "상단 알림창에 오늘 날씨·옷차림·우산 브리핑 푸시 알림을 발송했습니다!"
                    } else {
                        "알림 권한이 꺼져 있어 알림을 표시하지 못했습니다. 알림 권한을 허용해 주세요."
                    }
                )
            }
        }
    }

    fun requestPinHomeWidget(context: Context) {
        _uiState.value.weatherBundle?.let { w ->
            WidgetDataSyncManager.saveAndBroadcast(context, w, _uiState.value.geminiAdvice)
        }
        val pinned = SkyMindWeatherWidgetProvider.requestPinWidgetToHomeScreen(context)
        _uiState.update {
            it.copy(
                notificationStatusMessage = if (pinned) {
                    "홈 화면 위젯 추가 창을 열었습니다. 최신 날씨와 옷차림 데이터가 위젯에 동기화되었습니다."
                } else {
                    "최신 날씨·옷차림 데이터를 위젯에 동기화했습니다. 홈 화면 빈 공간을 길게 눌러 '하늘옷장 날씨' 위젯을 추가하실 수 있습니다."
                }
            )
        }
    }

    fun clearNotificationStatusMessage() {
        _uiState.update { it.copy(notificationStatusMessage = null) }
    }

    fun selectCity(city: CityLocation) {
        val matchingFavorite = _uiState.value.pinnedLocations.firstOrNull {
            abs(it.latitude - city.latitude) < 0.01 &&
                abs(it.longitude - city.longitude) < 0.01
        }
        val enrichedCity = if (matchingFavorite != null && city.customAlias.isBlank()) {
            city.copy(
                customAlias = matchingFavorite.customAlias,
                categoryTag = matchingFavorite.categoryTag
            )
        } else {
            city
        }
        _uiState.update {
            it.copy(
                selectedLocation = enrichedCity,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
        loadWeatherAndAdvice(enrichedCity)
    }

    fun selectPersona(persona: String) {
        if (_uiState.value.selectedPersona == persona && _uiState.value.geminiAdvice != null) return
        _uiState.update { it.copy(selectedPersona = persona) }
        val currentWeather = _uiState.value.weatherBundle ?: return
        refreshGeminiAdviceOnly(currentWeather, persona)
    }

    fun refreshAll() {
        loadWeatherAndAdvice(_uiState.value.selectedLocation)
    }

    private fun loadWeatherAndAdvice(location: CityLocation) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingWeather = true,
                    errorMessage = null,
                    isGeneratingAdvice = true
                )
            }

            val result = repository.fetchWeatherBundle(location)
            result.onSuccess { bundle ->
                val initialTheme = if (_uiState.value.isAutoThemeEnabled) {
                    WeatherThemeCatalog.selectBestThemeForWeather(
                        weatherCode = bundle.weatherCode,
                        tempC = bundle.currentTemp,
                        isDay = bundle.isDay,
                        pm10 = bundle.pm10,
                        pm25 = bundle.pm25,
                        windSpeedMs = bundle.windSpeedMs,
                        uvIndex = maxOf(bundle.uvIndexCurrent, bundle.uvIndexMaxToday)
                    )
                } else {
                    _uiState.value.activeTheme
                }

                _uiState.update {
                    it.copy(
                        isLoadingWeather = false,
                        weatherBundle = bundle,
                        activeTheme = initialTheme,
                        errorMessage = null
                    )
                }
                refreshBackdropForTheme(initialTheme)

                val advice = repository.generateGeminiAdvice(bundle, _uiState.value.selectedPersona)
                val geminiChosenTheme = if (_uiState.value.isAutoThemeEnabled) {
                    WeatherThemeCatalog.findById(advice.selectedThemeId)
                } else {
                    _uiState.value.activeTheme
                }

                val themeChanged = geminiChosenTheme.id != initialTheme.id
                _uiState.update {
                    it.copy(
                        isGeneratingAdvice = false,
                        geminiAdvice = advice,
                        activeTheme = geminiChosenTheme
                    )
                }
                if (themeChanged) {
                    refreshBackdropForTheme(geminiChosenTheme)
                }
                appContext?.let { ctx ->
                    WidgetDataSyncManager.saveAndBroadcast(ctx, bundle, advice)
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingWeather = false,
                        isGeneratingAdvice = false,
                        errorMessage = "기상 데이터를 불러오지 못했습니다: ${err.localizedMessage ?: "네트워크 연결을 확인해 주세요."}"
                    )
                }
            }
        }
    }

    private fun refreshGeminiAdviceOnly(weather: WeatherBundle, persona: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingAdvice = true) }
            val advice = repository.generateGeminiAdvice(weather, persona)
            val updatedTheme = if (_uiState.value.isAutoThemeEnabled) {
                WeatherThemeCatalog.findById(advice.selectedThemeId)
            } else {
                _uiState.value.activeTheme
            }
            _uiState.update {
                it.copy(
                    isGeneratingAdvice = false,
                    geminiAdvice = advice,
                    activeTheme = updatedTheme
                )
            }
            refreshBackdropForTheme(updatedTheme)
            appContext?.let { ctx ->
                WidgetDataSyncManager.saveAndBroadcast(ctx, weather, advice)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearchingCities = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingCities = true) }
            val results = repository.searchCities(query)
            _uiState.update {
                it.copy(
                    isSearchingCities = false,
                    searchResults = results
                )
            }
        }
    }

    fun askFollowUp(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        val weather = _uiState.value.weatherBundle ?: return

        viewModelScope.launch {
            val pendingMsg = QaMessage(
                question = trimmed,
                answer = "실시간 기상 데이터를 바탕으로 답변을 준비하고 있어요...",
                isLoading = true
            )
            _uiState.update {
                it.copy(qaHistory = it.qaHistory + pendingMsg)
            }
            val answer = repository.askFollowUpQuestion(weather, trimmed)
            _uiState.update { state ->
                val updatedList = state.qaHistory.map { msg ->
                    if (msg.question == trimmed && msg.isLoading) {
                        QaMessage(question = trimmed, answer = answer, isLoading = false)
                    } else msg
                }
                state.copy(qaHistory = updatedList)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentGpsLocation(context: Context) {
        if (!hasLocationPermission(context)) {
            _uiState.update {
                it.copy(
                    locationPermissionState = LocationPermissionState.DENIED,
                    isFetchingGps = false
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                locationPermissionState = LocationPermissionState.GRANTED,
                isFetchingGps = true,
                gpsStatusMessage = "GPS 현위치 좌표를 수신하고 있습니다..."
            )
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val tokenSource = CancellationTokenSource()

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    applyResolvedGpsLocation(context, location)
                } else {
                    fusedClient.lastLocation
                        .addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                applyResolvedGpsLocation(context, lastLoc)
                            } else {
                                val managerLoc = getFallbackLocationFromManager(context)
                                if (managerLoc != null) {
                                    applyResolvedGpsLocation(context, managerLoc)
                                } else {
                                    _uiState.update {
                                        it.copy(
                                            isFetchingGps = false,
                                            gpsStatusMessage = "기기의 GPS 신호를 찾지 못해 현재 선택된 지역(${it.selectedLocation.name}) 날씨를 유지합니다."
                                        )
                                    }
                                }
                            }
                        }
                        .addOnFailureListener {
                            val managerLoc = getFallbackLocationFromManager(context)
                            if (managerLoc != null) {
                                applyResolvedGpsLocation(context, managerLoc)
                            } else {
                                _uiState.update {
                                    it.copy(
                                        isFetchingGps = false,
                                        gpsStatusMessage = "위치 정보를 가져올 수 없습니다. 지도에서 직접 원하는 위치를 터치해 선택해 보세요!"
                                    )
                                }
                            }
                        }
                }
            }
            .addOnFailureListener {
                val managerLoc = getFallbackLocationFromManager(context)
                if (managerLoc != null) {
                    applyResolvedGpsLocation(context, managerLoc)
                } else {
                    _uiState.update {
                        it.copy(
                            isFetchingGps = false,
                            gpsStatusMessage = "위치 확인 중 오류가 발생했습니다. 지도 탭에서 직접 위치를 찍어 선택할 수 있습니다."
                        )
                    }
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun getFallbackLocationFromManager(context: Context): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }
            bestLocation
        } catch (e: Exception) {
            null
        }
    }

    private fun applyResolvedGpsLocation(context: Context, location: Location) {
        viewModelScope.launch {
            val gpsCity = withContext(Dispatchers.IO) {
                resolveCoordinateToCityLocation(context, location.latitude, location.longitude, isGps = true)
            }
            _uiState.update {
                it.copy(
                    isFetchingGps = false,
                    gpsStatusMessage = "현재 위치(${gpsCity.name}) 날씨로 업데이트되었습니다."
                )
            }
            selectCity(gpsCity)
        }
    }

    private fun resolveCoordinateToCityLocation(
        context: Context,
        lat: Double,
        lon: Double,
        isGps: Boolean
    ): CityLocation {
        var resolvedName = "${String.format(Locale.US, "%.2f", lat)}°N, ${String.format(Locale.US, "%.2f", lon)}°E"
        var admin = if (isGps) "GPS 현위치" else "지도 직접 선택 위치"
        var countryName = "대한민국"

        try {
            val geocoder = Geocoder(context, Locale.KOREA)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val adminArea = addr.adminArea ?: ""
                val locality = addr.locality ?: addr.subAdminArea ?: ""
                val subLocality = addr.subLocality ?: addr.thoroughfare ?: ""
                countryName = addr.countryName ?: "대한민국"

                resolvedName = when {
                    locality.isNotBlank() && subLocality.isNotBlank() && locality != subLocality ->
                        "$locality $subLocality"
                    subLocality.isNotBlank() ->
                        if (adminArea.isNotBlank()) "$adminArea $subLocality" else subLocality
                    locality.isNotBlank() ->
                        locality
                    adminArea.isNotBlank() ->
                        adminArea
                    addr.featureName != null ->
                        addr.featureName
                    else -> resolvedName
                }
                admin = when {
                    adminArea.isNotBlank() && isGps -> "$adminArea · GPS 현위치"
                    adminArea.isNotBlank() -> adminArea
                    isGps -> "GPS 현위치"
                    else -> "지도 선택 지점"
                }
            }
        } catch (_: Exception) {
        }

        return CityLocation(
            name = resolvedName,
            adminArea = admin,
            country = countryName,
            latitude = lat,
            longitude = lon,
            isCurrentGps = isGps
        )
    }
}
