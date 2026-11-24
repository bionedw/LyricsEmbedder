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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                title = { Text("Cài đặt lưu trữ lời bài hát") },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsToggleCard(
                title = "Gửi lời bài hát đến Poweramp",
                subtitle = "Lời bài hát sẽ được lưu trong bộ nhớ đệm của Poweramp, cho phép cập nhật trực tiếp từ giao diện Poweramp.",
                checked = settings.sendLyricsToPoweramp,
                onCheckedChange = { viewModel.setSendLyricsToPoweramp(it) }
            )

            SettingsToggleCard(
                title = "Lưu lời bài hát dưới dạng tệp",
                subtitle = "Lưu lời bài hát dưới dạng tệp văn bản (.lrc / .txt) trong cùng thư mục với tệp nhạc. Đảm bảo bạn chọn đúng thư mục bên dưới.",
                checked = settings.saveLyricsAsFile,
                onCheckedChange = { viewModel.setSaveLyricsAsFile(it) }
            )

            SettingsToggleCard(
                title = "Nhúng lời bài hát vào tệp",
                subtitle = "Nhúng lời bài hát trực tiếp vào tệp nhạc.",
                checked = settings.embedLyricsInFile,
                onCheckedChange = { viewModel.setEmbedLyricsInFile(it) }
            )

            SettingsToggleCard(
                title = "Sửa siêu dữ liệu bài hát",
                subtitle = "Tự động cập nhật tên bài hát, nghệ sĩ và tên album khi lưu lời bài hát.",
                checked = settings.updateTrackMetadataOnLyricsSave,
                onCheckedChange = { viewModel.setUpdateMetadataOnLyricsSave(it) }
            )

            // MusicBee-inspired tag & artwork storage options
            SettingsToggleCard(
                title = "Nhúng ảnh bìa vào tệp nhạc",
                subtitle = "Tự động nhúng ảnh bìa album trực tiếp vào tag của từng bài hát (ID3v2.3 + ID3v1).",
                checked = settings.embedArtworkInFile,
                onCheckedChange = { viewModel.setEmbedArtworkInFile(it) }
            )

            SettingsToggleCard(
                title = "Lưu bản sao ảnh bìa vào thư mục album",
                subtitle = "Tải thêm 1 tệp ảnh artwork vào thư mục đích khi gắn metadata.",
                checked = settings.saveArtworkCopyToFolder,
                onCheckedChange = { viewModel.setSaveArtworkCopyToFolder(it) }
            )

            if (settings.saveArtworkCopyToFolder) {
                OutlinedTextField(
                    value = settings.artworkFileName,
                    onValueChange = { viewModel.setArtworkFileName(it) },
                    label = { Text("Tên tệp ảnh bìa lưu trong thư mục") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(4.dp))

            // Poweramp Music Folders section
            FolderSectionCard(
                title = "Thư mục nhạc từ Poweramp",
                folders = settings.powerampFolders,
                displayPathOf = { viewModel.extractDisplayPath(it) },
                onAdd = { powerampFolderLauncher.launch(null) },
                onRemove = { viewModel.removePowerampFolder(it) }
            )

            // Custom Selected Folders section
            FolderSectionCard(
                title = "Thư mục tùy chỉnh đã chọn",
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
                        contentDescription = "Thêm thư mục",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (folders.isEmpty()) {
                Text(
                    text = "Chưa có thư mục nào. Nhấn nút + để chọn thư mục.",
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
                                contentDescription = "Xóa thư mục",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
