package com.lyricsembedder.app.ui.metadata

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.lyricsembedder.app.R
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lyricsembedder.app.data.model.ArtworkSourceMode
import com.lyricsembedder.app.data.model.MetadataSource
import com.lyricsembedder.app.data.model.ReleaseFormatFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataSearchDialog(
    state: MetadataUiState,
    viewModel: MetadataViewModel
) {
    val artworkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateAlbumDraft {
                it.copy(
                    localArtworkUriString = uri.toString(),
                    artworkSourceMode = ArtworkSourceMode.LOCAL_IMAGE
                )
            }
        }
    }

    val filteredReleases = when (state.formatFilter) {
        ReleaseFormatFilter.ALL -> state.searchResults
        else -> state.searchResults.filter { it.formatCategory == state.formatFilter }
    }

    Dialog(
        onDismissRequest = { if (!state.isApplying) viewModel.closeSearchDialog() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (state.searchTargetSingleFile) {
                                    stringResource(R.string.metadata_set_file)
                                } else {
                                    stringResource(R.string.metadata_set_folder)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { viewModel.closeSearchDialog() },
                                enabled = !state.isApplying
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
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
                                Text(stringResource(R.string.action_apply))
                            }
                        }
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .navigationBarsPadding()
                        .imePadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isApplying) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(state.applyStatusText, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { state.applyProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 1. Search Form
                    item {
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
                                var sourceMenuExpanded by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box {
                                        OutlinedButton(onClick = { sourceMenuExpanded = true }) {
                                            Text(
                                                text = "${stringResource(R.string.metadata_source_label)}: ${state.selectedSource.displayName}",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(
                                            expanded = sourceMenuExpanded,
                                            onDismissRequest = { sourceMenuExpanded = false }
                                        ) {
                                            MetadataSource.entries.forEach { src ->
                                                DropdownMenuItem(
                                                    text = { Text(src.displayName) },
                                                    onClick = {
                                                        sourceMenuExpanded = false
                                                        viewModel.setSelectedSource(src)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Row {
                                    OutlinedButton(onClick = { viewModel.closeSearchDialog() }) {
                                        Text(stringResource(R.string.action_cancel))
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
                                        Text(stringResource(R.string.action_find))
                                    }
                                    }
                                }
                            }
                        }
                    }

                    // 2. MusicBrainz Results + All Variants Loader + Format Filter Chips
                    if (state.searchResults.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.metadata_mb_results, filteredReleases.size),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilledTonalButton(
                                        onClick = { viewModel.loadAllAlbumVariants() },
                                        enabled = !state.isLoadingVariants
                                    ) {
                                        if (state.isLoadingVariants) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Filled.Collections, null, Modifier.size(16.dp))
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.metadata_load_all_variants))
                                    }
                                }

                                // Format / Variant filter chips (All / Digital / CD / Vinyl / Other)
                                val digitalCount = state.searchResults.count { it.formatCategory == ReleaseFormatFilter.DIGITAL }
                                val cdCount = state.searchResults.count { it.formatCategory == ReleaseFormatFilter.CD }
                                val vinylCount = state.searchResults.count { it.formatCategory == ReleaseFormatFilter.VINYL }
                                val otherCount = state.searchResults.count { it.formatCategory == ReleaseFormatFilter.OTHER }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = state.formatFilter == ReleaseFormatFilter.ALL,
                                        onClick = { viewModel.setFormatFilter(ReleaseFormatFilter.ALL) },
                                        label = { Text("${stringResource(R.string.metadata_filter_all)} (${state.searchResults.size})") }
                                    )
                                    FilterChip(
                                        selected = state.formatFilter == ReleaseFormatFilter.DIGITAL,
                                        onClick = { viewModel.setFormatFilter(ReleaseFormatFilter.DIGITAL) },
                                        label = { Text("${stringResource(R.string.metadata_filter_digital)} ($digitalCount)") }
                                    )
                                    FilterChip(
                                        selected = state.formatFilter == ReleaseFormatFilter.CD,
                                        onClick = { viewModel.setFormatFilter(ReleaseFormatFilter.CD) },
                                        label = { Text("${stringResource(R.string.metadata_filter_cd)} ($cdCount)") }
                                    )
                                    FilterChip(
                                        selected = state.formatFilter == ReleaseFormatFilter.VINYL,
                                        onClick = { viewModel.setFormatFilter(ReleaseFormatFilter.VINYL) },
                                        label = { Text("${stringResource(R.string.metadata_filter_vinyl)} ($vinylCount)") }
                                    )
                                    if (otherCount > 0) {
                                        FilterChip(
                                            selected = state.formatFilter == ReleaseFormatFilter.OTHER,
                                            onClick = { viewModel.setFormatFilter(ReleaseFormatFilter.OTHER) },
                                            label = { Text("${stringResource(R.string.metadata_filter_other)} ($otherCount)") }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TableHeaderCell("format", 110)
                                        TableHeaderCell("variant", 140)
                                        TableHeaderCell("country", 58)
                                        TableHeaderCell("year", 50)
                                        TableHeaderCell("tracks", 52)
                                        TableHeaderCell("album", 210)
                                        TableHeaderCell("source", 120)
                                        TableHeaderCell("publisher", 130)
                                        TableHeaderCell("cover", 110)
                                    }
                                    HorizontalDivider()
                                    filteredReleases.forEach { release ->
                                        val selected = release.id == state.selectedReleaseId
                                        Row(
                                            modifier = Modifier
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                                .clickable { viewModel.selectRelease(release.id) }
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TableCell(release.mediaFormatDisplay.ifBlank { release.releaseType }, 110, bold = release.formatCategory == ReleaseFormatFilter.DIGITAL)
                                            TableCell(release.disambiguation.orEmpty().ifBlank { release.releaseType }, 140)
                                            TableCell(release.country.orEmpty(), 58)
                                            TableCell(release.year, 50)
                                            TableCell(if (release.totalTracks > 0) release.totalTracks.toString() else "-", 52)
                                            TableCell(release.title, 210, bold = selected)
                                            TableCell(release.source.displayName, 120)
                                            TableCell(release.primaryLabel, 130)
                                            TextButton(
                                                onClick = { viewModel.adoptArtworkFromRelease(release) },
                                                modifier = Modifier.width(110.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.metadata_use_this_cover),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }

                    // 3. Album Artwork (Source / Custom Local Image / None) & Editable Metadata
                    if (state.isLoadingReleaseDetail) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (state.selectedReleaseId != null) {
                        item {
                            val draft = state.albumDraft
                            val activePreviewModel: Any? = when (draft.artworkSourceMode) {
                                ArtworkSourceMode.REMOTE_SOURCE -> draft.artworkUrl
                                ArtworkSourceMode.LOCAL_IMAGE -> draft.localArtworkUriString
                                ArtworkSourceMode.NONE -> null
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(104.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .clickable { artworkPicker.launch("image/*") },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (activePreviewModel != null) {
                                                AsyncImage(
                                                    model = activePreviewModel,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.Album,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(44.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.metadata_artwork_source_title),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.horizontalScroll(rememberScrollState())
                                            ) {
                                                FilterChip(
                                                    selected = draft.artworkSourceMode == ArtworkSourceMode.REMOTE_SOURCE,
                                                    onClick = {
                                                        viewModel.updateAlbumDraft {
                                                            it.copy(artworkSourceMode = ArtworkSourceMode.REMOTE_SOURCE)
                                                        }
                                                    },
                                                    leadingIcon = { Icon(Icons.Filled.Cloud, null, Modifier.size(16.dp)) },
                                                    label = { Text(stringResource(R.string.metadata_artwork_from_source)) }
                                                )
                                                FilterChip(
                                                    selected = draft.artworkSourceMode == ArtworkSourceMode.LOCAL_IMAGE,
                                                    onClick = { artworkPicker.launch("image/*") },
                                                    leadingIcon = { Icon(Icons.Filled.Image, null, Modifier.size(16.dp)) },
                                                    label = { Text(stringResource(R.string.metadata_artwork_pick_local)) }
                                                )
                                                FilterChip(
                                                    selected = draft.artworkSourceMode == ArtworkSourceMode.NONE,
                                                    onClick = {
                                                        viewModel.updateAlbumDraft {
                                                            it.copy(artworkSourceMode = ArtworkSourceMode.NONE)
                                                        }
                                                    },
                                                    leadingIcon = { Icon(Icons.Filled.HideImage, null, Modifier.size(16.dp)) },
                                                    label = { Text(stringResource(R.string.metadata_artwork_none)) }
                                                )
                                            }
                                        }
                                    }

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
                        }

                        // 4. Full-Content Track Mapping Header + Select All
                        item {
                            val allChecked = state.trackMappings.isNotEmpty() && state.trackMappings.all { it.included }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.metadata_mapping_header),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = allChecked,
                                        onCheckedChange = { viewModel.toggleAllTrackMappings(it) }
                                    )
                                    Text(
                                        text = stringResource(R.string.lyrics_select_all),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = state.trackMappings,
                            key = { _, item -> item.localTrack.uri.toString() }
                        ) { idx, row ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleTrackMapping(idx) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (row.included) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Checkbox(
                                        checked = row.included,
                                        onCheckedChange = { viewModel.toggleTrackMapping(idx) }
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "%02d. %s".format(row.trackNumber, row.fetchedTitle),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = row.formattedFetchedDuration,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = row.fetchedArtist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp))
                                        Text(
                                            text = "${stringResource(R.string.metadata_local_file_label)}: ${row.localTrack.fileName} (${row.localTrack.formattedDuration})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (row.localTrack.title.isNotBlank()) {
                                            Text(
                                                text = "${stringResource(R.string.metadata_original_title_label)}: ${row.localTrack.title}",
                                                style = MaterialTheme.typography.bodySmall,
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
