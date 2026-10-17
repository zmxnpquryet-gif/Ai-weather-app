package com.example.repository

import com.example.BuildConfig
import com.example.model.CityLocation
import com.example.model.DailyForecastItem
import com.example.model.GeminiWeatherAdvice
import com.example.model.HourlyForecastItem
import com.example.model.WeatherBundle
import com.example.model.WeatherInterpreter
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.GenerationConfig
import com.example.network.NetworkModule
import com.example.network.Part
import com.example.ui.theme.WeatherThemeCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.roundToInt

class WeatherRepository {

    suspend fun searchCities(query: String): List<CityLocation> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val response = NetworkModule.geocodingApi.searchCity(query.trim())
            response.results?.map { item ->
                CityLocation(
                    name = item.name,
                    adminArea = item.admin1 ?: "",
                    country = item.country ?: "",
                    latitude = item.latitude,
                    longitude = item.longitude
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchWeatherBundle(location: CityLocation): Result<WeatherBundle> = withContext(Dispatchers.IO) {
        try {
            val weatherDeferred = async {
                NetworkModule.weatherApi.getForecast(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            }
            val airDeferred = async {
                try {
                    NetworkModule.airQualityApi.getAirQuality(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val weatherResp = weatherDeferred.await()
            val airResp = airDeferred.await()

            val current = weatherResp.current
            val daily = weatherResp.daily
            val hourly = weatherResp.hourly
            val airCurrent = airResp?.current

            val currentTemp = current?.temperature2m ?: 18.0
            val feelsLike = current?.apparentTemperature ?: currentTemp
            val highTemp = daily?.temperatureMax?.firstOrNull() ?: (currentTemp + 4.0)
            val lowTemp = daily?.temperatureMin?.firstOrNull() ?: (currentTemp - 4.0)
            val weatherCode = current?.weatherCode ?: 0
            val isDay = (current?.isDay ?: 1) == 1

            val windSpeedMs = current?.windSpeed10m ?: 0.0
            val windGustsMs = current?.windGusts10m ?: windSpeedMs
            val windDirDeg = current?.windDirection10m ?: 0
            val windCardinal = WeatherInterpreter.degreeToCardinalKorean(windDirDeg)
            val windFeel = WeatherInterpreter.windSpeedFeelKorean(windSpeedMs)

            val currentHourPrefix = current?.time?.take(13) ?: ""
            val matchedHourIndex = hourly?.time?.indexOfFirst { it.startsWith(currentHourPrefix) }
                ?.takeIf { it >= 0 } ?: 0

            val uvCurrent = airCurrent?.uvIndex
                ?: hourly?.uvIndex?.getOrNull(matchedHourIndex)
                ?: 0.0
            val uvMaxToday = daily?.uvIndexMax?.firstOrNull() ?: uvCurrent
            val (uvGrade, uvShortAdvice) = WeatherInterpreter.uvIndexGradeKorean(maxOf(uvCurrent, uvMaxToday))

            val pm10Val = airCurrent?.pm10 ?: 24.0
            val pm25Val = airCurrent?.pm25 ?: 12.0
            val ozoneVal = airCurrent?.ozone ?: 45.0
            val aqiVal = airCurrent?.usAqi ?: 35

            val precipProbNow = hourly?.precipitationProbability?.getOrNull(matchedHourIndex) ?: 0
            val precipProbMaxToday = daily?.precipitationProbMax?.firstOrNull() ?: precipProbNow
            val currentPrecipMm = current?.precipitation ?: 0.0
            val todayTotalPrecipMm = daily?.precipitationSum?.firstOrNull() ?: currentPrecipMm

            // Parse Humidity & Dew Point Data
            val humidityNow = current?.relativeHumidity2m
                ?: hourly?.relativeHumidity2m?.getOrNull(matchedHourIndex)
                ?: 50
            val todayHourlyHumidities = hourly?.relativeHumidity2m
                ?.subList(matchedHourIndex, minOf(matchedHourIndex + 24, hourly.relativeHumidity2m.size))
                ?.filterNotNull()
                .orEmpty()
            val humidityMinToday = todayHourlyHumidities.minOrNull() ?: maxOf(20, humidityNow - 12)
            val humidityMaxToday = todayHourlyHumidities.maxOrNull() ?: minOf(100, humidityNow + 12)
            val dewPointNow = hourly?.dewPoint2m?.getOrNull(matchedHourIndex)
                ?: calculateMagnusDewPoint(currentTemp, humidityNow)
            val (humidityGrade, humidityAdviceShort) = WeatherInterpreter.humidityGradeKorean(humidityNow)

            // Parse Surface Pressure, Sea-Level Pressure & 3-Hour Pressure Trend
            val surfacePressureNow = current?.surfacePressure
                ?: hourly?.surfacePressure?.getOrNull(matchedHourIndex)
                ?: 1013.0
            val seaLevelPressureNow = current?.pressureMsl
                ?: hourly?.pressureMsl?.getOrNull(matchedHourIndex)
                ?: surfacePressureNow
            val prev3hIndex = (matchedHourIndex - 3).coerceAtLeast(0)
            val pressure3hAgo = hourly?.pressureMsl?.getOrNull(prev3hIndex)
                ?: hourly?.surfacePressure?.getOrNull(prev3hIndex)
                ?: seaLevelPressureNow
            val pressureDiff3h = seaLevelPressureNow - pressure3hAgo
            val (pressureGrade, pressureAdviceShort) = WeatherInterpreter.pressureGradeKorean(
                pressureHpa = seaLevelPressureNow,
                diff3h = pressureDiff3h
            )

            val sunriseRaw = daily?.sunrise?.firstOrNull() ?: "06:30"
            val sunsetRaw = daily?.sunset?.firstOrNull() ?: "18:30"
            val sunriseFormatted = sunriseRaw.substringAfter("T", sunriseRaw)
            val sunsetFormatted = sunsetRaw.substringAfter("T", sunsetRaw)

            val hourlyItems = mutableListOf<HourlyForecastItem>()
            val hourlyTimes = hourly?.time ?: emptyList()
            val endIndex = minOf(matchedHourIndex + 24, hourlyTimes.size)
            for (idx in matchedHourIndex until endIndex) {
                val rawTime = hourlyTimes[idx]
                val hourPart = rawTime.substringAfter("T", "00:00").take(5)
                val label = if (idx == matchedHourIndex) "지금" else "${hourPart.take(2)}시"
                val hTemp = hourly?.temperature2m?.getOrNull(idx) ?: currentTemp
                val hProb = hourly?.precipitationProbability?.getOrNull(idx) ?: 0
                val hMm = hourly?.precipitation?.getOrNull(idx) ?: 0.0
                val hCode = hourly?.weatherCode?.getOrNull(idx) ?: weatherCode
                val hWind = hourly?.windSpeed10m?.getOrNull(idx) ?: windSpeedMs
                val hWindDeg = hourly?.windDirection10m?.getOrNull(idx) ?: windDirDeg
                val hUv = hourly?.uvIndex?.getOrNull(idx) ?: 0.0
                val hHumidity = hourly?.relativeHumidity2m?.getOrNull(idx) ?: humidityNow
                val hPressure = hourly?.pressureMsl?.getOrNull(idx)
                    ?: hourly?.surfacePressure?.getOrNull(idx)
                    ?: seaLevelPressureNow

                hourlyItems.add(
                    HourlyForecastItem(
                        hourLabel = label,
                        temp = hTemp,
                        precipProb = hProb,
                        precipMm = hMm,
                        weatherCode = hCode,
                        windSpeedMs = hWind,
                        windDirCardinal = WeatherInterpreter.degreeToCardinalKorean(hWindDeg).substringBefore(" "),
                        uvIndex = hUv,
                        humidityPercent = hHumidity,
                        pressureHpa = hPressure
                    )
                )
            }

            val dailyItems = mutableListOf<DailyForecastItem>()
            val dailyTimes = daily?.time ?: emptyList()
            for (idx in dailyTimes.indices) {
                val rawDate = dailyTimes[idx]
                val monthDay = if (rawDate.length >= 10) {
                    "${rawDate.substring(5, 7)}.${rawDate.substring(8, 10)}"
                } else rawDate
                val dayName = if (idx == 0) "오늘" else if (idx == 1) "내일" else parseDayOfWeekKorean(rawDate)
                val dCode = daily?.weatherCode?.getOrNull(idx) ?: 0
                val dMin = daily?.temperatureMin?.getOrNull(idx) ?: lowTemp
                val dMax = daily?.temperatureMax?.getOrNull(idx) ?: highTemp
                val dProb = daily?.precipitationProbMax?.getOrNull(idx) ?: 0
                val dSum = daily?.precipitationSum?.getOrNull(idx) ?: 0.0
                val dUv = daily?.uvIndexMax?.getOrNull(idx) ?: 0.0
                val dWind = daily?.windSpeedMax?.getOrNull(idx) ?: 0.0

                dailyItems.add(
                    DailyForecastItem(
                        dateLabel = monthDay,
                        dayOfWeek = dayName,
                        weatherCode = dCode,
                        weatherDescription = WeatherInterpreter.weatherCodeToKorean(dCode),
                        minTemp = dMin,
                        maxTemp = dMax,
                        precipProbMax = dProb,
                        precipSumMm = dSum,
                        uvMax = dUv,
                        windMaxMs = dWind
                    )
                )
            }

            val nowStr = SimpleDateFormat("HH:mm 업데이트", Locale.KOREA).format(Date())

            Result.success(
                WeatherBundle(
                    location = location,
                    updatedAt = nowStr,
                    currentTemp = currentTemp,
                    feelsLikeTemp = feelsLike,
                    highTemp = highTemp,
                    lowTemp = lowTemp,
                    weatherCode = weatherCode,
                    weatherSummary = WeatherInterpreter.weatherCodeToKorean(weatherCode),
                    isDay = isDay,
                    windSpeedMs = windSpeedMs,
                    windGustsMs = windGustsMs,
                    windDirectionDeg = windDirDeg,
                    windDirectionCardinal = windCardinal,
                    windFeelDescription = windFeel,
                    uvIndexCurrent = uvCurrent,
                    uvIndexMaxToday = uvMaxToday,
                    uvGrade = uvGrade,
                    uvAdviceShort = uvShortAdvice,
                    pm10 = pm10Val,
                    pm10Grade = WeatherInterpreter.pm10GradeKorean(pm10Val),
                    pm25 = pm25Val,
                    pm25Grade = WeatherInterpreter.pm25GradeKorean(pm25Val),
                    ozone = ozoneVal,
                    aqiScore = aqiVal,
                    precipProbabilityNow = precipProbNow,
                    precipProbabilityMaxToday = precipProbMaxToday,
                    currentPrecipMm = currentPrecipMm,
                    todayTotalPrecipMm = todayTotalPrecipMm,
                    humidityPercent = humidityNow,
                    dewPointC = dewPointNow,
                    humidityMinToday = humidityMinToday,
                    humidityMaxToday = humidityMaxToday,
                    humidityGrade = humidityGrade,
                    humidityAdviceShort = humidityAdviceShort,
                    pressureHpa = surfacePressureNow,
                    seaLevelPressureHpa = seaLevelPressureNow,
                    pressureDiff3h = pressureDiff3h,
                    pressureGrade = pressureGrade,
                    pressureAdviceShort = pressureAdviceShort,
                    cloudCoverPercent = current?.cloudCover ?: 0,
                    sunriseTime = sunriseFormatted,
                    sunsetTime = sunsetFormatted,
                    hourlyItems = hourlyItems,
                    dailyItems = dailyItems
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateMagnusDewPoint(tempC: Double, humidityPercent: Int): Double {
        val rh = humidityPercent.coerceIn(1, 100) / 100.0
        val a = 17.27
        val b = 237.7
        val alpha = ((a * tempC) / (b + tempC)) + ln(rh)
        return (b * alpha) / (a - alpha)
    }

    suspend fun generateGeminiAdvice(
        weather: WeatherBundle,
        personaStyle: String
    ): GeminiWeatherAdvice = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext createSmartLocalFallbackAdvice(weather, personaStyle)
        }

        val themeCatalogText = WeatherThemeCatalog.allThemes.joinToString("\n") {
            "- ${it.id}: ${it.nameKo} (${it.categoryKo}) - ${it.subtitleKo}"
        }

        val prompt = buildString {
            appendLine("다음은 ${weather.location.name}의 실시간 종합 기상 관측 및 예보 데이터입니다.")
            appendLine("- 현재 기온: ${weather.currentTemp.roundToInt()}°C (체감 ${weather.feelsLikeTemp.roundToInt()}°C)")
            appendLine("- 오늘 최저/최고 기온: ${weather.lowTemp.roundToInt()}°C / ${weather.highTemp.roundToInt()}°C (일교차 ${(weather.highTemp - weather.lowTemp).roundToInt()}°C)")
            appendLine("- 하늘 상태: ${weather.weatherSummary} (낮 여부: ${if (weather.isDay) "낮" else "밤/저녁"})")
            appendLine("- 풍속 및 풍향: 풍속 ${String.format(Locale.US, "%.1f", weather.windSpeedMs)}m/s (돌풍 최대 ${String.format(Locale.US, "%.1f", weather.windGustsMs)}m/s), 풍향 ${weather.windDirectionCardinal} (${weather.windDirectionDeg}°)")
            appendLine("- 비 올 확률(강수확률): 현재 ${weather.precipProbabilityNow}%, 오늘 최대 ${weather.precipProbabilityMaxToday}%")
            appendLine("- 강수량: 현재 시간당 ${weather.currentPrecipMm}mm, 오늘 예상 누적 강수량 ${weather.todayTotalPrecipMm}mm")
            appendLine("- 자외선 지수(UV): 현재 ${String.format(Locale.US, "%.1f", weather.uvIndexCurrent)}, 오늘 최고 ${String.format(Locale.US, "%.1f", weather.uvIndexMaxToday)} (${weather.uvGrade})")
            appendLine("- 미세먼지(PM10): ${weather.pm10.roundToInt()} µg/m³ (${weather.pm10Grade}), 초미세먼지(PM2.5): ${weather.pm25.roundToInt()} µg/m³ (${weather.pm25Grade})")
            appendLine("- 습도: ${weather.humidityPercent}% (${weather.humidityGrade}, 이슬점 ${weather.dewPointC.roundToInt()}°C), 해면기압: ${weather.seaLevelPressureHpa.roundToInt()} hPa (${weather.pressureGrade})")
            appendLine("- 사용자 선호 상황/스타일: $personaStyle")
            appendLine()
            appendLine("[선택 가능한 30가지 앱 분위기 테마 목록]")
            appendLine(themeCatalogText)
            appendLine()
            appendLine("위 기상 데이터를 종합 판단하여:")
            appendLine("1) 30가지 테마 중 현재 날씨·기온·바람·미세먼지·시간대 분위기에 가장 잘 어울리는 `selectedThemeId`를 정확히 하나 선택하고, 선정 이유(`themeSelectionReason`)를 적어주세요.")
            appendLine("2) 풍속·일교차·체감온도·습도를 반영한 겉옷/상의/하의/신발 추천과, 강수확률·강수량·풍속을 함께 고려한 우산 지참 여부, 미세먼지 마스크, 자외선 조언을 구체적인 한국어 JSON으로 작성해 주세요.")
        }

        val responseSchema = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("selectedThemeId") {
                    put("type", "STRING")
                    put("description", "30가지 테마 ID 중 현재 날씨에 가장 잘 어울리는 테마 ID")
                }
                putJsonObject("themeSelectionReason") {
                    put("type", "STRING")
                    put("description", "현재 기상 수치를 바탕으로 이 테마를 선택한 이유 (1문장)")
                }
                putJsonObject("headline") {
                    put("type", "STRING")
                    put("description", "오늘 날씨와 옷차림 핵심을 요약한 친근한 한 줄 헤드라인 (예: 오늘은 하늘이 정말 맑네요!)")
                }
                putJsonObject("summaryComment") {
                    put("type", "STRING")
                    put("description", "기온, 바람, 비, 미세먼지, 자외선, 습도를 종합한 2문장의 다정한 브리핑")
                }
                putJsonObject("umbrellaStatus") {
                    put("type", "STRING")
                    put("description", "필수 지참 / 접이식 우산 권장 / 불필요 중 하나")
                }
                putJsonObject("umbrellaReason") {
                    put("type", "STRING")
                    put("description", "강수확률, 예상 강수량(mm), 풍속을 근거로 우산이 왜 필요한지/불필요한지 설명")
                }
                putJsonObject("maskStatus") {
                    put("type", "STRING")
                    put("description", "KF94 필수 / 가벼운 마스크 권장 / 불필요 (상쾌함) 중 하나")
                }
                putJsonObject("maskReason") {
                    put("type", "STRING")
                    put("description", "PM10 및 PM2.5 수치를 바탕으로 한 호흡기 및 환기 조언")
                }
                putJsonObject("uvCareStatus") {
                    put("type", "STRING")
                    put("description", "차단제·선글라스 필수 / 가벼운 선크림 권장 / 안심 단계 중 하나")
                }
                putJsonObject("uvCareReason") {
                    put("type", "STRING")
                    put("description", "자외선 최고 지수와 구름양을 고려한 피부 보호 조언")
                }
                putJsonObject("outfitOuter") {
                    put("type", "STRING")
                    put("description", "추천 아우터/겉옷")
                }
                putJsonObject("outfitTop") {
                    put("type", "STRING")
                    put("description", "추천 상의 아이템")
                }
                putJsonObject("outfitBottom") {
                    put("type", "STRING")
                    put("description", "추천 하의 아이템")
                }
                putJsonObject("outfitShoesAndAcc") {
                    put("type", "STRING")
                    put("description", "추천 신발 및 패션 소품")
                }
                putJsonObject("stylingReason") {
                    put("type", "STRING")
                    put("description", "체감온도, 풍속, 습도, 일교차를 근거로 이 옷차림을 추천한 이유")
                }
                putJsonObject("timeSlotTips") {
                    put("type", "ARRAY")
                    putJsonObject("items") { put("type", "STRING") }
                    put("description", "오전, 오후, 저녁 시간대별 날씨·복장 포인트 3가지")
                }
                putJsonObject("activityRecommendation") {
                    put("type", "STRING")
                    put("description", "오늘 날씨에 딱 맞는 야외/실내 활동이나 생활 팁")
                }
            }
            putJsonArray("required") {
                add("selectedThemeId")
                add("themeSelectionReason")
                add("headline")
                add("summaryComment")
                add("umbrellaStatus")
                add("umbrellaReason")
                add("maskStatus")
                add("maskReason")
                add("uvCareStatus")
                add("uvCareReason")
                add("outfitOuter")
                add("outfitTop")
                add("outfitBottom")
                add("outfitShoesAndAcc")
                add("stylingReason")
                add("timeSlotTips")
                add("activityRecommendation")
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = responseSchema,
                temperature = 0.65f
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(text = "당신은 SkyMind 날씨 비서이자 퍼스널 스타일리스트입니다. 날씨 데이터를 분석해 30가지 테마 중 최적의 테마를 고르고, 옷차림과 우산 지참 여부를 다정하고 정확하게 브리핑합니다.")
                )
            )
        )

        try {
            val response = NetworkModule.geminiApi.generateContent(apiKey, request)
            val rawJson = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!rawJson.isNullOrBlank()) {
                NetworkModule.json.decodeFromString<GeminiWeatherAdvice>(rawJson)
            } else {
                createSmartLocalFallbackAdvice(weather, personaStyle)
            }
        } catch (e: Exception) {
            createSmartLocalFallbackAdvice(weather, personaStyle)
        }
    }

    suspend fun askFollowUpQuestion(
        weather: WeatherBundle,
        question: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val maxProb = maxOf(weather.precipProbabilityNow, weather.precipProbabilityMaxToday)
            return@withContext "현재 ${weather.location.name} 기온은 ${weather.currentTemp.roundToInt()}°C(체감 ${weather.feelsLikeTemp.roundToInt()}°C), 습도는 ${weather.humidityPercent}%(${weather.humidityGrade}), 기압은 ${weather.seaLevelPressureHpa.roundToInt()}hPa(${weather.pressureGrade})입니다. 오늘 강수확률은 최대 ${maxProb}%, 예상 강수량은 ${weather.todayTotalPrecipMm}mm이며 바람은 ${weather.windDirectionCardinal} ${String.format(Locale.US, "%.1f", weather.windSpeedMs)}m/s로 불어 ${if (maxProb >= 40) "작은 우산과 가벼운 겉옷을 챙기시면 좋겠어요!" else "가벼운 겉옷과 함께 쾌적하게 외출하기 좋은 날씨예요!"}"
        }

        val contextPrompt = """
            [현재 ${weather.location.name} 기상 요약]
            - 현재 기온: ${weather.currentTemp.roundToInt()}°C (체감 ${weather.feelsLikeTemp.roundToInt()}°C), 최저 ${weather.lowTemp.roundToInt()}°C / 최고 ${weather.highTemp.roundToInt()}°C
            - 하늘: ${weather.weatherSummary}, 강수확률 최대 ${weather.precipProbabilityMaxToday}%, 예상 강수량 ${weather.todayTotalPrecipMm}mm
            - 바람: ${weather.windDirectionCardinal} ${String.format(Locale.US, "%.1f", weather.windSpeedMs)}m/s (돌풍 ${String.format(Locale.US, "%.1f", weather.windGustsMs)}m/s)
            - 자외선: 최고 ${String.format(Locale.US, "%.1f", weather.uvIndexMaxToday)} (${weather.uvGrade})
            - 미세먼지(PM10): ${weather.pm10.roundToInt()}µg/m³(${weather.pm10Grade}), 초미세먼지(PM2.5): ${weather.pm25.roundToInt()}µg/m³(${weather.pm25Grade})
            - 습도: ${weather.humidityPercent}% (${weather.humidityGrade}, 이슬점 ${weather.dewPointC.roundToInt()}°C), 기압: ${weather.seaLevelPressureHpa.roundToInt()} hPa (${weather.pressureGrade})
            
            사용자 질문: "$question"
            위 기상 수치를 직접 인용하면서 다정하고 명확하게 2~3문장으로 답변해 주세요.
        """.trimIndent()

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = contextPrompt)))),
                generationConfig = GenerationConfig(temperature = 0.7f)
            )
            val response = NetworkModule.geminiApi.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "죄송합니다. 답변을 생성하지 못했습니다."
        } catch (e: Exception) {
            "네트워크 또는 API 호출 중 오류가 발생했습니다: ${e.localizedMessage ?: "잠시 후 다시 시도해 주세요."}"
        }
    }

    private fun createSmartLocalFallbackAdvice(
        weather: WeatherBundle,
        personaStyle: String
    ): GeminiWeatherAdvice {
        val matchedTheme = WeatherThemeCatalog.selectBestThemeForWeather(
            weatherCode = weather.weatherCode,
            tempC = weather.currentTemp,
            isDay = weather.isDay,
            pm10 = weather.pm10,
            pm25 = weather.pm25,
            windSpeedMs = weather.windSpeedMs,
            uvIndex = maxOf(weather.uvIndexCurrent, weather.uvIndexMaxToday)
        )

        val feels = weather.feelsLikeTemp
        val tempGap = (weather.highTemp - weather.lowTemp).roundToInt()
        val maxRainProb = maxOf(weather.precipProbabilityNow, weather.precipProbabilityMaxToday)
        val totalRain = maxOf(weather.currentPrecipMm, weather.todayTotalPrecipMm)

        val (umbrellaStat, umbrellaDesc) = when {
            totalRain >= 5.0 || (maxRainProb >= 60 && weather.windSpeedMs >= 6.0) ->
                "필수 지참 (장우산)" to "강수확률 ${maxRainProb}%, 예상 강수량 ${String.format(Locale.US, "%.1f", totalRain)}mm에 풍속 ${String.format(Locale.US, "%.1f", weather.windSpeedMs)}m/s 바람이 동반되어 튼튼한 장우산이 필요합니다."
            maxRainProb >= 40 || totalRain >= 0.5 ->
                "접이식 우산 권장" to "오늘 최대 강수확률이 ${maxRainProb}%로 예보되어 있으니 가방에 가벼운 우산을 챙기시는 것이 안전합니다."
            else ->
                "우산 불필요" to "강수확률 ${maxRainProb}%, 예상 강수량 ${String.format(Locale.US, "%.1f", totalRain)}mm로 비 걱정 없이 가볍게 외출하셔도 좋습니다."
        }

        val (maskStat, maskDesc) = when {
            weather.pm10 > 80 || weather.pm25 > 35 ->
                "KF94 마스크 필수" to "미세먼지(${weather.pm10.roundToInt()}µg/m³) 농도가 높습니다. 외출 시 보건용 마스크를 꼭 착용하세요."
            weather.pm10 > 45 || weather.pm25 > 20 ->
                "보통 (민감군 권장)" to "대기질은 보통 수준(PM10 ${weather.pm10.roundToInt()}µg/m³)으로 무난하지만 호흡기가 예민하시다면 가벼운 마스크를 챙기세요."
            else ->
                "좋음 (마스크 불필요)" to "미세먼지 ${weather.pm10.roundToInt()}µg/m³(${weather.pm10Grade})로 공기가 깨끗해 산책과 환기에 아주 좋습니다."
        }

        val maxUv = maxOf(weather.uvIndexCurrent, weather.uvIndexMaxToday)
        val (uvStat, uvDesc) = when {
            maxUv >= 6.0 ->
                "선크림·선글라스 필수" to "오후 자외선 지수가 ${String.format(Locale.US, "%.1f", maxUv)}(${weather.uvGrade})까지 오르니 자외선 차단제를 꼭 챙기세요!"
            maxUv >= 3.0 ->
                "가벼운 선크림 권장" to "한낮 최고 자외선 지수 ${String.format(Locale.US, "%.1f", maxUv)}(보통)로 외출 전 가벼운 선크림을 권장합니다."
            else ->
                "안심 단계" to "자외선 지수 ${String.format(Locale.US, "%.1f", maxUv)}(낮음)로 자외선 부담 없이 활동하기 좋습니다."
        }

        val (outer, top, bottom, shoes) = when {
            feels >= 27.0 -> listOf(
                "얇은 린넨 셔츠 (실내 냉방 대비)",
                "통기성 좋은 반팔 티셔츠",
                "시원한 쿨링 슬랙스 또는 하프 팬츠",
                "통풍이 잘 되는 스니커즈 & 선글라스"
            )
            feels >= 21.0 -> listOf(
                "가벼운 가디건 또는 얇은 데님 자켓",
                "가벼운 반팔 또는 얇은 긴팔 티셔츠",
                "편안한 코튼 팬츠 또는 와이드 데님",
                "가벼운 캔버스화 또는 로퍼"
            )
            feels >= 15.0 -> listOf(
                "가벼운 겉옷(블레이저, 바람막이, 트렌치코트)",
                "산뜻한 셔츠, 얇은 니트 또는 맨투맨",
                "생지 데님 팬츠 또는 테이퍼드 슬랙스",
                "편안한 레더 스니커즈"
            )
            feels >= 9.0 -> listOf(
                "도톰한 자켓, 트러커 자켓 또는 울 가디건",
                "포근한 니트 + 셔츠 레이어드",
                "탄탄한 코튼 팬츠 또는 울 슬랙스",
                "발목을 감싸는 스니커즈나 부츠"
            )
            feels >= 3.0 -> listOf(
                "울 코트, 경량 패딩 또는 플리스 자켓",
                "보온 내의 + 도톰한 니트",
                "기모 팬츠 또는 코듀로이 팬츠",
                "방풍 부츠 및 가벼운 머플러"
            )
            else -> listOf(
                "두꺼운 롱패딩 또는 헤비 다운 코트",
                "발열 내의 + 터틀넥 니트",
                "방풍 기모 팬츠",
                "방한 부츠, 목도리, 장갑 필수"
            )
        }

        return GeminiWeatherAdvice(
            selectedThemeId = matchedTheme.id,
            themeSelectionReason = "${weather.weatherSummary} 하늘과 기온 ${weather.currentTemp.roundToInt()}°C, 습도 ${weather.humidityPercent}%(${weather.humidityGrade}), 미세먼지 '${weather.pm10Grade}' 상태를 종합하여 '${matchedTheme.nameKo}' 테마를 자동 적용했어요.",
            headline = "오늘은 ${weather.weatherSummary}, 가벼운 겉옷이 딱 좋아요!",
            summaryComment = "현재 기온은 ${weather.currentTemp.roundToInt()}°C(체감 ${weather.feelsLikeTemp.roundToInt()}°C), 습도 ${weather.humidityPercent}%이며 ${weather.windDirectionCardinal}이 ${String.format(Locale.US, "%.1f", weather.windSpeedMs)}m/s로 불고 있어요. 낮에는 포근하지만 아침·저녁 일교차(${tempGap}°C)가 있으니 겉옷을 챙기면 좋을 것 같아요.",
            umbrellaStatus = umbrellaStat,
            umbrellaReason = umbrellaDesc,
            maskStatus = maskStat,
            maskReason = maskDesc,
            uvCareStatus = uvStat,
            uvCareReason = uvDesc,
            outfitOuter = outer,
            outfitTop = top,
            outfitBottom = bottom,
            outfitShoesAndAcc = shoes,
            stylingReason = "낮에는 최고 ${weather.highTemp.roundToInt()}°C로 따뜻하지만 아침·저녁 최저 ${weather.lowTemp.roundToInt()}°C로 쌀쌀할 수 있어요. 체감온도(${weather.feelsLikeTemp.roundToInt()}°C), 습도(${weather.humidityPercent}%), 풍속(${String.format(Locale.US, "%.1f", weather.windSpeedMs)}m/s)을 고려한 $personaStyle 코디입니다.",
            timeSlotTips = listOf(
                "오전 10시 ~ 오후 1시: ${weather.weatherSummary} 날씨, 외출 및 산책하기 좋아요.",
                "오후 2시 이후: 기온이 ${weather.highTemp.roundToInt()}°C까지 올라 다소 포근할 수 있어요.",
                "자외선 & 저녁 바람: 자외선 지수(${String.format(Locale.US, "%.1f", maxUv)})와 저녁 바람(${String.format(Locale.US, "%.1f", weather.windSpeedMs)}m/s)을 대비해 선크림과 겉옷을 챙기세요!"
            ),
            activityRecommendation = if (maxRainProb < 30 && weather.pm10 <= 80) {
                "산책(좋음) · 피크닉(좋음) · 카페 투어(좋음) — 오늘은 대체로 맑고 쾌적한 날씨가 이어지며 활동하기 좋은 날씨예요!"
            } else {
                "실내 카페 투어(추천) · 전시 관람(좋음) — 강수확률(${maxRainProb}%) 또는 대기질을 고려해 쾌적한 실내 일정을 추천해요."
            }
        )
    }

    private fun parseDayOfWeekKorean(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
            val date = sdf.parse(dateStr) ?: return dateStr
            SimpleDateFormat("EEE", Locale.KOREA).format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}
