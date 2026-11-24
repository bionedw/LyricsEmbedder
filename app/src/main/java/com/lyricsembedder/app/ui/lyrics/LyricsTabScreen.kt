package com.lyricsembedder.app.ui.lyrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyricsembedder.app.data.model.AudioTrack
import com.lyricsembedder.app.data.model.LyricsDownloadStatus
import com.lyricsembedder.app.data.model.LyricsFilterTab

/**
 * Tab 2: Thêm Lyrics
 * - Scans all songs with 3 filters: Không có lời bài hát, Đã có lời bài hát, Chưa có metadata
 * - Files without metadata cannot download lyrics
 * - Includes an expandable/draggable BottomSheet showing live download progress AND track selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsTabScreen(
    state: LyricsUiState,
    viewModel: LyricsViewModel,
    onOpenEditor: (AudioTrack) -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 84.dp,
        sheetContent = {
            LyricsProgressBottomSheet(
                state = state,
                onSelectAllMissing = { viewModel.selectAllWithoutLyrics() },
                onStartDownload = { viewModel.startDownloadSelected() },
                onCancelDownload = { viewModel.cancelDownload() },
                onToggleTrack = { viewModel.toggleTrackSelection(it) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            state.statusMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearStatusMessage() }) {
                            Text("Đóng")
                        }
                    }
                }
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.activeFilter == LyricsFilterTab.NO_LYRICS,
                    onClick = { viewModel.selectFilter(LyricsFilterTab.NO_LYRICS) },
                    label = { Text("Không có lời (${state.noLyricsCount})") }
                )
                FilterChip(
                    selected = state.activeFilter == LyricsFilterTab.HAS_LYRICS,
                    onClick = { viewModel.selectFilter(LyricsFilterTab.HAS_LYRICS) },
                    label = { Text("Đã có lời (${state.hasLyricsCount})") }
                )
                FilterChip(
                    selected = state.activeFilter == LyricsFilterTab.NO_METADATA,
                    onClick = { viewModel.selectFilter(LyricsFilterTab.NO_METADATA) },
                    label = { Text("Chưa có metadata (${state.noMetadataCount})") }
                )
            }

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.selectAllWithoutLyrics() },
                        enabled = state.noLyricsCount > 0
                    ) {
                        Icon(Icons.Filled.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Chọn tất cả chưa có lời")
                    }
                    if (state.selectedTrackUris.isNotEmpty()) {
                        OutlinedButton(onClick = { viewModel.clearSelection() }) {
                            Text("Bỏ chọn (${state.selectedTrackUris.size})")
                        }
                    }
                }
                IconButton(onClick = { viewModel.scanAllLibraryTracks() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Quét lại thư viện")
                }
            }

            if (state.activeFilter == LyricsFilterTab.NO_METADATA) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Các tệp chưa có metadata không thể tải lời bài hát. Hãy gắn metadata ở tab Gắn Metadata trước.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (state.isScanning && state.allTracks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.filteredTracks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Không có bài hát nào trong mục này.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(state.filteredTracks, key = { it.uri.toString() }) { track ->
                        val isSelected = track.uri.toString() in state.selectedTrackUris
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (track.hasMetadata) {
                                        viewModel.toggleTrackSelection(track)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    enabled = track.hasMetadata,
                                    onCheckedChange = { viewModel.toggleTrackSelection(track) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.displayTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (track.hasMetadata) {
                                            "${track.artist} • ${track.album.ifBlank { track.folderName }}"
                                        } else {
                                            "Chưa có metadata • ${track.fileName}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (track.hasMetadata) {
                                    IconButton(onClick = { onOpenEditor(track) }) {
                                        Icon(
                                            imageVector = Icons.Filled.EditNote,
                                            contentDescription = "Chỉnh sửa lời bài hát"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsProgressBottomSheet(
    state: LyricsUiState,
    onSelectAllMissing: () -> Unit,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onToggleTrack: (AudioTrack) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(min = 120.dp, max = 460.dp)
    ) {
        // Collapsed Peek Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.isDownloading) {
                        "Đang tải lời: ${state.completedCount}/${state.queue.size}"
                    } else if (state.queue.isNotEmpty()) {
                        "Tiến trình tải (${state.completedCount}/${state.queue.size})"
                    } else {
                        "Tải lời bài hát (Đã chọn ${state.selectedTrackUris.size} bài)"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (state.isDownloading) {
                        state.currentTrackName
                    } else {
                        "Kéo lên để chọn nhạc cần tải hoặc xem tiến trình chi tiết"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (state.isDownloading) {
                OutlinedButton(onClick = onCancelDownload) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dừng")
                }
            } else {
                Button(
                    onClick = onStartDownload,
                    enabled = state.selectedTrackUris.isNotEmpty()
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tải xuống")
                }
            }
        }

        if (state.isDownloading && state.queue.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { state.completedCount.toFloat() / state.queue.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Expanded Sheet: Progress List + Quick Track Selection inside the Bottom Sheet
        if (state.queue.isNotEmpty()) {
            Text(
                text = "Chi tiết tiến trình tải xuống",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.queue, key = { it.track.uri.toString() }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (item.status) {
                            LyricsDownloadStatus.SAVED -> Icons.Filled.CheckCircle
                            LyricsDownloadStatus.INSTRUMENTAL -> Icons.Filled.MusicOff
                            LyricsDownloadStatus.NOT_FOUND, LyricsDownloadStatus.ERROR,
                            LyricsDownloadStatus.SKIPPED_NO_METADATA -> Icons.Filled.ErrorOutline
                            else -> Icons.Filled.Lyrics
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = when (item.status) {
                                LyricsDownloadStatus.SAVED -> MaterialTheme.colorScheme.primary
                                LyricsDownloadStatus.ERROR, LyricsDownloadStatus.NOT_FOUND -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.track.displayTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.message.ifBlank { item.status.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // Allow selecting eligible songs directly inside the bottom sheet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chọn nhanh bài hát chưa có lời",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onSelectAllMissing) {
                    Text("Chọn tất cả")
                }
            }
            val missingTracks = state.allTracks.filter { it.hasMetadata && !it.hasLyrics }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(missingTracks, key = { "sheet_${it.uri}" }) { track ->
                    val checked = track.uri.toString() in state.selectedTrackUris
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTrack(track) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onToggleTrack(track) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.displayTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
