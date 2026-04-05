package com.geny.app.data.dto

import com.google.gson.annotations.SerializedName

// ============================================================================
// Index & Stats
// ============================================================================

data class MemoryIndexResponse(
    val index: MemoryIndexDto?,
    val stats: MemoryStatsDto?
)

data class MemoryIndexDto(
    val files: Map<String, MemoryFileMetaDto>?,
    @SerializedName("tag_map") val tagMap: Map<String, List<String>>?,
    @SerializedName("total_files") val totalFiles: Int?,
    @SerializedName("total_chars") val totalChars: Int?
)

data class MemoryFileMetaDto(
    val filename: String,
    val title: String?,
    val category: String?,
    val tags: List<String>?,
    val importance: String?,
    val created: String?,
    val modified: String?,
    val source: String?,
    @SerializedName("char_count") val charCount: Int?,
    val summary: String?,
    @SerializedName("links_to") val linksTo: List<String>?,
    @SerializedName("linked_from") val linkedFrom: List<String>?
)

data class MemoryStatsDto(
    @SerializedName("long_term_entries") val longTermEntries: Int?,
    @SerializedName("short_term_entries") val shortTermEntries: Int?,
    @SerializedName("total_files") val totalFiles: Int?,
    @SerializedName("last_write") val lastWrite: String?,
    val categories: Map<String, Int>?,
    @SerializedName("total_tags") val totalTags: Int?,
    @SerializedName("total_chars") val totalChars: Int?,
    @SerializedName("total_links") val totalLinks: Int?
)

data class MemoryTagsResponse(
    val tags: Map<String, Int>
)

// ============================================================================
// Graph
// ============================================================================

data class MemoryGraphResponse(
    val nodes: List<MemoryGraphNodeDto>?,
    val edges: List<MemoryGraphEdgeDto>?
)

data class MemoryGraphNodeDto(
    val id: String,
    val label: String?,
    val category: String?,
    val importance: String?,
    @SerializedName("link_count") val linkCount: Int?
)

data class MemoryGraphEdgeDto(
    val source: String,
    val target: String
)

// ============================================================================
// Search
// ============================================================================

data class MemorySearchResponse(
    val query: String,
    val results: List<MemorySearchResultDto>,
    val total: Int?
)

data class MemorySearchResultDto(
    val entry: MemoryEntryDto?,
    val score: Float?,
    val snippet: String?,
    @SerializedName("match_type") val matchType: String?
)

data class MemoryEntryDto(
    val source: String?,
    val content: String?,
    val timestamp: String?,
    val filename: String?,
    val title: String?,
    val category: String?,
    val tags: List<String>?,
    val importance: String?,
    val summary: String?,
    @SerializedName("char_count") val charCount: Int?
)

// ============================================================================
// File Detail
// ============================================================================

data class MemoryFileResponse(
    val filename: String,
    val title: String?,
    val metadata: Map<String, Any?>?,
    val body: String?,
    val raw: String?,
    @SerializedName("links_to") val linksTo: List<String>?,
    @SerializedName("linked_from") val linkedFrom: List<String>?
)

// ============================================================================
// Write / Update Requests
// ============================================================================

data class WriteNoteRequest(
    val title: String,
    val content: String,
    val category: String = "topics",
    val tags: List<String> = emptyList(),
    val importance: String = "medium",
    val source: String = "user",
    @SerializedName("links_to") val linksTo: List<String> = emptyList()
)

data class UpdateNoteRequest(
    val content: String? = null,
    val tags: List<String>? = null,
    val importance: String? = null,
    @SerializedName("links_to") val linksTo: List<String>? = null
)
