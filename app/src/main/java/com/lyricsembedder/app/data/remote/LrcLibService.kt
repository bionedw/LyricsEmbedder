package com.lyricsembedder.app.data.remote

import com.lyricsembedder.app.data.model.LrcLibResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class LrcLibService(
    private val client: OkHttpClient,
    private val json: Json
) {
    /**
     * Exact lyrics lookup via GET {apiBase}/get
     */
    suspend fun getExactLyrics(
        apiBaseUrl: String,
        trackName: String,
        artistName: String,
        albumName: String,
        durationSeconds: Int?
    ): Result<LrcLibResponse?> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedBase = apiBaseUrl.trimEnd('/')
            val builder = "$normalizedBase/get".toHttpUrlOrNull()?.newBuilder()
                ?: throw IllegalArgumentException("URL API LrcLib không hợp lệ: $apiBaseUrl")

            builder.addQueryParameter("track_name", trackName)
            builder.addQueryParameter("artist_name", artistName)
            if (albumName.isNotBlank()) {
                builder.addQueryParameter("album_name", albumName)
            }
            if (durationSeconds != null && durationSeconds > 0) {
                builder.addQueryParameter("duration", durationSeconds.toString())
            }

            val request = Request.Builder()
                .url(builder.build())
                .header("User-Agent", MusicBrainzService.USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                when (response.code) {
                    404 -> null
                    in 200..299 -> {
                        val body = response.body?.string().orEmpty()
                        json.decodeFromString<LrcLibResponse>(body)
                    }
                    else -> throw IOException("LrcLib HTTP ${response.code}")
                }
            }
        }
    }

    /**
     * Extended fuzzy search via GET {apiBase}/search when exact lookup returns no match.
     */
    suspend fun searchExtendedLyrics(
        apiBaseUrl: String,
        trackName: String,
        artistName: String,
        albumName: String
    ): Result<List<LrcLibResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedBase = apiBaseUrl.trimEnd('/')
            val builder = "$normalizedBase/search".toHttpUrlOrNull()?.newBuilder()
                ?: throw IllegalArgumentException("URL API LrcLib không hợp lệ: $apiBaseUrl")

            builder.addQueryParameter("track_name", trackName)
            if (artistName.isNotBlank()) {
                builder.addQueryParameter("artist_name", artistName)
            }
            if (albumName.isNotBlank()) {
                builder.addQueryParameter("album_name", albumName)
            }

            val request = Request.Builder()
                .url(builder.build())
                .header("User-Agent", MusicBrainzService.USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("LrcLib search HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                json.decodeFromString<List<LrcLibResponse>>(body)
            }
        }
    }
}
