package com.sakethh.linkora.domain.dto.server

import com.sakethh.linkora.domain.AppPreferences
import kotlinx.serialization.Serializable

@Serializable
data class IDBasedDTO(
    val id: Long,
    val eventTimestamp: Long,
    val correlation: Correlation? = null
)
