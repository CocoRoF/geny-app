package com.geny.app.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import com.geny.app.core.ui.components.EmptyState
import com.geny.app.core.ui.components.ErrorState
import com.geny.app.core.ui.components.LoadingOverlay
import com.geny.app.core.ui.components.RoleBadge
import com.geny.app.core.ui.components.StatusBadge
import com.geny.app.domain.model.Agent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAgentClick: (String) -> Unit,
    onVTuberClick: ((String) -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Agents") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showCreateDialog,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "Create Agent")
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.agents.isEmpty() -> {
                LoadingOverlay(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null && uiState.agents.isEmpty() -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = viewModel::loadAgents,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    if (uiState.agents.isEmpty()) {
                        EmptyState(
                            title = "No Agents",
                            subtitle = "Create your first agent to get started",
                            icon = Icons.Filled.SmartToy
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = uiState.agents,
                                key = { it.sessionId }
                            ) { agent ->
                                val cliSession = if (agent.isVTuber) {
                                    uiState.allAgents.find {
                                        it.linkedSessionId == agent.sessionId &&
                                                it.sessionType == "cli"
                                    }
                                } else null

                                AgentCard(
                                    agent = agent,
                                    cliSession = cliSession,
                                    onClick = {
                                        if (agent.isVTuber && onVTuberClick != null) {
                                            onVTuberClick(agent.sessionId)
                                        } else {
                                            onAgentClick(agent.sessionId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showCreateDialog) {
            CreateAgentDialog(
                name = uiState.newAgentName,
                model = uiState.newAgentModel,
                role = uiState.newAgentRole,
                isCreating = uiState.isCreating,
                onNameChanged = viewModel::onNewAgentNameChanged,
                onModelChanged = viewModel::onNewAgentModelChanged,
                onRoleChanged = viewModel::onNewAgentRoleChanged,
                onCreate = viewModel::createAgent,
                onDismiss = viewModel::dismissCreateDialog
            )
        }
    }
}

@Composable
private fun AgentCard(
    agent: Agent,
    cliSession: Agent? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = agent.sessionName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cliSession != null) {
                        CliBadge(isRunning = cliSession.status.name == "RUNNING")
                    }
                    StatusBadge(status = agent.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoleBadge(role = agent.role.name)

                agent.model?.let { model ->
                    Text(
                        text = model,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            agent.totalCost?.let { cost ->
                if (cost > 0) {
                    Text(
                        text = "Cost: $${String.format("%.4f", cost)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            agent.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CliBadge(isRunning: Boolean) {
    val bgColor = if (isRunning) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isRunning) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val indicator = if (isRunning) "\u25CF" else "\u25CB"

    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CLI $indicator",
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAgentDialog(
    name: String,
    model: String,
    role: String,
    isCreating: Boolean,
    onNameChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onRoleChanged: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    val roles = listOf("WORKER", "RESEARCHER", "PLANNER", "VTUBER")
    var roleExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Create Agent") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    label = { Text("Agent Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = onModelChanged,
                    label = { Text("Model (optional)") },
                    placeholder = { Text("claude-sonnet-4-5-20250929") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )

                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = it }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        enabled = !isCreating
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        roles.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    onRoleChanged(r)
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCreate,
                enabled = name.isNotBlank() && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .then(Modifier.height(16.dp).width(16.dp)),
                        strokeWidth = 2.dp
                    )
                }
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel")
            }
        }
    )
}
