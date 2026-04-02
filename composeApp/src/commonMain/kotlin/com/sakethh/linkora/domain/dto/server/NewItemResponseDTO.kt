package com.sakethh.linkora.domain.dto.server

import com.sakethh.linkora.domain.AppPreferences
import kotlinx.serialization.Serializable

@Serializable
data class NewItemResponseDTO(
    val timeStampBasedResponse: TimeStampBasedResponse,
    val id: Long,
    val correlation: Correlation
)