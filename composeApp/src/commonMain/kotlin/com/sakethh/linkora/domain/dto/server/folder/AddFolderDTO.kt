package com.sakethh.linkora.domain.dto.server.folder

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.dto.server.Correlation
import kotlinx.serialization.Serializable

@Serializable
data class AddFolderDTO(
    val name: String,
    val note: String,
    val parentFolderId: Long?,
    val isArchived: Boolean,
    val eventTimestamp: Long,
    val correlation: Correlation,
    val offlineSyncItemId: Long = 0
)