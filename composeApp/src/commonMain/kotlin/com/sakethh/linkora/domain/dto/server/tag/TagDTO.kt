package com.sakethh.linkora.domain.dto.server.tag

import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.AppPreferences
import kotlinx.serialization.Serializable

@Serializable
data class TagDTO(
    val id: Long,
    val name: String,
    val eventTimestamp: Long,
    val correlation: Correlation,
)