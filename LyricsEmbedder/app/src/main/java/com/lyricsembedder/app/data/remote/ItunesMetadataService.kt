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
 * Free, no-API-key iTunes / Apple Music Search & Lookup service.
 * Ideal for Digital Media variants and high-resolution (1200x1200) official cover artwork.
 */
class ItunesMetadataService(
    private val client: OkHttpClient,
    private val json: Json
) {
    suspend fun searchAlbums(
        albumArtist: String,
        album: String
    ): Result<List<MbReleaseSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val term = listOf(albumArtist, album).filter { it.isNotBlank() }.joinToString(" ").trim()
            if (term.isBlank()) return@runCatching emptyList()

            val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
                .addQueryParameter("term", term)
                .addQueryParameter("entity", "album")
                .addQueryParameter("limit", "25")
                .build()

            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("iTunes search HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val results = json.parseToJsonElement(body).jsonObject["results"]?.jsonArray
                    ?: return@runCatching emptyList()

                results.mapNotNull { el ->
                    val obj = el.jsonObject
                    val collectionId = obj["collectionId"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val title = obj["collectionName"]?.jsonPrimitive?.content.orEmpty()
                    val artist = obj["artistName"]?.jsonPrimitive?.content.orEmpty()
                    val country = obj["country"]?.jsonPrimitive?.content
                    val releaseDate = obj["releaseDate"]?.jsonPrimitive?.content
                    val copyright = obj["copyright"]?.jsonPrimitive?.content.orEmpty()
                    val trackCount = obj["trackCount"]?.jsonPrimitive?.intOrNull ?: 0
                    val art100 = obj["artworkUrl100"]?.jsonPrimitive?.content
                    val artHiRes = art100?.replace("100x100bb", "1200x1200bb")

                    MbReleaseSummary(
                        id = "itunes:$collectionId",
                        title = title,
                        status = "Official",
                        country = country,
                        date = releaseDate,
                        disambiguation = "Apple Music Digital",
                        releaseGroup = MbReleaseGroup(primaryType = "Album"),
                        artistCredit = listOf(MbArtistCredit(name = artist)),
                        labelInfo = if (copyright.isNotBlank()) listOf(MbLabelInfo(MbLabel(name = copyright))) else emptyList(),
                        media = listOf(MbMedium(position = 1, format = "Digital Media", trackCount = trackCount)),
                        source = MetadataSource.ITUNES,
                        artworkPreviewUrl = artHiRes
                    )
                }
            }
        }
    }

    suspend fun getAlbumDetail(collectionIdRaw: String): Result<MbReleaseDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val collectionId = collectionIdRaw.removePrefix("itunes:")
            val url = "https://itunes.apple.com/lookup".toHttpUrl().newBuilder()
                .addQueryParameter("id", collectionId)
                .addQueryParameter("entity", "song")
                .addQueryParameter("limit", "200")
                .build()

            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("iTunes lookup HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val results = json.parseToJsonElement(body).jsonObject["results"]?.jsonArray
                    ?: throw IOException("Không tìm thấy chi tiết album trên iTunes")

                val albumObj = results.firstOrNull {
                    it.jsonObject["wrapperType"]?.jsonPrimitive?.content == "collection"
                }?.jsonObject ?: throw IOException("Dữ liệu album iTunes không hợp lệ")

                val title = albumObj["collectionName"]?.jsonPrimitive?.content.orEmpty()
                val artist = albumObj["artistName"]?.jsonPrimitive?.content.orEmpty()
                val country = albumObj["country"]?.jsonPrimitive?.content
                val releaseDate = albumObj["releaseDate"]?.jsonPrimitive?.content
                val copyright = albumObj["copyright"]?.jsonPrimitive?.content.orEmpty()
                val genre = albumObj["primaryGenreName"]?.jsonPrimitive?.content.orEmpty()
                val artHiRes = albumObj["artworkUrl100"]?.jsonPrimitive?.content
                    ?.replace("100x100bb", "1200x1200bb")

                val trackObjects = results.filter {
                    it.jsonObject["wrapperType"]?.jsonPrimitive?.content == "track"
                }.map { it.jsonObject }

                val byDisc = trackObjects.groupBy {
                    it["discNumber"]?.jsonPrimitive?.intOrNull ?: 1
                }.toSortedMap()

                val media = byDisc.map { (discNo, tracksInDisc) ->
                    val mbTracks = tracksInDisc
                        .sortedBy { it["trackNumber"]?.jsonPrimitive?.intOrNull ?: 0 }
                        .mapIndexed { idx, tObj ->
                            val trackNo = tObj["trackNumber"]?.jsonPrimitive?.intOrNull ?: (idx + 1)
                            MbTrack(
                                id = tObj["trackId"]?.jsonPrimitive?.content.orEmpty(),
                                number = trackNo.toString(),
                                position = trackNo,
                                title = tObj["trackName"]?.jsonPrimitive?.content.orEmpty(),
                                length = tObj["trackTimeMillis"]?.jsonPrimitive?.longOrNull,
                                artistCredit = listOf(
                                    MbArtistCredit(name = tObj["artistName"]?.jsonPrimitive?.content ?: artist)
                                )
                            )
                        }
                    MbMedium(
                        position = discNo,
                        format = "Digital Media",
                        trackCount = mbTracks.size,
                        tracks = mbTracks
                    )
                }

                MbReleaseDetail(
                    id = "itunes:$collectionId",
                    title = title,
                    status = "Official",
                    country = country,
                    date = releaseDate,
                    disambiguation = "Apple Music Digital",
                    releaseGroup = MbReleaseGroup(primaryType = "Album"),
                    artistCredit = listOf(MbArtistCredit(name = artist)),
                    labelInfo = if (copyright.isNotBlank()) listOf(MbLabelInfo(MbLabel(name = copyright))) else emptyList(),
                    media = media,
                    genres = if (genre.isNotBlank()) listOf(MbGenre(name = genre, count = 1)) else emptyList(),
                    directArtworkUrl = artHiRes
                )
            }
        }
    }
}
