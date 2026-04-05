package com.geny.app.presentation.vtuber

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geny.app.domain.model.ChatMessage
import com.geny.app.domain.model.MessageType
import kotlin.math.max

/**
 * Semi-transparent chat message overlay for the VTuber viewer.
 * Shows recent messages floating above the Live2D avatar.
 */
@Composable
fun ChatOverlay(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    maxVisibleMessages: Int = 15
) {
    val recentMessages = messages.takeLast(maxVisibleMessages)
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (recentMessages.isNotEmpty()) {
            listState.animateScrollToItem(recentMessages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.heightIn(max = 320.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(recentMessages, key = { it.id }) { message ->
            val index = recentMessages.indexOf(message)
            val fadeAlpha = if (recentMessages.size > 5) {
                val fadeCount = (recentMessages.size * 0.4f).toInt().coerceAtLeast(1)
                if (index < fadeCount) {
                    0.4f + 0.6f * (index.toFloat() / fadeCount)
                } else 1f
            } else 1f

            OverlayMessageBubble(
                message = message,
                modifier = Modifier.graphicsLayer { alpha = fadeAlpha }
            )
        }
    }
}

@Composable
private fun OverlayMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.type == MessageType.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    }

    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val shape = if (isUser) {
        RoundedCornerShape(12.dp, 12.dp, 4.dp, 12.dp)
    } else {
        RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                if (!isUser && message.sessionName != null) {
                    Text(
                        text = message.sessionName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Thinking indicator shown above the chat input while agent is processing.
 */
@Composable
fun ThinkingIndicator(
    agentName: String?,
    thinkingPreview: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Animated dots
            ThinkingDots()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${agentName ?: "Agent"} is thinking...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                thinkingPreview?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingDots() {
    // Simple "..." text that indicates thinking
    Text(
        text = "...",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}
