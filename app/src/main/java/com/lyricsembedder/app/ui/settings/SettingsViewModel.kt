package com.lyricsembedder.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyricsembedder.app.AppContainer
import com.lyricsembedder.app.data.model.AppSettings
import com.lyricsembedder.app.data.model.Mp3TagVersion
import com.lyricsembedder.app.data.model.PreferredLyricsType
import com.lyricsembedder.app.data.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val container: AppContainer
) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            container.settingsRepository.updateSettings(transform)
        }
    }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    fun setUseExtendedSearch(enabled: Boolean) = update { it.copy(useExtendedSearch = enabled) }
    fun setNotifyOnFailure(enabled: Boolean) = update { it.copy(notifyOnFailure = enabled) }
    fun setReplacePreviousNotification(enabled: Boolean) = update { it.copy(replacePreviousNotification = enabled) }
    fun setPreferredLyricsType(type: PreferredLyricsType) = update { it.copy(preferredLyricsType = type) }
    fun setMarkInstrumental(enabled: Boolean) = update { it.copy(markInstrumentalTracks = enabled) }

    fun setSendLyricsToPoweramp(enabled: Boolean) = update { it.copy(sendLyricsToPoweramp = enabled) }
    fun setSaveLyricsAsFile(enabled: Boolean) = update { it.copy(saveLyricsAsFile = enabled) }
    fun setEmbedLyricsInFile(enabled: Boolean) = update { it.copy(embedLyricsInFile = enabled) }
    fun setUpdateMetadataOnLyricsSave(enabled: Boolean) = update { it.copy(updateTrackMetadataOnLyricsSave = enabled) }
    fun setMp3TagVersion(version: Mp3TagVersion) = update { it.copy(mp3TagVersion = version) }
    fun setAlsoSaveId3v1(enabled: Boolean) = update { it.copy(alsoSaveId3v1 = enabled) }
    fun setEmbedArtworkInFile(enabled: Boolean) = update { it.copy(embedArtworkInFile = enabled) }
    fun setSaveArtworkCopyToFolder(enabled: Boolean) = update { it.copy(saveArtworkCopyToFolder = enabled) }
    fun setArtworkFileName(name: String) = update { it.copy(artworkFileName = name) }

    fun addPowerampFolder(uri: Uri) = update {
        it.copy(powerampFolders = (it.powerampFolders + uri.toString()).distinct())
    }

    fun removePowerampFolder(uriString: String) = update {
        it.copy(powerampFolders = it.powerampFolders - uriString)
    }

    fun addCustomFolder(uri: Uri) = update {
        it.copy(customFolders = (it.customFolders + uri.toString()).distinct())
    }

    fun removeCustomFolder(uriString: String) = update {
        it.copy(customFolders = it.customFolders - uriString)
    }

    fun addLrcLibApiUrl(url: String) {
        val cleaned = url.trim().trimEnd('/')
        if (cleaned.isBlank()) return
        update {
            it.copy(
                lrcLibApiUrls = (it.lrcLibApiUrls + cleaned).distinct(),
                activeLrcLibApiUrl = cleaned
            )
        }
    }

    fun removeLrcLibApiUrl(url: String) {
        if (url == AppSettings.DEFAULT_LRCLIB_API) return
        update { current ->
            val remaining = (current.lrcLibApiUrls - url).ifEmpty { listOf(AppSettings.DEFAULT_LRCLIB_API) }
            val nextActive = if (current.activeLrcLibApiUrl == url) remaining.first() else current.activeLrcLibApiUrl
            current.copy(lrcLibApiUrls = remaining, activeLrcLibApiUrl = nextActive)
        }
    }

    fun setActiveLrcLibApiUrl(url: String) = update { it.copy(activeLrcLibApiUrl = url) }

    fun setGeminiApiKey(key: String) = update { it.copy(geminiApiKey = key) }
    fun setGeminiModel(model: String) = update { it.copy(geminiModel = model) }
    fun setOpenRouterApiKey(key: String) = update { it.copy(openRouterApiKey = key) }
    fun setOpenRouterModel(model: String) = update { it.copy(openRouterModel = model) }
    fun setTimestampStepMs(stepMs: Long) = update { it.copy(timestampStepMs = stepMs) }

    fun addTitleFilter(pattern: String) {
        if (pattern.isBlank()) return
        update { it.copy(titleFilters = (it.titleFilters + pattern.trim()).distinct()) }
    }

    fun removeTitleFilter(pattern: String) = update { it.copy(titleFilters = it.titleFilters - pattern) }

    fun addArtistFilter(pattern: String) {
        if (pattern.isBlank()) return
        update { it.copy(artistFilters = (it.artistFilters + pattern.trim()).distinct()) }
    }

    fun removeArtistFilter(pattern: String) = update { it.copy(artistFilters = it.artistFilters - pattern) }

    fun addAlbumFilter(pattern: String) {
        if (pattern.isBlank()) return
        update { it.copy(albumFilters = (it.albumFilters + pattern.trim()).distinct()) }
    }

    fun removeAlbumFilter(pattern: String) = update { it.copy(albumFilters = it.albumFilters - pattern) }

    fun extractDisplayPath(uriString: String): String {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return uriString
        return container.audioScanner.extractDisplayPath(uri)
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(container) as T
        }
    }
}
