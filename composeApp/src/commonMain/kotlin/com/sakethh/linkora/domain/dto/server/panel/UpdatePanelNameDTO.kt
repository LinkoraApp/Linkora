package com.sakethh.linkora.domain.dto.server.panel

import com.sakethh.linkora.domain.dto.server.Correlation
import kotlinx.serialization.Serializable

@Serializable
data class UpdatePanelNameDTO(
    val newName: String, val panelId: Long,
    val eventTimestamp: Long,
    val correlation: Correlation
)
