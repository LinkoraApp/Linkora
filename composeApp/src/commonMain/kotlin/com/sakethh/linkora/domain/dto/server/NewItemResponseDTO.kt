package com.sakethh.linkora.domain.dto.server

import kotlinx.serialization.Serializable

@Serializable
data class NewItemResponseDTO(
    val timeStampBasedResponse: TimeStampBasedResponse,
    val id: Long,
    val correlation: Correlation? = null,
)
