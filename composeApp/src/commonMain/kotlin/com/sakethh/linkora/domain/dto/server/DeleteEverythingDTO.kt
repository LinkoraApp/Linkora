package com.sakethh.linkora.domain.dto.server

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.utils.getSystemEpochSeconds
import kotlinx.serialization.Serializable

@Serializable
data class DeleteEverythingDTO(
    val eventTimestamp: Long = getSystemEpochSeconds(), val correlation: Correlation
)