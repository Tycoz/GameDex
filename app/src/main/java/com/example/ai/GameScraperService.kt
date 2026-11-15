package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ConsoleCatalog
import com.example.data.model.GameCatalog
import com.example.data.network.NetworkClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ScrapedGameData(
    val title: String,
    val platform: String,
    val publisher: String,
    val releaseYear: Int,
    val coverUrl: String,
    val genre: String = "",
    val format: String = "Cartouche",
    val region: String = "PAL",
    val estimatedValue: Double = 35.0,
    val description: String = "",
    val source: String = "Wikipédia & PriceCharting.com",
    val priceChartingUrl: String? = null,
    val priceLooseEur: Double? = null,
    val priceCibEur: Double? = null,
    val priceNewEur: Double? = null,
    val priceLooseUsd: Double? = null,
    val priceCibUsd: Double? = null,
    val priceNewUsd: Double? = null
)

object GameScraperService {

    private const val TAG = "GameScraper"
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val WIKIPEDIA_USER_AGENT = "GameDexApp/1.0 (Android; +https://github.com/aistudio/gamedex; contact: info@gamedex.app)"

    private val httpClient = NetworkClient.baseClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val YEAR_PATTERN = Pattern.compile("\\b(197\\d|198\\d|199\\d|200\\d|201\\d|202\\d)\\b")
    private val DISAMBIGUATION_REGEX = "\\s*\\([^)]*\\)".toRegex()
    private val PUBLISHER_EDIT_REGEX = "édité par ([A-Z][a-zA-Z0-9\\s]{2,20})".toRegex(RegexOption.IGNORE_CASE)
    private val SORTED_PLATFORM_CANDIDATES by lazy {
        (ConsoleCatalog.popularPlatforms + ConsoleCatalog.allConsoles.map { it.name })
            .filter { it != "Toutes" }
            .distinct()
            .sortedByDescending { it.length }
    }
    private val KNOWN_PUBLISHERS = listOf(
        "Nintendo", "Square Enix", "Square", "Capcom", "Konami", "Sega", "Bandai Namco",
        "Bandai", "Namco", "Sony Interactive Entertainment", "Sony", "Electronic Arts",
        "Ubisoft", "Atari", "LucasArts", "Bethesda", "Activision", "Blizzard", "Rockstar Games",
        "Enix", "Hudson Soft", "SNK", "Taito", "Tecmo", "Koei", "Midway", "Acclaim"
    )
    private val KNOWN_GENRES = listOf(
        "jeu de rôle" to "RPG",
        "jeu vidéo de rôle" to "RPG",
        "plates-formes" to "Plates-formes",
        "action-aventure" to "Action-Aventure",
        "tir à la première personne" to "FPS",
        "combat" to "Combat",
        "course" to "Course",
        "survie" to "Survival Horror",
        "aventure" to "Aventure",
        "sport" to "Sport",
        "stratégie" to "Stratégie",
        "infiltration" to "Infiltration"
    )

    /**
     * Cleans and formats image URLs (e.g. from Wikipedia / Wikimedia)
     */
    fun sanitizeCoverUrl(url: String): String {
        if (url.isBlank()) return ""
        var clean = url.trim()
        if (clean.startsWith("//")) {
            clean = "https:$clean"
        }
        if (clean.contains("?utm_source=")) {
            clean = clean.substringBefore("?")
        }
        return clean
    }

    /**
     * Scrapes video game metadata (cover photo, platform, publisher, release year, genre, etc.)
     * and calculates real market value using https://www.pricecharting.com/.
     */
    suspend fun scrapeGame(query: String, platformHint: String? = null): Result<ScrapedGameData> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Veuillez saisir un nom de jeu pour lancer le scraping."))
        }

        try {
            // Step 1: Scrape Wikipedia (fr first, then en) to get official cover art & intro text
            val wikiData = scrapeWikipedia(trimmedQuery, platformHint)

            // Step 2: Query PriceCharting.com to calculate the accurate market value
            val priceTargetTitle = wikiData?.title ?: trimmedQuery
            val priceTargetConsole = platformHint?.takeIf { it != "Toutes" && it != "Autre" } ?: wikiData?.platform
            val priceData = PriceChartingScraper.scrapePrice(priceTargetTitle, priceTargetConsole)

            val priceChartingUrl = priceData?.productUrl
                ?: PriceChartingScraper.buildSearchUrl(priceTargetTitle, priceTargetConsole)

            // Step 3: If Gemini API key is configured, enrich description & genre
            val geminiKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }

            if (geminiKey.isNotBlank() && geminiKey != "MY_GEMINI_API_KEY") {
                val aiResult = scrapeWithGemini(trimmedQuery, platformHint, wikiData?.description, geminiKey)
                if (aiResult != null) {
                    val wikiCover = wikiData?.coverUrl
                    val finalCover = if (!wikiCover.isNullOrBlank()) {
                        wikiCover
                    } else if (aiResult.coverUrl.isNotBlank() && (aiResult.coverUrl.startsWith("http://") || aiResult.coverUrl.startsWith("https://"))) {
                        sanitizeCoverUrl(aiResult.coverUrl)
                    } else {
                        findCuratedCover(trimmedQuery)
                    }

                    val finalValue = priceData?.estimatedValueEur ?: aiResult.estimatedValue
                    val sourceBadge = if (priceData != null) "Wikipédia & PriceCharting.com" else "Gemini IA & Wikipédia"

                    val finalData = aiResult.copy(
                        coverUrl = finalCover,
                        estimatedValue = finalValue,
                        source = sourceBadge,
                        priceChartingUrl = priceChartingUrl,
                        priceLooseEur = priceData?.loosePriceEur,
                        priceCibEur = priceData?.cibPriceEur,
                        priceNewEur = priceData?.newPriceEur,
                        priceLooseUsd = priceData?.loosePriceUsd,
                        priceCibUsd = priceData?.cibPriceUsd,
                        priceNewUsd = priceData?.newPriceUsd
                    )
                    return@withContext Result.success(finalData)
                }
            }

            // Step 4: If Gemini is unavailable, use Wikipedia extracted data with PriceCharting value
            if (wikiData != null) {
                val curatedCover = if (wikiData.coverUrl.isBlank()) findCuratedCover(trimmedQuery) else wikiData.coverUrl
                val format = deduceFormat(wikiData.platform)
                val finalValue = priceData?.estimatedValueEur ?: wikiData.estimatedValue
                val sourceBadge = if (priceData != null) "Wikipédia & PriceCharting.com" else "Wikipédia"

                return@withContext Result.success(
                    wikiData.copy(
                        coverUrl = curatedCover,
                        format = format,
                        estimatedValue = finalValue,
                        source = sourceBadge,
                        priceChartingUrl = priceChartingUrl,
                        priceLooseEur = priceData?.loosePriceEur,
                        priceCibEur = priceData?.cibPriceEur,
                        priceNewEur = priceData?.newPriceEur,
                        priceLooseUsd = priceData?.loosePriceUsd,
                        priceCibUsd = priceData?.cibPriceUsd,
                        priceNewUsd = priceData?.newPriceUsd
                    )
                )
            }

            // Step 5: Fallback to Curated Catalog with PriceCharting
            val curatedMatch = findCuratedGame(trimmedQuery)
            if (curatedMatch != null) {
                val finalValue = priceData?.estimatedValueEur ?: curatedMatch.estimatedValue
                val sourceBadge = if (priceData != null) "Catalogue & PriceCharting.com" else curatedMatch.source
                return@withContext Result.success(
                    curatedMatch.copy(
                        estimatedValue = finalValue,
                        source = sourceBadge,
                        priceChartingUrl = priceChartingUrl,
                        priceLooseEur = priceData?.loosePriceEur,
                        priceCibEur = priceData?.cibPriceEur,
                        priceNewEur = priceData?.newPriceEur,
                        priceLooseUsd = priceData?.loosePriceUsd,
                        priceCibUsd = priceData?.cibPriceUsd,
                        priceNewUsd = priceData?.newPriceUsd
                    )
                )
            }

            // If only PriceCharting returned something for this query
            if (priceData != null) {
                return@withContext Result.success(
                    ScrapedGameData(
                        title = priceData.gameTitle,
                        platform = platformHint?.takeIf { it != "Toutes" && it != "Autre" } ?: priceData.consoleName,
                        publisher = "Inconnu",
                        releaseYear = 2000,
                        coverUrl = findCuratedCover(priceData.gameTitle),
                        estimatedValue = priceData.estimatedValueEur,
                        source = "PriceCharting.com",
                        priceChartingUrl = priceChartingUrl,
                        priceLooseEur = priceData.loosePriceEur,
                        priceCibEur = priceData.cibPriceEur,
                        priceNewEur = priceData.newPriceEur,
                        priceLooseUsd = priceData.loosePriceUsd,
                        priceCibUsd = priceData.cibPriceUsd,
                        priceNewUsd = priceData.newPriceUsd,
                        description = "Cote calculée d'après l'historique des ventes PriceCharting.com."
                    )
                )
            }

            Result.failure(NoSuchElementException("Aucune donnée trouvée pour '$trimmedQuery'. Vérifiez l'orthographe ou précisez la console."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error scraping game", e)
            Result.failure(e)
        }
    }

    /**
     * Queries Wikipedia Search and Page API to retrieve real cover art thumbnail, publisher, year, and platform.
     */
    fun scrapeWikipedia(query: String, platformHint: String? = null): ScrapedGameData? {
        val searchTerms = if (!platformHint.isNullOrBlank() && platformHint != "Toutes" && platformHint != "Autre") {
            if (!query.contains(platformHint, ignoreCase = true)) {
                "$query $platformHint"
            } else query
        } else {
            query
        }

        // Try French Wikipedia first (great for French descriptions, titles, and localized info)
        val frResult = queryWikipediaEndpoint("https://fr.wikipedia.org", searchTerms, platformHint, langTag = "jeu vidéo")

        // Check if French result has a genuine box art cover (not just an SVG logo or missing)
        val hasBoxCover = frResult != null &&
                frResult.coverUrl.isNotBlank() &&
                !frResult.coverUrl.contains(".svg", ignoreCase = true) &&
                !frResult.coverUrl.contains("logo", ignoreCase = true)

        if (hasBoxCover) {
            return frResult
        }

        // Search English Wikipedia (en.wikipedia.org hosts original fair-use box art / jaquettes for almost every video game)
        val enResult = queryWikipediaEndpoint("https://en.wikipedia.org", searchTerms, platformHint, langTag = "video game")

        return when {
            frResult != null && enResult != null && enResult.coverUrl.isNotBlank() -> {
                // Combine French description & details with English official box art!
                frResult.copy(coverUrl = enResult.coverUrl)
            }
            frResult != null -> {
                frResult
            }
            else -> {
                enResult
            }
        }
    }

    private fun queryWikipediaEndpoint(
        baseUrl: String,
        searchTerms: String,
        platformHint: String?,
        langTag: String
    ): ScrapedGameData? {
        return try {
            val encodedSearch = URLEncoder.encode("$searchTerms $langTag", "UTF-8")
            val searchUrl = "$baseUrl/w/api.php?action=query&list=search&srsearch=$encodedSearch&srlimit=5&format=json&utf8=1"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", WIKIPEDIA_USER_AGENT)
                .build()

            val searchBody = httpClient.newCall(searchRequest).execute().use { searchResponse ->
                if (!searchResponse.isSuccessful) return null
                searchResponse.body?.string()
            } ?: return null
            val searchJson = JSONObject(searchBody)
            val searchHits = searchJson.optJSONObject("query")?.optJSONArray("search") ?: return null

            if (searchHits.length() == 0) return null

            var bestData: ScrapedGameData? = null

            // Inspect top hits to find one with a valid game page and cover thumbnail
            for (idx in 0 until minOf(searchHits.length(), 4)) {
                val hit = searchHits.getJSONObject(idx)
                val pageTitle = hit.getString("title")

                // Skip generic list, discography, or console hardware pages
                val lowerTitle = pageTitle.lowercase()
                if (lowerTitle.startsWith("liste ") || lowerTitle.startsWith("list of ") || lowerTitle.contains("discography") ||
                    ConsoleCatalog.allConsoles.any { it.name.equals(pageTitle, ignoreCase = true) } ||
                    pageTitle.equals(platformHint, ignoreCase = true)
                ) {
                    continue
                }

                val encodedTitle = URLEncoder.encode(pageTitle, "UTF-8")
                // CRITICAL: pilicense=any is required on Wikipedia to include fair-use video game box art thumbnails
                val detailUrl = "$baseUrl/w/api.php?action=query&prop=pageimages|extracts&exintro=1&explaintext=1&pithumbsize=800&pilicense=any&titles=$encodedTitle&format=json&utf8=1"
                val detailRequest = Request.Builder()
                    .url(detailUrl)
                    .header("User-Agent", WIKIPEDIA_USER_AGENT)
                    .build()

                val detailBody = httpClient.newCall(detailRequest).execute().use { detailResponse ->
                    if (!detailResponse.isSuccessful) null else detailResponse.body?.string()
                } ?: continue
                val detailJson = JSONObject(detailBody)
                val pages = detailJson.optJSONObject("query")?.optJSONObject("pages") ?: continue

                val pageKeys = pages.keys()
                if (!pageKeys.hasNext()) continue
                val pageObj = pages.getJSONObject(pageKeys.next())

                val rawTitle = pageObj.optString("title", pageTitle)
                val extract = pageObj.optString("extract", "")
                val rawThumbnail = pageObj.optJSONObject("thumbnail")?.optString("source", "") ?: ""
                val thumbnail = sanitizeCoverUrl(rawThumbnail)

                // Clean title (remove Wikipedia disambiguation suffixes like "(jeu vidéo)" or "(video game)")
                val cleanTitle = rawTitle.replace(DISAMBIGUATION_REGEX, "").trim()

                val platform = platformHint?.takeIf { it.isNotBlank() && it != "Toutes" && it != "Autre" }
                    ?: extractPlatformFromText(extract)
                val year = extractYearFromText(extract)
                val publisher = extractPublisherFromText(extract)
                val genre = extractGenreFromText(extract)

                val candidate = ScrapedGameData(
                    title = cleanTitle,
                    platform = platform,
                    publisher = publisher,
                    releaseYear = year,
                    coverUrl = thumbnail,
                    genre = genre,
                    format = deduceFormat(platform),
                    description = extract.take(400).trim()
                )

                if (bestData == null) {
                    bestData = candidate
                }

                // If this hit has a box cover (and not just an SVG logo), it is the ideal result
                if (thumbnail.isNotBlank() && !thumbnail.contains(".svg", ignoreCase = true)) {
                    bestData = candidate
                    break
                }
            }

            bestData
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia scrape error on $baseUrl", e)
            null
        }
    }

    /**
     * Calls Gemini 3.5 Flash to extract canonical video game data in structured JSON format.
     */
    private fun scrapeWithGemini(
        query: String,
        platformHint: String?,
        contextExtract: String?,
        apiKey: String
    ): ScrapedGameData? {
        return try {
            val url = "$GEMINI_BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"

            val prompt = """
                Tu es une API de base de données rétro-gaming et jeux vidéo.
                Pour le jeu vidéo demandé : "$query"${if (!platformHint.isNullOrBlank()) " sur $platformHint" else ""}
                ${if (!contextExtract.isNullOrBlank()) "Contexte Wikipédia : $contextExtract" else ""}
                
                Renvoie EXCLUSIVEMENT un objet JSON valide avec les clés suivantes :
                {
                  "title": "Nom officiel du jeu",
                  "platform": "Console / Plateforme officielle (ex: Nintendo 64, Super Nintendo, PlayStation, etc.)",
                  "publisher": "Éditeur officiel (ex: Nintendo, Square, Capcom, Konami, Sega...)",
                  "releaseYear": 1998,
                  "genre": "Genre principal (ex: Plates-formes, Action-Aventure, RPG...)",
                  "format": "Cartouche ou Disque ou Dématérialisé",
                  "estimatedValue": 35.0,
                  "coverUrl": "",
                  "description": "Court synopsis ou description en 2 phrases"
                }
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val respBody = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            } ?: return null
            val responseJson = JSONObject(respBody)
            val candidates = responseJson.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val text = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val parsedJson = JSONObject(text.trim())

            ScrapedGameData(
                title = parsedJson.optString("title", query),
                platform = parsedJson.optString("platform", platformHint ?: "Autre"),
                publisher = parsedJson.optString("publisher", "Inconnu"),
                releaseYear = parsedJson.optInt("releaseYear", 2000),
                coverUrl = parsedJson.optString("coverUrl", ""),
                genre = parsedJson.optString("genre", ""),
                format = parsedJson.optString("format", deduceFormat(parsedJson.optString("platform"))),
                estimatedValue = parsedJson.optDouble("estimatedValue", 30.0),
                description = parsedJson.optString("description", "")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gemini scrape failed, falling back", e)
            null
        }
    }

    private fun extractYearFromText(text: String): Int {
        val matcher = YEAR_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.toIntOrNull() ?: 2000
        }
        return 2000
    }

    private fun extractPlatformFromText(text: String): String {
        // Prioritize longer platform names so "Nintendo Switch 2" matches before "Nintendo Switch"
        for (p in SORTED_PLATFORM_CANDIDATES) {
            if (text.contains(p, ignoreCase = true)) {
                return p
            }
        }
        return "Autre"
    }

    private fun extractPublisherFromText(text: String): String {
        for (pub in KNOWN_PUBLISHERS) {
            if (text.contains(pub, ignoreCase = true)) {
                return pub
            }
        }
        // Try regex "édité par X"
        val match = PUBLISHER_EDIT_REGEX.find(text)
        return match?.groupValues?.getOrNull(1)?.trim() ?: "Inconnu"
    }

    private fun extractGenreFromText(text: String): String {
        for ((term, genre) in KNOWN_GENRES) {
            if (text.contains(term, ignoreCase = true)) {
                return genre
            }
        }
        return "Action"
    }

    private fun deduceFormat(platform: String): String {
        val p = platform.lowercase()
        return when {
            p.contains("playstation") || p.contains("ps1") || p.contains("ps2") || p.contains("ps3") ||
                    p.contains("ps4") || p.contains("ps5") || p.contains("gamecube") || p.contains("wii") ||
                    p.contains("xbox") || p.contains("dreamcast") || p.contains("saturn") || p.contains("mega-cd") ||
                    p.contains("pc engine cd") -> "Disque"
            p.contains("steam") || p.contains("demat") || p.contains("démat") || p.contains("eshop") -> "Dématérialisé"
            else -> "Cartouche"
        }
    }

    private fun findCuratedGame(query: String): ScrapedGameData? {
        val clean = query.lowercase().trim()
        val match = GameCatalog.sampleGames.firstOrNull {
            it.title.lowercase().contains(clean) || clean.contains(it.title.lowercase())
        } ?: return null

        return ScrapedGameData(
            title = match.title,
            platform = match.platform,
            publisher = match.publisher,
            releaseYear = match.releaseYear,
            coverUrl = match.coverUrl,
            genre = match.genre,
            format = match.format,
            estimatedValue = match.averageMarketValue,
            description = match.description,
            source = "Catalogue GameDex"
        )
    }

    private fun findCuratedCover(query: String): String {
        val clean = query.lowercase().trim()
        val match = GameCatalog.sampleGames.firstOrNull {
            it.title.lowercase().contains(clean) || clean.contains(it.title.lowercase())
        }
        return match?.coverUrl ?: ""
    }
}
