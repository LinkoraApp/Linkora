package com.sakethh.linkora.domain.dto.server.link

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.dto.server.Correlation
import kotlinx.serialization.Serializable

@Serializable
data class DeleteDuplicateLinksDTO(
    val linkIds: List<Long>,
    val eventTimestamp: Long,
    val correlation: Correlation
)
