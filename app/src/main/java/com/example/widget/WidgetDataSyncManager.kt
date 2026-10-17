package com.example.widget

import android.content.Context
import com.example.model.CityLocation
import com.example.model.DefaultStartingLocation
import com.example.model.GeminiWeatherAdvice
import com.example.model.WeatherBundle
import com.example.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class WidgetWeatherSnapshot(
    val locationName: String,
    val locationDisplayName: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentGps: Boolean,
    val currentTempC: Int,
    val feelsLikeC: Int,
    val highTempC: Int,
    val lowTempC: Int,
    val weatherCode: Int,
    val weatherSummary: String,
    val humidityPercent: Int,
    val pressureHpa: Int,
    val pm10Grade: String,
    val umbrellaStatus: String,
    val shortOutfitGuide: String,
    val outfitOuter: String,
    val outfitTop: String,
    val outfitBottom: String,
    val outfitTitle: String,
    val outfitDetail: String,
    val updatedAt: String
)

object WidgetDataSyncManager {
    private const val PREF_NAME = "skymind_widget_snapshot_prefs"

    fun saveAndBroadcast(
        context: Context,
        weather: WeatherBundle,
        advice: GeminiWeatherAdvice?
    ) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val outer = advice?.outfitOuter?.takeIf { it.isNotBlank() } ?: defaultOuterForTemp(weather.feelsLikeTemp)
        val top = advice?.outfitTop?.takeIf { it.isNotBlank() } ?: defaultTopForTemp(weather.feelsLikeTemp)
        val bottom = advice?.outfitBottom?.takeIf { it.isNotBlank() } ?: "코튼 팬츠 / 데님"
        val shoes = advice?.outfitShoesAndAcc?.takeIf { it.isNotBlank() } ?: "편안한 스니커즈"
        val umbrella = advice?.umbrellaStatus?.takeIf { it.isNotBlank() }
            ?: if (maxOf(weather.precipProbabilityNow, weather.precipProbabilityMaxToday) >= 40) "접이식 우산 권장" else "우산 불필요"

        val conciseGuide = buildConciseOutfitGuide(weather, advice, outer, top)
        val outfitTitle = "✨ $conciseGuide"
        val outfitDetail = "👟 $shoes · ${advice?.umbrellaReason?.takeIf { it.isNotBlank() } ?: "외출 전 체감온도와 일교차를 확인하세요"}"

        prefs.edit()
            .putString("location_name", weather.location.name)
            .putString("location_display_name", weather.location.displayWithOriginalName)
            .putFloat("latitude", weather.location.latitude.toFloat())
            .putFloat("longitude", weather.location.longitude.toFloat())
            .putBoolean("is_gps", weather.location.isCurrentGps)
            .putInt("current_temp", weather.currentTemp.roundToInt())
            .putInt("feels_like", weather.feelsLikeTemp.roundToInt())
            .putInt("high_temp", weather.highTemp.roundToInt())
            .putInt("low_temp", weather.lowTemp.roundToInt())
            .putInt("weather_code", weather.weatherCode)
            .putString("weather_summary", weather.weatherSummary)
            .putInt("humidity", weather.humidityPercent)
            .putInt("pressure", weather.seaLevelPressureHpa.roundToInt())
            .putString("pm10_grade", weather.pm10Grade)
            .putString("umbrella_status", umbrella)
            .putString("short_outfit_guide", conciseGuide)
            .putString("outfit_outer", outer)
            .putString("outfit_top", top)
            .putString("outfit_bottom", bottom)
            .putString("outfit_title", outfitTitle)
            .putString("outfit_detail", outfitDetail)
            .putString("updated_at", weather.updatedAt)
            .apply()

        SkyMindWeatherWidgetProvider.updateAllWidgets(appCtx)
    }

    fun buildConciseOutfitGuide(
        weather: WeatherBundle?,
        advice: GeminiWeatherAdvice?,
        outer: String,
        top: String
    ): String {
        val rawHeadline = advice?.headline?.trim().orEmpty()
        if (rawHeadline.isNotBlank()) {
            return rawHeadline
        }
        val feels = weather?.feelsLikeTemp?.roundToInt() ?: 18
        val tempGap = ((weather?.highTemp ?: 22.0) - (weather?.lowTemp ?: 12.0)).roundToInt()
        return when {
            (weather?.precipProbabilityMaxToday ?: 0) >= 50 ->
                "비 소식이 있어요! 방수되는 $outer 차림과 우산을 챙기세요"
            tempGap >= 9 ->
                "일교차 ${tempGap}°C! 입고 벗기 편한 $outer & $top 코디 추천"
            feels >= 25 ->
                "한낮 기온이 높아요. 통기성 좋은 $top 중심의 가벼운 옷차림"
            feels <= 8 ->
                "공기가 쌀쌀해요. 보온성 좋은 $outer 레이어드로 따뜻하게 입으세요"
            else ->
                "오늘 체감 ${feels}°C, $outer & $top 조합이 가장 쾌적해요"
        }
    }

    fun loadSnapshot(context: Context): WidgetWeatherSnapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val locName = prefs.getString("location_name", DefaultStartingLocation.name) ?: DefaultStartingLocation.name
        val locDisplay = prefs.getString("location_display_name", locName) ?: locName
        val outer = prefs.getString("outfit_outer", "가벼운 트렌치코트/자켓") ?: "가벼운 트렌치코트/자켓"
        val top = prefs.getString("outfit_top", "산뜻한 셔츠/니트") ?: "산뜻한 셔츠/니트"
        val bottom = prefs.getString("outfit_bottom", "데님 팬츠/슬랙스") ?: "데님 팬츠/슬랙스"
        val shortGuide = prefs.getString(
            "short_outfit_guide",
            "일교차가 있으니 $outer & $top 레이어드 코디를 추천해요"
        ) ?: "일교차가 있으니 $outer & $top 레이어드 코디를 추천해요"

        return WidgetWeatherSnapshot(
            locationName = locName,
            locationDisplayName = locDisplay,
            latitude = prefs.getFloat("latitude", DefaultStartingLocation.latitude.toFloat()).toDouble(),
            longitude = prefs.getFloat("longitude", DefaultStartingLocation.longitude.toFloat()).toDouble(),
            isCurrentGps = prefs.getBoolean("is_gps", false),
            currentTempC = prefs.getInt("current_temp", 18),
            feelsLikeC = prefs.getInt("feels_like", 17),
            highTempC = prefs.getInt("high_temp", 22),
            lowTempC = prefs.getInt("low_temp", 12),
            weatherCode = prefs.getInt("weather_code", 0),
            weatherSummary = prefs.getString("weather_summary", "맑고 쾌청함") ?: "맑고 쾌청함",
            humidityPercent = prefs.getInt("humidity", 50),
            pressureHpa = prefs.getInt("pressure", 1013),
            pm10Grade = prefs.getString("pm10_grade", "좋음") ?: "좋음",
            umbrellaStatus = prefs.getString("umbrella_status", "우산 불필요") ?: "우산 불필요",
            shortOutfitGuide = shortGuide,
            outfitOuter = outer,
            outfitTop = top,
            outfitBottom = bottom,
            outfitTitle = prefs.getString("outfit_title", "✨ $shortGuide") ?: "✨ $shortGuide",
            outfitDetail = prefs.getString("outfit_detail", "👟 레더 스니커즈 · 외출 전 체감온도와 일교차를 확인하세요")
                ?: "👟 레더 스니커즈 · 외출 전 체감온도와 일교차를 확인하세요",
            updatedAt = prefs.getString("updated_at", "방금 업데이트") ?: "방금 업데이트"
        )
    }

    suspend fun refreshFromNetworkAndBroadcast(context: Context) = withContext(Dispatchers.IO) {
        val currentSnap = loadSnapshot(context)
        val location = CityLocation(
            name = currentSnap.locationName,
            latitude = currentSnap.latitude,
            longitude = currentSnap.longitude,
            isCurrentGps = currentSnap.isCurrentGps
        )
        val repository = WeatherRepository()
        val weatherResult = repository.fetchWeatherBundle(location)
        weatherResult.onSuccess { bundle ->
            val advice = repository.generateGeminiAdvice(bundle, "의상 · 일상 캐주얼")
            saveAndBroadcast(context, bundle, advice)
        }
    }

    fun weatherEmojiForCode(code: Int): String = when (code) {
        0, 1 -> "☀️"
        2, 3, 45, 48 -> "☁️"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌤️"
    }

    private fun defaultOuterForTemp(feelsC: Double): String = when {
        feelsC >= 27.0 -> "얇은 린넨 셔츠"
        feelsC >= 21.0 -> "가벼운 가디건"
        feelsC >= 15.0 -> "가벼운 트렌치코트/자켓"
        feelsC >= 9.0 -> "도톰한 자켓/울 가디건"
        feelsC >= 3.0 -> "울 코트/경량 패딩"
        else -> "두꺼운 롱패딩/다운 코트"
    }

    private fun defaultTopForTemp(feelsC: Double): String = when {
        feelsC >= 23.0 -> "통기성 반팔 티셔츠"
        feelsC >= 15.0 -> "산뜻한 셔츠/얇은 니트"
        feelsC >= 8.0 -> "포근한 니트 레이어드"
        else -> "보온 내의 & 도톰한 니트"
    }
}
