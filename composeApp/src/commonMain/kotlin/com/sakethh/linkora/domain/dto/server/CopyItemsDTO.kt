package com.sakethh.linkora.domain.dto.server

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.LinkType
import kotlinx.serialization.Serializable

@Serializable
data class CopyItemsDTO(
    val folders: List<CopyFolderDTO>,
    val linkIds: Map<Long, Long>,// `key` belongs to this client, `value` belongs to server's db
    val linkType: LinkType,
    val newParentFolderId: Long,
    val eventTimestamp: Long,
    val correlation: Correlation? = null
)