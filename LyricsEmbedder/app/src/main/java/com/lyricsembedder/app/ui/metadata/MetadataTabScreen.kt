package com.lyricsembedder.app.ui.metadata

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyricsembedder.app.R
import com.lyricsembedder.app.data.model.AudioTrack
import com.lyricsembedder.app.data.model.FolderSource
import com.lyricsembedder.app.data.model.MusicFolder

@Composable
fun MetadataTabScreen(
    state: MetadataUiState,
    isExpandedScreen: Boolean,
    viewModel: MetadataViewModel
) {
    val context = LocalContext.current
    var showAlbumEditDialog by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            viewModel.addCustomFolder(uri)
        }
    }

    if (state.showSearchDialog) {
        MetadataSearchDialog(
            state = state,
            viewModel = viewModel
        )
    }

    if (showAlbumEditDialog && state.selectedFolder != null) {
        val sample = state.folderTracks.firstOrNull()
        EditAlbumMetadataDialog(
            initialAlbum = sample?.album ?: state.selectedFolder.displayName,
            initialAlbumArtist = sample?.albumArtist ?: sample?.artist.orEmpty(),
            initialYear = sample?.year.orEmpty(),
            initialGenre = sample?.genre.orEmpty(),
            initialPublisher = sample?.publisher.orEmpty(),
            onDismiss = { showAlbumEditDialog = false },
            onApply = { album, albumArtist, year, genre, publisher ->
                showAlbumEditDialog = false
                viewModel.updateAlbumMetadataManually(album, albumArtist, year, genre, publisher)
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        state.statusMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearStatusMessage() }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            }
        }

        if (state.isApplying) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(state.applyStatusText, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { state.applyProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (isExpandedScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                FolderListPane(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                    state = state,
                    onPickFolder = { folderPickerLauncher.launch(null) },
                    onRefresh = { viewModel.refreshFolders() },
                    onSelectFolder = { viewModel.selectFolder(it) }
                )
                VerticalDivider()
                if (state.selectedFolder != null) {
                    FolderDetailPane(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        folder = state.selectedFolder,
                        tracks = state.folderTracks,
                        isScanning = state.isScanning,
                        showBackButton = false,
                        onBack = {},
                        onSetMetadataForFolder = { viewModel.openMetadataSearchForFolder() },
                        onEditAlbumManual = { showAlbumEditDialog = true },
                        onSetMetadataForFile = { viewModel.openMetadataSearchForFile(it) }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.metadata_tablet_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            if (state.selectedFolder == null) {
                FolderListPane(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    onPickFolder = { folderPickerLauncher.launch(null) },
                    onRefresh = { viewModel.refreshFolders() },
                    onSelectFolder = { viewModel.selectFolder(it) }
                )
            } else {
                FolderDetailPane(
                    modifier = Modifier.fillMaxSize(),
                    folder = state.selectedFolder,
                    tracks = state.folderTracks,
                    isScanning = state.isScanning,
                    showBackButton = true,
                    onBack = { viewModel.clearSelectedFolder() },
                    onSetMetadataForFolder = { viewModel.openMetadataSearchForFolder() },
                    onEditAlbumManual = { showAlbumEditDialog = true },
                    onSetMetadataForFile = { viewModel.openMetadataSearchForFile(it) }
                )
            }
        }
    }
}

@Composable
private fun FolderListPane(
    modifier: Modifier,
    state: MetadataUiState,
    onPickFolder: () -> Unit,
    onRefresh: () -> Unit,
    onSelectFolder: (MusicFolder) -> Unit
) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.metadata_folders_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.metadata_folders_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                }
                FilledTonalButton(onClick = onPickFolder) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.metadata_pick_folder))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (state.isScanning && state.folders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.folders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.metadata_empty_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.metadata_empty_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(state.folders, key = { it.uri.toString() }) { folder ->
                    val isSelected = state.selectedFolder?.uri == folder.uri
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFolder(folder) },
                        colors = CardDefaults.elevatedCardColors(
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
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = folder.displayPath,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.metadata_tracks_count, folder.trackCount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (folder.source == FolderSource.POWERAMP) {
                                        stringResource(R.string.metadata_source_poweramp)
                                    } else {
                                        stringResource(R.string.metadata_source_custom)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderDetailPane(
    modifier: Modifier,
    folder: MusicFolder,
    tracks: List<AudioTrack>,
    isScanning: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSetMetadataForFolder: () -> Unit,
    onEditAlbumManual: () -> Unit,
    onSetMetadataForFile: (AudioTrack) -> Unit
) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.metadata_folder_detail_subtitle, tracks.size, folder.displayPath),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSetMetadataForFolder,
                enabled = tracks.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.metadata_set_folder), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onEditAlbumManual,
                enabled = tracks.isNotEmpty()
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.metadata_edit_album))
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        if (isScanning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val noAlbumLabel = stringResource(R.string.metadata_no_album)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(tracks, key = { it.uri.toString() }) { track ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (track.hasMetadata) Icons.Filled.Album else Icons.Filled.AudioFile,
                                contentDescription = null,
                                tint = if (track.hasMetadata) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            Spacer(Modifier.width(10.dp))
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
                                        "${track.artist} • ${track.album.ifBlank { noAlbumLabel }} • ${track.formattedDuration}"
                                    } else {
                                        stringResource(R.string.metadata_no_metadata_file, track.fileName)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = { onSetMetadataForFile(track) }) {
                                Text(stringResource(R.string.metadata_set_short), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditAlbumMetadataDialog(
    initialAlbum: String,
    initialAlbumArtist: String,
    initialYear: String,
    initialGenre: String,
    initialPublisher: String,
    onDismiss: () -> Unit,
    onApply: (album: String, albumArtist: String, year: String, genre: String, publisher: String) -> Unit
) {
    var album by remember { mutableStateOf(initialAlbum) }
    var albumArtist by remember { mutableStateOf(initialAlbumArtist) }
    var year by remember { mutableStateOf(initialYear) }
    var genre by remember { mutableStateOf(initialGenre) }
    var publisher by remember { mutableStateOf(initialPublisher) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.metadata_edit_album_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Album") }, singleLine = true)
                OutlinedTextField(value = albumArtist, onValueChange = { albumArtist = it }, label = { Text("Album Artist") }, singleLine = true)
                OutlinedTextField(value = publisher, onValueChange = { publisher = it }, label = { Text("Publisher") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApply(album, albumArtist, year, genre, publisher) }) {
                Text(stringResource(R.string.metadata_apply_album_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
