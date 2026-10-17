<div align="center">

# 🌤️ 하늘옷장 날씨 (SkyMind Weather)

**실시간 기상 · 대기질 데이터와 맞춤형 옷차림 · 외출 가이드를 한눈에 제공하는 씨글래스(Sea Glass) 감성 안드로이드 날씨 앱**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](#)
[![Open-Meteo](https://img.shields.io/badge/Weather%20API-Open--Meteo%20%26%20KMA-00B4D8?style=flat-square)](#)
[![OSM](https://img.shields.io/badge/Map-OpenStreetMap-7EBC6F?style=flat-square&logo=openstreetmap&logoColor=white)](#)

<br/>

> **"오늘 기온이 18도라는데… 그래서 뭘 입어야 하지? 우산은 챙겨야 하나?"**  
> **하늘옷장 날씨**는 단순한 기상 수치 나열을 넘어, **현재 위치의 기온·체감온도·일교차·강수확률·습도·기압·미세먼지**를 종합 분석해  
> **오늘의 겉옷·상의·하의 코디 추천**부터 **시간대별 기온/강수 복합 그래프, 인터랙티브 기상 지도, 글래스모피즘 홈 위젯, 아침 출발 브리핑 푸시 알림**까지 제공하는 올인원 라이프스타일 날씨 애플리케이션입니다.

</div>

---

## 📌 프로젝트 소개 및 기획 의도 (Overview)

매일 아침 외출 준비를 할 때 우리는 날씨 앱을 켜서 기온과 강수확률을 확인하지만, 정작 **"오늘 코트를 입어야 할지 가디건을 입어야 할지"**, **"퇴근길에 비가 올지"** 판단하기 위해 여러 화면을 오가야 합니다.

**하늘옷장 날씨**는 이러한 일상의 번거로움을 해결하기 위해 기획되었습니다.
1. **직관적인 의사결정 지원**: 기온, 체감온도, 바람, 습도, 미세먼지를 바탕으로 **겉옷 / 상의 / 하의 구체적인 착장 아이템**과 **우산·마스크 필요 여부**를 즉시 알려줍니다.
2. **시각적 몰입감 (Sea Glass & Glassmorphism)**: 바다 유리(Sea Glass)에서 영감을 얻은 반투명 프로스트 글래스 디자인과 **맑음·비·눈·흐림 실시간 배경 애니메이션**으로 날씨를 감각적으로 경험할 수 있습니다.
3. **앱을 열지 않아도 이어지는 경험**: 바쁜 아침에는 **지정 시각 푸시 알림(모닝 브리핑)**과 **홈 화면 글래스모피즘 위젯**만으로도 오늘의 날씨와 옷차림을 1초 만에 확인할 수 있습니다.

---

## ✨ 주요 기능 상세 설명 (Detailed Features)

### 1. 🧥 맞춤형 옷차림 가이드 & 외출 체크리스트
- **기상 맞춤형 레이어드 코디 추천**: 단순 현재 기온뿐만 아니라 **체감온도, 일교차, 풍속, 강수확률, 습도**를 함께 고려하여 오늘 입기 좋은 **겉옷 · 상의 · 하의** 조합과 한 줄 스타일링 가이드를 제공합니다.
- **생활 기상 지수 카드**: **습도(%) · 해면기압(hPa) · 미세먼지(PM10) · 초미세먼지(PM2.5) · 자외선 지수(UV)**를 전용 아이콘과 상태 배지로 시각화하여 가독성을 높였습니다.

### 2. 📊 시간대별 기온 · 강수량 이중 축(Dual-Axis) 복합 그래프
- **한눈에 보는 기온 & 비 예보**: 메인 화면 하단과 예보 탭에서 **왼쪽 Y축(기온 °C 곡선)**과 **오른쪽 Y축(강수량 mm 막대 & 강수확률 % 점선)**을 결합한 이중 축 그래프를 제공합니다.
- **인터랙티브 탐색**: `12시간 / 24시간` 범위 전환과 `복합(기온+강수) / 기온 곡선 / 강수량·확률` 보기 모드를 지원하며, 차트를 터치하거나 드래그하면 해당 시간대의 상세 기상 정보를 툴팁으로 즉시 보여줍니다.

### 3. ⭐ '나의 장소' 즐겨찾기 & 오픈스트리트맵(OSM) 핀 선택
- **나만의 장소 별칭 관리**: 자주 방문하는 장소를 저장하고 `🏠 우리집`, `🏢 회사/학교`, `🏋️ 운동/산책`, `✈️ 여행지`, `☕ 단골장소` 카테고리와 나만의 이름(예: *우리집*, *강남 본사*)을 붙여 관리할 수 있습니다.
- **실제 지도(OSM) 기반 자유로운 위치 선택**: 고정된 도시 목록에 제한되지 않고, 실제 **오픈스트리트맵(OpenStreetMap)** 위에서 원하는 위치를 직접 터치하거나 키워드로 검색해 실시간 날씨를 조회하고 핀으로 저장할 수 있습니다.
- **기상 레이더 오버레이**: 지도 위에서 **강수 레이더 · 기온 분포 · 미세먼지(PM2.5)** 모드를 전환하며 주변 지역의 날씨 흐름을 직관적으로 확인할 수 있습니다.

### 4. 🌊 씨글래스(Sea Glass) UI & 실시간 날씨 애니메이션 배경
- **시스템 연동 라이트 / 다크 모드**: 기기 시스템 테마에 맞춰 밝고 청량한 **민트 프로스트 글래스(Light)**와 깊이감 있는 **오션 슬레이트 글래스(Dark)** 컬러 시스템으로 자동 전환됩니다.
- **날씨 상태별 동적 파티클 애니메이션**: 현재 기상 상태에 따라 **맑음(햇살 광선과 빛망울) · 비(낙하하는 빗줄기와 물방울 파문) · 눈(바람에 흩날리는 눈송이) · 흐림(드리우는 안개구름)** 오버레이 애니메이션이 배경 위에서 부드럽게 재생됩니다.
- **나만의 배경 사진 블러 커스텀**: 갤러리에서 원하는 사진을 선택하면 글래스 카드의 텍스트 가독성을 해치지 않도록 감성적인 보케(Bokeh) 수채화 블러 배경으로 자동 변환해 적용합니다.

### 5. 📱 글래스모피즘(Glassmorphism) 안드로이드 홈 화면 위젯
- **3단 핵심 레이아웃 배치**:
  1. **상단**: 반투명 캡슐 형태의 **현재 위치(나의 장소 별칭 포함)** 및 날씨 상태 배지, 즉시 새로고침 버튼
  2. **중단**: 한눈에 들어오는 초대형 **현재 기온(°C)**과 체감온도 · 최고/최저 기온 · 습도/기압 요약
  3. **하단**: **추천 옷차림 한 줄 가이드**와 **겉옷 · 상의 · 하의 개별 태그 칩**, 우산·미세먼지 알림 배지
- 앱 내 설정 및 메인 화면에서 위젯 디자인을 실시간으로 미리 보고 **홈 화면에 바로 고정**할 수 있습니다.

### 6. 🔔 외출 전 아침 날씨 · 옷차림 푸시 알림 (Morning Briefing)
- **사용자 지정 시각 스케줄링**: 출근·등교 전 원하는 시간(예: `07:30`)을 지정하면 매일 아침 실시간 날씨를 조회해 **오늘의 기온, 비 소식(우산 필요 여부), 추천 옷차림, 미세먼지 상태**를 확장형 푸시 알림으로 전달합니다.
- **즉시 미리보기 지원**: 설정 화면의 **지금 브리핑 푸시 알림 미리 받아보기** 버튼으로 알림 동작을 즉시 테스트할 수 있습니다.

---

## ⚡ 핵심 기술 및 구현 포인트 (Technical Highlights)

- **100% Jetpack Compose & 커스텀 Canvas 그래픽스**:
  - 외부 무거운 차트/파티클 뷰에 의존하지 않고 Compose `Canvas`와 `Cubic Bezier Path`, `pointerInput` 제스처 감지를 활용하여 **60fps 실시간 날씨 파티클 애니메이션**과 **이중 축 인터랙티브 복합 차트**를 구현했습니다.
- **적응형 컬러 시스템 (`CompositionLocal` 기반 Sea Glass Theme)**:
  - `LocalSkyMindColors`를 통해 라이트/다크 모드별 글래스 배경 투명도, 테두리 하이라이트, 텍스트 대비를 일관되게 관리하여 어떤 배경 사진 위에서도 높은 가독성을 보장합니다.
- **실시간 기상 API + GPS + 역지오코딩 파이프라인**:
  - 안드로이드 위치 권한(`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) 연동 및 Open-Meteo 기상·대기질 API, Nominatim 지오코딩을 결합해 정확한 동네 단위 날씨를 제공합니다.
- **앱 ↔ 홈 위젯 ↔ 백그라운드 알람 데이터 동기화**:
  - 사용자가 선택한 위치나 즐겨찾기 별칭이 변경될 때마다 `WidgetDataSyncManager`를 통해 홈 화면 위젯(`AppWidgetManager`)과 아침 브리핑 알림(`AlarmManager`)이 즉시 최신 상태로 동기화됩니다.

---

## 🛠️ 기술 스택 (Tech Stack)

| 분류 | 사용 기술 |
| :--- | :--- |
| **언어 & UI** | Kotlin, Jetpack Compose, Material Design 3 (`Dynamic / Sea Glass Theme`) |
| **아키텍처** | MVVM (`ViewModel`, `StateFlow`, `Repository Pattern`, 단방향 데이터 플로우) |
| **기상 · 대기질 데이터** | Open-Meteo Forecast API (KMA/GFS 고해상도 모델), Open-Meteo Air Quality API (`PM10`, `PM2.5`, `UV Index`, `습도`, `해면기압`) |
| **지도 & 위치** | OpenStreetMap (OSM Slippy Tile Map), Nominatim Geocoding, Android `LocationManager` |
| **백그라운드 & 위젯** | `AppWidgetProvider` (RemoteViews Glassmorphism Widget), `AlarmManager` & `BroadcastReceiver` (정밀 시각 브리핑 알림) |
| **데이터 저장소** | `SharedPreferences` & `kotlinx.serialization` (나의 장소 즐겨찾기, 알림 스케줄, 위젯 동기화, 커스텀 배경 캐시) |

---

## 📂 프로젝트 구조 (Project Structure)

```text
app/src/main/java/com/example/
├── MainActivity.kt                          # Edge-to-Edge 설정 및 메인 엔트리포인트
├── model/
│   └── WeatherModels.kt                     # 날씨, 대기질, 옷차림, 나의 장소 데이터 모델
├── network/
│   └── RealWeatherApiClient.kt              # 실시간 기상·대기질 API 및 지오코딩 클라이언트
├── notification/
│   ├── MorningWeatherNotificationHelper.kt  # 아침 브리핑 푸시 알림 생성 및 발송
│   ├── MorningWeatherAlarmScheduler.kt      # 사용자 지정 시각 AlarmManager 스케줄러
│   └── MorningWeatherAlarmReceiver.kt       # 정시 알림 및 기기 재부팅(BOOT_COMPLETED) 리시버
├── repository/
│   ├── WeatherRepository.kt                 # 기상 데이터 통합 및 맞춤형 옷차림 분석 로직
│   ├── PinnedLocationStore.kt               # '나의 장소' 즐겨찾기, 테마, 알림 설정 저장소
│   └── DeviceLocationProvider.kt            # GPS 현재 위치 조회 및 한국어 역지오코딩
├── ui/
│   ├── components/
│   │   ├── HourlyTempPrecipChartSection.kt  # 시간대별 기온·강수량 이중 축 복합 그래프
│   │   ├── MyFavoritePlacesComponents.kt    # '나의 장소' 즐겨찾기 카드 및 관리 모달
│   │   ├── GlassmorphismWidgetCard.kt       # 글래스모피즘 홈 위젯 실시간 프리뷰 카드
│   │   ├── WeatherBackdropAnimation.kt      # 맑음/비/눈/흐림 실시간 배경 애니메이션
│   │   ├── OsmSlippyMapComponent.kt         # 인터랙티브 오픈스트리트맵 타일 뷰
│   │   └── ForecastSections.kt              # 습도·기압·대기질 상세 카드 및 주간 예보
│   ├── screens/
│   │   ├── WeatherDashboardScreen.kt        # 메인 홈, 옷차림, 예보 대시보드 화면
│   │   └── SkyMindExtraScreens.kt           # 기상 레이더 지도 및 테마·알림·위젯 설정 화면
│   └── theme/
│       └── Theme.kt                         # 씨글래스(Sea Glass) 라이트/다크 컬러 시스템
├── viewmodel/
│   └── WeatherViewModel.kt                  # 전역 UI 상태, 즐겨찾기, 테마, 위젯 동기화 관리
└── widget/
    ├── SkyMindWeatherWidgetProvider.kt      # 안드로이드 홈 화면 글래스모피즘 위젯 프로바이더
    └── WidgetDataSyncManager.kt             # 앱 ↔ 홈 위젯 실시간 데이터 브릿지
```

---

## 🚀 빌드 및 실행 방법 (Getting Started)

1. **저장소 클론**
   ```bash
   git clone https://github.com/<your-username>/<your-repo-name>.git
   cd <your-repo-name>
   ```

2. **Android Studio에서 열기**
   - **Android Studio Ladybug (2024.2.1)** 이상 버전에서 프로젝트를 엽니다.
   - Gradle Sync가 완료되면 별도의 유료 기상 API 키 발급 없이도 실시간 기상·대기질 조회 및 지도 기능이 즉시 동작합니다.

3. **앱 실행**
   - 실제 안드로이드 기기 또는 에뮬레이터(API 26+)를 연결한 뒤 **Run (`Shift + F10`)**을 눌러 빌드 및 설치합니다.
