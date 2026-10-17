package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CityLocation(
    val name: String,
    val adminArea: String = "",
    val country: String = "대한민국",
    val latitude: Double,
    val longitude: Double,
    val isCurrentGps: Boolean = false,
    val customAlias: String = "",
    val categoryTag: String = "일반"
) {
    val coordinateKey: String
        get() = String.format(java.util.Locale.US, "%.3f_%.3f", latitude, longitude)

    val displayTitle: String
        get() = if (customAlias.isNotBlank()) customAlias else name

    val displayWithOriginalName: String
        get() = if (customAlias.isNotBlank() && customAlias != name) {
            "$customAlias ($name)"
        } else {
            name
        }
}

val DefaultStartingLocation = CityLocation("서울", "서울특별시", "대한민국", 37.5665, 126.9780)

val PresetKoreanCities = listOf(
    DefaultStartingLocation
)

// --- Open-Meteo Geocoding Response ---
@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
data class GeocodingResult(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null
)

// --- Open-Meteo Weather Forecast Response ---
@Serializable
data class OpenMeteoWeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
    val current: CurrentWeatherDto? = null,
    val hourly: HourlyWeatherDto? = null,
    val daily: DailyWeatherDto? = null
)

@Serializable
data class CurrentWeatherDto(
    val time: String,
    @SerialName("temperature_2m") val temperature2m: Double = 0.0,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int = 0,
    @SerialName("apparent_temperature") val apparentTemperature: Double = 0.0,
    @SerialName("is_day") val isDay: Int = 1,
    val precipitation: Double = 0.0,
    val rain: Double = 0.0,
    val showers: Double = 0.0,
    val snowfall: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 0,
    @SerialName("cloud_cover") val cloudCover: Int = 0,
    @SerialName("pressure_msl") val pressureMsl: Double? = null,
    @SerialName("surface_pressure") val surfacePressure: Double = 1013.0,
    @SerialName("wind_speed_10m") val windSpeed10m: Double = 0.0,
    @SerialName("wind_direction_10m") val windDirection10m: Int = 0,
    @SerialName("wind_gusts_10m") val windGusts10m: Double = 0.0
)

@Serializable
data class HourlyWeatherDto(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @SerialName("relative_humidity_2m") val relativeHumidity2m: List<Int?> = emptyList(),
    @SerialName("dew_point_2m") val dewPoint2m: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("pressure_msl") val pressureMsl: List<Double?> = emptyList(),
    @SerialName("surface_pressure") val surfacePressure: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
    @SerialName("wind_direction_10m") val windDirection10m: List<Int?> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList()
)

@Serializable
data class DailyWeatherDto(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
    @SerialName("apparent_temperature_max") val apparentMax: List<Double?> = emptyList(),
    @SerialName("apparent_temperature_min") val apparentMin: List<Double?> = emptyList(),
    val sunrise: List<String?> = emptyList(),
    val sunset: List<String?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbMax: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double?> = emptyList(),
    @SerialName("wind_direction_10m_dominant") val windDirectionDominant: List<Int?> = emptyList()
)

// --- Open-Meteo Air Quality Response ---
@Serializable
data class OpenMeteoAirQualityResponse(
    val current: CurrentAirQualityDto? = null
)

@Serializable
data class CurrentAirQualityDto(
    val time: String = "",
    val pm10: Double? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("carbon_monoxide") val carbonMonoxide: Double? = null,
    @SerialName("nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    val ozone: Double? = null,
    @SerialName("uv_index") val uvIndex: Double? = null
)

// --- Processed Domain Models ---
data class WeatherBundle(
    val location: CityLocation,
    val updatedAt: String,
    val currentTemp: Double,
    val feelsLikeTemp: Double,
    val highTemp: Double,
    val lowTemp: Double,
    val weatherCode: Int,
    val weatherSummary: String,
    val isDay: Boolean,
    val windSpeedMs: Double,
    val windGustsMs: Double,
    val windDirectionDeg: Int,
    val windDirectionCardinal: String,
    val windFeelDescription: String,
    val uvIndexCurrent: Double,
    val uvIndexMaxToday: Double,
    val uvGrade: String,
    val uvAdviceShort: String,
    val pm10: Double,
    val pm10Grade: String,
    val pm25: Double,
    val pm25Grade: String,
    val ozone: Double,
    val aqiScore: Int,
    val precipProbabilityNow: Int,
    val precipProbabilityMaxToday: Int,
    val currentPrecipMm: Double,
    val todayTotalPrecipMm: Double,
    val humidityPercent: Int,
    val dewPointC: Double = 10.0,
    val humidityMinToday: Int = 35,
    val humidityMaxToday: Int = 75,
    val humidityGrade: String = "쾌적",
    val humidityAdviceShort: String = "실내외 활동하기 쾌적한 습도예요",
    val pressureHpa: Double,
    val seaLevelPressureHpa: Double = 1013.0,
    val pressureDiff3h: Double = 0.0,
    val pressureGrade: String = "표준 기압",
    val pressureAdviceShort: String = "기압 변화가 완만해 대기 상태가 안정적이에요",
    val cloudCoverPercent: Int,
    val sunriseTime: String,
    val sunsetTime: String,
    val hourlyItems: List<HourlyForecastItem>,
    val dailyItems: List<DailyForecastItem>
)

data class HourlyForecastItem(
    val hourLabel: String,
    val temp: Double,
    val precipProb: Int,
    val precipMm: Double,
    val weatherCode: Int,
    val windSpeedMs: Double,
    val windDirCardinal: String,
    val uvIndex: Double,
    val humidityPercent: Int = 50,
    val pressureHpa: Double = 1013.0
)

data class DailyForecastItem(
    val dateLabel: String,
    val dayOfWeek: String,
    val weatherCode: Int,
    val weatherDescription: String,
    val minTemp: Double,
    val maxTemp: Double,
    val precipProbMax: Int,
    val precipSumMm: Double,
    val uvMax: Double,
    val windMaxMs: Double
)

// --- Gemini Structured AI Advisor Models ---
@Serializable
data class GeminiWeatherAdvice(
    val selectedThemeId: String = "SPRING_CHERRY_SKY",
    val themeSelectionReason: String = "",
    val headline: String = "",
    val summaryComment: String = "",
    val umbrellaStatus: String = "불필요",
    val umbrellaReason: String = "",
    val maskStatus: String = "보통",
    val maskReason: String = "",
    val uvCareStatus: String = "보통",
    val uvCareReason: String = "",
    val outfitOuter: String = "",
    val outfitTop: String = "",
    val outfitBottom: String = "",
    val outfitShoesAndAcc: String = "",
    val stylingReason: String = "",
    val timeSlotTips: List<String> = emptyList(),
    val activityRecommendation: String = ""
)

object WeatherInterpreter {
    fun weatherCodeToKorean(code: Int): String = when (code) {
        0 -> "맑고 쾌청함"
        1 -> "대체로 맑음"
        2 -> "구름 조금"
        3 -> "흐림"
        45, 48 -> "안개 낌"
        51, 53, 55 -> "이슬비"
        56, 57 -> "진눈깨비 섞인 이슬비"
        61 -> "약한 비"
        63 -> "보통 비"
        65 -> "강한 비"
        66, 67 -> "어는 비"
        71 -> "약한 눈"
        73 -> "보통 눈"
        75 -> "강한 눈"
        77 -> "싸락눈"
        80, 81, 82 -> "소나기"
        85, 86 -> "눈보라"
        95 -> "천둥번개"
        96, 99 -> "우박을 동반한 뇌우"
        else -> "구름 많음"
    }

    fun degreeToCardinalKorean(deg: Int): String {
        val normalized = ((deg % 360) + 360) % 360
        return when {
            normalized < 23 -> "북풍 (N)"
            normalized < 68 -> "북동풍 (NE)"
            normalized < 113 -> "동풍 (E)"
            normalized < 158 -> "남동풍 (SE)"
            normalized < 203 -> "남풍 (S)"
            normalized < 248 -> "남서풍 (SW)"
            normalized < 293 -> "서풍 (W)"
            normalized < 338 -> "북서풍 (NW)"
            else -> "북풍 (N)"
        }
    }

    fun windSpeedFeelKorean(speedMs: Double): String = when {
        speedMs < 1.5 -> "고요함 · 바람 거의 없음"
        speedMs < 3.4 -> "남실바람 · 얼굴에 바람이 느껴짐"
        speedMs < 5.5 -> "산들바람 · 나뭇잎이 흔들림"
        speedMs < 8.0 -> "건들바람 · 머리카락과 옷자락이 날림"
        speedMs < 10.8 -> "흔들바람 · 우산 쓰기가 다소 불편함"
        else -> "강풍 주의 · 보행 및 시설물 유의"
    }

    fun uvIndexGradeKorean(uv: Double): Pair<String, String> = when {
        uv < 3.0 -> "낮음" to "별도 차단제 없이 야외 활동이 안전해요"
        uv < 6.0 -> "보통" to "외출 시 선글라스와 자외선 차단제를 권장해요"
        uv < 8.0 -> "높음" to "한낮 외출 시 자외선 차단제·모자가 필수예요"
        uv < 11.0 -> "매우 높음" to "가급적 그늘에 머물고 긴소매나 양산을 챙기세요"
        else -> "위험" to "외출을 자제하고 자외선 차단에 각별히 유의하세요"
    }

    fun humidityGradeKorean(humidity: Int): Pair<String, String> = when {
        humidity < 30 -> "건조 주의" to "공기가 매우 건조해요. 수분 섭취와 보습에 신경 쓰세요"
        humidity < 40 -> "다소 건조" to "약간 건조한 편이에요. 가벼운 수분 보충을 권장해요"
        humidity <= 65 -> "쾌적" to "실내외 활동하기에 가장 상쾌하고 쾌적한 습도예요"
        humidity <= 80 -> "다소 습함" to "습도가 다소 높아 통기성 좋은 옷차림이 좋아요"
        else -> "매우 습함" to "습기가 많아 꿉꿉할 수 있으니 쾌적한 소재를 입으세요"
    }

    fun pressureGradeKorean(pressureHpa: Double, diff3h: Double): Pair<String, String> {
        val grade = when {
            pressureHpa >= 1020.0 -> "고기압 영향"
            pressureHpa <= 1005.0 -> "저기압 영향"
            else -> "표준 기압"
        }
        val advice = when {
            diff3h <= -2.0 -> "기압이 빠르게 내려가고 있어 구름이나 강수 가능성이 있어요"
            diff3h >= 2.0 -> "기압이 상승하며 날씨가 점차 맑고 안정되고 있어요"
            pressureHpa >= 1020.0 -> "고기압 영향권으로 대기가 대체로 맑고 안정적이에요"
            pressureHpa <= 1005.0 -> "저기압 영향권으로 흐리거나 바람·비 가능성이 있어요"
            else -> "기압 변화가 완만해 컨디션과 대기 상태가 안정적이에요"
        }
        return grade to advice
    }

    // 한국 환경부 기준 미세먼지 (PM10) 및 초미세먼지 (PM2.5) 등급
    fun pm10GradeKorean(pm10: Double): String = when {
        pm10 <= 30.0 -> "좋음"
        pm10 <= 80.0 -> "보통"
        pm10 <= 150.0 -> "나쁨"
        else -> "매우 나쁨"
    }

    fun pm25GradeKorean(pm25: Double): String = when {
        pm25 <= 15.0 -> "좋음"
        pm25 <= 35.0 -> "보통"
        pm25 <= 75.0 -> "나쁨"
        else -> "매우 나쁨"
    }
}
