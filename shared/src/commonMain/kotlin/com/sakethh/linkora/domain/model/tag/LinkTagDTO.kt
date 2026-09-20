package com.sakethh.linkora.domain.model.tag

import kotlinx.serialization.Serializable

@Serializable
data class LinkTagDTO(
    val linkId: Long,
    val tagId: Long,
    val eventTimestamp: Long = 0,
)
