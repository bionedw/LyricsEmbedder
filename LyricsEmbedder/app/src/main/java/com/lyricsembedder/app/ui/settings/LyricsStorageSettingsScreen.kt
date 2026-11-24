package com.lyricsembedder.app.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyricsembedder.app.R
import com.lyricsembedder.app.data.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsStorageSettingsScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val powerampFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            viewModel.addPowerampFolder(uri)
        }
    }

    val customFolderLauncher = rememberLauncherForActivityResult(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_storage_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsToggleCard(
                title = stringResource(R.string.storage_poweramp_title),
                subtitle = stringResource(R.string.storage_poweramp_subtitle),
                checked = settings.sendLyricsToPoweramp,
                onCheckedChange = { viewModel.setSendLyricsToPoweramp(it) }
            )

            SettingsToggleCard(
                title = stringResource(R.string.storage_file_title),
                subtitle = stringResource(R.string.storage_file_subtitle),
                checked = settings.saveLyricsAsFile,
                onCheckedChange = { viewModel.setSaveLyricsAsFile(it) }
            )

            SettingsToggleCard(
                title = stringResource(R.string.storage_embed_title),
                subtitle = stringResource(R.string.storage_embed_subtitle),
                checked = settings.embedLyricsInFile,
                onCheckedChange = { viewModel.setEmbedLyricsInFile(it) }
            )

            SettingsToggleCard(
                title = stringResource(R.string.storage_fix_meta_title),
                subtitle = stringResource(R.string.storage_fix_meta_subtitle),
                checked = settings.updateTrackMetadataOnLyricsSave,
                onCheckedChange = { viewModel.setUpdateMetadataOnLyricsSave(it) }
            )

            SettingsToggleCard(
                title = stringResource(R.string.storage_embed_art_title),
                subtitle = stringResource(R.string.storage_embed_art_subtitle),
                checked = settings.embedArtworkInFile,
                onCheckedChange = { viewModel.setEmbedArtworkInFile(it) }
            )

            SettingsToggleCard(
                title = stringResource(R.string.storage_copy_art_title),
                subtitle = stringResource(R.string.storage_copy_art_subtitle),
                checked = settings.saveArtworkCopyToFolder,
                onCheckedChange = { viewModel.setSaveArtworkCopyToFolder(it) }
            )

            if (settings.saveArtworkCopyToFolder) {
                OutlinedTextField(
                    value = settings.artworkFileName,
                    onValueChange = { viewModel.setArtworkFileName(it) },
                    label = { Text(stringResource(R.string.storage_art_filename_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(4.dp))

            FolderSectionCard(
                title = stringResource(R.string.storage_poweramp_folders),
                emptyText = stringResource(R.string.storage_folders_empty),
                folders = settings.powerampFolders,
                displayPathOf = { viewModel.extractDisplayPath(it) },
                onAdd = { powerampFolderLauncher.launch(null) },
                onRemove = { viewModel.removePowerampFolder(it) }
            )

            FolderSectionCard(
                title = stringResource(R.string.storage_custom_folders),
                emptyText = stringResource(R.string.storage_folders_empty),
                folders = settings.customFolders,
                displayPathOf = { viewModel.extractDisplayPath(it) },
                onAdd = { customFolderLauncher.launch(null) },
                onRemove = { viewModel.removeCustomFolder(it) }
            )
        }
    }
}

@Composable
private fun FolderSectionCard(
    title: String,
    emptyText: String,
    folders: List<String>,
    displayPathOf: (String) -> String,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = stringResource(R.string.action_add),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (folders.isEmpty()) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                folders.forEachIndexed { index, uriStr ->
                    val path = displayPathOf(uriStr)
                    val shortName = path.substringAfterLast('/').ifBlank { path }
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. $shortName",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = path,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onRemove(uriStr) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
