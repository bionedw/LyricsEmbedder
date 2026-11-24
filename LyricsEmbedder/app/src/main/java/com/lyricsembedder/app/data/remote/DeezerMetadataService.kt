package com.lyricsembedder.app.data.remote

import com.lyricsembedder.app.data.model.MbArtistCredit
import com.lyricsembedder.app.data.model.MbGenre
import com.lyricsembedder.app.data.model.MbLabel
import com.lyricsembedder.app.data.model.MbLabelInfo
import com.lyricsembedder.app.data.model.MbMedium
import com.lyricsembedder.app.data.model.MbReleaseDetail
import com.lyricsembedder.app.data.model.MbReleaseGroup
import com.lyricsembedder.app.data.model.MbReleaseSummary
import com.lyricsembedder.app.data.model.MbTrack
import com.lyricsembedder.app.data.model.MetadataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Free, no-API-key Deezer Public API metadata service.
 * Provides Digital Media releases with clean 1000x1000 (cover_xl) artwork, record labels, and full tracklists.
 */
class DeezerMetadataService(
    private val client: OkHttpClient,
    private val json: Json
) {
    suspend fun searchAlbums(
        albumArtist: String,
        album: String
    ): Result<List<MbReleaseSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val q = buildList {
                if (albumArtist.isNotBlank()) add("artist:\"$albumArtist\"")
                if (album.isNotBlank()) add("album:\"$album\"")
            }.joinToString(" ").ifBlank {
                listOf(albumArtist, album).filter { it.isNotBlank() }.joinToString(" ")
            }
            if (q.isBlank()) return@runCatching emptyList()

            val url = "https://api.deezer.com/search/album".toHttpUrl().newBuilder()
                .addQueryParameter("q", q)
                .addQueryParameter("limit", "25")
                .build()

            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("Deezer search HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val data = json.parseToJsonElement(body).jsonObject["data"]?.jsonArray
                    ?: return@runCatching emptyList()

                data.mapNotNull { el ->
                    val obj = el.jsonObject
                    val albumId = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val title = obj["title"]?.jsonPrimitive?.content.orEmpty()
                    val artist = obj["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.content.orEmpty()
                    val nbTracks = obj["nb_tracks"]?.jsonPrimitive?.intOrNull ?: 0
                    val coverXl = obj["cover_xl"]?.jsonPrimitive?.content
                        ?: obj["cover_big"]?.jsonPrimitive?.content

                    MbReleaseSummary(
                        id = "deezer:$albumId",
                        title = title,
                        status = "Official",
                        country = "XW",
                        date = null,
                        disambiguation = "Deezer Digital",
                        releaseGroup = MbReleaseGroup(primaryType = "Album"),
                        artistCredit = listOf(MbArtistCredit(name = artist)),
                        media = listOf(MbMedium(position = 1, format = "Digital Media", trackCount = nbTracks)),
                        source = MetadataSource.DEEZER,
                        artworkPreviewUrl = coverXl
                    )
                }
            }
        }
    }

    suspend fun getAlbumDetail(albumIdRaw: String): Result<MbReleaseDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val albumId = albumIdRaw.removePrefix("deezer:")
            val url = "https://api.deezer.com/album/$albumId"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("Deezer detail HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val obj = json.parseToJsonElement(body).jsonObject

                val title = obj["title"]?.jsonPrimitive?.content.orEmpty()
                val artist = obj["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.content.orEmpty()
                val label = obj["label"]?.jsonPrimitive?.content.orEmpty()
                val releaseDate = obj["release_date"]?.jsonPrimitive?.content
                val coverXl = obj["cover_xl"]?.jsonPrimitive?.content
                    ?: obj["cover_big"]?.jsonPrimitive?.content
                val genres = obj["genres"]?.jsonObject?.get("data")?.jsonArray
                    ?.mapNotNull { g ->
                        g.jsonObject["name"]?.jsonPrimitive?.content?.let { MbGenre(name = it, count = 1) }
                    }.orEmpty()

                val trackArray = obj["tracks"]?.jsonObject?.get("data")?.jsonArray
                val tracks = trackArray?.mapIndexed { idx, el ->
                    val tObj = el.jsonObject
                    val tPos = tObj["track_position"]?.jsonPrimitive?.intOrNull ?: (idx + 1)
                    val durationSec = tObj["duration"]?.jsonPrimitive?.longOrNull
                    val tArtist = tObj["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: artist
                    MbTrack(
                        id = tObj["id"]?.jsonPrimitive?.content.orEmpty(),
                        number = tPos.toString(),
                        position = tPos,
                        title = tObj["title"]?.jsonPrimitive?.content.orEmpty(),
                        length = durationSec?.times(1000L),
                        artistCredit = listOf(MbArtistCredit(name = tArtist))
                    )
                }.orEmpty()

                MbReleaseDetail(
                    id = "deezer:$albumId",
                    title = title,
                    status = "Official",
                    country = "XW",
                    date = releaseDate,
                    disambiguation = "Deezer Digital",
                    releaseGroup = MbReleaseGroup(primaryType = "Album"),
                    artistCredit = listOf(MbArtistCredit(name = artist)),
                    labelInfo = if (label.isNotBlank()) listOf(MbLabelInfo(MbLabel(name = label))) else emptyList(),
                    media = listOf(MbMedium(position = 1, format = "Digital Media", trackCount = tracks.size, tracks = tracks)),
                    genres = genres,
                    directArtworkUrl = coverXl
                )
            }
        }
    }
}
