package com.geny.app.presentation.agent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geny.app.data.repository.AgentRepository
import com.geny.app.domain.model.Agent
import com.geny.app.domain.model.ExecutionEvent
import com.geny.app.domain.model.StorageFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogEntry(
    val message: String,
    val level: String? = null,
    val timestamp: String? = null,
    val toolName: String? = null
)

data class AgentDetailUiState(
    val agent: Agent? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Execution
    val prompt: String = "",
    val isExecuting: Boolean = false,
    val logs: List<LogEntry> = emptyList(),
    val executionResult: String? = null,
    val executionError: String? = null,
    // Storage
    val storageFiles: List<StorageFile> = emptyList(),
    val isLoadingStorage: Boolean = false,
    val selectedFileContent: String? = null,
    val selectedFilePath: String? = null,
    // Tab
    val selectedTab: Int = 0
)

@HiltViewModel
class AgentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val agentId: String = savedStateHandle.get<String>("agentId") ?: ""

    private val _uiState = MutableStateFlow(AgentDetailUiState())
    val uiState: StateFlow<AgentDetailUiState> = _uiState.asStateFlow()

    private var sseJob: Job? = null

    init {
        loadAgent()
    }

    fun loadAgent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            agentRepository.getAgent(agentId)
                .onSuccess { agent ->
                    _uiState.value = _uiState.value.copy(
                        agent = agent,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load agent"
                    )
                }
        }
    }

    fun onPromptChanged(prompt: String) {
        _uiState.value = _uiState.value.copy(prompt = prompt)
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
        if (index == 1 && _uiState.value.storageFiles.isEmpty()) {
            loadStorage()
        }
    }

    fun executePrompt() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExecuting = true,
                logs = emptyList(),
                executionResult = null,
                executionError = null
            )

            agentRepository.startExecution(agentId, prompt)
                .onSuccess {
                    subscribeToEvents()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isExecuting = false,
                        executionError = e.message ?: "Execution failed"
                    )
                }
        }
    }

    private fun subscribeToEvents() {
        sseJob?.cancel()
        sseJob = viewModelScope.launch {
            agentRepository.streamExecutionEvents(agentId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isExecuting = false,
                        executionError = e.message ?: "Stream error"
                    )
                }
                .collect { event ->
                    handleEvent(event)
                }
        }
    }

    private fun handleEvent(event: ExecutionEvent) {
        when (event) {
            is ExecutionEvent.Log -> {
                val entry = LogEntry(
                    message = event.message,
                    level = event.level,
                    timestamp = event.timestamp,
                    toolName = event.toolName
                )
                _uiState.value = _uiState.value.copy(
                    logs = _uiState.value.logs + entry
                )
            }
            is ExecutionEvent.Status -> {
                val entry = LogEntry(
                    message = "[STATUS] ${event.status}: ${event.message ?: ""}",
                    level = "info"
                )
                _uiState.value = _uiState.value.copy(
                    logs = _uiState.value.logs + entry
                )
            }
            is ExecutionEvent.Result -> {
                _uiState.value = _uiState.value.copy(
                    executionResult = event.output,
                    executionError = event.error
                )
            }
            is ExecutionEvent.Error -> {
                _uiState.value = _uiState.value.copy(
                    executionError = event.message
                )
            }
            is ExecutionEvent.Done -> {
                _uiState.value = _uiState.value.copy(
                    isExecuting = false,
                    prompt = ""
                )
                sseJob?.cancel()
                loadAgent() // refresh status
            }
            is ExecutionEvent.Heartbeat -> { /* ignore */ }
        }
    }

    fun stopExecution() {
        viewModelScope.launch {
            agentRepository.stopExecution(agentId)
            sseJob?.cancel()
            _uiState.value = _uiState.value.copy(isExecuting = false)
        }
    }

    fun loadStorage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStorage = true)
            agentRepository.listStorage(agentId)
                .onSuccess { files ->
                    _uiState.value = _uiState.value.copy(
                        storageFiles = files,
                        isLoadingStorage = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingStorage = false)
                }
        }
    }

    fun openFile(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedFilePath = path, selectedFileContent = null)
            agentRepository.readFile(agentId, path)
                .onSuccess { content ->
                    _uiState.value = _uiState.value.copy(selectedFileContent = content)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        selectedFileContent = "Error reading file: ${it.message}"
                    )
                }
        }
    }

    fun closeFile() {
        _uiState.value = _uiState.value.copy(selectedFilePath = null, selectedFileContent = null)
    }

    override fun onCleared() {
        super.onCleared()
        sseJob?.cancel()
    }
}
