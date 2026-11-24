package com.lyricsembedder.app.ui.metadata

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Full-screen MusicBee-style Album Metadata Search & Track Mapping dialog:
 * - Search bar: album artist, album, track on album, disc #, tracks on disc + Find / Cancel
 * - Results table: type | country | year | album | album artist | publisher | source (MusicBrainz)
 * - Editable album fields + Cover Art preview + Apply button
 * - Track mapping table: checkbox | # | artist | title | time | original title | time | filename
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataSearchDialog(
    state: MetadataUiState,
    viewModel: MetadataViewModel
) {
    Dialog(
        onDismissRequest = { if (!state.isApplying) viewModel.closeSearchDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (state.searchTargetSingleFile) {
                                    "Set metadata for this music file"
                                } else {
                                    "Set metadata for this folder"
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { viewModel.closeSearchDialog() },
                                enabled = !state.isApplying
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Đóng")
                            }
                        },
                        actions = {
                            Button(
                                onClick = { viewModel.applyMetadataToFolder() },
                                enabled = state.trackMappings.any { it.included } && !state.isApplying,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Apply")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isApplying) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(state.applyStatusText, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { state.applyProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 1. Search inputs panel (MusicBee style)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.queryAlbumArtist,
                                    onValueChange = { viewModel.updateSearchQuery(albumArtist = it) },
                                    label = { Text("album artist") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = state.queryAlbum,
                                    onValueChange = { viewModel.updateSearchQuery(album = it) },
                                    label = { Text("album") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = state.queryTrackCountOnAlbum,
                                    onValueChange = { viewModel.updateSearchQuery(trackCountOnAlbum = it) },
                                    label = { Text("track on album") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = state.queryDiscNumber,
                                    onValueChange = { viewModel.updateSearchQuery(discNumber = it) },
                                    label = { Text("disc #") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f)
                                )
                                OutlinedTextField(
                                    value = state.queryTracksOnDisc,
                                    onValueChange = { viewModel.updateSearchQuery(tracksOnDisc = it) },
                                    label = { Text("tracks on disc") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { viewModel.closeSearchDialog() }) {
                                    Text("Cancel")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.runMusicBrainzSearch() },
                                    enabled = !state.isSearchingMb
                                ) {
                                    if (state.isSearchingMb) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text("Find")
                                }
                            }
                        }
                    }

                    // 2. MusicBrainz Search Results Table
                    if (state.searchResults.isNotEmpty()) {
                        Text(
                            text = "Kết quả từ MusicBrainz (${state.searchResults.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    TableHeaderCell("type", 110)
                                    TableHeaderCell("country", 60)
                                    TableHeaderCell("year", 55)
                                    TableHeaderCell("album", 220)
                                    TableHeaderCell("album artist", 150)
                                    TableHeaderCell("publisher", 140)
                                    TableHeaderCell("source", 95)
                                }
                                HorizontalDivider()
                                state.searchResults.forEach { release ->
                                    val selected = release.id == state.selectedReleaseId
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .clickable { viewModel.selectRelease(release.id) }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        TableCell(release.releaseType, 110)
                                        TableCell(release.country.orEmpty(), 60)
                                        TableCell(release.year, 55)
                                        TableCell(release.title, 220, bold = selected)
                                        TableCell(release.primaryArtist, 150)
                                        TableCell(release.primaryLabel, 140)
                                        TableCell("MusicBrainz", 95)
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }

                    // 3. Album Artwork & Editable Tag Fields
                    if (state.isLoadingReleaseDetail) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (state.selectedReleaseId != null) {
                        val draft = state.albumDraft
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!draft.artworkUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = draft.artworkUrl,
                                                contentDescription = "Ảnh bìa album",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Album,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CheckableFieldRow(
                                            checked = draft.applyAlbumArtist,
                                            onCheckedChange = { c -> viewModel.updateAlbumDraft { it.copy(applyAlbumArtist = c) } },
                                            label = "album artist",
                                            value = draft.albumArtist,
                                            onValueChange = { v -> viewModel.updateAlbumDraft { it.copy(albumArtist = v) } }
                                        )
                                        CheckableFieldRow(
                                            checked = draft.applyComposer,
                                            onCheckedChange = { c -> viewModel.updateAlbumDraft { it.copy(applyComposer = c) } },
                                            label = "composer",
                                            value = draft.composer,
                                            onValueChange = { v -> viewModel.updateAlbumDraft { it.copy(composer = v) } }
                                        )
                                        CheckableFieldRow(
                                            checked = draft.applyAlbum,
                                            onCheckedChange = { c -> viewModel.updateAlbumDraft { it.copy(applyAlbum = c) } },
                                            label = "album",
                                            value = draft.album,
                                            onValueChange = { v -> viewModel.updateAlbumDraft { it.copy(album = v) } }
                                        )
                                    }
                                }

                                CheckableFieldRow(
                                    checked = draft.applyPublisher,
                                    onCheckedChange = { c -> viewModel.updateAlbumDraft { it.copy(applyPublisher = c) } },
                                    label = "publisher",
                                    value = draft.publisher,
                                    onValueChange = { v -> viewModel.updateAlbumDraft { it.copy(publisher = v) } }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = draft.applyDiscNumber,
                                        onCheckedChange = { c -> viewModel.updateAlbumDraft { it.copy(applyDiscNumber = c) } }
                                    )
                                    OutlinedTextField(
                                        value = draft.discNumber.toString(),
                                        onValueChange = { v ->
                                            viewModel.updateAlbumDraft { it.copy(discNumber = v.toIntOrNull() ?: 1) }
                                        },
                                        label = { Text("disc #") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.7f)
                                    )
                                    Text("of")
                                    OutlinedTextField(
                                        value = draft.discTotal.toString(),
                                        onValueChange = { v ->
                                            viewModel.updateAlbumDraft { it.copy(discTotal = v.toIntOrNull() ?: 1) }
                                        },
                                        label = { Text("total") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.7f)
                                    )
                                    Checkbox(
                                        checked = draft.applyYear,
                                        onCheckedChange = { c -> viewModel.updateAlbumDraft { it.copy(applyYear = c) } }
                                    )
                                    OutlinedTextField(
                                        value = draft.year,
                                        onValueChange = { v -> viewModel.updateAlbumDraft { it.copy(year = v) } },
                                        label = { Text("year") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                CheckableFieldRow(
                                    checked = draft.applyGenre,
                                    onCheckedChange = { c -> viewModel.updateAlbumDraft { it.copy(applyGenre = c) } },
                                    label = "genre",
                                    value = draft.genre,
                                    onValueChange = { v -> viewModel.updateAlbumDraft { it.copy(genre = v) } }
                                )
                            }
                        }

                        // 4. Track Mapping Table
                        Text(
                            text = "Ánh xạ bài hát (Mapping) — Sẽ tự động lưu 1 tệp ảnh bìa vào thư mục đích",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                val allChecked = state.trackMappings.isNotEmpty() && state.trackMappings.all { it.included }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = allChecked,
                                        onCheckedChange = { viewModel.toggleAllTrackMappings(it) }
                                    )
                                    TableHeaderCell("#", 34)
                                    TableHeaderCell("artist", 150)
                                    TableHeaderCell("title", 200)
                                    TableHeaderCell("time", 52)
                                    TableHeaderCell("original title", 160)
                                    TableHeaderCell("time", 52)
                                    TableHeaderCell("filename", 160)
                                }
                                HorizontalDivider()
                                state.trackMappings.forEachIndexed { idx, row ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = row.included,
                                            onCheckedChange = { viewModel.toggleTrackMapping(idx) }
                                        )
                                        TableCell("%02d".format(row.trackNumber), 34)
                                        TableCell(row.fetchedArtist, 150)
                                        TableCell(row.fetchedTitle, 200, bold = true)
                                        TableCell(row.formattedFetchedDuration, 52)
                                        TableCell(row.localTrack.displayTitle, 160)
                                        TableCell(row.localTrack.formattedDuration, 52)
                                        TableCell(row.localTrack.fileName, 160)
                                    }
                                    HorizontalDivider()
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
private fun CheckableFieldRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TableHeaderCell(text: String, widthDp: Int) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(widthDp.dp)
    )
}

@Composable
private fun TableCell(text: String, widthDp: Int, bold: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(widthDp.dp)
    )
}
