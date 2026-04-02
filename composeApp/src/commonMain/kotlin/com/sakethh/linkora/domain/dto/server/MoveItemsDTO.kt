package com.sakethh.linkora.domain.dto.server

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.LinkType
import kotlinx.serialization.Serializable

@Serializable
data class MoveItemsDTO(
    val folderIds: List<Long>,
    val linkIds: List<Long>,
    val linkType: LinkType,
    val newParentFolderId: Long,
    val eventTimestamp: Long,
    val correlation: Correlation
)