package com.geny.app.presentation.vtuber

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geny.app.core.ui.components.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VTuberViewerScreen(
    viewModel: VTuberViewerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("VTuber Viewer")
                        if (uiState.isConnected) {
                            Icon(
                                Icons.Filled.Circle,
                                null,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(8.dp),
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingOverlay(modifier = Modifier.padding(paddingValues))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar State Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Avatar State",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Emotion display
                    val emotion = uiState.avatarState?.emotion ?: "neutral"
                    val emotionEmoji = getEmotionVisual(emotion)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(getEmotionColor(emotion).copy(alpha = 0.1f))
                            .border(
                                2.dp,
                                getEmotionColor(emotion).copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = emotionEmoji,
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = emotion.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = getEmotionColor(emotion)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // State details
                    uiState.avatarState?.let { state ->
                        StateDetailRow("Motion", state.motionGroup ?: "idle")
                        state.intensity?.let {
                            StateDetailRow("Intensity", String.format("%.1f", it))
                        }
                        state.trigger?.let {
                            StateDetailRow("Trigger", it)
                        }
                    }
                }
            }

            // Touch Interaction
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.TouchApp, null, modifier = Modifier.size(20.dp))
                        Text(
                            text = " Touch Interaction",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HitAreaHead", "HitAreaBody").forEach { area ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.interact(area) },
                                label = { Text(area.removePrefix("HitArea")) }
                            )
                        }
                    }
                }
            }

            // Emotion Control
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEmotions, null, modifier = Modifier.size(20.dp))
                        Text(
                            text = " Emotion Override",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val emotions = listOf(
                        "neutral", "happy", "sad", "angry", "surprised", "thinking", "excited"
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        emotions.forEach { emotion ->
                            FilterChip(
                                selected = uiState.selectedEmotion == emotion,
                                onClick = { viewModel.setEmotion(emotion) },
                                label = {
                                    Text("${getEmotionVisual(emotion)} ${emotion.replaceFirstChar { it.uppercase() }}")
                                }
                            )
                        }
                    }
                }
            }

            // Model Selector
            if (uiState.models.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Model",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = uiState.models.firstOrNull()?.name ?: "Select model",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                uiState.models.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(model.name)
                                                model.description?.let {
                                                    Text(
                                                        text = it,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.assignModel(model.name)
                                            expanded = false
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
}

@Composable
private fun StateDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getEmotionVisual(emotion: String): String = when (emotion.lowercase()) {
    "happy" -> "\uD83D\uDE0A"
    "sad" -> "\uD83D\uDE22"
    "angry" -> "\uD83D\uDE20"
    "surprised" -> "\uD83D\uDE2E"
    "thinking" -> "\uD83E\uDD14"
    "excited" -> "\uD83E\uDD29"
    "neutral" -> "\uD83D\uDE10"
    else -> "\uD83D\uDE36"
}

private fun getEmotionColor(emotion: String): Color = when (emotion.lowercase()) {
    "happy" -> Color(0xFFFFCA28)
    "sad" -> Color(0xFF42A5F5)
    "angry" -> Color(0xFFEF5350)
    "surprised" -> Color(0xFFFF7043)
    "thinking" -> Color(0xFF7E57C2)
    "excited" -> Color(0xFFEC407A)
    else -> Color(0xFF78909C)
}
