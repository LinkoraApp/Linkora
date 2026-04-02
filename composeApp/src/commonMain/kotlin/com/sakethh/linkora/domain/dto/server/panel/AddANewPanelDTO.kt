package com.sakethh.linkora.domain.dto.server.panel

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.dto.server.Correlation
import kotlinx.serialization.Serializable

@Serializable
data class AddANewPanelDTO(
    val panelName: String,
    val correlation: Correlation,
    val eventTimestamp: Long,
    val offlineSyncItemId: Long = 0
)