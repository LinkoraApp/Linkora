package com.sakethh.linkora.domain.dto.server.panel

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.dto.server.Correlation
import kotlinx.serialization.Serializable

@Serializable
data class DeleteAFolderFromAPanelDTO(
    val panelId: Long,
    val folderID: Long,
    val eventTimestamp: Long,
    val correlation: Correlation? = null
)
