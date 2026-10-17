package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.CityLocation
import com.example.model.WeatherBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Standard Web Mercator (EPSG:3857) Slippy Map Projection Math for OpenStreetMap.
 */
object OsmMercator {
    fun latLonToTileFraction(lat: Double, lon: Double, zoom: Int): Pair<Double, Double> {
        val clampedLat = lat.coerceIn(-85.0511, 85.0511)
        val n = (1 shl zoom).toDouble()
        val xTile = ((lon + 180.0) / 360.0) * n
        val latRad = clampedLat * PI / 180.0
        val yTile = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
        return xTile to yTile
    }

    fun tileFractionToLatLon(xTile: Double, yTile: Double, zoom: Int): Pair<Double, Double> {
        val n = (1 shl zoom).toDouble()
        val wrappedX = ((xTile % n) + n) % n
        val clampedY = yTile.coerceIn(0.0, n)
        val lon = (wrappedX / n) * 360.0 - 180.0
        val rad = atan(sinh(PI * (1.0 - 2.0 * clampedY / n)))
        val lat = (rad * 180.0 / PI).coerceIn(-85.0511, 85.0511)
        return lat to lon
    }
}

/**
 * Singleton In-Memory Tile Cache & Downloader for Real OpenStreetMap & RainViewer Radar Tiles.
 */
object OsmTileRepository {
    private val tileCache = LruCache<String, ImageBitmap>(220)
    private val inFlightKeys: MutableSet<String> = Collections.synchronizedSet(HashSet())

    @Volatile
    var latestRainViewerPath: String? = null
        private set

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    fun getCachedTile(key: String): ImageBitmap? = synchronized(tileCache) {
        tileCache.get(key)
    }

    private fun putCachedTile(key: String, bitmap: ImageBitmap) = synchronized(tileCache) {
        tileCache.put(key, bitmap)
    }

    suspend fun ensureRainViewerTimestamp() {
        if (latestRainViewerPath != null) return
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("https://api.rainviewer.com/public/weather-maps.json")
                    .header("User-Agent", "SkyMindWeatherApp/2.0 (Android; OpenStreetMap)")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string().orEmpty()
                        val root = JSONObject(bodyStr)
                        val radar = root.optJSONObject("radar")
                        val past = radar?.optJSONArray("past")
                        if (past != null && past.length() > 0) {
                            val lastItem = past.getJSONObject(past.length() - 1)
                            val path = lastItem.optString("path")
                            if (path.isNotBlank()) {
                                latestRainViewerPath = path
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun fetchOsmTile(z: Int, x: Int, y: Int, onLoaded: () -> Unit) {
        val n = 1 shl z
        val wrappedX = ((x % n) + n) % n
        if (y < 0 || y >= n) return

        val key = "osm_${z}_${wrappedX}_$y"
        if (getCachedTile(key) != null) return
        if (!inFlightKeys.add(key)) return

        withContext(Dispatchers.IO) {
            try {
                val primaryUrl = "https://tile.openstreetmap.org/$z/$wrappedX/$y.png"
                val fallbackUrl = "https://a.basemaps.cartocdn.com/rastertiles/voyager/$z/$wrappedX/$y.png"

                val bitmap = downloadBitmap(primaryUrl) ?: downloadBitmap(fallbackUrl)
                if (bitmap != null) {
                    putCachedTile(key, bitmap)
                    withContext(Dispatchers.Main) {
                        onLoaded()
                    }
                }
            } finally {
                inFlightKeys.remove(key)
            }
        }
    }

    suspend fun fetchRainRadarTile(z: Int, x: Int, y: Int, onLoaded: () -> Unit) {
        val radarPath = latestRainViewerPath ?: return
        if (z > 12) return // RainViewer tiles support up to zoom 12
        val n = 1 shl z
        val wrappedX = ((x % n) + n) % n
        if (y < 0 || y >= n) return

        val key = "rain_${radarPath}_${z}_${wrappedX}_$y"
        if (getCachedTile(key) != null) return
        if (!inFlightKeys.add(key)) return

        withContext(Dispatchers.IO) {
            try {
                val url = "https://tilecache.rainviewer.com$radarPath/256/$z/$wrappedX/$y/2/1_1.png"
                val bitmap = downloadBitmap(url)
                if (bitmap != null) {
                    putCachedTile(key, bitmap)
                    withContext(Dispatchers.Main) {
                        onLoaded()
                    }
                }
            } finally {
                inFlightKeys.remove(key)
            }
        }
    }

    private fun downloadBitmap(url: String): ImageBitmap? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SkyMindWeatherApp/2.0 (Android; OpenStreetMap)")
                .header("Accept", "image/png,image/*;q=0.8")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Interactive Real OpenStreetMap (OSM) Slippy Map with:
 * - Real OSM Street/City/Neighborhood Tiles (zoom 5..15)
 * - Live RainViewer Radar Tile Overlay + Weather Layer Contours
 * - Smooth Drag-to-Pan & Tap-to-Pick exact coordinates
 * - Zoom In / Zoom Out & Quick Zoom Level Chips (전국 / 시·구 / 동네 상세)
 * - Custom Pinned City Markers & Active Target Pin
 */
@Composable
fun InteractiveOsmWeatherMap(
    weather: WeatherBundle,
    pinnedLocations: List<CityLocation>,
    selectedLayer: String,
    timeOffsetHours: Float,
    onPickCoordinates: (lat: Double, lon: Double) -> Unit,
    onRequestGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Map camera center & zoom level
    var cameraLat by remember { mutableDoubleStateOf(weather.location.latitude) }
    var cameraLon by remember { mutableDoubleStateOf(weather.location.longitude) }
    var zoomLevel by remember { mutableIntStateOf(11) }

    // Selected marker coordinate
    var markerLat by remember(weather.location.latitude) { mutableDoubleStateOf(weather.location.latitude) }
    var markerLon by remember(weather.location.longitude) { mutableDoubleStateOf(weather.location.longitude) }

    // Re-center map camera when user selects a city via search or pinned chip
    LaunchedEffect(weather.location.latitude, weather.location.longitude) {
        markerLat = weather.location.latitude
        markerLon = weather.location.longitude
        cameraLat = weather.location.latitude
        cameraLon = weather.location.longitude
    }

    // Counter incremented whenever an OSM or RainViewer tile finishes downloading
    var tileInvalidateCounter by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        OsmTileRepository.ensureRainViewerTimestamp()
        tileInvalidateCounter++
    }

    val cyanRain = SkyMindCyanRain
    val sunOrange = SkyMindSunOrange
    val greenGood = SkyMindGreenGood
    val blueAccent = SkyMindBlueAccent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .testTag("interactive_weather_map_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zoomLevel) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val tileSizePx = 256.dp.toPx()
                            val (camTileX, camTileY) = OsmMercator.latLonToTileFraction(cameraLat, cameraLon, zoomLevel)
                            val deltaTilesX = (tapOffset.x - size.width / 2f) / tileSizePx
                            val deltaTilesY = (tapOffset.y - size.height / 2f) / tileSizePx
                            val (pickedLat, pickedLon) = OsmMercator.tileFractionToLatLon(
                                camTileX + deltaTilesX,
                                camTileY + deltaTilesY,
                                zoomLevel
                            )
                            markerLat = pickedLat
                            markerLon = pickedLon
                            onPickCoordinates(pickedLat, pickedLon)
                        },
                        onDoubleTap = { tapOffset ->
                            val tileSizePx = 256.dp.toPx()
                            val (camTileX, camTileY) = OsmMercator.latLonToTileFraction(cameraLat, cameraLon, zoomLevel)
                            val deltaTilesX = (tapOffset.x - size.width / 2f) / tileSizePx
                            val deltaTilesY = (tapOffset.y - size.height / 2f) / tileSizePx
                            val (tappedLat, tappedLon) = OsmMercator.tileFractionToLatLon(
                                camTileX + deltaTilesX,
                                camTileY + deltaTilesY,
                                zoomLevel
                            )
                            cameraLat = tappedLat
                            cameraLon = tappedLon
                            if (zoomLevel < 15) zoomLevel++
                        }
                    )
                }
                .pointerInput(zoomLevel) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val tileSizePx = 256.dp.toPx()
                        val (camTileX, camTileY) = OsmMercator.latLonToTileFraction(cameraLat, cameraLon, zoomLevel)
                        val newTileX = camTileX - (dragAmount.x / tileSizePx)
                        val newTileY = camTileY - (dragAmount.y / tileSizePx)
                        val (newLat, newLon) = OsmMercator.tileFractionToLatLon(newTileX, newTileY, zoomLevel)
                        cameraLat = newLat
                        cameraLon = newLon
                    }
                }
        ) {
            // Read tileInvalidateCounter so Canvas redraws as tiles arrive
            val redrawTick = tileInvalidateCounter
            if (redrawTick < 0) return@Canvas

            val w = size.width
            val h = size.height
            val tileSizePx = 256.dp.toPx()
            val n = 1 shl zoomLevel

            // Soft ocean backdrop while tiles load
            drawRect(color = Color(0xFFD6F5F2), size = size)

            val (camTileX, camTileY) = OsmMercator.latLonToTileFraction(cameraLat, cameraLon, zoomLevel)
            val halfTilesW = (w / 2f) / tileSizePx
            val halfTilesH = (h / 2f) / tileSizePx

            val minTileX = floor(camTileX - halfTilesW).toInt() - 1
            val maxTileX = floor(camTileX + halfTilesW).toInt() + 1
            val minTileY = floor(camTileY - halfTilesH).toInt().coerceAtLeast(0)
            val maxTileY = floor(camTileY + halfTilesH).toInt().coerceAtMost(n - 1)

            val tileSizeInt = tileSizePx.roundToInt() + 1

            // 1. Render Real OpenStreetMap Tiles
            for (tx in minTileX..maxTileX) {
                val wrappedTx = ((tx % n) + n) % n
                val screenX = (w / 2f) + ((tx - camTileX) * tileSizePx).toFloat()

                for (ty in minTileY..maxTileY) {
                    val screenY = (h / 2f) + ((ty - camTileY) * tileSizePx).toFloat()
                    val dstOffset = IntOffset(screenX.roundToInt(), screenY.roundToInt())
                    val dstSize = IntSize(tileSizeInt, tileSizeInt)

                    val tileKey = "osm_${zoomLevel}_${wrappedTx}_$ty"
                    val cachedBmp = OsmTileRepository.getCachedTile(tileKey)

                    if (cachedBmp != null) {
                        drawImage(
                            image = cachedBmp,
                            dstOffset = dstOffset,
                            dstSize = dstSize
                        )
                    } else {
                        // Draw parent tile quadrant if available so zooming is seamless
                        if (zoomLevel > 5) {
                            val parentKey = "osm_${zoomLevel - 1}_${wrappedTx / 2}_${ty / 2}"
                            val parentBmp = OsmTileRepository.getCachedTile(parentKey)
                            if (parentBmp != null) {
                                val srcHalfW = parentBmp.width / 2
                                val srcHalfH = parentBmp.height / 2
                                val srcX = (wrappedTx % 2) * srcHalfW
                                val srcY = (ty % 2) * srcHalfH
                                drawImage(
                                    image = parentBmp,
                                    srcOffset = IntOffset(srcX, srcY),
                                    srcSize = IntSize(srcHalfW, srcHalfH),
                                    dstOffset = dstOffset,
                                    dstSize = dstSize
                                )
                            }
                        }

                        // Trigger async download of this OSM tile
                        scope.launch {
                            OsmTileRepository.fetchOsmTile(zoomLevel, wrappedTx, ty) {
                                tileInvalidateCounter++
                            }
                        }
                    }

                    // 2. Render Real RainViewer Radar Tile when "강수" layer is selected
                    if (selectedLayer == "강수" && zoomLevel <= 12) {
                        val radarPath = OsmTileRepository.latestRainViewerPath
                        if (radarPath != null) {
                            val rainKey = "rain_${radarPath}_${zoomLevel}_${wrappedTx}_$ty"
                            val rainBmp = OsmTileRepository.getCachedTile(rainKey)
                            if (rainBmp != null) {
                                drawImage(
                                    image = rainBmp,
                                    dstOffset = dstOffset,
                                    dstSize = dstSize,
                                    alpha = 0.65f
                                )
                            } else {
                                scope.launch {
                                    OsmTileRepository.fetchRainRadarTile(zoomLevel, wrappedTx, ty) {
                                        tileInvalidateCounter++
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Helper to convert any (lat, lon) to current screen pixel Offset
            fun latLonToScreenOffset(lat: Double, lon: Double): Offset {
                val (tx, ty) = OsmMercator.latLonToTileFraction(lat, lon, zoomLevel)
                val sx = (w / 2f) + ((tx - camTileX) * tileSizePx).toFloat()
                val sy = (h / 2f) + ((ty - camTileY) * tileSizePx).toFloat()
                return Offset(sx, sy)
            }

            // 3. Weather Layer Radial Aura & Wind Streamlines around selected marker
            val markerScreenPos = latLonToScreenOffset(markerLat, markerLon)
            val shiftX = (timeOffsetHours * 10f).dp.toPx()
            val overlayColor = when (selectedLayer) {
                "강수" -> cyanRain
                "기온" -> sunOrange
                "미세먼지" -> greenGood
                else -> blueAccent
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        overlayColor.copy(alpha = 0.42f),
                        overlayColor.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(markerScreenPos.x + shiftX, markerScreenPos.y),
                    radius = 120.dp.toPx()
                ),
                radius = 120.dp.toPx(),
                center = Offset(markerScreenPos.x + shiftX, markerScreenPos.y)
            )

            // Wind streamlines when "바람" layer is active
            if (selectedLayer == "바람") {
                val angleRad = Math.toRadians(weather.windDirectionDeg.toDouble())
                val dx = cos(angleRad).toFloat() * 34.dp.toPx()
                val dy = sin(angleRad).toFloat() * 34.dp.toPx()
                for (gx in 1..4) {
                    for (gy in 1..4) {
                        val st = Offset(w * (gx / 5f), h * (gy / 5f))
                        drawLine(
                            color = blueAccent.copy(alpha = 0.75f),
                            start = st,
                            end = Offset(st.x + dx, st.y + dy),
                            strokeWidth = 2.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 4. Draw User's Pinned Locations on the Real OSM Map
            pinnedLocations.forEach { pin ->
                val pinPos = latLonToScreenOffset(pin.latitude, pin.longitude)
                if (pinPos.x in -40f..(w + 40f) && pinPos.y in -40f..(h + 40f)) {
                    drawCircle(
                        color = Color.White,
                        radius = 10.dp.toPx(),
                        center = pinPos
                    )
                    drawCircle(
                        color = Color(0xFF0D9488),
                        radius = 7.dp.toPx(),
                        center = pinPos
                    )
                }
            }

            // 5. Draw Active Selected Location Pin Marker
            if (markerScreenPos.x in -60f..(w + 60f) && markerScreenPos.y in -60f..(h + 60f)) {
                // Outer pulse ring
                drawCircle(
                    color = Color(0xFFE11D48).copy(alpha = 0.25f),
                    radius = 24.dp.toPx(),
                    center = markerScreenPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 24.dp.toPx(),
                    center = markerScreenPos,
                    style = Stroke(width = 2.dp.toPx())
                )
                // Pin Tear-Drop Silhouette pointing to markerScreenPos
                val pinHeight = 28.dp.toPx()
                val pinRadius = 11.dp.toPx()
                val pinCenterY = markerScreenPos.y - pinHeight * 0.65f
                val pinPath = Path().apply {
                    moveTo(markerScreenPos.x, markerScreenPos.y)
                    lineTo(markerScreenPos.x - pinRadius * 0.85f, pinCenterY + pinRadius * 0.3f)
                    lineTo(markerScreenPos.x + pinRadius * 0.85f, pinCenterY + pinRadius * 0.3f)
                    close()
                }
                drawPath(path = pinPath, color = Color(0xFFE11D48))
                drawCircle(
                    color = Color(0xFFE11D48),
                    radius = pinRadius,
                    center = Offset(markerScreenPos.x, pinCenterY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.5.dp.toPx(),
                    center = Offset(markerScreenPos.x, pinCenterY)
                )
            }
        }

        // Top-Left Instruction & Quick Zoom Presets Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SeaGlassSurface(
                cornerRadius = 50.dp,
                tintColor = Color(0xFFE0F7FA),
                surfaceAlpha = 0.42f
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = SkyMindBlueAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "지도 드래그 이동 · 터치하여 위치 선택",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyMindTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Zoom Level Presets (전국 / 시·구 / 동네)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val presets = listOf("전국" to 7, "시·구" to 11, "동네" to 14)
                presets.forEach { (label, targetZoom) ->
                    val isCurrentPreset = zoomLevel == targetZoom
                    SeaGlassPill(
                        isSelected = isCurrentPreset,
                        activeColor = Color(0xFF0D9488),
                        onClick = { zoomLevel = targetZoom }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrentPreset) Color.White else SkyMindTextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Right-side Map Controls: Zoom In (+), Zoom Out (-), Recenter Pin, GPS
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SeaGlassSurface(
                cornerRadius = 14.dp,
                tintColor = Color(0xFFE0F7FA),
                surfaceAlpha = 0.42f,
                onClick = { if (zoomLevel < 16) zoomLevel++ },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("map_zoom_in_button")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "확대",
                        tint = SkyMindTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            SeaGlassSurface(
                cornerRadius = 14.dp,
                tintColor = Color(0xFFE0F7FA),
                surfaceAlpha = 0.42f,
                onClick = { if (zoomLevel > 5) zoomLevel-- },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("map_zoom_out_button")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "축소",
                        tint = SkyMindTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            SeaGlassSurface(
                cornerRadius = 14.dp,
                tintColor = Color(0xFFE0F7FA),
                surfaceAlpha = 0.42f,
                onClick = {
                    cameraLat = markerLat
                    cameraLon = markerLon
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("map_recenter_pin_button")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "선택 위치로 이동",
                        tint = SkyMindBlueAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            SeaGlassSurface(
                cornerRadius = 14.dp,
                tintColor = Color(0xFFE0F7FA),
                surfaceAlpha = 0.42f,
                onClick = onRequestGps,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("map_gps_button")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "내 위치",
                        tint = Color(0xFF0D9488),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // OpenStreetMap Attribution Badge (Bottom-Right above callout)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 46.dp, end = 10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.65f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "© OpenStreetMap",
                style = MaterialTheme.typography.labelSmall,
                color = SkyMindTextSecondary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
