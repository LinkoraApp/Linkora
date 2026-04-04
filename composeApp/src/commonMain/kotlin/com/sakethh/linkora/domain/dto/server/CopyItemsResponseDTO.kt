package com.sakethh.linkora.domain.dto.server

import kotlinx.serialization.Serializable

@Serializable
data class CopyItemsHTTPResponseDTO(
    val folders: List<CopiedFolderResponse>,
    val linkIds: Map<Long, Long>,
    val correlation: Correlation? = null,
    val eventTimestamp: Long
)

@Serializable
data class CopiedFolderResponse(
    val currentFolder: CurrentFolder, val links: List<FolderLink>
)

@Serializable
data class CopyItemsSocketResponseDTO(
    val eventTimestamp: Long, val correlation: Correlation? = null
)