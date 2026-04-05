package com.geny.app.presentation.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geny.app.core.ui.components.ErrorState
import com.geny.app.core.ui.components.LoadingOverlay
import com.geny.app.core.ui.components.RoleBadge
import com.geny.app.core.ui.components.StatusBadge
import com.geny.app.core.ui.theme.StatusError
import com.geny.app.core.ui.theme.StatusRunning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDetailScreen(
    viewModel: AgentDetailViewModel,
    onBack: () -> Unit,
    onVTuberClick: ((String) -> Unit)? = null,
    onMemoryClick: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val isVTuber = uiState.agent?.isVTuber == true
    val tabs = if (isVTuber) listOf("VTuber", "Execute", "Storage", "Memory", "Info")
               else listOf("Execute", "Storage", "Memory", "Info")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = uiState.agent?.sessionName ?: "Agent",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        uiState.agent?.let { agent ->
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(status = agent.status)
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
        when {
            uiState.isLoading -> LoadingOverlay(modifier = Modifier.padding(paddingValues))
            uiState.error != null && uiState.agent == null -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = viewModel::loadAgent,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                Column(modifier = Modifier.padding(paddingValues)) {
                    SecondaryTabRow(selectedTabIndex = uiState.selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = uiState.selectedTab == index,
                                onClick = {
                                    if (isVTuber && index == 0) {
                                        // VTuber tab → navigate to fullscreen viewer
                                        uiState.agent?.sessionId?.let { id ->
                                            onVTuberClick?.invoke(id)
                                        }
                                    } else if (title == "Memory") {
                                        // Memory tab → navigate to memory browser
                                        uiState.agent?.sessionId?.let { id ->
                                            onMemoryClick?.invoke(id)
                                        }
                                    } else {
                                        viewModel.selectTab(index)
                                    }
                                },
                                text = { Text(title) }
                            )
                        }
                    }

                    val contentTab = if (isVTuber) uiState.selectedTab - 1 else uiState.selectedTab
                    when (contentTab) {
                        0 -> ExecutionPanel(
                            prompt = uiState.prompt,
                            isExecuting = uiState.isExecuting,
                            logs = uiState.logs,
                            result = uiState.executionResult,
                            error = uiState.executionError,
                            onPromptChanged = viewModel::onPromptChanged,
                            onExecute = viewModel::executePrompt,
                            onStop = viewModel::stopExecution
                        )
                        1 -> StorageBrowser(
                            files = uiState.storageFiles,
                            isLoading = uiState.isLoadingStorage,
                            selectedPath = uiState.selectedFilePath,
                            fileContent = uiState.selectedFileContent,
                            onFileClick = viewModel::openFile,
                            onClose = viewModel::closeFile,
                            onRefresh = viewModel::loadStorage
                        )
                        // 2 = Memory tab — handled by navigation, show placeholder
                        2 -> {}
                        3 -> AgentInfoPanel(
                            agent = uiState.agent,
                            isEditingPrompt = uiState.isEditingPrompt,
                            editingPromptText = uiState.editingPromptText,
                            isSavingPrompt = uiState.isSavingPrompt,
                            promptSaveError = uiState.promptSaveError,
                            onStartEditing = viewModel::startEditingPrompt,
                            onEditingTextChanged = viewModel::onEditingPromptChanged,
                            onSave = viewModel::saveSystemPrompt,
                            onCancel = viewModel::cancelEditingPrompt
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutionPanel(
    prompt: String,
    isExecuting: Boolean,
    logs: List<LogEntry>,
    result: String?,
    error: String?,
    onPromptChanged: (String) -> Unit,
    onExecute: () -> Unit,
    onStop: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Log output area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(logs) { entry ->
                LogEntryItem(entry)
            }

            result?.let {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            error?.let {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChanged,
                placeholder = { Text("Enter prompt...") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                enabled = !isExecuting
            )

            if (isExecuting) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StatusError.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Filled.Stop,
                        "Stop",
                        tint = StatusError
                    )
                }
            } else {
                IconButton(
                    onClick = onExecute,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    enabled = prompt.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        "Execute",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(entry: LogEntry) {
    val color = when (entry.level) {
        "error" -> StatusError
        "warning" -> com.geny.app.core.ui.theme.StatusStarting
        "info" -> StatusRunning
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        entry.toolName?.let { tool ->
            Text(
                text = "[$tool] ",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            color = color
        )
    }
}

@Composable
fun StorageBrowser(
    files: List<com.geny.app.domain.model.StorageFile>,
    isLoading: Boolean,
    selectedPath: String?,
    fileContent: String?,
    onFileClick: (String) -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit
) {
    if (selectedPath != null) {
        // File viewer
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedPath,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp)
            ) {
                if (fileContent != null) {
                    Text(
                        text = fileContent,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )
                } else {
                    LoadingOverlay()
                }
            }
        }
        return
    }

    if (isLoading) {
        LoadingOverlay()
        return
    }

    if (files.isEmpty()) {
        com.geny.app.core.ui.components.EmptyState(
            title = "No files",
            subtitle = "Storage is empty",
            icon = Icons.Filled.FolderOpen
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(files) { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!file.isDirectory) onFileClick(file.path) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    file.size?.let {
                        Text(
                            text = formatFileSize(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

@Composable
fun AgentInfoPanel(
    agent: com.geny.app.domain.model.Agent?,
    isEditingPrompt: Boolean = false,
    editingPromptText: String = "",
    isSavingPrompt: Boolean = false,
    promptSaveError: String? = null,
    onStartEditing: () -> Unit = {},
    onEditingTextChanged: (String) -> Unit = {},
    onSave: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    if (agent == null) return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { InfoRow("Session ID", agent.sessionId) }
        item { InfoRow("Name", agent.sessionName) }
        item { InfoRow("Status", agent.status.name) }
        item { InfoRow("Role", agent.role.name) }
        agent.model?.let { item { InfoRow("Model", it) } }
        agent.workingDir?.let { item { InfoRow("Working Dir", it) } }
        agent.createdAt?.let { item { InfoRow("Created", it) } }
        agent.totalCost?.let { item { InfoRow("Total Cost", "$${String.format("%.6f", it)}") } }
        agent.maxTurns?.let { item { InfoRow("Max Turns", it.toString()) } }
        agent.maxIterations?.let { item { InfoRow("Max Iterations", it.toString()) } }
        agent.workflowId?.let { item { InfoRow("Workflow", it) } }
        agent.graphName?.let { item { InfoRow("Graph", it) } }
        agent.storagePath?.let { item { InfoRow("Storage Path", it) } }

        // System Prompt with inline editing
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Prompt",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!isEditingPrompt) {
                        IconButton(
                            onClick = onStartEditing,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Edit, "Edit",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                if (isEditingPrompt) {
                    OutlinedTextField(
                        value = editingPromptText,
                        onValueChange = onEditingTextChanged,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 12,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        enabled = !isSavingPrompt
                    )
                    promptSaveError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            enabled = !isSavingPrompt
                        ) {
                            Icon(Icons.Filled.Close, null, Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                        Button(
                            onClick = onSave,
                            enabled = !isSavingPrompt
                        ) {
                            if (isSavingPrompt) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Save, null, Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = agent.systemPrompt ?: "(no system prompt)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.65f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
