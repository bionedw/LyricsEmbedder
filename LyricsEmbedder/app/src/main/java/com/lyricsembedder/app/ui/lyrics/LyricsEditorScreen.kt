package com.lyricsembedder.app.ui.lyrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyricsembedder.app.R
import com.lyricsembedder.app.data.remote.AiProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditorScreen(
    state: LyricsUiState,
    timestampStepMs: Long,
    viewModel: LyricsViewModel,
    onBack: () -> Unit
) {
    val track = state.editingTrack
    var showAiDialog by remember { mutableStateOf(false) }

    if (showAiDialog) {
        AiLyricsAssistDialog(
            onDismiss = { showAiDialog = false },
            onRun = { provider, instruction ->
                showAiDialog = false
                viewModel.runAiLyricsCleanup(provider, instruction)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = track?.displayTitle ?: stringResource(R.string.editor_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (track != null) {
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiDialog = true },
                        enabled = !state.isAiProcessing
                    ) {
                        if (state.isAiProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = stringResource(R.string.editor_ai_title))
                        }
                    }
                    Button(
                        onClick = { viewModel.saveEditedLyrics(onSaved = onBack) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.editor_timestamp_shift, timestampStepMs),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { viewModel.shiftEditorTimestamps(-1) }) {
                        Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("-${timestampStepMs}ms")
                    }
                    FilledTonalButton(onClick = { viewModel.shiftEditorTimestamps(1) }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+${timestampStepMs}ms")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.editorText,
                onValueChange = { viewModel.updateEditorText(it) },
                label = { Text(stringResource(R.string.editor_placeholder)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun AiLyricsAssistDialog(
    onDismiss: () -> Unit,
    onRun: (AiProvider, String) -> Unit
) {
    val defaultPrompt = stringResource(R.string.editor_ai_default_instruction)
    var instruction by remember { mutableStateOf(defaultPrompt) }
    var selectedProvider by remember { mutableStateOf(AiProvider.GEMINI) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_ai_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { selectedProvider = AiProvider.GEMINI },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (selectedProvider == AiProvider.GEMINI) "✓ Gemini" else "Gemini")
                    }
                    OutlinedButton(
                        onClick = { selectedProvider = AiProvider.OPEN_ROUTER },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (selectedProvider == AiProvider.OPEN_ROUTER) "✓ OpenRouter" else "OpenRouter")
                    }
                }
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text(stringResource(R.string.editor_ai_instruction_label)) },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onRun(selectedProvider, instruction) }) {
                Text(stringResource(R.string.editor_ai_run))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
