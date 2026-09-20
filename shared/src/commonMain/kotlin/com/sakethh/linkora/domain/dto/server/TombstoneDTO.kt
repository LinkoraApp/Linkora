package com.sakethh.linkora.domain.dto.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TombstoneDTO(
    val deletedAt: Long,
    val operation: String,
    val payload: JsonElement,
)
