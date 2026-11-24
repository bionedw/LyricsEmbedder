package com.lyricsembedder.app.data.repository

import android.net.Uri
import com.lyricsembedder.app.data.local.AudioTagManager
import com.lyricsembedder.app.data.model.AlbumMetadataDraft
import com.lyricsembedder.app.data.model.AppSettings
import com.lyricsembedder.app.data.model.AudioTrack
import com.lyricsembedder.app.data.model.MbReleaseDetail
import com.lyricsembedder.app.data.model.MbReleaseSummary
import com.lyricsembedder.app.data.model.TrackMappingItem
import com.lyricsembedder.app.data.remote.CoverArtService
import com.lyricsembedder.app.data.remote.MusicBrainzService
import com.lyricsembedder.app.util.TrackOrderHelper

class MetadataRepository(
    private val musicBrainzService: MusicBrainzService,
    private val coverArtService: CoverArtService,
    private val tagManager: AudioTagManager
) {
    suspend fun searchReleases(
        albumArtist: String,
        album: String,
        trackCountOnAlbum: Int?,
        discNumber: Int?,
        tracksOnDisc: Int?
    ): Result<List<MbReleaseSummary>> {
        return musicBrainzService.searchReleases(
            albumArtist = albumArtist,
            album = album,
            trackCountOnAlbum = trackCountOnAlbum,
            discNumber = discNumber,
            tracksOnDisc = tracksOnDisc
        )
    }

    suspend fun loadReleaseAndMapping(
        releaseId: String,
        discNumber: Int,
        localTracks: List<AudioTrack>
    ): Result<Pair<AlbumMetadataDraft, List<TrackMappingItem>>> {
        val detailResult = musicBrainzService.getReleaseDetail(releaseId)
        val detail: MbReleaseDetail = detailResult.getOrElse { return Result.failure(it) }

        val artworkUrl = coverArtService.resolveArtworkUrl(
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
            artworkUrl = artworkUrl,
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
     * into the destination folder when configured.
     */
    suspend fun applyAlbumMetadata(
        folderUri: Uri,
        draft: AlbumMetadataDraft,
        mappings: List<TrackMappingItem>,
        settings: AppSettings,
        onProgress: (completed: Int, total: Int, currentName: String) -> Unit
    ): Result<Int> {
        val artworkBytes = draft.artworkUrl?.let { coverArtService.downloadArtworkBytes(it) }
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

        // Save 1 artwork image file into the destination folder
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
}
