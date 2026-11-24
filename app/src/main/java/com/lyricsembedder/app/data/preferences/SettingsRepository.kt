package com.lyricsembedder.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lyricsembedder.app.data.model.AppSettings
import com.lyricsembedder.app.data.model.Mp3TagVersion
import com.lyricsembedder.app.data.model.PreferredLyricsType
import com.lyricsembedder.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lyrics_embedder_prefs")

class SettingsRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        val USE_EXTENDED_SEARCH = booleanPreferencesKey("use_extended_search")
        val NOTIFY_ON_FAILURE = booleanPreferencesKey("notify_on_failure")
        val REPLACE_PREV_NOTIFICATION = booleanPreferencesKey("replace_prev_notification")
        val PREFERRED_LYRICS_TYPE = stringPreferencesKey("preferred_lyrics_type")
        val MARK_INSTRUMENTAL = booleanPreferencesKey("mark_instrumental")

        val SEND_TO_POWERAMP = booleanPreferencesKey("send_to_poweramp")
        val SAVE_LYRICS_AS_FILE = booleanPreferencesKey("save_lyrics_as_file")
        val EMBED_LYRICS_IN_FILE = booleanPreferencesKey("embed_lyrics_in_file")
        val UPDATE_METADATA_ON_LYRICS_SAVE = booleanPreferencesKey("update_metadata_on_lyrics_save")
        val MP3_TAG_VERSION = stringPreferencesKey("mp3_tag_version")
        val ALSO_SAVE_ID3V1 = booleanPreferencesKey("also_save_id3v1")
        val EMBED_ARTWORK_IN_FILE = booleanPreferencesKey("embed_artwork_in_file")
        val SAVE_ARTWORK_COPY = booleanPreferencesKey("save_artwork_copy")
        val ARTWORK_FILE_NAME = stringPreferencesKey("artwork_file_name")
        val POWERAMP_FOLDERS = stringPreferencesKey("poweramp_folders")
        val CUSTOM_FOLDERS = stringPreferencesKey("custom_folders")

        val LRCLIB_URLS = stringPreferencesKey("lrclib_urls")
        val ACTIVE_LRCLIB_URL = stringPreferencesKey("active_lrclib_url")

        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val TIMESTAMP_STEP_MS = longPreferencesKey("timestamp_step_ms")

        val TITLE_FILTERS = stringPreferencesKey("title_filters")
        val ARTIST_FILTERS = stringPreferencesKey("artist_filters")
        val ALBUM_FILTERS = stringPreferencesKey("album_filters")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,

            useExtendedSearch = prefs[Keys.USE_EXTENDED_SEARCH] ?: false,
            notifyOnFailure = prefs[Keys.NOTIFY_ON_FAILURE] ?: true,
            replacePreviousNotification = prefs[Keys.REPLACE_PREV_NOTIFICATION] ?: false,
            preferredLyricsType = runCatching {
                PreferredLyricsType.valueOf(
                    prefs[Keys.PREFERRED_LYRICS_TYPE] ?: PreferredLyricsType.SYNCED.name
                )
            }.getOrDefault(PreferredLyricsType.SYNCED),
            markInstrumentalTracks = prefs[Keys.MARK_INSTRUMENTAL] ?: false,

            sendLyricsToPoweramp = prefs[Keys.SEND_TO_POWERAMP] ?: true,
            saveLyricsAsFile = prefs[Keys.SAVE_LYRICS_AS_FILE] ?: false,
            embedLyricsInFile = prefs[Keys.EMBED_LYRICS_IN_FILE] ?: true,
            updateTrackMetadataOnLyricsSave = prefs[Keys.UPDATE_METADATA_ON_LYRICS_SAVE] ?: false,
            mp3TagVersion = runCatching {
                Mp3TagVersion.valueOf(prefs[Keys.MP3_TAG_VERSION] ?: Mp3TagVersion.ID3V23.name)
            }.getOrDefault(Mp3TagVersion.ID3V23),
            alsoSaveId3v1 = prefs[Keys.ALSO_SAVE_ID3V1] ?: true,
            embedArtworkInFile = prefs[Keys.EMBED_ARTWORK_IN_FILE] ?: true,
            saveArtworkCopyToFolder = prefs[Keys.SAVE_ARTWORK_COPY] ?: true,
            artworkFileName = prefs[Keys.ARTWORK_FILE_NAME] ?: "Cover.jpg",
            powerampFolders = decodeList(prefs[Keys.POWERAMP_FOLDERS]),
            customFolders = decodeList(prefs[Keys.CUSTOM_FOLDERS]),

            lrcLibApiUrls = decodeList(prefs[Keys.LRCLIB_URLS])
                .ifEmpty { listOf(AppSettings.DEFAULT_LRCLIB_API) },
            activeLrcLibApiUrl = prefs[Keys.ACTIVE_LRCLIB_URL] ?: AppSettings.DEFAULT_LRCLIB_API,

            geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
            geminiModel = prefs[Keys.GEMINI_MODEL] ?: "gemini-2.5-flash",
            openRouterApiKey = prefs[Keys.OPENROUTER_API_KEY] ?: "",
            openRouterModel = prefs[Keys.OPENROUTER_MODEL] ?: "openrouter/free",
            timestampStepMs = prefs[Keys.TIMESTAMP_STEP_MS] ?: 100L,

            titleFilters = decodeList(prefs[Keys.TITLE_FILTERS]),
            artistFilters = decodeList(prefs[Keys.ARTIST_FILTERS]),
            albumFilters = decodeList(prefs[Keys.ALBUM_FILTERS])
        )
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                themeMode = runCatching {
                    ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
                }.getOrDefault(ThemeMode.SYSTEM),
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
                useExtendedSearch = prefs[Keys.USE_EXTENDED_SEARCH] ?: false,
                notifyOnFailure = prefs[Keys.NOTIFY_ON_FAILURE] ?: true,
                replacePreviousNotification = prefs[Keys.REPLACE_PREV_NOTIFICATION] ?: false,
                preferredLyricsType = runCatching {
                    PreferredLyricsType.valueOf(
                        prefs[Keys.PREFERRED_LYRICS_TYPE] ?: PreferredLyricsType.SYNCED.name
                    )
                }.getOrDefault(PreferredLyricsType.SYNCED),
                markInstrumentalTracks = prefs[Keys.MARK_INSTRUMENTAL] ?: false,
                sendLyricsToPoweramp = prefs[Keys.SEND_TO_POWERAMP] ?: true,
                saveLyricsAsFile = prefs[Keys.SAVE_LYRICS_AS_FILE] ?: false,
                embedLyricsInFile = prefs[Keys.EMBED_LYRICS_IN_FILE] ?: true,
                updateTrackMetadataOnLyricsSave = prefs[Keys.UPDATE_METADATA_ON_LYRICS_SAVE] ?: false,
                mp3TagVersion = runCatching {
                    Mp3TagVersion.valueOf(prefs[Keys.MP3_TAG_VERSION] ?: Mp3TagVersion.ID3V23.name)
                }.getOrDefault(Mp3TagVersion.ID3V23),
                alsoSaveId3v1 = prefs[Keys.ALSO_SAVE_ID3V1] ?: true,
                embedArtworkInFile = prefs[Keys.EMBED_ARTWORK_IN_FILE] ?: true,
                saveArtworkCopyToFolder = prefs[Keys.SAVE_ARTWORK_COPY] ?: true,
                artworkFileName = prefs[Keys.ARTWORK_FILE_NAME] ?: "Cover.jpg",
                powerampFolders = decodeList(prefs[Keys.POWERAMP_FOLDERS]),
                customFolders = decodeList(prefs[Keys.CUSTOM_FOLDERS]),
                lrcLibApiUrls = decodeList(prefs[Keys.LRCLIB_URLS])
                    .ifEmpty { listOf(AppSettings.DEFAULT_LRCLIB_API) },
                activeLrcLibApiUrl = prefs[Keys.ACTIVE_LRCLIB_URL] ?: AppSettings.DEFAULT_LRCLIB_API,
                geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
                geminiModel = prefs[Keys.GEMINI_MODEL] ?: "gemini-2.5-flash",
                openRouterApiKey = prefs[Keys.OPENROUTER_API_KEY] ?: "",
                openRouterModel = prefs[Keys.OPENROUTER_MODEL] ?: "openrouter/free",
                timestampStepMs = prefs[Keys.TIMESTAMP_STEP_MS] ?: 100L,
                titleFilters = decodeList(prefs[Keys.TITLE_FILTERS]),
                artistFilters = decodeList(prefs[Keys.ARTIST_FILTERS]),
                albumFilters = decodeList(prefs[Keys.ALBUM_FILTERS])
            )
            val updated = transform(current)
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = updated.dynamicColor
            prefs[Keys.USE_EXTENDED_SEARCH] = updated.useExtendedSearch
            prefs[Keys.NOTIFY_ON_FAILURE] = updated.notifyOnFailure
            prefs[Keys.REPLACE_PREV_NOTIFICATION] = updated.replacePreviousNotification
            prefs[Keys.PREFERRED_LYRICS_TYPE] = updated.preferredLyricsType.name
            prefs[Keys.MARK_INSTRUMENTAL] = updated.markInstrumentalTracks
            prefs[Keys.SEND_TO_POWERAMP] = updated.sendLyricsToPoweramp
            prefs[Keys.SAVE_LYRICS_AS_FILE] = updated.saveLyricsAsFile
            prefs[Keys.EMBED_LYRICS_IN_FILE] = updated.embedLyricsInFile
            prefs[Keys.UPDATE_METADATA_ON_LYRICS_SAVE] = updated.updateTrackMetadataOnLyricsSave
            prefs[Keys.MP3_TAG_VERSION] = updated.mp3TagVersion.name
            prefs[Keys.ALSO_SAVE_ID3V1] = updated.alsoSaveId3v1
            prefs[Keys.EMBED_ARTWORK_IN_FILE] = updated.embedArtworkInFile
            prefs[Keys.SAVE_ARTWORK_COPY] = updated.saveArtworkCopyToFolder
            prefs[Keys.ARTWORK_FILE_NAME] = updated.artworkFileName
            prefs[Keys.POWERAMP_FOLDERS] = json.encodeToString(updated.powerampFolders.distinct())
            prefs[Keys.CUSTOM_FOLDERS] = json.encodeToString(updated.customFolders.distinct())
            prefs[Keys.LRCLIB_URLS] = json.encodeToString(updated.lrcLibApiUrls.distinct())
            prefs[Keys.ACTIVE_LRCLIB_URL] = updated.activeLrcLibApiUrl
            prefs[Keys.GEMINI_API_KEY] = updated.geminiApiKey
            prefs[Keys.GEMINI_MODEL] = updated.geminiModel
            prefs[Keys.OPENROUTER_API_KEY] = updated.openRouterApiKey
            prefs[Keys.OPENROUTER_MODEL] = updated.openRouterModel
            prefs[Keys.TIMESTAMP_STEP_MS] = updated.timestampStepMs
            prefs[Keys.TITLE_FILTERS] = json.encodeToString(updated.titleFilters)
            prefs[Keys.ARTIST_FILTERS] = json.encodeToString(updated.artistFilters)
            prefs[Keys.ALBUM_FILTERS] = json.encodeToString(updated.albumFilters)
        }
    }

    private fun decodeList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }
}
