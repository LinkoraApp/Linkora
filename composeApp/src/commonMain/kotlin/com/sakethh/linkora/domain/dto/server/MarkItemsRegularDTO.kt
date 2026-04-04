package com.sakethh.linkora.domain.dto.server

import com.sakethh.linkora.domain.AppPreferences
import kotlinx.serialization.Serializable

@Serializable
data class MarkItemsRegularDTO(
    val foldersIds: List<Long>,
    val linkIds: List<Long>,
    val eventTimestamp: Long,
    val correlation: Correlation? = null
)