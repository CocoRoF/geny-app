package com.geny.app.presentation.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geny.app.core.ui.components.EmptyState
import com.geny.app.core.ui.components.ErrorState
import com.geny.app.core.ui.components.LoadingOverlay
import com.geny.app.domain.model.ChatRoom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onRoomClick: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Rooms") },
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
                Icon(Icons.Filled.Add, "Create Room")
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.rooms.isEmpty() -> {
                LoadingOverlay(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null && uiState.rooms.isEmpty() -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = viewModel::loadRooms,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    if (uiState.rooms.isEmpty()) {
                        EmptyState(
                            title = "No Chat Rooms",
                            subtitle = "Create a room to start broadcasting",
                            icon = Icons.Filled.Chat
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(uiState.rooms, key = { it.id }) { room ->
                                RoomCard(
                                    room = room,
                                    onClick = { onRoomClick(room.id) },
                                    onDelete = { viewModel.deleteRoom(room.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showCreateDialog) {
            CreateRoomDialog(
                roomName = uiState.newRoomName,
                agents = uiState.agents,
                selectedAgentIds = uiState.selectedAgentIds,
                isCreating = uiState.isCreating,
                onRoomNameChanged = viewModel::onRoomNameChanged,
                onAgentToggle = viewModel::toggleAgentSelection,
                onCreate = viewModel::createRoom,
                onDismiss = viewModel::dismissCreateDialog
            )
        }
    }
}

@Composable
private fun RoomCard(
    room: ChatRoom,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${room.sessionIds.size} agents | ${room.messageCount} messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CreateRoomDialog(
    roomName: String,
    agents: List<com.geny.app.domain.model.Agent>,
    selectedAgentIds: Set<String>,
    isCreating: Boolean,
    onRoomNameChanged: (String) -> Unit,
    onAgentToggle: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Create Chat Room") },
        text = {
            Column {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = onRoomNameChanged,
                    label = { Text("Room Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select Agents",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(agents) { agent ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAgentToggle(agent.sessionId) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedAgentIds.contains(agent.sessionId),
                                onCheckedChange = { onAgentToggle(agent.sessionId) },
                                enabled = !isCreating
                            )
                            Text(
                                text = agent.sessionName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCreate,
                enabled = roomName.isNotBlank() && !isCreating
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text("Cancel")
            }
        }
    )
}
