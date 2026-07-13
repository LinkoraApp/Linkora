package com.sakethh.linkora.domain.dto.server

import kotlinx.serialization.Serializable

@Serializable
data class IDBasedDTO(
    val id: Long,
    val eventTimestamp: Long,
    val correlation: Correlation? = null,
)
