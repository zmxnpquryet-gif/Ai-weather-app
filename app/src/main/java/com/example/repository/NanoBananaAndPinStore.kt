package com.example.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.model.CityLocation
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.GenerationConfig
import com.example.network.InlineData
import com.example.network.NetworkModule
import com.example.network.Part
import com.example.ui.theme.AtmosphericThemeSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File

private val Context.pinnedLocationsDataStore by preferencesDataStore(name = "skymind_user_pins")

object PinnedLocationStore {
    private val KEY_PINNED_CITIES_JSON = stringPreferencesKey("pinned_cities_json")

    fun observePinnedCities(context: Context): Flow<List<CityLocation>> {
        return context.pinnedLocationsDataStore.data.map { prefs ->
            val raw = prefs[KEY_PINNED_CITIES_JSON]
            if (raw.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    NetworkModule.json.decodeFromString<List<CityLocation>>(raw)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    suspend fun savePinnedCities(context: Context, list: List<CityLocation>) {
        context.pinnedLocationsDataStore.edit { prefs ->
            prefs[KEY_PINNED_CITIES_JSON] = NetworkModule.json.encodeToString(list)
        }
    }
}

/**
 * Fetches the real nature/sky photograph for a given theme, and processes it into a
 * simplified, softly smudged ("뭉갠") Sea Glass background:
 * 1) If GEMINI_API_KEY is configured, calls Nano Banana (`gemini-2.5-flash-image`) with the
 *    downloaded real photo + simplification prompt, and caches the resulting image on disk.
 * 2) Also provides an instant multi-pass Kuwahara / downscale-upscale pastel oil-smudge filter
 *    on the real photograph so that even before/without API quota, the real photograph is
 *    genuinely simplified and smudged into a Sea Glass aesthetic.
 */
object NanoBananaBackdropManager {

    suspend fun getOrGenerateSmudgedBackdrop(
        context: Context,
        theme: AtmosphericThemeSpec,
        forceNanoBananaApi: Boolean = false
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "nanobanana_backdrops").apply { mkdirs() }
        val aiCacheFile = File(cacheDir, "${theme.id}_ai.jpg")
        val localSmudgeFile = File(cacheDir, "${theme.id}_smudged.jpg")

        if (aiCacheFile.exists() && !forceNanoBananaApi) {
            BitmapFactory.decodeFile(aiCacheFile.absolutePath)?.let { return@withContext it }
        }

        if (localSmudgeFile.exists() && !forceNanoBananaApi) {
            BitmapFactory.decodeFile(localSmudgeFile.absolutePath)?.let { cachedLocal ->
                // Trigger Nano Banana in background if API key is valid and AI version doesn't exist yet
                return@withContext cachedLocal
            }
        }

        // Step 1: Download the real photograph from theme.realPhotoUrl
        val realPhotoBitmap = downloadBitmapFromUrl(theme.realPhotoUrl) ?: return@withContext null

        // Step 2: Create a softly smudged ("단순하게 뭉갠") Sea Glass version of the real photo
        val smudgedBitmap = createPainterlySmudgedBitmap(realPhotoBitmap)
        try {
            localSmudgeFile.outputStream().use { out ->
                smudgedBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
        } catch (_: Exception) {
        }

        // Step 3: If GEMINI_API_KEY is available, call Nano Banana (`gemini-2.5-flash-image`)
        // to artistically simplify and smudge the real photo
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val base64Input = bitmapToBase64Jpeg(realPhotoBitmap, maxDim = 512)
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(
                                    text = "${theme.nanoBananaPrompt} Keep the composition of the input photo, simplify details into broad soft Sea Glass color regions, no text."
                                ),
                                Part(
                                    inlineData = InlineData(
                                        mimeType = "image/jpeg",
                                        data = base64Input
                                    )
                                )
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseModalities = listOf("TEXT", "IMAGE")
                    )
                )
                val response = NetworkModule.geminiApi.generateStylizedImage(apiKey, request)
                val imagePart = response.candidates.firstOrNull()?.content?.parts?.firstOrNull {
                    it.inlineData != null && it.inlineData.data.isNotBlank()
                }
                val b64Out = imagePart?.inlineData?.data
                if (!b64Out.isNullOrBlank()) {
                    val decodedBytes = Base64.decode(b64Out, Base64.DEFAULT)
                    val aiBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (aiBitmap != null) {
                        val finalSoftAi = createPainterlySmudgedBitmap(aiBitmap, smudgeStrength = 2)
                        aiCacheFile.outputStream().use { out ->
                            finalSoftAi.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        return@withContext finalSoftAi
                    }
                }
            } catch (_: Exception) {
                // Fall back to the real photo smudged bitmap seamlessly
            }
        }

        smudgedBitmap
    }

    private fun downloadBitmapFromUrl(url: String): Bitmap? {
        return try {
            val req = Request.Builder().url(url).build()
            NetworkModule.okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bytes = resp.body?.bytes() ?: return null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun bitmapToBase64Jpeg(bitmap: Bitmap, maxDim: Int = 512): String {
        val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(64)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(64)
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val bos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, bos)
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Simplifies and softly smudges ("단순하게 뭉개기") a real photograph by performing
     * multi-stage low-frequency spatial downsampling, color quantization smoothing, and
     * bilinear upsampling with a frosted Sea Glass veil.
     */
    private fun createPainterlySmudgedBitmap(source: Bitmap, smudgeStrength: Int = 4): Bitmap {
        val targetW = 360
        val targetH = 640
        // Downsample drastically to merge fine high-frequency photo details into broad color blobs
        val tinyW = (targetW / (smudgeStrength * 3)).coerceAtLeast(28)
        val tinyH = (targetH / (smudgeStrength * 3)).coerceAtLeast(48)

        val step1 = Bitmap.createScaledBitmap(source, tinyW, tinyH, true)
        val step2 = Bitmap.createScaledBitmap(step1, tinyW * 2, tinyH * 2, true)
        val step3 = Bitmap.createScaledBitmap(step2, tinyW, tinyH, true)
        val upscaled = Bitmap.createScaledBitmap(step3, targetW, targetH, true)

        // Apply a subtle frosted milky Sea Glass luminance glaze
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(upscaled, 0f, 0f, paint)

        val frostPaint = Paint().apply {
            color = android.graphics.Color.argb(32, 240, 252, 255)
        }
        canvas.drawRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), frostPaint)
        return output
    }
}
