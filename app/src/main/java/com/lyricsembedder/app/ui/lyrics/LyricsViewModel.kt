package com.lyricsembedder.app.ui.lyrics

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyricsembedder.app.AppContainer
import com.lyricsembedder.app.data.model.AppSettings
import com.lyricsembedder.app.data.model.AudioTrack
import com.lyricsembedder.app.data.model.LyricsDownloadStatus
import com.lyricsembedder.app.data.model.LyricsFilterTab
import com.lyricsembedder.app.data.model.LyricsQueueItem
import com.lyricsembedder.app.data.remote.AiProvider
import com.lyricsembedder.app.util.LrcTimestampHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LyricsUiState(
    val allTracks: List<AudioTrack> = emptyList(),
    val activeFilter: LyricsFilterTab = LyricsFilterTab.NO_LYRICS,
    val selectedTrackUris: Set<String> = emptySet(),
    val isScanning: Boolean = false,

    // Bottom sheet queue & download progress
    val queue: List<LyricsQueueItem> = emptyList(),
    val isDownloading: Boolean = false,
    val completedCount: Int = 0,
    val currentTrackName: String = "",

    // Lyrics editor state
    val editingTrack: AudioTrack? = null,
    val editorText: String = "",
    val isAiProcessing: Boolean = false,
    val statusMessage: String? = null
) {
    val filteredTracks: List<AudioTrack>
        get() = when (activeFilter) {
            LyricsFilterTab.NO_LYRICS -> allTracks.filter { it.hasMetadata && !it.hasLyrics }
            LyricsFilterTab.HAS_LYRICS -> allTracks.filter { it.hasMetadata && it.hasLyrics }
            LyricsFilterTab.NO_METADATA -> allTracks.filter { !it.hasMetadata }
        }

    val noLyricsCount: Int get() = allTracks.count { it.hasMetadata && !it.hasLyrics }
    val hasLyricsCount: Int get() = allTracks.count { it.hasMetadata && it.hasLyrics }
    val noMetadataCount: Int get() = allTracks.count { !it.hasMetadata }
}

class LyricsViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            container.settingsRepository.settingsFlow.collect { settings ->
                scanAllLibraryTracks(settings)
            }
        }
    }

    fun scanAllLibraryTracks(settings: AppSettings? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val activeSettings = settings ?: container.settingsRepository.settingsFlow.first()
            val allFolderUris = (activeSettings.powerampFolders + activeSettings.customFolders).distinct()
            val scanned = mutableListOf<AudioTrack>()
            for (uriStr in allFolderUris) {
                val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: continue
                scanned += container.audioScanner.scanTracksInFolder(uri, recursive = true)
            }
            val deduped = scanned.distinctBy { it.uri.toString() }
            _uiState.update {
                it.copy(
                    allTracks = deduped,
                    isScanning = false
                )
            }
        }
    }

    fun selectFilter(filter: LyricsFilterTab) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun toggleTrackSelection(track: AudioTrack) {
        if (!track.hasMetadata) {
            _uiState.update {
                it.copy(statusMessage = "Tệp chưa có metadata không thể tải lời bài hát.")
            }
            return
        }
        val key = track.uri.toString()
        _uiState.update { state ->
            val updated = if (key in state.selectedTrackUris) {
                state.selectedTrackUris - key
            } else {
                state.selectedTrackUris + key
            }
            state.copy(selectedTrackUris = updated)
        }
    }

    fun selectAllWithoutLyrics() {
        val eligibleKeys = _uiState.value.allTracks
            .filter { it.hasMetadata && !it.hasLyrics }
            .map { it.uri.toString() }
            .toSet()
        _uiState.update { it.copy(selectedTrackUris = eligibleKeys) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedTrackUris = emptySet()) }
    }

    fun startDownloadSelected() {
        val selectedKeys = _uiState.value.selectedTrackUris
        val targets = _uiState.value.allTracks.filter { it.uri.toString() in selectedKeys && it.hasMetadata }
        if (targets.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Hãy chọn ít nhất 1 bài hát đã có metadata.") }
            return
        }
        startDownloadForTracks(targets)
    }

    fun startDownloadForTracks(tracks: List<AudioTrack>) {
        downloadJob?.cancel()
        val initialQueue = tracks.map { LyricsQueueItem(track = it, status = LyricsDownloadStatus.QUEUED) }
        _uiState.update {
            it.copy(
                queue = initialQueue,
                isDownloading = true,
                completedCount = 0,
                currentTrackName = tracks.firstOrNull()?.displayTitle.orEmpty()
            )
        }

        downloadJob = viewModelScope.launch {
            val settings = container.settingsRepository.settingsFlow.first()
            for (index in initialQueue.indices) {
                val track = initialQueue[index].track
                _uiState.update { state ->
                    state.copy(
                        currentTrackName = track.displayTitle,
                        queue = state.queue.mapIndexed { i, item ->
                            if (i == index) item.copy(status = LyricsDownloadStatus.FETCHING, message = "Đang tải...")
                            else item
                        }
                    )
                }

                val outcome = container.lyricsRepository.fetchAndSaveLyrics(track, settings)

                _uiState.update { state ->
                    val updatedTracks = if (outcome.status == LyricsDownloadStatus.SAVED ||
                        outcome.status == LyricsDownloadStatus.INSTRUMENTAL
                    ) {
                        state.allTracks.map { t ->
                            if (t.uri == track.uri) t.copy(embeddedLyrics = outcome.lyrics) else t
                        }
                    } else state.allTracks

                    state.copy(
                        allTracks = updatedTracks,
                        completedCount = index + 1,
                        queue = state.queue.mapIndexed { i, item ->
                            if (i == index) item.copy(status = outcome.status, message = outcome.message)
                            else item
                        }
                    )
                }
            }
            _uiState.update {
                it.copy(
                    isDownloading = false,
                    currentTrackName = "Hoàn tất",
                    selectedTrackUris = emptySet()
                )
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _uiState.update { it.copy(isDownloading = false, currentTrackName = "Đã dừng") }
    }

    // --- Lyrics Editor ---
    fun openLyricsEditor(track: AudioTrack) {
        _uiState.update {
            it.copy(
                editingTrack = track,
                editorText = track.embeddedLyrics,
                statusMessage = null
            )
        }
    }

    fun updateEditorText(newText: String) {
        _uiState.update { it.copy(editorText = newText) }
    }

    fun shiftEditorTimestamps(direction: Int) {
        viewModelScope.launch {
            val stepMs = container.settingsRepository.settingsFlow.first().timestampStepMs
            val shifted = LrcTimestampHelper.shiftAllTimestamps(_uiState.value.editorText, stepMs * direction)
            _uiState.update { it.copy(editorText = shifted) }
        }
    }

    fun runAiLyricsCleanup(provider: AiProvider, instruction: String) {
        viewModelScope.launch {
            val settings = container.settingsRepository.settingsFlow.first()
            val apiKey = when (provider) {
                AiProvider.GEMINI -> settings.geminiApiKey
                AiProvider.OPEN_ROUTER -> settings.openRouterApiKey
            }
            val model = when (provider) {
                AiProvider.GEMINI -> settings.geminiModel
                AiProvider.OPEN_ROUTER -> settings.openRouterModel
            }
            _uiState.update { it.copy(isAiProcessing = true, statusMessage = null) }
            val result = container.aiLyricsService.assistLyrics(
                provider = provider,
                apiKey = apiKey,
                model = model,
                instruction = instruction,
                currentLyrics = _uiState.value.editorText
            )
            result.fold(
                onSuccess = { cleaned ->
                    _uiState.update {
                        it.copy(
                            editorText = cleaned,
                            isAiProcessing = false,
                            statusMessage = "AI đã xử lý xong lời bài hát."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isAiProcessing = false,
                            statusMessage = err.localizedMessage ?: "Lỗi gọi AI API"
                        )
                    }
                }
            )
        }
    }

    fun saveEditedLyrics(onSaved: () -> Unit = {}) {
        val track = _uiState.value.editingTrack ?: return
        val lyricsText = _uiState.value.editorText
        viewModelScope.launch {
            val settings = container.settingsRepository.settingsFlow.first()
            val result = container.lyricsRepository.persistLyrics(track, lyricsText, settings)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            allTracks = state.allTracks.map { t ->
                                if (t.uri == track.uri) t.copy(embeddedLyrics = lyricsText) else t
                            },
                            statusMessage = "Đã lưu lời bài hát thành công."
                        )
                    }
                    onSaved()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(statusMessage = "Lỗi lưu lời bài hát: ${err.localizedMessage}")
                    }
                }
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LyricsViewModel(container) as T
        }
    }
}
