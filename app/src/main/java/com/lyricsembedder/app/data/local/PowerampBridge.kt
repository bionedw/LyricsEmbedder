package com.lyricsembedder.app.data.local

import android.content.Context
import android.content.Intent
import com.lyricsembedder.app.data.model.AudioTrack

/**
 * Integrates with Poweramp's public Lyrics Plugin API to push lyrics directly into Poweramp's
 * lyrics cache and mark instrumental tracks.
 */
class PowerampBridge(private val context: Context) {

    fun sendLyricsToPoweramp(
        track: AudioTrack,
        lyrics: String,
        requestId: Long = System.currentTimeMillis()
    ): Boolean {
        return runCatching {
            val intent = Intent(ACTION_LYRICS_RESPONSE).apply {
                setPackage(POWERAMP_PACKAGE)
                putExtra(EXTRA_ID, requestId)
                putExtra(EXTRA_LYRICS, lyrics)
                putExtra(EXTRA_TITLE, track.title)
                putExtra(EXTRA_ARTIST, track.artist)
                putExtra(EXTRA_ALBUM, track.album)
                putExtra(EXTRA_INFO_LINE, "LyricsEmbedder")
            }
            context.startService(intent)
            true
        }.getOrElse {
            runCatching {
                val broadcast = Intent(ACTION_LYRICS_RESPONSE).apply {
                    setPackage(POWERAMP_PACKAGE)
                    putExtra(EXTRA_ID, requestId)
                    putExtra(EXTRA_LYRICS, lyrics)
                    putExtra(EXTRA_INFO_LINE, "LyricsEmbedder")
                }
                context.sendBroadcast(broadcast)
                true
            }.getOrDefault(false)
        }
    }

    companion object {
        const val POWERAMP_PACKAGE = "com.maxmpz.audioplayer"
        const val ACTION_NEED_LYRICS = "com.maxmpz.audioplayer.ACTION_NEED_LYRICS"
        const val ACTION_LYRICS_RESPONSE = "com.maxmpz.audioplayer.ACTION_LYRICS_RESPONSE"
        const val EXTRA_ID = "id"
        const val EXTRA_LYRICS = "lyrics"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_ALBUM = "album"
        const val EXTRA_INFO_LINE = "info_line"
    }
}
