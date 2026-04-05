package com.geny.app.domain.model

data class MemoryFile(
    val filename: String,
    val title: String?,
    val category: String?,
    val tags: List<String>,
    val importance: String?,
    val created: String?,
    val modified: String?,
    val charCount: Int?,
    val summary: String?,
    val linksTo: List<String> = emptyList(),
    val linkedFrom: List<String> = emptyList(),
    val source: String? = null
)

data class MemoryStats(
    val totalFiles: Int,
    val longTermEntries: Int,
    val shortTermEntries: Int,
    val lastWrite: String?,
    val categories: Map<String, Int>,
    val totalTags: Int,
    val totalChars: Int = 0,
    val totalLinks: Int = 0
)

data class MemorySearchResult(
    val filename: String?,
    val title: String?,
    val snippet: String?,
    val score: Float?,
    val category: String?,
    val content: String?,
    val matchType: String? = null,
    val importance: String? = null,
    val tags: List<String> = emptyList()
)

data class MemoryFileDetail(
    val filename: String,
    val title: String?,
    val body: String?,
    val raw: String? = null,
    val linksTo: List<String>,
    val linkedFrom: List<String>,
    val metadata: Map<String, Any?>
)

data class MemoryGraphNode(
    val id: String,
    val label: String?,
    val category: String?,
    val importance: String?,
    val linkCount: Int = 0
)

data class MemoryGraphEdge(
    val source: String,
    val target: String
)

data class MemoryGraph(
    val nodes: List<MemoryGraphNode>,
    val edges: List<MemoryGraphEdge>
)
