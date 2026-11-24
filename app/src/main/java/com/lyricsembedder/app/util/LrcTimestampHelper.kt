package com.lyricsembedder.app.util

/**
 * Parses and shifts LRC synced timestamps such as [01:23.45] or [01:23.456].
 */
object LrcTimestampHelper {
    private val TIMESTAMP_REGEX = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})]")

    fun shiftAllTimestamps(lyrics: String, deltaMs: Long): String {
        if (lyrics.isBlank() || deltaMs == 0L) return lyrics
        return TIMESTAMP_REGEX.replace(lyrics) { match ->
            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val fracRaw = match.groupValues[3]
            val millis = if (fracRaw.length == 2) fracRaw.toLong() * 10L else fracRaw.toLong()
            val totalMs = (minutes * 60_000L + seconds * 1_000L + millis + deltaMs).coerceAtLeast(0L)
            formatTimestamp(totalMs, useThreeDigits = fracRaw.length == 3)
        }
    }

    fun formatTimestamp(totalMs: Long, useThreeDigits: Boolean = false): String {
        val clamped = totalMs.coerceAtLeast(0L)
        val minutes = clamped / 60_000L
        val seconds = (clamped % 60_000L) / 1_000L
        val millis = clamped % 1_000L
        return if (useThreeDigits) {
            "[%02d:%02d.%03d]".format(minutes, seconds, millis)
        } else {
            "[%02d:%02d.%02d]".format(minutes, seconds, millis / 10L)
        }
    }

    fun isSyncedLyrics(lyrics: String): Boolean = TIMESTAMP_REGEX.containsMatchIn(lyrics)
}
