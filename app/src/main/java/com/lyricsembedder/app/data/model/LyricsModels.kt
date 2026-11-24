package com.lyricsembedder.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LrcLibResponse(
    val id: Long = 0,
    val trackName: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

enum class LyricsFilterTab {
    NO_LYRICS,
    HAS_LYRICS,
    NO_METADATA
}

enum class LyricsDownloadStatus {
    QUEUED,
    FETCHING,
    SAVED,
    INSTRUMENTAL,
    NOT_FOUND,
    SKIPPED_NO_METADATA,
    ERROR
}

data class LyricsQueueItem(
    val track: AudioTrack,
    val status: LyricsDownloadStatus = LyricsDownloadStatus.QUEUED,
    val message: String = ""
)
