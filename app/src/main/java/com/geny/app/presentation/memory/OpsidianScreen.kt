package com.geny.app.presentation.memory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geny.app.core.ui.components.EmptyState
import com.geny.app.core.ui.components.ErrorState
import com.geny.app.core.ui.components.LoadingOverlay
import com.geny.app.core.ui.util.formatTimestamp
import com.geny.app.domain.model.Agent
import com.geny.app.domain.model.MemoryFile
import com.geny.app.domain.model.MemoryFileDetail
import com.geny.app.domain.model.MemoryGraph
import com.geny.app.domain.model.MemorySearchResult
import com.geny.app.domain.model.MemoryStats
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ============================================================================
// Main Screen
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpsidianScreen(
    viewModel: OpsidianViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Editor overlay
    if (state.isEditing) {
        NoteEditorView(
            isNew = state.isCreatingNew,
            title = state.editTitle,
            content = state.editContent,
            category = state.editCategory,
            tags = state.editTags,
            importance = state.editImportance,
            linksTo = state.editLinksTo,
            isSaving = state.isSaving,
            onTitleChanged = viewModel::onEditTitleChanged,
            onContentChanged = viewModel::onEditContentChanged,
            onCategoryChanged = viewModel::onEditCategoryChanged,
            onTagsChanged = viewModel::onEditTagsChanged,
            onImportanceChanged = viewModel::onEditImportanceChanged,
            onLinksToChanged = viewModel::onEditLinksToChanged,
            onSave = viewModel::saveNote,
            onCancel = viewModel::cancelEdit
        )
        return
    }

    // Session selector sheet
    if (state.showSessionSelector) {
        SessionSelectorSheet(
            sessions = state.sessions,
            selectedSessionId = state.selectedSessionId,
            isLoading = state.isLoadingSessions,
            onSelect = viewModel::selectSession,
            onDismiss = viewModel::hideSessionSelector
        )
    }

    // Note info sheet
    if (state.showNoteInfo && state.selectedFile != null) {
        NoteInfoSheet(
            detail = state.selectedFile!!,
            meta = state.selectedFileMeta,
            onDismiss = viewModel::hideNoteInfo,
            onLinkClick = { viewModel.hideNoteInfo(); viewModel.openFile(it) }
        )
    }

    Scaffold(
        topBar = {
            OpsidianTopBar(
                vaultMode = state.vaultMode,
                stats = state.stats,
                selectedSessionId = state.selectedSessionId,
                sessions = state.sessions,
                onBack = {
                    if (state.viewMode == ViewMode.NOTE) viewModel.closeFile()
                    else onBack()
                },
                onVaultSwitch = viewModel::switchVault,
                onShowSessionSelector = viewModel::showSessionSelector,
                onSearch = { viewModel.setViewMode(ViewMode.SEARCH) },
                onCreateNote = viewModel::startCreateNote
            )
        },
        bottomBar = {
            OpsidianBottomBar(
                viewMode = state.viewMode,
                selectedFileName = state.selectedFile?.filename,
                onViewModeChange = viewModel::setViewMode
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.viewMode == ViewMode.FILES) {
                FloatingActionButton(onClick = viewModel::startCreateNote) {
                    Icon(Icons.Filled.Add, "New Note")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state.viewMode) {
                ViewMode.FILES -> FileExplorer(
                    files = state.files,
                    filter = state.fileFilter,
                    expandedCategories = state.expandedCategories,
                    isLoading = state.isLoading,
                    error = state.error,
                    onFilterChanged = viewModel::onFileFilterChanged,
                    onToggleCategory = viewModel::toggleCategory,
                    onFileClick = viewModel::openFile,
                    onRetry = viewModel::loadData
                )
                ViewMode.NOTE -> NoteViewer(
                    detail = state.selectedFile,
                    meta = state.selectedFileMeta,
                    isLoading = state.isLoadingFile,
                    vaultMode = state.vaultMode,
                    onLinkClick = viewModel::openFile,
                    onEdit = viewModel::startEditNote,
                    onDelete = { state.selectedFile?.let { viewModel.deleteNote(it.filename) } },
                    onPromote = { state.selectedFile?.let { viewModel.promoteToGlobal(it.filename) } },
                    onShowInfo = viewModel::toggleNoteInfo
                )
                ViewMode.GRAPH -> GraphView(
                    graph = state.graph,
                    isLoading = state.isLoadingGraph,
                    onNodeClick = viewModel::openFile
                )
                ViewMode.SEARCH -> SearchPanel(
                    query = state.searchQuery,
                    results = state.searchResults,
                    isSearching = state.isSearching,
                    onQueryChanged = viewModel::onSearchQueryChanged,
                    onSearch = viewModel::search,
                    onResultClick = { it?.let { f -> viewModel.openFile(f) } }
                )
            }
        }
    }
}

// ============================================================================
// Top Bar
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpsidianTopBar(
    vaultMode: VaultMode,
    stats: MemoryStats?,
    selectedSessionId: String?,
    sessions: List<Agent>,
    onBack: () -> Unit,
    onVaultSwitch: (VaultMode) -> Unit,
    onShowSessionSelector: () -> Unit,
    onSearch: () -> Unit,
    onCreateNote: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                // Vault switcher chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = vaultMode == VaultMode.USER,
                        onClick = { onVaultSwitch(VaultMode.USER) },
                        label = { Text("My Vault", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Filled.Person, null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = vaultMode == VaultMode.SESSION,
                        onClick = {
                            if (vaultMode == VaultMode.SESSION) onShowSessionSelector()
                            else onVaultSwitch(VaultMode.SESSION)
                        },
                        label = {
                            val sessionName = selectedSessionId?.let { sid ->
                                sessions.find { it.sessionId == sid }?.sessionName?.take(12)
                            }
                            Text(
                                sessionName ?: "Sessions",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.Hub, null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = vaultMode == VaultMode.GLOBAL,
                        onClick = { onVaultSwitch(VaultMode.GLOBAL) },
                        label = { Text("Global", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Filled.Public, null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.height(28.dp)
                    )
                }
                // Stats line
                stats?.let { s ->
                    Text(
                        text = buildString {
                            append("${s.totalFiles} files")
                            if (s.totalLinks > 0) append(" · ${s.totalLinks} links")
                            if (s.totalChars > 0) append(" · ${s.totalChars / 1000}K chars")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "Search") }
        }
    )
}

// ============================================================================
// Bottom Bar
// ============================================================================

@Composable
private fun OpsidianBottomBar(
    viewMode: ViewMode,
    selectedFileName: String?,
    onViewModeChange: (ViewMode) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.FolderOpen, null) },
            label = { Text("Files") },
            selected = viewMode == ViewMode.FILES,
            onClick = { onViewModeChange(ViewMode.FILES) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Hub, null) },
            label = { Text("Graph") },
            selected = viewMode == ViewMode.GRAPH,
            onClick = { onViewModeChange(ViewMode.GRAPH) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Search, null) },
            label = { Text("Search") },
            selected = viewMode == ViewMode.SEARCH,
            onClick = { onViewModeChange(ViewMode.SEARCH) }
        )
    }
}

// ============================================================================
// File Explorer
// ============================================================================

@Composable
private fun FileExplorer(
    files: List<MemoryFile>,
    filter: String,
    expandedCategories: Set<String>,
    isLoading: Boolean,
    error: String?,
    onFilterChanged: (String) -> Unit,
    onToggleCategory: (String) -> Unit,
    onFileClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    when {
        isLoading -> LoadingOverlay()
        error != null -> ErrorState(message = error, onRetry = onRetry)
        files.isEmpty() -> EmptyState(
            title = "No memories",
            subtitle = "Tap + to create your first note",
            icon = Icons.Filled.FolderOpen
        )
        else -> {
            val filterLower = filter.lowercase()
            val filtered = if (filter.isBlank()) files
            else files.filter { f ->
                (f.title ?: f.filename).lowercase().contains(filterLower) ||
                        f.tags.any { it.lowercase().contains(filterLower) }
            }
            val grouped = filtered.groupBy { it.category ?: "root" }
            val categoryOrder = listOf("daily", "topics", "entities", "projects", "insights", "root")

            Column(modifier = Modifier.fillMaxSize()) {
                // Filter bar
                OutlinedTextField(
                    value = filter,
                    onValueChange = onFilterChanged,
                    placeholder = { Text("Filter files...", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (filter.isNotEmpty()) {
                            IconButton(onClick = { onFilterChanged("") }) {
                                Icon(Icons.Filled.Close, "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    textStyle = MaterialTheme.typography.bodySmall
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    categoryOrder.forEach { cat ->
                        val catFiles = grouped[cat] ?: return@forEach
                        if (catFiles.isEmpty()) return@forEach
                        val isExpanded = cat in expandedCategories

                        // Category header
                        item(key = "cat_$cat") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleCategory(cat) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isExpanded) "▼" else "▶",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor(cat))
                                )
                                Text(
                                    text = cat.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = categoryColor(cat),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${catFiles.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Files in category
                        if (isExpanded) {
                            items(catFiles, key = { it.filename }) { file ->
                                FileItem(file = file, onClick = { onFileClick(file.filename) })
                            }
                        }
                    }

                    // Other categories not in order
                    grouped.forEach { (cat, catFiles) ->
                        if (cat !in categoryOrder && catFiles.isNotEmpty()) {
                            item(key = "cat_$cat") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleCategory(cat) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor(cat))
                                    )
                                    Text(
                                        text = cat.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${catFiles.size}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            items(catFiles, key = { it.filename }) { file ->
                                FileItem(file = file, onClick = { onFileClick(file.filename) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItem(file: MemoryFile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 24.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(importanceColor(file.importance))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.title ?: file.filename,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (file.tags.isNotEmpty()) {
                    Text(
                        text = file.tags.take(2).joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1
                    )
                }
            }
        }
        val ts = formatTimestamp(file.modified ?: file.created)
        if (ts.isNotBlank()) {
            Text(
                text = ts,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ============================================================================
// Note Viewer
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteViewer(
    detail: MemoryFileDetail?,
    meta: MemoryFile?,
    isLoading: Boolean,
    vaultMode: VaultMode,
    onLinkClick: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPromote: () -> Unit,
    onShowInfo: () -> Unit
) {
    if (isLoading) { LoadingOverlay(); return }
    if (detail == null) {
        EmptyState(title = "Select a note", subtitle = "Choose from Files or Search", icon = Icons.Filled.FolderOpen)
        return
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Delete \"${detail.title ?: detail.filename}\"?") },
            confirmButton = { Button(onClick = { showDeleteDialog = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(modifier = Modifier.padding(16.dp)) {
            // Category + Title
            meta?.category?.let { cat ->
                Text(
                    text = cat.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = categoryColor(cat)
                )
            }
            Text(
                text = detail.title ?: detail.filename,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Badge row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                meta?.importance?.let { imp ->
                    Badge(text = imp, color = importanceColor(imp))
                }
                meta?.category?.let { Badge(text = it, color = categoryColor(it)) }
                meta?.source?.let { Badge(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                meta?.created?.let {
                    Badge(text = formatTimestamp(it), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
                meta?.charCount?.let {
                    Badge(text = "$it chars", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }

            // Tags
            if (meta != null && meta.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    meta.tags.forEach { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Links
        if (detail.linksTo.isNotEmpty() || detail.linkedFrom.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (detail.linksTo.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("→ ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            detail.linksTo.forEach { link ->
                                Text(
                                    text = link.removeSuffix(".md"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable { onLinkClick(link) }
                                )
                            }
                        }
                    }
                }
                if (detail.linkedFrom.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("← ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            detail.linkedFrom.forEach { link ->
                                Text(
                                    text = link.removeSuffix(".md"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable { onLinkClick(link) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Body — markdown + wikilinks
        val bodyText = detail.body ?: "(empty)"
        val linkColor = MaterialTheme.colorScheme.primary
        val headingColor = MaterialTheme.colorScheme.onSurface
        val annotated = renderMarkdownWithWikilinks(bodyText, linkColor, headingColor)

        @Suppress("DEPRECATION")
        ClickableText(
            text = annotated,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(16.dp),
            onClick = { offset ->
                annotated.getStringAnnotations("wikilink", offset, offset)
                    .firstOrNull()?.let { onLinkClick(it.item) }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            IconButton(onClick = onShowInfo) {
                Icon(Icons.Filled.Info, "Info", tint = MaterialTheme.colorScheme.primary)
            }
            if (vaultMode == VaultMode.SESSION) {
                IconButton(onClick = onPromote) {
                    Icon(Icons.Filled.Public, "Promote", tint = MaterialTheme.colorScheme.tertiary)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// ============================================================================
// Note Info Sheet
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteInfoSheet(
    detail: MemoryFileDetail,
    meta: MemoryFile?,
    onDismiss: () -> Unit,
    onLinkClick: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Properties
            Text("Properties", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            PropertyRow("Category", meta?.category ?: "—")
            PropertyRow("Importance", meta?.importance ?: "—", importanceColor(meta?.importance))
            PropertyRow("Source", meta?.source ?: "—")
            PropertyRow("Size", "${meta?.charCount ?: 0} chars")
            PropertyRow("Created", meta?.created?.let { formatTimestamp(it) } ?: "—")
            PropertyRow("Modified", meta?.modified?.let { formatTimestamp(it) } ?: "—")

            // Outline (headings from body)
            val headings = extractHeadings(detail.body ?: "")
            if (headings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Outline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                headings.forEach { (level, text) ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = ((level - 1) * 12).dp, top = 2.dp)
                    )
                }
            }

            // Tags
            if (meta != null && meta.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    meta.tags.forEach { Text("#$it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                }
            }

            // Links
            if (detail.linksTo.isNotEmpty() || detail.linkedFrom.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Links", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                detail.linksTo.forEach { link ->
                    Text(
                        text = "→ ${link.removeSuffix(".md")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onLinkClick(link) }
                            .padding(vertical = 2.dp)
                    )
                }
                detail.linkedFrom.forEach { link ->
                    Text(
                        text = "← ${link.removeSuffix(".md")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .clickable { onLinkClick(link) }
                            .padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PropertyRow(key: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun extractHeadings(body: String): List<Pair<Int, String>> {
    val result = mutableListOf<Pair<Int, String>>()
    body.split("\n").forEach { line ->
        val match = Regex("^(#{1,6})\\s+(.+)").find(line)
        if (match != null) {
            result.add(match.groupValues[1].length to match.groupValues[2])
        }
    }
    return result
}

// ============================================================================
// Graph View
// ============================================================================

@Composable
private fun GraphView(
    graph: MemoryGraph?,
    isLoading: Boolean,
    onNodeClick: (String) -> Unit
) {
    when {
        isLoading -> LoadingOverlay()
        graph == null || graph.nodes.isEmpty() -> EmptyState(
            title = "No graph data",
            subtitle = "Notes need links to form a graph",
            icon = Icons.Filled.Hub
        )
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                // Stats card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("${graph.nodes.size}", "Nodes", MaterialTheme.colorScheme.primary)
                        StatItem("${graph.edges.size}", "Edges", MaterialTheme.colorScheme.tertiary)
                        val cats = graph.nodes.mapNotNull { it.category }.toSet().size
                        StatItem("$cats", "Categories", MaterialTheme.colorScheme.secondary)
                    }
                }

                // Canvas graph
                val nodePositions = remember(graph) { layoutGraph(graph) }
                var scale by remember { mutableFloatStateOf(1f) }
                var offsetX by remember { mutableFloatStateOf(0f) }
                var offsetY by remember { mutableFloatStateOf(0f) }

                val catColors = mapOf(
                    "daily" to Color(0xFFF59E0B),
                    "topics" to Color(0xFF3B82F6),
                    "entities" to Color(0xFF10B981),
                    "projects" to Color(0xFF8B5CF6),
                    "insights" to Color(0xFFEC4899)
                )
                val defaultNodeColor = Color(0xFF64748B)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.3f, 3f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                        .pointerInput(nodePositions) {
                            detectTapGestures { tapOffset ->
                                val centerX = size.width / 2f + offsetX
                                val centerY = size.height / 2f + offsetY
                                nodePositions.forEach { (nodeId, pos) ->
                                    val nodeX = centerX + pos.first * scale
                                    val nodeY = centerY + pos.second * scale
                                    val dist = sqrt(
                                        (tapOffset.x - nodeX) * (tapOffset.x - nodeX) +
                                                (tapOffset.y - nodeY) * (tapOffset.y - nodeY)
                                    )
                                    if (dist < 30f * scale) {
                                        onNodeClick(nodeId)
                                        return@detectTapGestures
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f + offsetX
                        val cy = size.height / 2f + offsetY

                        // Draw edges
                        graph.edges.forEach { edge ->
                            val from = nodePositions[edge.source]
                            val to = nodePositions[edge.target]
                            if (from != null && to != null) {
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    start = Offset(cx + from.first * scale, cy + from.second * scale),
                                    end = Offset(cx + to.first * scale, cy + to.second * scale),
                                    strokeWidth = 1f * scale
                                )
                            }
                        }

                        // Draw nodes
                        graph.nodes.forEach { node ->
                            val pos = nodePositions[node.id] ?: return@forEach
                            val color = catColors[node.category] ?: defaultNodeColor
                            val radius = when (node.importance) {
                                "critical" -> 14f
                                "high" -> 11f
                                "medium" -> 8f
                                else -> 6f
                            } * scale

                            drawCircle(
                                color = color.copy(alpha = 0.2f),
                                radius = radius + 4f * scale,
                                center = Offset(cx + pos.first * scale, cy + pos.second * scale)
                            )
                            drawCircle(
                                color = color,
                                radius = radius,
                                center = Offset(cx + pos.first * scale, cy + pos.second * scale)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun layoutGraph(graph: MemoryGraph): Map<String, Pair<Float, Float>> {
    if (graph.nodes.isEmpty()) return emptyMap()
    val positions = mutableMapOf<String, Pair<Float, Float>>()
    val n = graph.nodes.size
    // Initial circle layout
    graph.nodes.forEachIndexed { i, node ->
        val angle = 2.0 * Math.PI * i / n
        val r = 120f + n * 2f
        positions[node.id] = Pair(
            (r * cos(angle)).toFloat(),
            (r * sin(angle)).toFloat()
        )
    }
    // Simple force-directed iterations
    repeat(60) {
        val forces = mutableMapOf<String, Pair<Float, Float>>()
        graph.nodes.forEach { forces[it.id] = 0f to 0f }

        // Repulsion between all nodes
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val a = graph.nodes[i]; val b = graph.nodes[j]
                val pa = positions[a.id]!!; val pb = positions[b.id]!!
                val dx = pa.first - pb.first
                val dy = pa.second - pb.second
                val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val force = 5000f / (dist * dist)
                val fx = dx / dist * force; val fy = dy / dist * force
                forces[a.id] = (forces[a.id]!!.first + fx) to (forces[a.id]!!.second + fy)
                forces[b.id] = (forces[b.id]!!.first - fx) to (forces[b.id]!!.second - fy)
            }
        }

        // Attraction along edges
        graph.edges.forEach { edge ->
            val pa = positions[edge.source] ?: return@forEach
            val pb = positions[edge.target] ?: return@forEach
            val dx = pb.first - pa.first
            val dy = pb.second - pa.second
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val force = dist * 0.01f
            val fx = dx / dist * force; val fy = dy / dist * force
            forces[edge.source] = (forces[edge.source]!!.first + fx) to (forces[edge.source]!!.second + fy)
            forces[edge.target] = (forces[edge.target]!!.first - fx) to (forces[edge.target]!!.second - fy)
        }

        // Apply forces
        graph.nodes.forEach { node ->
            val f = forces[node.id]!!
            val p = positions[node.id]!!
            positions[node.id] = (p.first + f.first * 0.3f) to (p.second + f.second * 0.3f)
        }
    }
    return positions
}

// ============================================================================
// Search Panel
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchPanel(
    query: String,
    results: List<MemorySearchResult>,
    isSearching: Boolean,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onResultClick: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = { Text("Search memory notes...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(Icons.Filled.Close, "Clear")
                        }
                    }
                }
            )
            Button(
                onClick = onSearch,
                enabled = query.isNotBlank() && !isSearching
            ) { Text("Search") }
        }

        when {
            isSearching -> LoadingOverlay()
            results.isEmpty() && query.isNotBlank() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No results for \"$query\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            results.isNotEmpty() -> {
                Text(
                    text = "${results.size} result${if (results.size > 1) "s" else ""} for \"$query\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResultClick(result.filename) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    result.importance?.let {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(importanceColor(it))
                                        )
                                    }
                                    Text(
                                        text = result.title ?: result.filename ?: "Untitled",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    result.score?.let {
                                        Badge(
                                            text = "${(it * 100).toInt()}%",
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                                result.snippet?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = it.take(200),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                FlowRow(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    result.category?.let {
                                        Badge(text = it, color = categoryColor(it))
                                    }
                                    result.matchType?.let {
                                        Badge(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    }
                                    result.tags.take(3).forEach { tag ->
                                        Text(
                                            "#$tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Search, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Search across all memory notes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Text + semantic vector search", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

// ============================================================================
// Session Selector Sheet
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionSelectorSheet(
    sessions: List<Agent>,
    selectedSessionId: String?,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Select Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                LoadingOverlay()
            } else if (sessions.isEmpty()) {
                Text("No sessions found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(400.dp)
                ) {
                    items(sessions, key = { it.sessionId }) { agent ->
                        val isSelected = agent.sessionId == selectedSessionId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(agent.sessionId) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = agent.sessionName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = agent.sessionId.take(12),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ============================================================================
// Note Editor
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorView(
    isNew: Boolean,
    title: String,
    content: String,
    category: String,
    tags: String,
    importance: String,
    linksTo: String,
    isSaving: Boolean,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onTagsChanged: (String) -> Unit,
    onImportanceChanged: (String) -> Unit,
    onLinksToChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val categories = listOf("daily", "topics", "entities", "projects", "insights")
    val importanceLevels = listOf("critical", "high", "medium", "low")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, "Cancel") }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = title.isNotBlank() && content.isNotBlank() && !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Save, "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = onTitleChanged,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                enabled = isNew
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = category, onValueChange = {}, readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { onCategoryChanged(cat); categoryExpanded = false })
                        }
                    }
                }

                var importanceExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = importanceExpanded,
                    onExpandedChange = { importanceExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = importance, onValueChange = {}, readOnly = true,
                        label = { Text("Importance") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = importanceExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = importanceExpanded, onDismissRequest = { importanceExpanded = false }) {
                        importanceLevels.forEach { level ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(importanceColor(level)))
                                        Text(level)
                                    }
                                },
                                onClick = { onImportanceChanged(level); importanceExpanded = false }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = tags, onValueChange = onTagsChanged,
                label = { Text("Tags (comma separated)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("e.g. python, architecture") }
            )

            OutlinedTextField(
                value = linksTo, onValueChange = onLinksToChanged,
                label = { Text("Links to (comma separated)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("e.g. topics/design.md") }
            )

            OutlinedTextField(
                value = content, onValueChange = onContentChanged,
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .height(300.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 22.sp)
            )
        }
    }
}

// ============================================================================
// Markdown + Wikilink Renderer
// ============================================================================

private fun renderMarkdownWithWikilinks(
    text: String,
    linkColor: Color,
    headingColor: Color
): AnnotatedString = buildAnnotatedString {
    val lines = text.split("\n")
    for ((lineIndex, line) in lines.withIndex()) {
        if (lineIndex > 0) append("\n")
        val headerMatch = Regex("^(#{1,3})\\s+(.*)").find(line)
        if (headerMatch != null) {
            val level = headerMatch.groupValues[1].length
            val style = when (level) {
                1 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = headingColor)
                2 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = headingColor)
                else -> SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = headingColor)
            }
            withStyle(style) { appendInlineFormatted(headerMatch.groupValues[2], linkColor) }
            continue
        }
        val bulletMatch = Regex("^\\s*[-*]\\s+(.*)").find(line)
        if (bulletMatch != null) {
            append("  \u2022 ")
            appendInlineFormatted(bulletMatch.groupValues[1], linkColor)
            continue
        }
        appendInlineFormatted(line, linkColor)
    }
}

private fun AnnotatedString.Builder.appendInlineFormatted(text: String, linkColor: Color) {
    val pattern = Regex("""(\[\[([^|\]]+?)(?:\|([^\]]+?))?\]\])|(\*\*(.+?)\*\*)|(\*(.+?)\*)|(`([^`]+)`)""")
    var lastIndex = 0
    for (match in pattern.findAll(text)) {
        if (match.range.first > lastIndex) append(text.substring(lastIndex, match.range.first))
        when {
            match.groupValues[1].isNotEmpty() -> {
                val target = match.groupValues[2]
                val alias = match.groupValues[3].ifEmpty { target.removeSuffix(".md") }
                pushStringAnnotation("wikilink", target)
                withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) { append(alias) }
                pop()
            }
            match.groupValues[4].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[5]) }
            match.groupValues[6].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(match.groupValues[7]) }
            match.groupValues[8].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)) { append(match.groupValues[9]) }
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) append(text.substring(lastIndex))
}

// ============================================================================
// Color Helpers
// ============================================================================

@Composable
private fun categoryColor(category: String?): Color {
    return when (category) {
        "daily" -> Color(0xFFF59E0B)
        "topics" -> Color(0xFF3B82F6)
        "entities" -> Color(0xFF10B981)
        "projects" -> Color(0xFF8B5CF6)
        "insights" -> Color(0xFFEC4899)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun importanceColor(importance: String?): Color {
    return when (importance) {
        "critical" -> Color(0xFFEF4444)
        "high" -> Color(0xFFF59E0B)
        "medium" -> Color(0xFF3B82F6)
        "low" -> Color(0xFF64748B)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
