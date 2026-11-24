package com.lyricsembedder.app.data.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lyricsembedder.app.LyricsEmbedderApp
import com.lyricsembedder.app.data.model.LrcLibResponse
import com.lyricsembedder.app.data.model.PreferredLyricsType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Background BroadcastReceiver for Poweramp's `com.maxmpz.audioplayer.ACTION_NEED_LYRICS`.
 * Fetches lyrics from the active LrcLib API using the user's filters and settings, then
 * immediately replies to Poweramp via `com.maxmpz.audioplayer.ACTION_UPDATE_LYRICS` with
 * the exact `realId`.
 */
class PowerampLyricsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != PowerampBridge.ACTION_NEED_LYRICS) return

        val app = context.applicationContext as? LyricsEmbedderApp ?: return
        val container = app.container

        val realId = intent.getLongExtra(PowerampBridge.EXTRA_ID, -1L)
        val rawTitle = intent.getStringExtra(PowerampBridge.EXTRA_TITLE).orEmpty()
        val rawArtist = intent.getStringExtra(PowerampBridge.EXTRA_ARTIST).orEmpty()
        val rawAlbum = intent.getStringExtra(PowerampBridge.EXTRA_ALBUM).orEmpty()
        val durationSec = intent.getIntExtra(PowerampBridge.EXTRA_DURATION, 0).takeIf { it > 0 }
        val trackPath = intent.getStringExtra(PowerampBridge.EXTRA_PATH)
        val callerPackage = intent.getStringExtra(PowerampBridge.EXTRA_PACKAGE)
            ?.ifBlank { PowerampBridge.POWERAMP_PACKAGE }
            ?: PowerampBridge.POWERAMP_PACKAGE

        if (realId <= 0L || rawTitle.isBlank()) return

        container.powerampBridge.rememberPowerampTrackId(
            realId = realId,
            title = rawTitle,
            artist = rawArtist,
            path = trackPath
        )

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = container.settingsRepository.settingsFlow.first()
                if (!settings.sendLyricsToPoweramp) return@launch

                val cleanTitle = FilterProcessor.applyFilters(rawTitle, settings.titleFilters)
                val cleanArtist = FilterProcessor.applyFilters(rawArtist, settings.artistFilters)
                val cleanAlbum = FilterProcessor.applyFilters(rawAlbum, settings.albumFilters)

                var response: LrcLibResponse? = container.lrcLibService.getExactLyrics(
                    apiBaseUrl = settings.activeLrcLibApiUrl,
                    trackName = cleanTitle,
                    artistName = cleanArtist,
                    albumName = cleanAlbum,
                    durationSeconds = durationSec
                ).getOrNull()

                if (response == null && settings.useExtendedSearch) {
                    val candidates = container.lrcLibService.searchExtendedLyrics(
                        apiBaseUrl = settings.activeLrcLibApiUrl,
                        trackName = cleanTitle,
                        artistName = cleanArtist,
                        albumName = cleanAlbum
                    ).getOrDefault(emptyList())
                    response = when (settings.preferredLyricsType) {
                        PreferredLyricsType.SYNCED ->
                            candidates.firstOrNull { !it.syncedLyrics.isNullOrBlank() } ?: candidates.firstOrNull()
                        PreferredLyricsType.TEXT_ONLY ->
                            candidates.firstOrNull { !it.plainLyrics.isNullOrBlank() } ?: candidates.firstOrNull()
                    }
                }

                if (response == null) {
                    if (settings.notifyOnFailure) {
                        container.notificationHelper.notifyLyricsFailure(
                            trackTitle = rawTitle,
                            reason = "Không tìm thấy lời trên LrcLib",
                            replacePrevious = settings.replacePreviousNotification
                        )
                    }
                    return@launch
                }

                val lyricsToSend = when {
                    response.instrumental -> {
                        if (settings.markInstrumentalTracks) AudioTagManager.INSTRUMENTAL_MARKER else null
                    }
                    settings.preferredLyricsType == PreferredLyricsType.SYNCED -> {
                        response.syncedLyrics?.takeIf { it.isNotBlank() }
                            ?: response.plainLyrics?.takeIf { it.isNotBlank() }
                    }
                    else -> {
                        response.plainLyrics?.takeIf { it.isNotBlank() }
                            ?: response.syncedLyrics?.takeIf { it.isNotBlank() }
                    }
                }

                if (!lyricsToSend.isNullOrBlank()) {
                    container.powerampBridge.sendLyricsToPoweramp(
                        realId = realId,
                        lyrics = lyricsToSend,
                        infoLine = "LyricsEmbedder • LrcLib",
                        targetPackage = callerPackage
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
