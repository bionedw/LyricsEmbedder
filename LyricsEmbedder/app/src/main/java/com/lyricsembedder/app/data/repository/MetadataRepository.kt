package com.lyricsembedder.app.data.repository

import android.content.Context
import android.net.Uri
import com.lyricsembedder.app.data.local.AudioTagManager
import com.lyricsembedder.app.data.model.AlbumMetadataDraft
import com.lyricsembedder.app.data.model.AppSettings
import com.lyricsembedder.app.data.model.ArtworkSourceMode
import com.lyricsembedder.app.data.model.AudioTrack
import com.lyricsembedder.app.data.model.MbReleaseDetail
import com.lyricsembedder.app.data.model.MbReleaseSummary
import com.lyricsembedder.app.data.model.MetadataSource
import com.lyricsembedder.app.data.model.TrackMappingItem
import com.lyricsembedder.app.data.remote.CoverArtService
import com.lyricsembedder.app.data.remote.DeezerMetadataService
import com.lyricsembedder.app.data.remote.ItunesMetadataService
import com.lyricsembedder.app.data.remote.MusicBrainzService
import com.lyricsembedder.app.util.TrackOrderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MetadataRepository(
    private val context: Context,
    private val musicBrainzService: MusicBrainzService,
    private val itunesMetadataService: ItunesMetadataService,
    private val deezerMetadataService: DeezerMetadataService,
    private val coverArtService: CoverArtService,
    private val tagManager: AudioTagManager
) {
    suspend fun searchReleases(
        source: MetadataSource,
        albumArtist: String,
        album: String,
        trackCountOnAlbum: Int?,
        discNumber: Int?,
        tracksOnDisc: Int?
    ): Result<List<MbReleaseSummary>> {
        return when (source) {
            MetadataSource.MUSICBRAINZ -> musicBrainzService.searchReleases(
                albumArtist, album, trackCountOnAlbum, discNumber, tracksOnDisc
            )
            MetadataSource.ITUNES -> itunesMetadataService.searchAlbums(albumArtist, album)
            MetadataSource.DEEZER -> deezerMetadataService.searchAlbums(albumArtist, album)
            MetadataSource.ALL -> runCatching {
                val combined = mutableListOf<MbReleaseSummary>()
                itunesMetadataService.searchAlbums(albumArtist, album).getOrNull()?.let { combined += it }
                deezerMetadataService.searchAlbums(albumArtist, album).getOrNull()?.let { combined += it }
                musicBrainzService.searchReleases(albumArtist, album, trackCountOnAlbum, discNumber, tracksOnDisc)
                    .getOrNull()?.let { combined += it }
                combined
            }
        }
    }

    suspend fun getAllVariantsByReleaseGroup(releaseGroupId: String): Result<List<MbReleaseSummary>> {
        return musicBrainzService.getAllVariantsByReleaseGroup(releaseGroupId)
    }

    suspend fun resolveArtworkForRelease(releaseId: String, artist: String, album: String): String? {
        return coverArtService.resolveArtworkUrl(releaseId, artist, album)
    }

    suspend fun loadReleaseAndMapping(
        releaseId: String,
        discNumber: Int,
        localTracks: List<AudioTrack>
    ): Result<Pair<AlbumMetadataDraft, List<TrackMappingItem>>> {
        val detailResult = when {
            releaseId.startsWith("itunes:") -> itunesMetadataService.getAlbumDetail(releaseId)
            releaseId.startsWith("deezer:") -> deezerMetadataService.getAlbumDetail(releaseId)
            else -> musicBrainzService.getReleaseDetail(releaseId)
        }
        val detail: MbReleaseDetail = detailResult.getOrElse { return Result.failure(it) }

        val artworkUrl = detail.directArtworkUrl ?: coverArtService.resolveArtworkUrl(
            releaseId = detail.id,
            artist = detail.primaryArtist,
            album = detail.title
        )

        val selectedMedium = detail.media.firstOrNull { it.position == discNumber }
            ?: detail.media.firstOrNull()
        val fetchedTracks = selectedMedium?.tracks.orEmpty()
        val orderedLocal = TrackOrderHelper.sortTracksForAlbum(localTracks)

        val draft = AlbumMetadataDraft(
            releaseId = detail.id,
            releaseGroupId = detail.releaseGroup?.id.orEmpty(),
            artworkUrl = artworkUrl,
            artworkSourceMode = if (artworkUrl != null) ArtworkSourceMode.REMOTE_SOURCE else ArtworkSourceMode.NONE,
            albumArtist = detail.primaryArtist,
            album = detail.title,
            publisher = detail.primaryLabel,
            discNumber = selectedMedium?.position ?: 1,
            discTotal = detail.media.size.coerceAtLeast(1),
            year = detail.year,
            genre = detail.primaryGenre
        )

        val mappings = orderedLocal.mapIndexed { index, localTrack ->
            val fetched = fetchedTracks.getOrNull(index)
            TrackMappingItem(
                included = fetched != null,
                trackNumber = fetched?.position ?: (index + 1),
                fetchedArtist = fetched?.artistDisplay?.ifBlank { detail.primaryArtist } ?: detail.primaryArtist,
                fetchedTitle = fetched?.title ?: localTrack.displayTitle,
                fetchedDurationMs = fetched?.length,
                localTrack = localTrack
            )
        }

        return Result.success(draft to mappings)
    }

    /**
     * Applies album & per-track metadata to all included tracks AND saves 1 artwork image file
     * into the destination folder when configured. Supports both remote source artwork and
     * user-picked local artwork images (MusicBee style).
     */
    suspend fun applyAlbumMetadata(
        folderUri: Uri,
        draft: AlbumMetadataDraft,
        mappings: List<TrackMappingItem>,
        settings: AppSettings,
        onProgress: (completed: Int, total: Int, currentName: String) -> Unit
    ): Result<Int> {
        val artworkBytes: ByteArray? = when (draft.artworkSourceMode) {
            ArtworkSourceMode.REMOTE_SOURCE -> draft.artworkUrl?.let { coverArtService.downloadArtworkBytes(it) }
            ArtworkSourceMode.LOCAL_IMAGE -> draft.localArtworkUriString?.let { readLocalArtworkBytes(it) }
            ArtworkSourceMode.NONE -> null
        }

        val activeMappings = mappings.filter { it.included }
        val total = activeMappings.size
        var successCount = 0

        for ((index, item) in activeMappings.withIndex()) {
            onProgress(index, total, item.localTrack.fileName)
            val updatedTrack = item.localTrack.copy(
                title = item.fetchedTitle,
                artist = item.fetchedArtist,
                album = if (draft.applyAlbum) draft.album else item.localTrack.album,
                albumArtist = if (draft.applyAlbumArtist) draft.albumArtist else item.localTrack.albumArtist,
                composer = if (draft.applyComposer) draft.composer else item.localTrack.composer,
                publisher = if (draft.applyPublisher) draft.publisher else item.localTrack.publisher,
                discNumber = if (draft.applyDiscNumber) draft.discNumber else item.localTrack.discNumber,
                discTotal = if (draft.applyDiscNumber) draft.discTotal else item.localTrack.discTotal,
                year = if (draft.applyYear) draft.year else item.localTrack.year,
                genre = if (draft.applyGenre) draft.genre else item.localTrack.genre,
                trackNumber = item.trackNumber,
                trackTotal = total
            )
            val writeResult = tagManager.writeTrackMetadata(updatedTrack, settings, artworkBytes)
            if (writeResult.isSuccess) {
                successCount++
            }
        }

        if (artworkBytes != null && artworkBytes.isNotEmpty() && settings.saveArtworkCopyToFolder) {
            tagManager.saveArtworkToFolder(
                folderUri = folderUri,
                artworkBytes = artworkBytes,
                fileName = settings.artworkFileName
            )
        }

        onProgress(total, total, "Hoàn tất")
        return Result.success(successCount)
    }

    suspend fun updateSingleTrack(
        track: AudioTrack,
        settings: AppSettings
    ): Result<Unit> {
        return tagManager.writeTrackMetadata(track, settings, artworkBytes = null)
    }

    private suspend fun readLocalArtworkBytes(uriString: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
    }
}
