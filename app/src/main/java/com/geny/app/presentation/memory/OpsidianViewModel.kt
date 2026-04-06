package com.geny.app.presentation.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geny.app.data.repository.AgentRepository
import com.geny.app.data.repository.MemoryRepository
import com.geny.app.domain.model.Agent
import com.geny.app.domain.model.MemoryFile
import com.geny.app.domain.model.MemoryFileDetail
import com.geny.app.domain.model.MemoryGraph
import com.geny.app.domain.model.MemorySearchResult
import com.geny.app.domain.model.MemoryStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VaultMode { USER, CURATED, SESSION }
enum class ViewMode { FILES, NOTE, GRAPH, SEARCH }

data class OpsidianUiState(
    // Vault
    val vaultMode: VaultMode = VaultMode.USER,
    val sessions: List<Agent> = emptyList(),
    val selectedSessionId: String? = null,
    val isLoadingSessions: Boolean = false,

    // Data
    val files: List<MemoryFile> = emptyList(),
    val stats: MemoryStats? = null,
    val tags: Map<String, Int> = emptyMap(),
    val graph: MemoryGraph? = null,

    // View
    val viewMode: ViewMode = ViewMode.FILES,
    val selectedFile: MemoryFileDetail? = null,
    val selectedFileMeta: MemoryFile? = null,

    // Search
    val searchQuery: String = "",
    val searchResults: List<MemorySearchResult> = emptyList(),
    val isSearching: Boolean = false,

    // Editor
    val isEditing: Boolean = false,
    val isCreatingNew: Boolean = false,
    val editTitle: String = "",
    val editContent: String = "",
    val editCategory: String = "topics",
    val editTags: String = "",
    val editImportance: String = "medium",
    val editLinksTo: String = "",
    val isSaving: Boolean = false,

    // Loading
    val isLoading: Boolean = true,
    val isLoadingFile: Boolean = false,
    val isLoadingGraph: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null,

    // UI
    val showSessionSelector: Boolean = false,
    val showNoteInfo: Boolean = false,

    // Filter
    val fileFilter: String = "",
    val expandedCategories: Set<String> = setOf("daily", "topics", "entities", "projects", "insights", "decisions", "reference", "root")
)

@HiltViewModel
class OpsidianViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpsidianUiState())
    val uiState: StateFlow<OpsidianUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadSessions()
    }

    // ========================================================================
    // Vault Switching
    // ========================================================================

    fun switchVault(mode: VaultMode) {
        if (_uiState.value.vaultMode == mode) return
        _uiState.value = _uiState.value.copy(
            vaultMode = mode,
            viewMode = ViewMode.FILES,
            selectedFile = null,
            selectedFileMeta = null,
            files = emptyList(),
            stats = null,
            tags = emptyMap(),
            graph = null,
            searchResults = emptyList(),
            searchQuery = "",
            error = null
        )
        if (mode == VaultMode.SESSION && _uiState.value.selectedSessionId == null) {
            _uiState.value = _uiState.value.copy(showSessionSelector = true, isLoading = false)
        } else {
            loadData()
        }
    }

    fun selectSession(sessionId: String) {
        _uiState.value = _uiState.value.copy(
            selectedSessionId = sessionId,
            showSessionSelector = false
        )
        loadData()
    }

    fun showSessionSelector() {
        _uiState.value = _uiState.value.copy(showSessionSelector = true)
    }

    fun hideSessionSelector() {
        _uiState.value = _uiState.value.copy(showSessionSelector = false)
    }

    // ========================================================================
    // Load Data
    // ========================================================================

    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSessions = true)
            agentRepository.listAgents()
                .onSuccess { agents ->
                    // Filter out CLI sessions bound to a VTuber (they share memory)
                    val visible = agents.filter { !it.isLinkedCli && !it.isDeleted }
                    _uiState.value = _uiState.value.copy(
                        sessions = visible,
                        isLoadingSessions = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingSessions = false)
                }
        }
    }

    fun loadData() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load files
            val filesResult = when (state.vaultMode) {
                VaultMode.USER -> memoryRepository.getUserFiles()
                VaultMode.CURATED -> memoryRepository.getCuratedFiles()
                VaultMode.SESSION -> {
                    val sid = state.selectedSessionId ?: return@launch
                    memoryRepository.getMemoryFiles(sid)
                }
            }
            filesResult
                .onSuccess { files ->
                    _uiState.value = _uiState.value.copy(files = files, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load"
                    )
                }

            // Load stats
            val statsResult = when (state.vaultMode) {
                VaultMode.USER -> memoryRepository.getUserStats()
                VaultMode.CURATED -> memoryRepository.getCuratedStats()
                VaultMode.SESSION -> {
                    val sid = state.selectedSessionId ?: return@launch
                    memoryRepository.getMemoryStats(sid)
                }
            }
            statsResult.onSuccess { stats ->
                _uiState.value = _uiState.value.copy(stats = stats)
            }

            // Load tags (not available for global)
            when (state.vaultMode) {
                VaultMode.USER -> memoryRepository.getUserTags().onSuccess { tags ->
                    _uiState.value = _uiState.value.copy(tags = tags)
                }
                VaultMode.SESSION -> {
                    val sid = state.selectedSessionId ?: return@launch
                    memoryRepository.getTags(sid).onSuccess { tags ->
                        _uiState.value = _uiState.value.copy(tags = tags)
                    }
                }
                VaultMode.CURATED -> memoryRepository.getCuratedTags().onSuccess { tags ->
                    _uiState.value = _uiState.value.copy(tags = tags)
                }
            }
        }
    }

    // ========================================================================
    // View Mode
    // ========================================================================

    fun setViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
        if (mode == ViewMode.GRAPH && _uiState.value.graph == null) {
            loadGraph()
        }
    }

    // ========================================================================
    // Graph
    // ========================================================================

    private fun loadGraph() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingGraph = true)
            val result = when (state.vaultMode) {
                VaultMode.USER -> memoryRepository.getUserGraph()
                VaultMode.SESSION -> {
                    val sid = state.selectedSessionId
                        ?: return@launch
                    memoryRepository.getGraph(sid)
                }
                VaultMode.CURATED -> memoryRepository.getCuratedGraph()
            }
            result
                .onSuccess { graph ->
                    _uiState.value = _uiState.value.copy(graph = graph, isLoadingGraph = false)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingGraph = false)
                }
        }
    }

    // ========================================================================
    // File Navigation
    // ========================================================================

    fun openFile(filename: String) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingFile = true, viewMode = ViewMode.NOTE)
            val result = when (state.vaultMode) {
                VaultMode.USER -> memoryRepository.getUserFile(filename)
                VaultMode.CURATED -> memoryRepository.getCuratedFile(filename)
                VaultMode.SESSION -> {
                    val sid = state.selectedSessionId ?: return@launch
                    memoryRepository.getFile(sid, filename)
                }
            }
            result
                .onSuccess { detail ->
                    val meta = _uiState.value.files.find { it.filename == filename }
                    _uiState.value = _uiState.value.copy(
                        selectedFile = detail,
                        selectedFileMeta = meta,
                        isLoadingFile = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoadingFile = false,
                        snackbarMessage = "Failed to load: ${it.message}"
                    )
                }
        }
    }

    fun closeFile() {
        _uiState.value = _uiState.value.copy(
            selectedFile = null,
            selectedFileMeta = null,
            viewMode = ViewMode.FILES,
            isEditing = false,
            isCreatingNew = false
        )
    }

    fun toggleNoteInfo() {
        _uiState.value = _uiState.value.copy(showNoteInfo = !_uiState.value.showNoteInfo)
    }

    fun hideNoteInfo() {
        _uiState.value = _uiState.value.copy(showNoteInfo = false)
    }

    // ========================================================================
    // File Filter
    // ========================================================================

    fun onFileFilterChanged(filter: String) {
        _uiState.value = _uiState.value.copy(fileFilter = filter)
    }

    fun toggleCategory(category: String) {
        val current = _uiState.value.expandedCategories
        val next = if (category in current) current - category else current + category
        _uiState.value = _uiState.value.copy(expandedCategories = next)
    }

    // ========================================================================
    // Search
    // ========================================================================

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun search() {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val result = when (state.vaultMode) {
                VaultMode.USER -> memoryRepository.searchUser(query)
                VaultMode.CURATED -> memoryRepository.searchCurated(query)
                VaultMode.SESSION -> {
                    val sid = state.selectedSessionId ?: return@launch
                    memoryRepository.search(sid, query)
                }
            }
            result
                .onSuccess { results ->
                    _uiState.value = _uiState.value.copy(searchResults = results, isSearching = false)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSearching = false)
                }
        }
    }

    // ========================================================================
    // Create / Edit / Delete
    // ========================================================================

    fun startCreateNote() {
        _uiState.value = _uiState.value.copy(
            isCreatingNew = true,
            isEditing = true,
            editTitle = "",
            editContent = "",
            editCategory = "topics",
            editTags = "",
            editImportance = "medium",
            editLinksTo = ""
        )
    }

    fun startEditNote() {
        val detail = _uiState.value.selectedFile ?: return
        val meta = _uiState.value.selectedFileMeta
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            isCreatingNew = false,
            editTitle = detail.title ?: detail.filename,
            editContent = detail.body ?: "",
            editCategory = meta?.category ?: "topics",
            editTags = meta?.tags?.joinToString(", ") ?: "",
            editImportance = meta?.importance ?: "medium",
            editLinksTo = detail.linksTo.joinToString(", ")
        )
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(
            isEditing = false,
            isCreatingNew = if (_uiState.value.isCreatingNew) false else _uiState.value.isCreatingNew
        )
    }

    fun onEditTitleChanged(v: String) { _uiState.value = _uiState.value.copy(editTitle = v) }
    fun onEditContentChanged(v: String) { _uiState.value = _uiState.value.copy(editContent = v) }
    fun onEditCategoryChanged(v: String) { _uiState.value = _uiState.value.copy(editCategory = v) }
    fun onEditTagsChanged(v: String) { _uiState.value = _uiState.value.copy(editTags = v) }
    fun onEditImportanceChanged(v: String) { _uiState.value = _uiState.value.copy(editImportance = v) }
    fun onEditLinksToChanged(v: String) { _uiState.value = _uiState.value.copy(editLinksTo = v) }

    fun saveNote() {
        val state = _uiState.value
        if (state.editTitle.isBlank() || state.editContent.isBlank()) return
        val tags = state.editTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            if (state.isCreatingNew) {
                val createResult = when (state.vaultMode) {
                    VaultMode.USER -> memoryRepository.createUserNote(
                        title = state.editTitle, content = state.editContent,
                        category = state.editCategory, tags = tags, importance = state.editImportance
                    )
                    VaultMode.CURATED -> memoryRepository.createCuratedNote(
                        title = state.editTitle, content = state.editContent,
                        category = state.editCategory, tags = tags, importance = state.editImportance
                    )
                    VaultMode.SESSION -> {
                        val sid = state.selectedSessionId ?: return@launch
                        memoryRepository.createNote(
                            agentId = sid, title = state.editTitle, content = state.editContent,
                            category = state.editCategory, tags = tags, importance = state.editImportance
                        )
                    }
                }
                createResult.onSuccess { filename ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false, isEditing = false, isCreatingNew = false,
                        snackbarMessage = "Note created"
                    )
                    loadData()
                    openFile(filename)
                }.onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false, snackbarMessage = "Failed: ${it.message}"
                    )
                }
            } else {
                val filename = state.selectedFile?.filename ?: return@launch
                val updateResult = when (state.vaultMode) {
                    VaultMode.USER -> memoryRepository.updateUserNote(
                        filename = filename, content = state.editContent,
                        tags = tags, importance = state.editImportance
                    )
                    VaultMode.CURATED -> memoryRepository.updateCuratedNote(
                        filename = filename, content = state.editContent,
                        tags = tags, importance = state.editImportance
                    )
                    VaultMode.SESSION -> {
                        val sid = state.selectedSessionId ?: return@launch
                        memoryRepository.updateNote(
                            agentId = sid, filename = filename, content = state.editContent,
                            tags = tags, importance = state.editImportance
                        )
                    }
                }
                updateResult.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false, isEditing = false, snackbarMessage = "Note updated"
                    )
                    openFile(filename)
                    loadData()
                }.onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false, snackbarMessage = "Failed: ${it.message}"
                    )
                }
            }
        }
    }

    fun deleteNote(filename: String) {
        val state = _uiState.value
        viewModelScope.launch {
            val result = when (state.vaultMode) {
                VaultMode.USER -> memoryRepository.deleteUserNote(filename)
                VaultMode.CURATED -> memoryRepository.deleteCuratedNote(filename)
                VaultMode.SESSION -> {
                    val sid = state.selectedSessionId ?: return@launch
                    memoryRepository.deleteNote(sid, filename)
                }
            }
            result
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        selectedFile = null, selectedFileMeta = null,
                        viewMode = ViewMode.FILES, snackbarMessage = "Note deleted"
                    )
                    loadData()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Delete failed: ${it.message}")
                }
        }
    }

    fun promoteToGlobal(filename: String) {
        val state = _uiState.value
        if (state.vaultMode != VaultMode.SESSION) return
        val sid = state.selectedSessionId ?: return
        viewModelScope.launch {
            memoryRepository.promoteToGlobal(sid, filename)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Promoted to global")
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Promote failed: ${it.message}")
                }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
