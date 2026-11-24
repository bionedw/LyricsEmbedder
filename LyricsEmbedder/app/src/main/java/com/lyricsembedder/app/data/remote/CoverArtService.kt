package com.lyricsembedder.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class CoverArtService(
    private val client: OkHttpClient,
    private val json: Json
) {
    /**
     * Returns Cover Art Archive front-500 URL if available, otherwise falls back to iTunes Search API.
     */
    suspend fun resolveArtworkUrl(
        releaseId: String,
        artist: String,
        album: String
    ): String? = withContext(Dispatchers.IO) {
        val caaUrl = "https://coverartarchive.org/release/$releaseId/front-500"
        val caaExists = runCatching {
            val req = Request.Builder()
                .url(caaUrl)
                .head()
                .header("User-Agent", MusicBrainzService.USER_AGENT)
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)

        if (caaExists) return@withContext caaUrl

        // Fallback to iTunes Store artwork search
        runCatching {
            val term = listOf(artist, album).filter { it.isNotBlank() }.joinToString(" ")
            if (term.isBlank()) return@runCatching null
            val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
                .addQueryParameter("term", term)
                .addQueryParameter("entity", "album")
                .addQueryParameter("limit", "1")
                .build()
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val body = resp.body?.string().orEmpty()
                val root = json.parseToJsonElement(body).jsonObject
                val first = root["results"]?.jsonArray?.firstOrNull()?.jsonObject
                first?.get("artworkUrl100")?.jsonPrimitive?.content
                    ?.replace("100x100bb", "600x600bb")
            }
        }.getOrNull()
    }

    suspend fun downloadArtworkBytes(artworkUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(artworkUrl)
                .header("User-Agent", MusicBrainzService.USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.bytes()
            }
        }.getOrNull()
    }
}
