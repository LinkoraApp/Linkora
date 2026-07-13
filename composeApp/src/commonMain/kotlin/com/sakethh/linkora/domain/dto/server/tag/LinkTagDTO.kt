package com.sakethh.linkora.domain.dto.server.tag

import kotlinx.serialization.Serializable

@Serializable
data class LinkTagDTO(
    val linkId: Long,
    val tagId: Long,
)
