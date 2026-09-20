package com.sakethh.linkora.domain.dto.server

import kotlinx.serialization.Serializable

@Serializable
data class MarkItemsRegularDTO(
    val foldersIds: List<Long>,
    val linkIds: List<Long>,
    val eventTimestamp: Long,
    val correlation: Correlation? = null,
)
