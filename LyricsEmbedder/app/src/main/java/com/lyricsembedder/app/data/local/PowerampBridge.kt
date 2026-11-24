package com.lyricsembedder.app.data.local

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.lyricsembedder.app.data.model.AudioTrack

/**
 * Implements Poweramp's official Lyrics Plugin API protocol:
 * - Receives track requests via `com.maxmpz.audioplayer.ACTION_NEED_LYRICS` (handled by PowerampLyricsReceiver)
 * - Caches Poweramp's internal track `realId` (`EXTRA_ID`) keyed by normalized track identity
 * - Pushes lyrics back via `com.maxmpz.audioplayer.ACTION_UPDATE_LYRICS` with the matching `EXTRA_ID`
 */
class PowerampBridge(private val context: Context) {

    private val idPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun rememberPowerampTrackId(
        realId: Long,
        title: String,
        artist: String,
        path: String? = null
    ) {
        if (realId <= 0L) return
        val editor = idPrefs.edit()
        val key = buildTrackKey(title, artist)
        if (key.isNotBlank()) {
            editor.putLong(key, realId)
        }
        val fileName = path?.substringAfterLast('/')?.lowercase().orEmpty()
        if (fileName.isNotBlank()) {
            editor.putLong("file:$fileName", realId)
        }
        editor.apply()
    }

    fun resolveCachedPowerampId(track: AudioTrack): Long? {
        val byMeta = idPrefs.getLong(buildTrackKey(track.title, track.artist), -1L)
        if (byMeta > 0L) return byMeta
        val byFile = idPrefs.getLong("file:${track.fileName.lowercase()}", -1L)
        if (byFile > 0L) return byFile
        return null
    }

    /**
     * Sends lyrics directly to Poweramp's lyrics cache using the exact `realId` from Poweramp.
     * Returns true if a valid `realId` was available and the broadcast was dispatched.
     */
    fun sendLyricsToPoweramp(
        realId: Long,
        lyrics: String?,
        infoLine: String = "LyricsEmbedder",
        targetPackage: String = POWERAMP_PACKAGE
    ): Boolean {
        if (realId <= 0L) return false
        val pkg = targetPackage.ifBlank { POWERAMP_PACKAGE }

        val explicitIntent = Intent(ACTION_UPDATE_LYRICS).apply {
            component = ComponentName(pkg, POWERAMP_API_RECEIVER)
            putExtra(EXTRA_ID, realId)
            putExtra(EXTRA_LYRICS, lyrics.orEmpty())
            putExtra(EXTRA_INFO_LINE, infoLine)
            putExtra(EXTRA_PACKAGE, context.packageName)
        }

        val sentExplicit = runCatching {
            context.sendBroadcast(explicitIntent)
            true
        }.getOrDefault(false)

        if (sentExplicit) return true

        // Fallback package-scoped broadcast for alternative Poweramp builds
        return runCatching {
            val fallbackIntent = Intent(ACTION_UPDATE_LYRICS).apply {
                setPackage(pkg)
                putExtra(EXTRA_ID, realId)
                putExtra(EXTRA_LYRICS, lyrics.orEmpty())
                putExtra(EXTRA_INFO_LINE, infoLine)
                putExtra(EXTRA_PACKAGE, context.packageName)
            }
            context.sendBroadcast(fallbackIntent)
            true
        }.getOrDefault(false)
    }

    /**
     * Convenience overload used when saving lyrics from inside LyricsEmbedder UI.
     * Looks up the cached Poweramp `realId` for this track (populated when Poweramp requests lyrics).
     */
    fun sendLyricsToPoweramp(
        track: AudioTrack,
        lyrics: String,
        explicitRealId: Long? = null
    ): Boolean {
        val id = explicitRealId ?: resolveCachedPowerampId(track) ?: return false
        return sendLyricsToPoweramp(
            realId = id,
            lyrics = lyrics,
            infoLine = "LyricsEmbedder"
        )
    }

    private fun buildTrackKey(title: String, artist: String): String {
        val t = title.trim().lowercase()
        val a = artist.trim().lowercase()
        return if (t.isBlank() && a.isBlank()) "" else "meta:$a|$t"
    }

    companion object {
        private const val PREFS_NAME = "poweramp_track_ids"

        const val POWERAMP_PACKAGE = "com.maxmpz.audioplayer"
        const val POWERAMP_API_RECEIVER = "com.maxmpz.audioplayer.player.PowerampAPIReceiver"

        const val ACTION_NEED_LYRICS = "com.maxmpz.audioplayer.ACTION_NEED_LYRICS"
        const val ACTION_UPDATE_LYRICS = "com.maxmpz.audioplayer.ACTION_UPDATE_LYRICS"

        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_ALBUM = "album"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_PATH = "path"
        const val EXTRA_LYRICS = "lyrics"
        const val EXTRA_INFO_LINE = "info_line"
        const val EXTRA_PACKAGE = "pak"
    }
}
