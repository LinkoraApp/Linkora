package com.sakethh.linkora.domain.dto.server

import kotlinx.serialization.Serializable

@Serializable
data class ProxyInfoDTO(
    val title: String?,
    val description: String?,
    val image: String?
)