package com.geny.app.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.geny.app.core.ui.components.LoadingOverlay
import com.geny.app.core.ui.theme.ChatAgent
import com.geny.app.core.ui.theme.ChatSystem
import com.geny.app.core.ui.theme.ChatUser
import com.geny.app.domain.model.ChatMessage
import com.geny.app.core.ui.util.formatTimestamp
import com.geny.app.domain.model.MessageType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    viewModel: ChatRoomViewModel,
    roomName: String = "Chat",
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(roomName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Broadcast progress
            uiState.broadcastProgress?.let { progress ->
                BroadcastProgressBar(progress)
            }

            // Messages
            if (uiState.isLoading) {
                LoadingOverlay(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }
            }

            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.inputMessage,
                    onValueChange = viewModel::onInputChanged,
                    placeholder = { Text("Broadcast message...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    enabled = !uiState.isSending
                )

                IconButton(
                    onClick = viewModel::sendBroadcast,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    enabled = uiState.inputMessage.isNotBlank() && !uiState.isSending
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.type == MessageType.USER
    val isSystem = message.type == MessageType.SYSTEM

    val alignment = when {
        isUser -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    val backgroundColor = when {
        isUser -> ChatUser.copy(alpha = 0.15f)
        isSystem -> ChatSystem.copy(alpha = 0.1f)
        else -> ChatAgent.copy(alpha = 0.08f)
    }

    val shape = when {
        isUser -> RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
        else -> RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser && message.sessionName != null) {
                    Text(
                        text = message.sessionName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Timestamp + duration
                val timeText = buildString {
                    val ts = formatTimestamp(message.timestamp)
                    if (ts.isNotBlank()) append(ts)
                    message.durationMs?.let { ms ->
                        if (isNotBlank()) append(" · ")
                        append("${ms}ms")
                    }
                }
                if (timeText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BroadcastProgressBar(progress: BroadcastProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Broadcasting...",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "${progress.completed}/${progress.total}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = {
                if (progress.total > 0) progress.completed.toFloat() / progress.total else 0f
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // Agent thinking previews
        progress.agentStates.forEach { (_, info) ->
            if (info.status == "executing") {
                Text(
                    text = "${info.sessionName ?: "Agent"}: ${info.thinkingPreview ?: "thinking..."}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
