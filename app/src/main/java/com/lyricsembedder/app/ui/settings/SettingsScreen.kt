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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
                title = { Text("Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
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
                    title = "Chủ đề",
                    subtitle = "Tùy chỉnh giao diện người dùng của ứng dụng",
                    onClick = onNavigateTheme
                )
            }
            item {
                SettingsMenuCard(
                    icon = Icons.Filled.LibraryMusic,
                    title = "Yêu cầu lời bài hát",
                    subtitle = "Cấu hình cách xử lý các yêu cầu lời bài hát từ Poweramp",
                    onClick = onNavigateLyricsRequest
                )
            }
            item {
                SettingsMenuCard(
                    icon = Icons.Filled.Storage,
                    title = "Cài đặt lưu trữ lời bài hát",
                    subtitle = "Quản lý nơi và cách lưu các tệp lời bài hát.",
                    onClick = onNavigateLyricsStorage
                )
            }
            item {
                SettingsMenuCard(
                    icon = Icons.Filled.Cloud,
                    title = "Nhà cung cấp lời bài hát",
                    subtitle = "Cấu hình nguồn lấy lời bài hát.",
                    onClick = onNavigateLyricsProvider
                )
            }
            item {
                SettingsMenuCard(
                    icon = Icons.Filled.EditNote,
                    title = "Trình chỉnh sửa lời bài hát",
                    subtitle = "Tùy chỉnh trải nghiệm chỉnh sửa lời bài hát",
                    onClick = onNavigateLyricsEditor
                )
            }
            item {
                SettingsMenuCard(
                    icon = Icons.Filled.FilterAlt,
                    title = "Bộ lọc",
                    subtitle = "Những từ này sẽ bị xóa khỏi các trường tương ứng trong quá trình xử lý. Không phân biệt chữ hoa chữ thường. Hỗ trợ regex.",
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
