package com.sakethh.linkora.domain.dto.server

import kotlinx.serialization.Serializable

@Serializable
data class TimeStampBasedResponse(
    val eventTimestamp: Long,
    val message: String,
)
