package com.sakethh.linkora.domain.dto.server.link

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.dto.server.Correlation
import kotlinx.serialization.Serializable

@Serializable
data class UpdateTitleOfTheLinkDTO(
    val linkId: Long,
    val newTitleOfTheLink: String,
    val eventTimestamp: Long,
    val correlation: Correlation? = null
)
