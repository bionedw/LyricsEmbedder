package com.lyricsembedder.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lyricsembedder.app.data.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSettingsScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bộ lọc") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Những từ này sẽ bị xóa khỏi các trường tương ứng trong quá trình xử lý. Không phân biệt chữ hoa chữ thường. Hỗ trợ regex.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            FilterCategoryCard(
                icon = Icons.Filled.MusicNote,
                title = "Bộ lọc tiêu đề",
                placeholder = "VD: \\(Remastered\\)|\\[Official Video\\]",
                patterns = settings.titleFilters,
                onAdd = { viewModel.addTitleFilter(it) },
                onRemove = { viewModel.removeTitleFilter(it) }
            )

            FilterCategoryCard(
                icon = Icons.Filled.Group,
                title = "Bộ lọc nghệ sĩ",
                placeholder = "VD: feat\\..*|ft\\..*",
                patterns = settings.artistFilters,
                onAdd = { viewModel.addArtistFilter(it) },
                onRemove = { viewModel.removeArtistFilter(it) }
            )

            FilterCategoryCard(
                icon = Icons.Filled.Album,
                title = "Bộ lọc album",
                placeholder = "VD: \\(Deluxe Edition\\)|Disc \\d+",
                patterns = settings.albumFilters,
                onAdd = { viewModel.addAlbumFilter(it) },
                onRemove = { viewModel.removeAlbumFilter(it) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterCategoryCard(
    icon: ImageVector,
    title: String,
    placeholder: String,
    patterns: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onAdd(draft)
                            draft = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Thêm bộ lọc")
                }
            }

            if (patterns.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    patterns.forEach { pattern ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(pattern) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Xóa mẫu lọc",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onRemove(pattern) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
