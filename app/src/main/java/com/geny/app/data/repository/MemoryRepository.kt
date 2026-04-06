package com.geny.app.data.repository

import com.geny.app.data.api.CuratedKnowledgeApi
import com.geny.app.data.api.GlobalMemoryApi
import com.geny.app.data.api.MemoryApi
import com.geny.app.data.api.UserOpsidianApi
import com.geny.app.data.dto.UpdateNoteRequest
import com.geny.app.data.dto.WriteNoteRequest
import com.geny.app.domain.model.MemoryFile
import com.geny.app.domain.model.MemoryFileDetail
import com.geny.app.domain.model.MemoryGraph
import com.geny.app.domain.model.MemoryGraphEdge
import com.geny.app.domain.model.MemoryGraphNode
import com.geny.app.domain.model.MemorySearchResult
import com.geny.app.domain.model.MemoryStats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val memoryApi: MemoryApi,
    private val globalMemoryApi: GlobalMemoryApi,
    private val userOpsidianApi: UserOpsidianApi,
    private val curatedKnowledgeApi: CuratedKnowledgeApi
) {
    // ========================================================================
    // Session Memory
    // ========================================================================

    suspend fun getMemoryFiles(agentId: String): Result<List<MemoryFile>> = runCatching {
        val response = memoryApi.getMemoryIndex(agentId)
        response.index?.files?.values?.map { it.toDomain() } ?: emptyList()
    }

    suspend fun getMemoryStats(agentId: String): Result<MemoryStats> = runCatching {
        val response = memoryApi.getMemoryIndex(agentId)
        val stats = response.stats
        MemoryStats(
            totalFiles = stats?.totalFiles ?: 0,
            longTermEntries = stats?.longTermEntries ?: 0,
            shortTermEntries = stats?.shortTermEntries ?: 0,
            lastWrite = stats?.lastWrite,
            categories = stats?.categories ?: emptyMap(),
            totalTags = stats?.totalTags ?: 0,
            totalChars = stats?.totalChars ?: 0,
            totalLinks = stats?.totalLinks ?: 0
        )
    }

    suspend fun getTags(agentId: String): Result<Map<String, Int>> = runCatching {
        memoryApi.getMemoryTags(agentId).tags
    }

    suspend fun getGraph(agentId: String): Result<MemoryGraph> = runCatching {
        val response = memoryApi.getMemoryGraph(agentId)
        MemoryGraph(
            nodes = response.nodes?.map { dto ->
                MemoryGraphNode(
                    id = dto.id,
                    label = dto.label,
                    category = dto.category,
                    importance = dto.importance,
                    linkCount = dto.linkCount ?: 0
                )
            } ?: emptyList(),
            edges = response.edges?.map { dto ->
                MemoryGraphEdge(source = dto.source, target = dto.target)
            } ?: emptyList()
        )
    }

    suspend fun search(agentId: String, query: String): Result<List<MemorySearchResult>> = runCatching {
        memoryApi.searchMemory(agentId, query).results.map { dto ->
            MemorySearchResult(
                filename = dto.entry?.filename,
                title = dto.entry?.title,
                snippet = dto.snippet,
                score = dto.score,
                category = dto.entry?.category,
                content = dto.entry?.content,
                matchType = dto.matchType,
                importance = dto.entry?.importance,
                tags = dto.entry?.tags ?: emptyList()
            )
        }
    }

    suspend fun getFile(agentId: String, filename: String): Result<MemoryFileDetail> = runCatching {
        val response = memoryApi.getMemoryFile(agentId, filename)
        MemoryFileDetail(
            filename = response.filename,
            title = response.title,
            body = response.body,
            raw = response.raw,
            linksTo = response.linksTo ?: emptyList(),
            linkedFrom = response.linkedFrom ?: emptyList(),
            metadata = response.metadata ?: emptyMap()
        )
    }

    suspend fun createNote(
        agentId: String,
        title: String,
        content: String,
        category: String = "topics",
        tags: List<String> = emptyList(),
        importance: String = "medium"
    ): Result<String> = runCatching {
        val response = memoryApi.createMemoryFile(
            agentId,
            WriteNoteRequest(
                title = title,
                content = content,
                category = category,
                tags = tags,
                importance = importance
            )
        )
        response["filename"] ?: ""
    }

    suspend fun updateNote(
        agentId: String,
        filename: String,
        content: String? = null,
        tags: List<String>? = null,
        importance: String? = null
    ): Result<Unit> = runCatching {
        memoryApi.updateMemoryFile(
            agentId,
            filename,
            UpdateNoteRequest(content = content, tags = tags, importance = importance)
        )
    }

    suspend fun deleteNote(agentId: String, filename: String): Result<Unit> = runCatching {
        memoryApi.deleteMemoryFile(agentId, filename)
    }

    suspend fun promoteToGlobal(agentId: String, filename: String): Result<String> = runCatching {
        val response = memoryApi.promoteToGlobal(agentId, mapOf("filename" to filename))
        response["global_filename"] ?: ""
    }

    suspend fun reindex(agentId: String): Result<Unit> = runCatching {
        memoryApi.reindex(agentId)
    }

    // ========================================================================
    // Global Memory
    // ========================================================================

    suspend fun getGlobalFiles(): Result<List<MemoryFile>> = runCatching {
        val response = globalMemoryApi.getGlobalIndex()
        response.index?.files?.values?.map { it.toDomain() } ?: emptyList()
    }

    suspend fun getGlobalStats(): Result<MemoryStats> = runCatching {
        val response = globalMemoryApi.getGlobalIndex()
        val stats = response.stats
        MemoryStats(
            totalFiles = stats?.totalFiles ?: 0,
            longTermEntries = stats?.longTermEntries ?: 0,
            shortTermEntries = stats?.shortTermEntries ?: 0,
            lastWrite = stats?.lastWrite,
            categories = stats?.categories ?: emptyMap(),
            totalTags = stats?.totalTags ?: 0,
            totalChars = stats?.totalChars ?: 0,
            totalLinks = stats?.totalLinks ?: 0
        )
    }

    suspend fun getGlobalFile(filename: String): Result<MemoryFileDetail> = runCatching {
        val response = globalMemoryApi.getGlobalFile(filename)
        MemoryFileDetail(
            filename = response.filename,
            title = response.title,
            body = response.body,
            raw = response.raw,
            linksTo = response.linksTo ?: emptyList(),
            linkedFrom = response.linkedFrom ?: emptyList(),
            metadata = response.metadata ?: emptyMap()
        )
    }

    suspend fun searchGlobal(query: String): Result<List<MemorySearchResult>> = runCatching {
        globalMemoryApi.searchGlobal(query).results.map { dto ->
            MemorySearchResult(
                filename = dto.entry?.filename,
                title = dto.entry?.title,
                snippet = dto.snippet,
                score = dto.score,
                category = dto.entry?.category,
                content = dto.entry?.content,
                matchType = dto.matchType,
                importance = dto.entry?.importance,
                tags = dto.entry?.tags ?: emptyList()
            )
        }
    }

    suspend fun createGlobalNote(
        title: String,
        content: String,
        category: String = "topics",
        tags: List<String> = emptyList(),
        importance: String = "medium"
    ): Result<String> = runCatching {
        val response = globalMemoryApi.createGlobalFile(
            WriteNoteRequest(
                title = title,
                content = content,
                category = category,
                tags = tags,
                importance = importance
            )
        )
        response["filename"] ?: ""
    }

    suspend fun updateGlobalNote(
        filename: String,
        content: String? = null,
        tags: List<String>? = null,
        importance: String? = null
    ): Result<Unit> = runCatching {
        globalMemoryApi.updateGlobalFile(
            filename,
            UpdateNoteRequest(content = content, tags = tags, importance = importance)
        )
    }

    suspend fun deleteGlobalNote(filename: String): Result<Unit> = runCatching {
        globalMemoryApi.deleteGlobalFile(filename)
    }

    // ========================================================================
    // User Opsidian (personal vault)
    // ========================================================================

    suspend fun getUserFiles(): Result<List<MemoryFile>> = runCatching {
        val response = userOpsidianApi.getIndex()
        response.index?.files?.values?.map { it.toDomain() } ?: emptyList()
    }

    suspend fun getUserStats(): Result<MemoryStats> = runCatching {
        val response = userOpsidianApi.getIndex()
        val stats = response.stats
        MemoryStats(
            totalFiles = stats?.totalFiles ?: 0,
            longTermEntries = stats?.longTermEntries ?: 0,
            shortTermEntries = stats?.shortTermEntries ?: 0,
            lastWrite = stats?.lastWrite,
            categories = stats?.categories ?: emptyMap(),
            totalTags = stats?.totalTags ?: 0,
            totalChars = stats?.totalChars ?: 0,
            totalLinks = stats?.totalLinks ?: 0
        )
    }

    suspend fun getUserTags(): Result<Map<String, Int>> = runCatching {
        userOpsidianApi.getTags().tags
    }

    suspend fun getUserGraph(): Result<MemoryGraph> = runCatching {
        val response = userOpsidianApi.getGraph()
        MemoryGraph(
            nodes = response.nodes?.map { dto ->
                MemoryGraphNode(
                    id = dto.id,
                    label = dto.label,
                    category = dto.category,
                    importance = dto.importance,
                    linkCount = dto.linkCount ?: 0
                )
            } ?: emptyList(),
            edges = response.edges?.map { dto ->
                MemoryGraphEdge(source = dto.source, target = dto.target)
            } ?: emptyList()
        )
    }

    suspend fun searchUser(query: String): Result<List<MemorySearchResult>> = runCatching {
        userOpsidianApi.search(query).results.map { dto ->
            MemorySearchResult(
                filename = dto.entry?.filename,
                title = dto.entry?.title,
                snippet = dto.snippet,
                score = dto.score,
                category = dto.entry?.category,
                content = dto.entry?.content,
                matchType = dto.matchType,
                importance = dto.entry?.importance,
                tags = dto.entry?.tags ?: emptyList()
            )
        }
    }

    suspend fun getUserFile(filename: String): Result<MemoryFileDetail> = runCatching {
        val response = userOpsidianApi.getFile(filename)
        MemoryFileDetail(
            filename = response.filename,
            title = response.title,
            body = response.body,
            raw = response.raw,
            linksTo = response.linksTo ?: emptyList(),
            linkedFrom = response.linkedFrom ?: emptyList(),
            metadata = response.metadata ?: emptyMap()
        )
    }

    suspend fun createUserNote(
        title: String,
        content: String,
        category: String = "topics",
        tags: List<String> = emptyList(),
        importance: String = "medium"
    ): Result<String> = runCatching {
        val response = userOpsidianApi.createFile(
            WriteNoteRequest(
                title = title,
                content = content,
                category = category,
                tags = tags,
                importance = importance
            )
        )
        response["filename"] ?: ""
    }

    suspend fun updateUserNote(
        filename: String,
        content: String? = null,
        tags: List<String>? = null,
        importance: String? = null
    ): Result<Unit> = runCatching {
        userOpsidianApi.updateFile(
            filename,
            UpdateNoteRequest(content = content, tags = tags, importance = importance)
        )
    }

    suspend fun deleteUserNote(filename: String): Result<Unit> = runCatching {
        userOpsidianApi.deleteFile(filename)
    }

    // ========================================================================
    // Curated Knowledge
    // ========================================================================

    suspend fun getCuratedFiles(): Result<List<MemoryFile>> = runCatching {
        val response = curatedKnowledgeApi.getIndex()
        response.index?.files?.values?.map { it.toDomain() } ?: emptyList()
    }

    suspend fun getCuratedStats(): Result<MemoryStats> = runCatching {
        val response = curatedKnowledgeApi.getIndex()
        val stats = response.stats
        MemoryStats(
            totalFiles = stats?.totalFiles ?: 0,
            longTermEntries = stats?.longTermEntries ?: 0,
            shortTermEntries = stats?.shortTermEntries ?: 0,
            lastWrite = stats?.lastWrite,
            categories = stats?.categories ?: emptyMap(),
            totalTags = stats?.totalTags ?: 0,
            totalChars = stats?.totalChars ?: 0,
            totalLinks = stats?.totalLinks ?: 0
        )
    }

    suspend fun getCuratedTags(): Result<Map<String, Int>> = runCatching {
        curatedKnowledgeApi.getTags().tags
    }

    suspend fun getCuratedGraph(): Result<MemoryGraph> = runCatching {
        val response = curatedKnowledgeApi.getGraph()
        MemoryGraph(
            nodes = response.nodes?.map { dto ->
                MemoryGraphNode(
                    id = dto.id,
                    label = dto.label,
                    category = dto.category,
                    importance = dto.importance,
                    linkCount = dto.linkCount ?: 0
                )
            } ?: emptyList(),
            edges = response.edges?.map { dto ->
                MemoryGraphEdge(source = dto.source, target = dto.target)
            } ?: emptyList()
        )
    }

    suspend fun searchCurated(query: String): Result<List<MemorySearchResult>> = runCatching {
        curatedKnowledgeApi.search(query).results.map { dto ->
            MemorySearchResult(
                filename = dto.entry?.filename,
                title = dto.entry?.title,
                snippet = dto.snippet,
                score = dto.score,
                category = dto.entry?.category,
                content = dto.entry?.content,
                matchType = dto.matchType,
                importance = dto.entry?.importance,
                tags = dto.entry?.tags ?: emptyList()
            )
        }
    }

    suspend fun getCuratedFile(filename: String): Result<MemoryFileDetail> = runCatching {
        val response = curatedKnowledgeApi.getFile(filename)
        MemoryFileDetail(
            filename = response.filename,
            title = response.title,
            body = response.body,
            raw = response.raw,
            linksTo = response.linksTo ?: emptyList(),
            linkedFrom = response.linkedFrom ?: emptyList(),
            metadata = response.metadata ?: emptyMap()
        )
    }

    suspend fun createCuratedNote(
        title: String,
        content: String,
        category: String = "topics",
        tags: List<String> = emptyList(),
        importance: String = "medium"
    ): Result<String> = runCatching {
        val response = curatedKnowledgeApi.createFile(
            WriteNoteRequest(
                title = title,
                content = content,
                category = category,
                tags = tags,
                importance = importance
            )
        )
        response["filename"] ?: ""
    }

    suspend fun updateCuratedNote(
        filename: String,
        content: String? = null,
        tags: List<String>? = null,
        importance: String? = null
    ): Result<Unit> = runCatching {
        curatedKnowledgeApi.updateFile(
            filename,
            UpdateNoteRequest(content = content, tags = tags, importance = importance)
        )
    }

    suspend fun deleteCuratedNote(filename: String): Result<Unit> = runCatching {
        curatedKnowledgeApi.deleteFile(filename)
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private fun com.geny.app.data.dto.MemoryFileMetaDto.toDomain() = MemoryFile(
        filename = filename,
        title = title,
        category = category,
        tags = tags ?: emptyList(),
        importance = importance,
        created = created,
        modified = modified,
        charCount = charCount,
        summary = summary,
        linksTo = linksTo ?: emptyList(),
        linkedFrom = linkedFrom ?: emptyList(),
        source = source
    )
}
