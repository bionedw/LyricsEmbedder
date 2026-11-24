package com.lyricsembedder.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyricsembedder.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateTheme: () -> Unit,
    onNavigateLyricsRequest: () -> Unit,
    onNavigateLyricsStorage: () -> Unit,
    onNavigateLyricsProvider: () -> Unit,
    onNavigateLyricsEditor: () -> Unit,
    onNavigateFilters: () -> Unit
) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SettingsMenuCard(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = stringResource(R.string.settings_theme_subtitle),
                    onClick = onNavigateTheme
                )
            }
            item {
                SettingsMenuCard(
                    icon = Icons.Filled.LibraryMusic,
                    title = stringResource(R.string.settings_request_title),
                    subtitle = stringResource(R.string.settings_request_subtitle),
                    onClick = onNavigateLyricsRequest
                )
            }
            item {
                SettingsMenuCard(
                    icon = Icons.Filled.Storage,
                    title = stringResource(R.string.settings_storage_title),
                    subtitle = stringResource(R.string.settings_storage_subtitle),
                    onClick = onNavigateLyricsStorage
                )
            }
            item {
                SettingsMenuCard(
                    icon = Icons.Filled.Cloud,
                    title = stringResource(R.string.settings_provider_title),
                    subtitle = stringResource(R.string.settings_provider_subtitle),
                    onClick = onNavigateLyricsProvider
                )
            }
            item {
                SettingsMenuCard(
                    icon = Icons.Filled.EditNote,
                    title = stringResource(R.string.settings_editor_title),
                    subtitle = stringResource(R.string.settings_editor_subtitle),
                    onClick = onNavigateLyricsEditor
                )
            }
            item {
                SettingsMenuCard(
                    icon = Icons.Filled.FilterAlt,
                    title = stringResource(R.string.settings_filter_title),
                    subtitle = stringResource(R.string.settings_filter_subtitle),
                    onClick = onNavigateFilters
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
