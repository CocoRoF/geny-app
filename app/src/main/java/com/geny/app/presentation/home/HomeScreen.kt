package com.geny.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geny.app.core.ui.components.EmptyState
import com.geny.app.core.ui.components.LoadingOverlay
import com.geny.app.core.ui.components.StatusBadge
import com.geny.app.core.ui.theme.RoleVTuber
import com.geny.app.domain.model.Agent

/**
 * Home screen — gateway that auto-navigates to the VTuber viewer.
 * Shown briefly while agents load, or as empty state when no VTuber agents exist.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAgentReady: (String) -> Unit,
    onNavigateToAgents: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Auto-navigate to VTuber viewer when agent is selected
    LaunchedEffect(uiState.selectedAgentId, uiState.isLoading) {
        if (!uiState.isLoading && uiState.selectedAgentId != null) {
            onAgentReady(uiState.selectedAgentId!!)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingOverlay()
            uiState.vtuberAgents.isEmpty() -> {
                EmptyState(
                    title = "No VTuber Agents",
                    subtitle = "Create a VTuber agent to get started",
                    icon = Icons.Filled.SmartToy
                )
            }
            // Waiting for navigation
            else -> LoadingOverlay()
        }
    }
}

/**
 * Drawer content for the VTuber-first navigation.
 * Shows VTuber session list + navigation menu.
 */
@Composable
fun AppDrawerContent(
    agents: List<Agent>,
    selectedAgentId: String?,
    onSelectAgent: (String) -> Unit,
    onNavigateToAgents: () -> Unit,
    onNavigateToChatRooms: () -> Unit,
    onNavigateToOpsidian: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Geny",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "VTuber Interaction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // VTuber Sessions section
            if (agents.isNotEmpty()) {
                Text(
                    text = "Sessions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                agents.forEach { agent ->
                    val isSelected = agent.sessionId == selectedAgentId
                    NavigationDrawerItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) RoleVTuber.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = agent.sessionName.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) RoleVTuber
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = agent.sessionName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                StatusBadge(status = agent.status)
                            }
                        },
                        selected = isSelected,
                        onClick = { onSelectAgent(agent.sessionId) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))
            }

            // Navigation menu
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Dashboard, null) },
                label = { Text("All Agents") },
                selected = false,
                onClick = onNavigateToAgents,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.Chat, null) },
                label = { Text("Chat Rooms") },
                selected = false,
                onClick = onNavigateToChatRooms,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            // Opsidian (Memory Browser)
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Description, null) },
                label = { Text("Opsidian") },
                selected = false,
                onClick = onNavigateToOpsidian,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Settings, null) },
                label = { Text("Settings") },
                selected = false,
                onClick = onNavigateToSettings,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
