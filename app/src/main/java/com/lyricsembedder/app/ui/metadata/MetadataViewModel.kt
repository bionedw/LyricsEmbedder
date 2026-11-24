package com.lyricsembedder.app.ui.metadata

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyricsembedder.app.AppContainer
import com.lyricsembedder.app.data.model.AlbumMetadataDraft
import com.lyricsembedder.app.data.model.AppSettings
import com.lyricsembedder.app.data.model.AudioTrack
import com.lyricsembedder.app.data.model.FolderSource
import com.lyricsembedder.app.data.model.MbReleaseSummary
import com.lyricsembedder.app.data.model.MusicFolder
import com.lyricsembedder.app.data.model.TrackMappingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MetadataUiState(
    val folders: List<MusicFolder> = emptyList(),
    val selectedFolder: MusicFolder? = null,
    val folderTracks: List<AudioTrack> = emptyList(),
    val isScanning: Boolean = false,

    // MusicBee-style Search & Mapping Dialog state
    val showSearchDialog: Boolean = false,
    val searchTargetTracks: List<AudioTrack> = emptyList(),
    val searchTargetSingleFile: Boolean = false,
    val queryAlbumArtist: String = "",
    val queryAlbum: String = "",
    val queryTrackCountOnAlbum: String = "",
    val queryDiscNumber: String = "1",
    val queryTracksOnDisc: String = "",
    val isSearchingMb: Boolean = false,
    val searchResults: List<MbReleaseSummary> = emptyList(),
    val selectedReleaseId: String? = null,
    val isLoadingReleaseDetail: Boolean = false,
    val albumDraft: AlbumMetadataDraft = AlbumMetadataDraft(),
    val trackMappings: List<TrackMappingItem> = emptyList(),

    // Apply progress & feedback
    val isApplying: Boolean = false,
    val applyProgress: Float = 0f,
    val applyStatusText: String = "",
    val statusMessage: String? = null
)

class MetadataViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetadataUiState())
    val uiState: StateFlow<MetadataUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settingsFlow.collect { settings ->
                refreshFolders(settings)
            }
        }
    }

    fun refreshFolders(settings: AppSettings? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val activeSettings = settings ?: container.settingsRepository.settingsFlow.first()
            val folders = mutableListOf<MusicFolder>()

            for (uriStr in activeSettings.powerampFolders) {
                val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: continue
                folders += container.audioScanner.listAlbumSubfolders(uri, FolderSource.POWERAMP)
            }
            for (uriStr in activeSettings.customFolders) {
                val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: continue
                folders += container.audioScanner.listAlbumSubfolders(uri, FolderSource.CUSTOM)
            }

            val deduped = folders.distinctBy { it.uri.toString() }
            _uiState.update {
                it.copy(
                    folders = deduped,
                    isScanning = false
                )
            }
        }
    }

    fun addCustomFolder(uri: Uri) {
        viewModelScope.launch {
            container.settingsRepository.updateSettings { current ->
                current.copy(customFolders = (current.customFolders + uri.toString()).distinct())
            }
        }
    }

    fun selectFolder(folder: MusicFolder) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(selectedFolder = folder, isScanning = true, statusMessage = null)
            }
            val tracks = container.audioScanner.scanTracksInFolder(folder.uri, recursive = false)
            _uiState.update {
                it.copy(folderTracks = tracks, isScanning = false)
            }
        }
    }

    fun clearSelectedFolder() {
        _uiState.update { it.copy(selectedFolder = null, folderTracks = emptyList()) }
    }

    fun openMetadataSearchForFolder() {
        val folder = _uiState.value.selectedFolder ?: return
        val tracks = _uiState.value.folderTracks
        val firstTrack = tracks.firstOrNull()
        _uiState.update {
            it.copy(
                showSearchDialog = true,
                searchTargetTracks = tracks,
                searchTargetSingleFile = false,
                queryAlbumArtist = firstTrack?.albumArtist?.ifBlank { firstTrack.artist }.orEmpty(),
                queryAlbum = firstTrack?.album?.ifBlank { folder.displayName } ?: folder.displayName,
                queryTrackCountOnAlbum = "",
                queryDiscNumber = (firstTrack?.discNumber ?: 1).toString(),
                queryTracksOnDisc = tracks.size.toString(),
                searchResults = emptyList(),
                selectedReleaseId = null,
                albumDraft = AlbumMetadataDraft(),
                trackMappings = emptyList()
            )
        }
    }

    fun openMetadataSearchForFile(track: AudioTrack) {
        _uiState.update {
            it.copy(
                showSearchDialog = true,
                searchTargetTracks = listOf(track),
                searchTargetSingleFile = true,
                queryAlbumArtist = track.albumArtist.ifBlank { track.artist },
                queryAlbum = track.album.ifBlank { track.folderName },
                queryTrackCountOnAlbum = "",
                queryDiscNumber = (track.discNumber ?: 1).toString(),
                queryTracksOnDisc = "",
                searchResults = emptyList(),
                selectedReleaseId = null,
                albumDraft = AlbumMetadataDraft(),
                trackMappings = emptyList()
            )
        }
    }

    fun closeSearchDialog() {
        _uiState.update { it.copy(showSearchDialog = false) }
    }

    fun updateSearchQuery(
        albumArtist: String = _uiState.value.queryAlbumArtist,
        album: String = _uiState.value.queryAlbum,
        trackCountOnAlbum: String = _uiState.value.queryTrackCountOnAlbum,
        discNumber: String = _uiState.value.queryDiscNumber,
        tracksOnDisc: String = _uiState.value.queryTracksOnDisc
    ) {
        _uiState.update {
            it.copy(
                queryAlbumArtist = albumArtist,
                queryAlbum = album,
                queryTrackCountOnAlbum = trackCountOnAlbum,
                queryDiscNumber = discNumber,
                queryTracksOnDisc = tracksOnDisc
            )
        }
    }

    fun runMusicBrainzSearch() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSearchingMb = true, statusMessage = null) }
            val result = container.metadataRepository.searchReleases(
                albumArtist = state.queryAlbumArtist,
                album = state.queryAlbum,
                trackCountOnAlbum = state.queryTrackCountOnAlbum.toIntOrNull(),
                discNumber = state.queryDiscNumber.toIntOrNull(),
                tracksOnDisc = state.queryTracksOnDisc.toIntOrNull()
            )
            result.fold(
                onSuccess = { releases ->
                    _uiState.update { it.copy(searchResults = releases, isSearchingMb = false) }
                    if (releases.isNotEmpty()) {
                        selectRelease(releases.first().id)
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSearchingMb = false,
                            statusMessage = "Lỗi tìm kiếm MusicBrainz: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun selectRelease(releaseId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(selectedReleaseId = releaseId, isLoadingReleaseDetail = true) }
            val discNo = state.queryDiscNumber.toIntOrNull() ?: 1
            val result = container.metadataRepository.loadReleaseAndMapping(
                releaseId = releaseId,
                discNumber = discNo,
                localTracks = state.searchTargetTracks
            )
            result.fold(
                onSuccess = { (draft, mappings) ->
                    _uiState.update {
                        it.copy(
                            albumDraft = draft,
                            trackMappings = mappings,
                            isLoadingReleaseDetail = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingReleaseDetail = false,
                            statusMessage = "Không tải được chi tiết album: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun updateAlbumDraft(transform: (AlbumMetadataDraft) -> AlbumMetadataDraft) {
        _uiState.update { it.copy(albumDraft = transform(it.albumDraft)) }
    }

    fun toggleTrackMapping(index: Int) {
        _uiState.update { state ->
            val updated = state.trackMappings.mapIndexed { i, item ->
                if (i == index) item.copy(included = !item.included) else item
            }
            state.copy(trackMappings = updated)
        }
    }

    fun toggleAllTrackMappings(checked: Boolean) {
        _uiState.update { state ->
            state.copy(trackMappings = state.trackMappings.map { it.copy(included = checked) })
        }
    }

    fun applyMetadataToFolder() {
        val folder = _uiState.value.selectedFolder ?: return
        viewModelScope.launch {
            val settings = container.settingsRepository.settingsFlow.first()
            val state = _uiState.value
            _uiState.update { it.copy(isApplying = true, applyProgress = 0f, applyStatusText = "Đang chuẩn bị...") }

            val result = container.metadataRepository.applyAlbumMetadata(
                folderUri = folder.uri,
                draft = state.albumDraft,
                mappings = state.trackMappings,
                settings = settings,
                onProgress = { completed, total, name ->
                    val fraction = if (total > 0) completed.toFloat() / total.toFloat() else 1f
                    _uiState.update {
                        it.copy(
                            applyProgress = fraction,
                            applyStatusText = "($completed/$total) $name"
                        )
                    }
                }
            )

            result.fold(
                onSuccess = { count ->
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            showSearchDialog = false,
                            statusMessage = "Đã gắn metadata cho $count tệp nhạc và lưu ảnh bìa vào thư mục."
                        )
                    }
                    selectFolder(folder)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            statusMessage = "Lỗi khi gắn metadata: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun updateAlbumMetadataManually(
        album: String,
        albumArtist: String,
        year: String,
        genre: String,
        publisher: String
    ) {
        val folder = _uiState.value.selectedFolder ?: return
        viewModelScope.launch {
            val settings = container.settingsRepository.settingsFlow.first()
            val tracks = _uiState.value.folderTracks
            _uiState.update { it.copy(isApplying = true, applyStatusText = "Đang áp dụng metadata album...") }
            var updatedCount = 0
            for (track in tracks) {
                val edited = track.copy(
                    album = album.ifBlank { track.album },
                    albumArtist = albumArtist.ifBlank { track.albumArtist },
                    year = year.ifBlank { track.year },
                    genre = genre.ifBlank { track.genre },
                    publisher = publisher.ifBlank { track.publisher }
                )
                if (container.metadataRepository.updateSingleTrack(edited, settings).isSuccess) {
                    updatedCount++
                }
            }
            _uiState.update {
                it.copy(
                    isApplying = false,
                    statusMessage = "Đã cập nhật metadata album cho $updatedCount/${tracks.size} bài hát."
                )
            }
            selectFolder(folder)
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MetadataViewModel(container) as T
        }
    }
}
