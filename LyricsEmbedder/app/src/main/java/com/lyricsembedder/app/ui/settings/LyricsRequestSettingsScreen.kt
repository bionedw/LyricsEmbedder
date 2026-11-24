package com.lyricsembedder.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lyricsembedder.app.R
import com.lyricsembedder.app.data.model.AppSettings
import com.lyricsembedder.app.data.model.PreferredLyricsType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsRequestSettingsScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_request_title)) },
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
                title = stringResource(R.string.req_extended_title),
                subtitle = stringResource(R.string.req_extended_subtitle),
                checked = settings.useExtendedSearch,
                onCheckedChange = { viewModel.setUseExtendedSearch(it) }
            )

            SettingsToggleCard(
                title = stringResource(R.string.req_notify_fail_title),
                subtitle = stringResource(R.string.req_notify_fail_subtitle),
                checked = settings.notifyOnFailure,
                onCheckedChange = { viewModel.setNotifyOnFailure(it) }
            )

            SettingsToggleCard(
                title = stringResource(R.string.req_replace_notif_title),
                subtitle = stringResource(R.string.req_replace_notif_subtitle),
                checked = settings.replacePreviousNotification,
                onCheckedChange = { viewModel.setReplacePreviousNotification(it) }
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { typeDropdownExpanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.req_preferred_type_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.req_preferred_type_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (settings.preferredLyricsType) {
                                    PreferredLyricsType.SYNCED -> stringResource(R.string.req_type_synced)
                                    PreferredLyricsType.TEXT_ONLY -> stringResource(R.string.req_type_text_only)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.req_type_synced)) },
                                onClick = {
                                    typeDropdownExpanded = false
                                    viewModel.setPreferredLyricsType(PreferredLyricsType.SYNCED)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.req_type_text_only)) },
                                onClick = {
                                    typeDropdownExpanded = false
                                    viewModel.setPreferredLyricsType(PreferredLyricsType.TEXT_ONLY)
                                }
                            )
                        }
                    }
                }
            }

            SettingsToggleCard(
                title = stringResource(R.string.req_instrumental_title),
                subtitle = stringResource(R.string.req_instrumental_subtitle),
                checked = settings.markInstrumentalTracks,
                onCheckedChange = { viewModel.setMarkInstrumental(it) }
            )
        }
    }
}

@Composable
fun SettingsToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
