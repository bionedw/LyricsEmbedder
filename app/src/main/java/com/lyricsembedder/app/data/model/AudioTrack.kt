package com.lyricsembedder.app.data.model

import android.net.Uri

/**
 * Supported taggable audio file extensions (excludes formats that cannot store embedded tags).
 */
object SupportedAudioFormats {
    val EXTENSIONS = setOf(
        "mp3", "flac", "m4a", "mp4", "ogg", "opus", "wav", "wma", "aiff", "aif", "dsf"
    )

    fun isSupported(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in EXTENSIONS
    }
}

data class AudioTrack(
    val uri: Uri,
    val folderUri: Uri,
    val folderName: String,
    val fileName: String,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val composer: String = "",
    val publisher: String = "",
    val genre: String = "",
    val year: String = "",
    val trackNumber: Int? = null,
    val trackTotal: Int? = null,
    val discNumber: Int? = null,
    val discTotal: Int? = null,
    val durationMs: Long = 0L,
    val embeddedLyrics: String = "",
    val externalLyricsPath: String? = null,
    val hasArtwork: Boolean = false,
    val isInstrumental: Boolean = false
) {
    /**
     * A track is considered to have valid metadata if it has at least a title and an artist.
     * Files without metadata cannot be queried for lyrics.
     */
    val hasMetadata: Boolean
        get() = title.isNotBlank() && artist.isNotBlank()

    val hasLyrics: Boolean
        get() = embeddedLyrics.isNotBlank() || !externalLyricsPath.isNullOrBlank()

    val displayTitle: String
        get() = title.ifBlank { fileName.substringBeforeLast('.') }

    val formattedDuration: String
        get() {
            if (durationMs <= 0L) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

data class MusicFolder(
    val uri: Uri,
    val displayName: String,
    val displayPath: String,
    val source: FolderSource,
    val trackCount: Int = 0
)

enum class FolderSource {
    POWERAMP,
    CUSTOM
}
