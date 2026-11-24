package com.lyricsembedder.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Header matching specification:
 * - Left: App name "LyricsEmbedder"
 * - Middle: 2 music note icons
 * - Right: 3-dot overflow menu with "Giới thiệu" (About) and "Cài đặt" (Settings)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "LyricsEmbedder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Biểu tượng âm nhạc 1",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Audiotrack,
                        contentDescription = "Biểu tượng âm nhạc 2",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        },
        actions = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Menu"
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Giới thiệu") },
                        leadingIcon = {
                            Icon(Icons.Filled.Info, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenAbout()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cài đặt") },
                        leadingIcon = {
                            Icon(Icons.Filled.Settings, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenSettings()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Giới thiệu LyricsEmbedder") },
        text = {
            Text(
                text = "LyricsEmbedder v1.0.0\n\n" +
                    "Ứng dụng Android (Material You, hỗ trợ từ Android 5.0 cho điện thoại và máy tính bảng) " +
                    "giúp lấy metadata album, ảnh bìa từ MusicBrainz & Cover Art Archive, gắn trực tiếp vào tệp âm thanh " +
                    "(MP3, FLAC, M4A, OGG, WAV, OPUS...) và tải/nhúng lời bài hát đồng bộ từ LrcLib, hỗ trợ đồng bộ với Poweramp."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}
