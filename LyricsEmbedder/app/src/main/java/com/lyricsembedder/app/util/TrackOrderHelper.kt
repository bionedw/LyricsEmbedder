package com.lyricsembedder.app.util

import com.lyricsembedder.app.data.model.AudioTrack

/**
 * Extracts album track order from metadata or untagged filenames such as:
 * "Track 1.flac", "01 Track 1.flac", "Track02.mp3", "03 - Song.flac", "12.wav".
 */
object TrackOrderHelper {
    private val LEADING_NUMBER_REGEX = Regex("^(\\d{1,3})\\s*[-._ )]")
    private val TRACK_WORD_REGEX = Regex("(?i)\\btrack\\s*[-_#]?\\s*(\\d{1,3})\\b")
    private val ANY_NUMBER_REGEX = Regex("(\\d{1,3})")

    fun extractOrderNumber(fileName: String, embeddedTrackNumber: Int? = null): Int {
        if (embeddedTrackNumber != null && embeddedTrackNumber > 0) {
            return embeddedTrackNumber
        }
        val baseName = fileName.substringBeforeLast('.')
        LEADING_NUMBER_REGEX.find(baseName)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        TRACK_WORD_REGEX.find(baseName)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        ANY_NUMBER_REGEX.find(baseName)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        return Int.MAX_VALUE
    }

    fun sortTracksForAlbum(tracks: List<AudioTrack>): List<AudioTrack> {
        return tracks.sortedWith(
            compareBy<AudioTrack>(
                { it.discNumber ?: 1 },
                { extractOrderNumber(it.fileName, it.trackNumber) },
                { it.fileName.lowercase() }
            )
        )
    }
}
