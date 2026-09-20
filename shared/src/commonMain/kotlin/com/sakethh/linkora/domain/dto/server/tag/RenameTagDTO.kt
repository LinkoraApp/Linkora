package com.sakethh.linkora.domain.dto.server.tag

import com.sakethh.linkora.domain.dto.server.Correlation
import kotlinx.serialization.Serializable

@Serializable
data class RenameTagDTO(
    val id: Long,
    val newName: String,
    val eventTimestamp: Long,
    val correlation: Correlation? = null,
)
