package com.lyricsembedder.app.data.remote

import com.lyricsembedder.app.data.model.MbReleaseDetail
import com.lyricsembedder.app.data.model.MbReleaseSearchResponse
import com.lyricsembedder.app.data.model.MbReleaseSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MusicBrainzService(
    private val client: OkHttpClient,
    private val json: Json
) {
    suspend fun searchReleases(
        albumArtist: String,
        album: String,
        trackCountOnAlbum: Int? = null,
        discNumber: Int? = null,
        tracksOnDisc: Int? = null
    ): Result<List<MbReleaseSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val queryParts = buildList {
                if (albumArtist.isNotBlank()) add("artist:\"${escapeLucene(albumArtist)}\"")
                if (album.isNotBlank()) add("release:\"${escapeLucene(album)}\"")
                if (tracksOnDisc != null && tracksOnDisc > 0) add("tracksmedium:$tracksOnDisc")
                if (trackCountOnAlbum != null && trackCountOnAlbum > 0) add("tracks:$trackCountOnAlbum")
                if (discNumber != null && discNumber > 0) add("mediums:$discNumber")
            }
            val luceneQuery = queryParts.joinToString(" AND ").ifBlank { "*" }

            val url = "$BASE_URL/release".toHttpUrl().newBuilder()
                .addQueryParameter("query", luceneQuery)
                .addQueryParameter("fmt", "json")
                .addQueryParameter("limit", "25")
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("MusicBrainz search failed: HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                val parsed = json.decodeFromString<MbReleaseSearchResponse>(body)
                parsed.releases
            }
        }
    }

    suspend fun getReleaseDetail(releaseId: String): Result<MbReleaseDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "$BASE_URL/release/$releaseId".toHttpUrl().newBuilder()
                .addQueryParameter("inc", "recordings+artist-credits+labels+release-groups+genres+tags")
                .addQueryParameter("fmt", "json")
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("MusicBrainz detail failed: HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                json.decodeFromString<MbReleaseDetail>(body)
            }
        }
    }

    private fun escapeLucene(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").trim()
    }

    companion object {
        private const val BASE_URL = "https://musicbrainz.org/ws/2"
        const val USER_AGENT = "LyricsEmbedder/1.0.0 ( https://musicbrainz.org )"
    }
}
