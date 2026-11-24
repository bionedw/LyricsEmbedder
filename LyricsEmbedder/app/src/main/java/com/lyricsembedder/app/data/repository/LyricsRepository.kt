package com.lyricsembedder.app.data.repository

import com.lyricsembedder.app.data.local.AudioTagManager
import com.lyricsembedder.app.data.local.FilterProcessor
import com.lyricsembedder.app.data.local.PowerampBridge
import com.lyricsembedder.app.data.model.AppSettings
import com.lyricsembedder.app.data.model.AudioTrack
import com.lyricsembedder.app.data.model.LrcLibResponse
import com.lyricsembedder.app.data.model.LyricsDownloadStatus
import com.lyricsembedder.app.data.model.PreferredLyricsType
import com.lyricsembedder.app.data.remote.LrcLibService
import com.lyricsembedder.app.util.LrcTimestampHelper
import com.lyricsembedder.app.util.NotificationHelper

data class LyricsFetchOutcome(
    val status: LyricsDownloadStatus,
    val lyrics: String = "",
    val message: String = ""
)

class LyricsRepository(
    private val lrcLibService: LrcLibService,
    private val tagManager: AudioTagManager,
    private val powerampBridge: PowerampBridge,
    private val notificationHelper: NotificationHelper
) {
    /**
     * Fetches and persists lyrics for a single track according to user settings.
     * Files without metadata are strictly rejected from lyrics fetching.
     */
    suspend fun fetchAndSaveLyrics(
        track: AudioTrack,
        settings: AppSettings
    ): LyricsFetchOutcome {
        if (!track.hasMetadata) {
            return LyricsFetchOutcome(
                status = LyricsDownloadStatus.SKIPPED_NO_METADATA,
                message = "Tệp chưa có metadata — không thể tải lời bài hát"
            )
        }

        val cleanTitle = FilterProcessor.applyFilters(track.title, settings.titleFilters)
        val cleanArtist = FilterProcessor.applyFilters(track.artist, settings.artistFilters)
        val cleanAlbum = FilterProcessor.applyFilters(track.album, settings.albumFilters)
        val durationSec = (track.durationMs / 1000L).toInt().takeIf { it > 0 }

        var response: LrcLibResponse? = lrcLibService.getExactLyrics(
            apiBaseUrl = settings.activeLrcLibApiUrl,
            trackName = cleanTitle,
            artistName = cleanArtist,
            albumName = cleanAlbum,
            durationSeconds = durationSec
        ).getOrElse { err ->
            if (settings.notifyOnFailure) {
                notificationHelper.notifyLyricsFailure(
                    trackTitle = track.displayTitle,
                    reason = err.localizedMessage ?: "Lỗi kết nối",
                    replacePrevious = settings.replacePreviousNotification
                )
            }
            return LyricsFetchOutcome(
                status = LyricsDownloadStatus.ERROR,
                message = err.localizedMessage ?: "Lỗi kết nối API"
            )
        }

        // Optional extended fuzzy search
        if (response == null && settings.useExtendedSearch) {
            val candidates = lrcLibService.searchExtendedLyrics(
                apiBaseUrl = settings.activeLrcLibApiUrl,
                trackName = cleanTitle,
                artistName = cleanArtist,
                albumName = cleanAlbum
            ).getOrDefault(emptyList())
            response = selectBestCandidate(candidates, settings.preferredLyricsType)
        }

        if (response == null) {
            if (settings.notifyOnFailure) {
                notificationHelper.notifyLyricsFailure(
                    trackTitle = track.displayTitle,
                    reason = "Không tìm thấy lời trên LrcLib",
                    replacePrevious = settings.replacePreviousNotification
                )
            }
            return LyricsFetchOutcome(
                status = LyricsDownloadStatus.NOT_FOUND,
                message = "Không tìm thấy lời bài hát"
            )
        }

        if (response.instrumental) {
            if (settings.markInstrumentalTracks) {
                persistLyrics(
                    track = track,
                    lyrics = AudioTagManager.INSTRUMENTAL_MARKER,
                    settings = settings,
                    matchedResponse = response
                )
            }
            return LyricsFetchOutcome(
                status = LyricsDownloadStatus.INSTRUMENTAL,
                lyrics = AudioTagManager.INSTRUMENTAL_MARKER,
                message = "Nhạc không lời"
            )
        }

        val chosenLyrics = when (settings.preferredLyricsType) {
            PreferredLyricsType.SYNCED -> response.syncedLyrics?.takeIf { it.isNotBlank() }
                ?: response.plainLyrics?.takeIf { it.isNotBlank() }
            PreferredLyricsType.TEXT_ONLY -> response.plainLyrics?.takeIf { it.isNotBlank() }
                ?: response.syncedLyrics?.takeIf { it.isNotBlank() }
        }

        if (chosenLyrics.isNullOrBlank()) {
            return LyricsFetchOutcome(
                status = LyricsDownloadStatus.NOT_FOUND,
                message = "API trả về lời trống"
            )
        }

        val saveResult = persistLyrics(
            track = track,
            lyrics = chosenLyrics,
            settings = settings,
            matchedResponse = response
        )

        return saveResult.fold(
            onSuccess = {
                LyricsFetchOutcome(
                    status = LyricsDownloadStatus.SAVED,
                    lyrics = chosenLyrics,
                    message = if (LrcTimestampHelper.isSyncedLyrics(chosenLyrics)) "Đã lưu lời đồng bộ" else "Đã lưu lời thường"
                )
            },
            onFailure = { err ->
                LyricsFetchOutcome(
                    status = LyricsDownloadStatus.ERROR,
                    message = err.localizedMessage ?: "Không thể lưu lời bài hát"
                )
            }
        )
    }

    suspend fun persistLyrics(
        track: AudioTrack,
        lyrics: String,
        settings: AppSettings,
        matchedResponse: LrcLibResponse? = null
    ): Result<Unit> = runCatching {
        if (settings.embedLyricsInFile) {
            tagManager.embedLyrics(
                track = track,
                lyrics = lyrics,
                settings = settings,
                updatedTitle = matchedResponse?.trackName,
                updatedArtist = matchedResponse?.artistName,
                updatedAlbum = matchedResponse?.albumName
            ).getOrThrow()
        }
        if (settings.saveLyricsAsFile) {
            tagManager.saveLyricsFileToFolder(
                folderUri = track.folderUri,
                audioFileName = track.fileName,
                lyrics = lyrics,
                isSynced = LrcTimestampHelper.isSyncedLyrics(lyrics)
            ).getOrThrow()
        }
        if (settings.sendLyricsToPoweramp) {
            powerampBridge.sendLyricsToPoweramp(track, lyrics)
        }
    }

    private fun selectBestCandidate(
        candidates: List<LrcLibResponse>,
        preferred: PreferredLyricsType
    ): LrcLibResponse? {
        if (candidates.isEmpty()) return null
        return when (preferred) {
            PreferredLyricsType.SYNCED ->
                candidates.firstOrNull { !it.syncedLyrics.isNullOrBlank() } ?: candidates.first()
            PreferredLyricsType.TEXT_ONLY ->
                candidates.firstOrNull { !it.plainLyrics.isNullOrBlank() } ?: candidates.first()
        }
    }
}
