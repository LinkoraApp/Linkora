package com.sakethh.linkora.domain.dto.server.folder

import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.dto.server.LocalIdSerializer
import kotlinx.serialization.Serializable

@Serializable
data class UpdateFolderNameDTO(
    val folderId: Long,
    val newFolderName: String,
    val correlation: Correlation = AppPreferences.getCorrelation(),
    @Serializable(with = LocalIdSerializer::class)
    val pendingQueueSyncLocalId: Long = 0
)