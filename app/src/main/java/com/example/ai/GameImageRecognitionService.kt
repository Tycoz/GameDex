package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.GameCatalog
import com.example.data.model.GameItem
import com.example.data.network.NetworkClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

import com.example.util.GameCoverCropUtil
import com.example.data.storage.GameCoverStorage

object GameImageRecognitionService {

    private const val TAG = "GameImageAI"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = NetworkClient.baseClient.newBuilder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Converts a Bitmap to base64 JPEG string with resolution downscaling if needed.
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1024
        val scaled = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
            val (targetWidth, targetHeight) = if (ratio > 1f) {
                maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
            }
            bitmap.scale(targetWidth, targetHeight)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        try {
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        } finally {
            if (scaled !== bitmap) {
                scaled.recycle()
            }
        }
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Decodes an Android image Uri into a memory-bounded software Bitmap (max 1600px on longest side).
     */
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        val maxSide = 1600
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                    val w = info.size.width
                    val h = info.size.height
                    if (w > maxSide || h > maxSide) {
                        val ratio = w.toFloat() / h.toFloat().coerceAtLeast(1f)
                        val (tw, th) = if (ratio >= 1f) {
                            maxSide to (maxSide / ratio).toInt().coerceAtLeast(1)
                        } else {
                            (maxSide * ratio).toInt().coerceAtLeast(1) to maxSide
                        }
                        decoder.setTargetSize(tw, th)
                    }
                }
            } else {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, boundsOpts)
                }
                var sampleSize = 1
                while (boundsOpts.outWidth / sampleSize > maxSide || boundsOpts.outHeight / sampleSize > maxSide) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inMutable = true
                }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOpts)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode URI to Bitmap", e)
            null
        }
    }

    /**
     * Recognizes a video game from photo, automatically crops the cover/box art,
     * and saves it locally so it serves as the official game illustration when imported.
     */
    suspend fun recognizeGameFromPhoto(
        context: Context?,
        bitmap: Bitmap
    ): Result<GameItem> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured or placeholder.")
            return@withContext Result.failure(
                IllegalStateException("Clé API Gemini non configurée dans AI Studio Secrets. Configurez votre clé GEMINI_API_KEY ou utilisez la détection avec les exemples.")
            )
        }

        val base64Image = bitmapToBase64(bitmap)
        val prompt = """
            Tu es un expert mondial en jeux vidéo et rétro-gaming, spécialisé dans l'identification précise de jeux vidéo (comme sur Discogs pour les disques).
            Analyse cette photo de pochette, de boîte ou de cartouche de jeu vidéo.
            Identifie avec précision :
            - title : Le titre officiel exact du jeu
            - platform : La console / plateforme officielle exacte (ex: "Nintendo Switch 2", "Nintendo Switch", "PlayStation 5", "PlayStation 4", "PlayStation 3", "PlayStation 2", "PlayStation", "Nintendo 64", "Super Nintendo", "NES", "Game Boy", "Game Boy Advance", "Nintendo GameCube", "Nintendo DS", "Sega Mega Drive", "Sega Saturn", "Sega Dreamcast", "Xbox", "Xbox 360", "Xbox Series X", "Atari 2600", "PC-Engine / TurboGrafx-16", "Neo-Geo AES", "Vectrex", "Steam Deck", "PC", etc.)
            - format : "Cartouche", "Disque", "Coffret Collector"
            - region : "PAL", "NTSC-U", "NTSC-J" ou "Region Free"
            - completeness : "CIB" si boîte et jeu sont visibles, "Loose" si cartouche ou disque seul sans boîte, "Neuf scellé" si blister
            - releaseYear : Année de sortie (nombre entier)
            - publisher : Éditeur ou développeur officiel (ex: Nintendo, Capcom, Square, Konami, Sega...)
            - genre : Genre de jeu (Action-Aventure, RPG, Plateforme, etc.)
            - estimatedValue : Cote marchande moyenne estimée en euros (nombre décimal)
            - notes : Description de l'état visible ou détails identifiés sur la photo
            - cover_box : Coordonnées normalisées [ymin, xmin, ymax, xmax] (entiers entre 0 et 1000) délimitant précisément la jaquette / boîte / cartouche du jeu dans l'image pour le recadrage automatique

            Réponds UNIQUEMENT au format JSON strict suivant :
            {
              "title": "Nom du jeu",
              "platform": "Nom console",
              "format": "Cartouche ou Disque",
              "region": "PAL",
              "completeness": "CIB",
              "releaseYear": 1998,
              "publisher": "Nintendo",
              "genre": "Action-Aventure",
              "estimatedValue": 65.0,
              "notes": "Identification IA réussie",
              "cover_box": [100, 200, 900, 800]
            }
        """.trimIndent()

        try {
            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            // Text prompt part
                            put(JSONObject().apply { put("text", prompt) })
                            // Image inlineData part
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                // Generation config for JSON output
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val parsedGame = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: "Erreur HTTP ${response.code}"
                    Log.e(TAG, "Gemini API error: $errBody")
                    return@withContext Result.failure(Exception("Erreur Gemini ($errBody)"))
                }

                val respString = response.body?.string() ?: ""
                parseGeminiResponse(context, bitmap, respString)
            }
            Result.success(parsedGame)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call Gemini API", e)
            Result.failure(e)
        }
    }

    /**
     * Overload without Context for backward compatibility / testing.
     */
    suspend fun recognizeGameFromPhoto(bitmap: Bitmap): Result<GameItem> =
        recognizeGameFromPhoto(null, bitmap)

    private fun parseGeminiResponse(context: Context?, originalBitmap: Bitmap, jsonString: String): GameItem {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val textPart = parts?.optJSONObject(0)?.optString("text") ?: "{}"

        val cleanJson = textPart.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val data = JSONObject(cleanJson)
        val title = data.optString("title", "Jeu Identifié par IA")
        val platform = data.optString("platform", "Nintendo 64")
        val format = data.optString("format", "Cartouche")
        val region = data.optString("region", "PAL")
        val completeness = data.optString("completeness", "CIB")
        val releaseYear = data.optInt("releaseYear", 2000)
        val publisher = data.optString("publisher", "")
        val genre = data.optString("genre", "")
        val estimatedValue = data.optDouble("estimatedValue", 40.0)
        val notes = data.optString("notes", "Identifié automatiquement par IA visuelle (pochette/cartouche)")

        // Auto-crop cover art using AI bounding box or smart heuristic
        val boxArray = data.optJSONArray("cover_box")
        val croppedBitmap = if (boxArray != null && boxArray.length() == 4) {
            val ymin = boxArray.optInt(0, 0)
            val xmin = boxArray.optInt(1, 0)
            val ymax = boxArray.optInt(2, 1000)
            val xmax = boxArray.optInt(3, 1000)
            val box = GameCoverCropUtil.NormalizedBox(ymin, xmin, ymax, xmax)
            if (box.isValid) {
                GameCoverCropUtil.cropFromNormalizedBox(originalBitmap, ymin, xmin, ymax, xmax)
            } else {
                GameCoverCropUtil.autoCropGameCover(originalBitmap)
            }
        } else {
            GameCoverCropUtil.autoCropGameCover(originalBitmap)
        }

        // Save cropped cover to private internal storage for fast & persistent rendering
        val localCoverUri = try {
            if (context != null) {
                GameCoverStorage.saveCoverBitmap(context, croppedBitmap, title)
            } else ""
        } finally {
            if (croppedBitmap !== originalBitmap) {
                croppedBitmap.recycle()
            }
        }

        // Enrich with Master Catalog cover if matched and local cover is empty
        val catalogMatch = GameCatalog.sampleGames.find {
            it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
        }

        val finalCoverUrl = when {
            localCoverUri.isNotBlank() -> localCoverUri
            catalogMatch?.coverUrl?.isNotBlank() == true -> catalogMatch.coverUrl
            else -> ""
        }

        return GameItem(
            title = catalogMatch?.title ?: title,
            platform = catalogMatch?.platform ?: platform,
            genre = if (genre.isNotBlank()) genre else (catalogMatch?.genre ?: ""),
            publisher = if (publisher.isNotBlank()) publisher else (catalogMatch?.publisher ?: ""),
            releaseYear = if (releaseYear > 0) releaseYear else (catalogMatch?.releaseYear ?: 2000),
            format = format,
            region = region,
            completeness = completeness,
            barcode = catalogMatch?.barcode ?: "",
            estimatedValue = if (estimatedValue > 0) estimatedValue else (catalogMatch?.averageMarketValue ?: 35.0),
            purchasePrice = if (estimatedValue > 0) estimatedValue else (catalogMatch?.averageMarketValue ?: 35.0),
            notes = "$notes • Jaquette photo recadrée par GameDex AI",
            coverUrl = finalCoverUrl
        )
    }

    /**
     * Fallback demo recognition for emulator testing when physical camera or API key isn't provided.
     */
    fun getDemoRecognitionItem(presetName: String): GameItem {
        return when (presetName) {
            "mario64_cartridge" -> GameItem(
                title = "Super Mario 64",
                platform = "Nintendo 64",
                genre = "Plateforme 3D",
                releaseYear = 1996,
                publisher = "Nintendo",
                format = "Cartouche",
                region = "PAL",
                barcode = "045496870027",
                completeness = "Loose",
                boxCondition = "Sans boîte",
                manualCondition = "Sans notice",
                mediaCondition = "Très bon état",
                estimatedValue = 35.0,
                purchasePrice = 30.0,
                notes = "Reconnaissance visuelle IA : Cartouche Super Mario 64 N64 (Loose)",
                coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co20wa.webp"
            )
            "zelda_box" -> GameItem(
                title = "The Legend of Zelda: Ocarina of Time",
                platform = "Nintendo 64",
                genre = "Action-Aventure",
                releaseYear = 1998,
                publisher = "Nintendo",
                format = "Cartouche",
                region = "PAL",
                barcode = "045496870232",
                completeness = "CIB",
                boxCondition = "Très bon état",
                manualCondition = "Présent & Parfait",
                mediaCondition = "Parfait / Mint",
                estimatedValue = 65.0,
                purchasePrice = 60.0,
                notes = "Reconnaissance visuelle IA : Boîte or Ocarina of Time (CIB)",
                coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co5vmg.webp"
            )
            "chrono_trigger" -> GameItem(
                title = "Chrono Trigger",
                platform = "Super Nintendo",
                genre = "JRPG",
                releaseYear = 1995,
                publisher = "Square Soft",
                format = "Cartouche",
                region = "NTSC-U",
                barcode = "045496830427",
                completeness = "CIB",
                boxCondition = "Comme neuf",
                manualCondition = "Présent & Parfait",
                mediaCondition = "Parfait / Mint",
                estimatedValue = 240.0,
                purchasePrice = 200.0,
                notes = "Reconnaissance visuelle IA : Boîte Super Nintendo US Chrono Trigger",
                coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1n5v.webp"
            )
            else -> GameItem(
                title = "Silent Hill 2",
                platform = "PlayStation 2",
                genre = "Survival Horror",
                releaseYear = 2001,
                publisher = "Konami",
                format = "Disque DVD",
                region = "PAL",
                barcode = "5016553818320",
                completeness = "CIB",
                boxCondition = "Très bon état",
                manualCondition = "Présent & Parfait",
                mediaCondition = "Très bon état",
                estimatedValue = 85.0,
                purchasePrice = 75.0,
                notes = "Reconnaissance visuelle IA : Jaquette PS2 Silent Hill 2 originale",
                coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2156.webp"
            )
        }
    }
}
