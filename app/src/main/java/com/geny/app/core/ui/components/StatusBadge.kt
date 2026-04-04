package com.geny.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.geny.app.core.ui.theme.StatusError
import com.geny.app.core.ui.theme.StatusIdle
import com.geny.app.core.ui.theme.StatusRunning
import com.geny.app.core.ui.theme.StatusStarting
import com.geny.app.core.ui.theme.StatusStopped
import com.geny.app.domain.model.AgentStatus

@Composable
fun StatusBadge(
    status: AgentStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon, label) = when (status) {
        AgentStatus.RUNNING -> Triple(StatusRunning, Icons.Filled.PlayArrow, "Running")
        AgentStatus.IDLE -> Triple(StatusIdle, Icons.Filled.Pause, "Idle")
        AgentStatus.ERROR -> Triple(StatusError, Icons.Filled.Error, "Error")
        AgentStatus.STOPPED -> Triple(StatusStopped, Icons.Filled.Stop, "Stopped")
        AgentStatus.STARTING -> Triple(StatusStarting, Icons.Filled.Circle, "Starting")
    }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(14.dp),
                tint = color
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.12f)
        ),
        border = AssistChipDefaults.assistChipBorder(
            borderColor = color.copy(alpha = 0.3f),
            enabled = true
        ),
        modifier = modifier
    )
}

@Composable
fun RoleBadge(
    role: String,
    modifier: Modifier = Modifier
) {
    val color = when (role.uppercase()) {
        "WORKER", "DEVELOPER" -> com.geny.app.core.ui.theme.RoleWorker
        "RESEARCHER" -> com.geny.app.core.ui.theme.RoleResearcher
        "PLANNER" -> com.geny.app.core.ui.theme.RolePlanner
        "VTUBER" -> com.geny.app.core.ui.theme.RoleVTuber
        else -> MaterialTheme.colorScheme.primary
    }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text = role.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = color
        ),
        border = AssistChipDefaults.assistChipBorder(
            borderColor = color.copy(alpha = 0.3f),
            enabled = true
        ),
        modifier = modifier
    )
}
