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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lyricsembedder.app.data.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditorSettingsScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var geminiModelMenu by remember { mutableStateOf(false) }
    var openRouterModelMenu by remember { mutableStateOf(false) }
    var stepMenu by remember { mutableStateOf(false) }

    val geminiModels = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash")
    val openRouterModels = listOf("openrouter/free", "google/gemini-2.5-flash", "openai/gpt-4o-mini")
    val stepOptions = listOf(
        50L to "5 phần trăm giây (50ms)",
        100L to "10 phần trăm giây (100ms)",
        250L to "25 phần trăm giây (250ms)",
        500L to "50 phần trăm giây (500ms)",
        1000L to "1 giây (1000ms)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trình chỉnh sửa lời bài hát") },
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
            // AI Provider info
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
                        text = "Bạn cần thêm khóa API của riêng mình để sử dụng tính năng AI. Khóa của bạn chỉ được lưu trữ cục bộ trên thiết bị và không bao giờ bị thu thập hay chia sẻ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Google Gemini card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Google Gemini", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = settings.geminiApiKey,
                        onValueChange = { viewModel.setGeminiApiKey(it) },
                        label = { Text("Khóa API") },
                        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { geminiModelMenu = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mô hình đã chọn", style = MaterialTheme.typography.bodyMedium)
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(settings.geminiModel, color = MaterialTheme.colorScheme.primary)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = geminiModelMenu,
                                onDismissRequest = { geminiModelMenu = false }
                            ) {
                                geminiModels.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            geminiModelMenu = false
                                            viewModel.setGeminiModel(m)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // OpenRouter card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Open Router", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = settings.openRouterApiKey,
                        onValueChange = { viewModel.setOpenRouterApiKey(it) },
                        label = { Text("Khóa API") },
                        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openRouterModelMenu = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mô hình đã chọn", style = MaterialTheme.typography.bodyMedium)
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(settings.openRouterModel, color = MaterialTheme.colorScheme.primary)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = openRouterModelMenu,
                                onDismissRequest = { openRouterModelMenu = false }
                            ) {
                                openRouterModels.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            openRouterModelMenu = false
                                            viewModel.setOpenRouterModel(m)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Timestamp adjustment step
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { stepMenu = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bước điều chỉnh dấu thời gian",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Khoảng thời gian để dịch chuyển dấu thời gian với mỗi lần nhấn nút + hoặc -.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val currentLabel = stepOptions.firstOrNull { it.first == settings.timestampStepMs }?.second
                                ?: "${settings.timestampStepMs}ms"
                            Text(currentLabel, color = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = stepMenu,
                            onDismissRequest = { stepMenu = false }
                        ) {
                            stepOptions.forEach { (ms, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        stepMenu = false
                                        viewModel.setTimestampStepMs(ms)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
