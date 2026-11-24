package com.lyricsembedder.app

import android.app.Application
import com.lyricsembedder.app.data.local.AudioScanner
import com.lyricsembedder.app.data.local.AudioTagManager
import com.lyricsembedder.app.data.local.PowerampBridge
import com.lyricsembedder.app.data.preferences.SettingsRepository
import com.lyricsembedder.app.data.remote.AiLyricsService
import com.lyricsembedder.app.data.remote.CoverArtService
import com.lyricsembedder.app.data.remote.LrcLibService
import com.lyricsembedder.app.data.remote.DeezerMetadataService
import com.lyricsembedder.app.data.remote.ItunesMetadataService
import com.lyricsembedder.app.data.remote.MusicBrainzService
import com.lyricsembedder.app.data.repository.LyricsRepository
import com.lyricsembedder.app.data.repository.MetadataRepository
import com.lyricsembedder.app.util.NotificationHelper
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class LyricsEmbedderApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val settingsRepository = SettingsRepository(application)
    val tagManager = AudioTagManager(application)
    val audioScanner = AudioScanner(application, tagManager)
    val powerampBridge = PowerampBridge(application)
    val notificationHelper = NotificationHelper(application)

    val musicBrainzService = MusicBrainzService(okHttpClient, json)
    val itunesMetadataService = ItunesMetadataService(okHttpClient, json)
    val deezerMetadataService = DeezerMetadataService(okHttpClient, json)
    val coverArtService = CoverArtService(okHttpClient, json)
    val lrcLibService = LrcLibService(okHttpClient, json)
    val aiLyricsService = AiLyricsService(okHttpClient, json)

    val metadataRepository = MetadataRepository(
        context = application,
        musicBrainzService = musicBrainzService,
        itunesMetadataService = itunesMetadataService,
        deezerMetadataService = deezerMetadataService,
        coverArtService = coverArtService,
        tagManager = tagManager
    )

    val lyricsRepository = LyricsRepository(
        lrcLibService = lrcLibService,
        tagManager = tagManager,
        powerampBridge = powerampBridge,
        notificationHelper = notificationHelper
    )
}
